package com.mineralord.tcg.data.netfirestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Un miembro de la sala (anfitrión o invitado), con su estado "Listo" y la baraja elegida. */
data class RoomMember(
    val uid: String,
    val name: String,
    val ready: Boolean,
    val deckName: String,
    val isHost: Boolean,
)

/**
 * Estado COMPARTIDO de la sala multijugador sobre Firestore, para que anfitrión e invitado se vean
 * mutuamente en el lobby (quién está, su baraja y si está "Listo") antes de empezar.
 *
 * Reutiliza el documento del match: `matches/{code}` (fields de lobby) + subcolección `members/{uid}`.
 * Así el mismo código de sala sirve de lobby y de partida, y encaja con las reglas ya desplegadas
 * (la colección matches es accesible para autenticados). El arranque se señaliza con lobbyStatus = "started".
 */
class FirestoreRoomRepository private constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    constructor() : this(Firebase.firestore, Firebase.auth)

    suspend fun myUid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid

    private fun room(code: String) = db.collection(MATCHES).document(code.trim().uppercase())
    private fun members(code: String) = room(code).collection(MEMBERS)

    /** El anfitrión crea la sala y se añade como primer miembro. */
    suspend fun enterAsHost(code: String, name: String, deckName: String) {
        val me = myUid()
        room(code).set(
            mapOf("host" to me, "lobbyStatus" to "waiting", "createdAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
        members(code).document(me).set(
            memberDoc(name, false, deckName, isHost = true),
        ).await()
    }

    /** El invitado se une a una sala existente como miembro. */
    suspend fun enterAsGuest(code: String, name: String, deckName: String) {
        val me = myUid()
        members(code).document(me).set(
            memberDoc(name, false, deckName, isHost = false),
        ).await()
    }

    private fun memberDoc(name: String, ready: Boolean, deckName: String, isHost: Boolean) = mapOf(
        "name" to name,
        "ready" to ready,
        "deckName" to deckName,
        "isHost" to isHost,
        "joinedAt" to FieldValue.serverTimestamp(),
    )

    /** Miembros de la sala en vivo (ambos jugadores con su estado). */
    fun membersFlow(code: String): Flow<List<RoomMember>> = callbackFlow {
        val reg = members(code).addSnapshotListener { snap, _ ->
            if (snap != null) trySend(
                snap.documents.map {
                    RoomMember(
                        uid = it.id,
                        name = it.getString("name").orEmpty(),
                        ready = it.getBoolean("ready") ?: false,
                        deckName = it.getString("deckName").orEmpty(),
                        isHost = it.getBoolean("isHost") ?: false,
                    )
                },
            )
        }
        awaitClose { reg.remove() }
    }

    /** Estado del lobby en vivo ("waiting" / "started" / ""). */
    fun lobbyStatusFlow(code: String): Flow<String> = callbackFlow {
        val reg = room(code).addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.getString("lobbyStatus").orEmpty())
        }
        awaitClose { reg.remove() }
    }

    suspend fun setReady(code: String, ready: Boolean) {
        members(code).document(myUid()).set(mapOf("ready" to ready), SetOptions.merge()).await()
    }

    suspend fun setDeck(code: String, deckName: String) {
        members(code).document(myUid()).set(mapOf("deckName" to deckName), SetOptions.merge()).await()
    }

    /** Señaliza el arranque: ambos dispositivos lo observan y caen al tapete. */
    suspend fun start(code: String) {
        room(code).set(mapOf("lobbyStatus" to "started"), SetOptions.merge()).await()
    }

    /**
     * Abandona la sala: borra tu miembro y, si eras el ÚLTIMO, borra también el documento de la sala
     * (y los restos de miembros) para no dejar salas huérfanas. Mejor esfuerzo.
     */
    suspend fun leave(code: String) {
        if (code.isBlank()) return
        runCatching {
            members(code).document(myUid()).delete().await()
            val remaining = members(code).get().await()
            if (remaining.isEmpty) {
                // Barre cualquier resto (p. ej. miembros que se fueron sin limpiar) y borra la sala.
                remaining.documents.forEach { runCatching { it.reference.delete().await() } }
                room(code).delete().await()
            }
        }
    }

    /**
     * Barre salas huérfanas SIN servidor (gratis, plan Spark): como las reglas permiten a cualquier
     * autenticado leer/escribir `matches`, cada cliente borra al arrancar las salas más antiguas que
     * [maxAgeMinutes] junto con sus subcolecciones. Así, si alguien mató la app dejando restos, el
     * siguiente que abra la app los limpia. Mejor esfuerzo: los fallos puntuales se ignoran.
     */
    suspend fun sweepStaleRooms(maxAgeMinutes: Long = 180) {
        runCatching {
            myUid() // asegura sesión anónima (las reglas exigen auth)
            val cutoff = com.google.firebase.Timestamp(
                java.util.Date(System.currentTimeMillis() - maxAgeMinutes * 60_000L),
            )
            val stale = db.collection(MATCHES).whereLessThan("createdAt", cutoff).get().await()
            for (doc in stale.documents) {
                SUBCOLLECTIONS.forEach { sub ->
                    runCatching {
                        doc.reference.collection(sub).get().await()
                            .documents.forEach { runCatching { it.reference.delete().await() } }
                    }
                }
                runCatching { doc.reference.delete().await() }
            }
        }
    }

    private companion object {
        const val MATCHES = "matches"
        const val MEMBERS = "members"
        val SUBCOLLECTIONS = listOf("members", "h2g", "g2h")
    }
}
