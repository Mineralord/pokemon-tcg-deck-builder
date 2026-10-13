package com.mineralord.tcg.data.netfirestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Un amigo confirmado (bidireccional). [uid] = id de Auth del amigo. [lastSeenMillis] = última
 * conexión (epoch ms, 0 si no compartida); [collectionCount] = total de cartas en su colección.
 */
data class FriendEntry(
    val uid: String,
    val username: String,
    val friendCode: String,
    val lastSeenMillis: Long = 0L,
    val collectionCount: Int = 0,
)

/** Una solicitud de amistad (entrante o saliente). [uid] = id de Auth del otro jugador. */
data class FriendRequest(val uid: String, val username: String, val friendCode: String)

/**
 * Una invitación a combate recibida de un amigo. [fromUid] = quién invita; [roomCode] = código de
 * la sala/partida a la que unirse al aceptar; [ts] = marca temporal (ms) para detectar las nuevas.
 */
data class MatchInvite(val fromUid: String, val fromName: String, val roomCode: String, val ts: Long)

/**
 * Backend REAL de amigos sobre Firestore (multidispositivo), reutilizando el mismo patrón que
 * [FirestoreMatchTransport]: login anónimo + `Firebase.firestore` + `await()` + listeners en vivo.
 *
 * Modelo de datos:
 *   users/{uid}                      → { friendCode, username, updatedAt }   (directorio por código)
 *   users/{uid}/friends/{otherUid}   → { username, friendCode, since }
 *   users/{uid}/incomingRequests/{fromUid} → { username, friendCode, ts }
 *   users/{uid}/outgoingRequests/{toUid}   → { username, friendCode, ts }
 *
 * Las escrituras cruzadas (crear la solicitud en el buzón del destino, hacerse amigo mutuo) están
 * permitidas por las reglas solo cuando el id del documento coincide con tu propio uid.
 */
class FirestoreFriendsRepository private constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    // Constructor público SIN exponer tipos de Firebase (que son `implementation` en este módulo y no
    // viajan al classpath de `app`). El resto de la API pública solo usa tipos de dominio.
    constructor() : this(Firebase.firestore, Firebase.auth)

    private suspend fun uid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid

    private fun users() = db.collection(USERS)

    /**
     * Publica (o actualiza) tu registro en el directorio para que otros te encuentren por tu ID.
     * Cada llamada refresca `lastSeen` (última conexión, visible para tus amigos) y `collectionCount`
     * (total de cartas en tu colección, visible en tu perfil para tus amigos).
     */
    suspend fun publishSelf(friendCode: String, username: String, collectionCount: Int) {
        val me = uid()
        users().document(me).set(
            mapOf(
                "friendCode" to friendCode,
                "username" to username,
                "collectionCount" to collectionCount,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    /**
     * Lista de amigos con el nombre y código SIEMPRE EN VIVO. La copia guardada en la subcolección
     * `friends` queda congelada al hacerse amigos, así que si un amigo se cambia el nombre después
     * no se reflejaría. Para evitarlo, escuchamos la subcolección `friends` (membresía) y, por cada
     * amigo, también su doc de directorio `users/{amigoUid}` (nombre/código autoritativos y vivos);
     * el nombre mostrado se toma del directorio, cayendo a la copia local solo si aún no ha llegado.
     */
    fun friends(): Flow<List<FriendEntry>> = callbackFlow {
        val me = uid()
        val lock = Any()
        // Copia local (de la subcolección `friends`) por amigo: respaldo si el directorio aún no carga.
        val stored = LinkedHashMap<String, FriendEntry>()
        // Valores vivos del directorio `users/{amigoUid}`.
        val liveName = HashMap<String, String>()
        val liveCode = HashMap<String, String>()
        val liveLastSeen = HashMap<String, Long>()
        val liveCount = HashMap<String, Int>()
        val userRegs = HashMap<String, ListenerRegistration>()

        fun emit() {
            val list = stored.map { (fuid, s) ->
                FriendEntry(
                    uid = fuid,
                    username = liveName[fuid]?.takeIf { it.isNotEmpty() } ?: s.username,
                    friendCode = liveCode[fuid]?.takeIf { it.isNotEmpty() } ?: s.friendCode,
                    lastSeenMillis = liveLastSeen[fuid] ?: 0L,
                    collectionCount = liveCount[fuid] ?: 0,
                )
            }
            trySend(list)
        }

        val reg = users().document(me).collection(FRIENDS).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            synchronized(lock) {
                stored.clear()
                val current = HashSet<String>()
                for (d in snap.documents) {
                    val fuid = d.id
                    current.add(fuid)
                    stored[fuid] = FriendEntry(
                        fuid, d.getString("username").orEmpty(), d.getString("friendCode").orEmpty(),
                    )
                    // Suscríbete al directorio de este amigo una sola vez (nombre/código en vivo).
                    if (!userRegs.containsKey(fuid)) {
                        userRegs[fuid] = users().document(fuid).addSnapshotListener { u, _ ->
                            if (u == null) return@addSnapshotListener
                            synchronized(lock) {
                                u.getString("username")?.let { liveName[fuid] = it }
                                u.getString("friendCode")?.let { liveCode[fuid] = it }
                                u.getTimestamp("lastSeen")?.let { liveLastSeen[fuid] = it.toDate().time }
                                u.getLong("collectionCount")?.let { liveCount[fuid] = it.toInt() }
                                emit()
                            }
                        }
                    }
                }
                // Deja de escuchar a quienes ya no son amigos.
                (userRegs.keys - current).toList().forEach { gone ->
                    userRegs.remove(gone)?.remove()
                    liveName.remove(gone); liveCode.remove(gone)
                    liveLastSeen.remove(gone); liveCount.remove(gone)
                }
                emit()
            }
        }
        awaitClose {
            reg.remove()
            synchronized(lock) { userRegs.values.forEach { it.remove() }; userRegs.clear() }
        }
    }

    fun incoming(): Flow<List<FriendRequest>> = subFlow(INCOMING) {
        FriendRequest(it.id, it.getString("username").orEmpty(), it.getString("friendCode").orEmpty())
    }

    fun outgoing(): Flow<List<FriendRequest>> = subFlow(OUTGOING) {
        FriendRequest(it.id, it.getString("username").orEmpty(), it.getString("friendCode").orEmpty())
    }

    /** Escucha en vivo una subcolección propia y mapea sus documentos. */
    private fun <T> subFlow(name: String, map: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val me = uid()
        val reg = users().document(me).collection(name).addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map(map))
        }
        awaitClose { reg.remove() }
    }

    /**
     * Envía una solicitud de amistad al jugador con [rawCode] (12 dígitos). Valida formato, que no
     * seas tú, que exista y que no sea ya tu amigo. Escribe la solicitud en el buzón del destino y en
     * tus salientes (atómico).
     */
    suspend fun sendRequest(rawCode: String): Result<Unit> {
        val code = rawCode.filter { it.isDigit() }
        if (code.length != 12) return Result.failure(FriendError("El ID debe tener 12 dígitos"))
        val me = uid()
        val meDoc = users().document(me).get().await()
        val myCode = meDoc.getString("friendCode").orEmpty()
        val myName = meDoc.getString("username").orEmpty()
        if (code == myCode) return Result.failure(FriendError("No puedes agregarte a ti mismo"))
        val target = users().whereEqualTo("friendCode", code).limit(1).get().await()
            .documents.firstOrNull() ?: return Result.failure(FriendError("No se encontró ese ID de amigo"))
        val targetUid = target.id
        if (users().document(me).collection(FRIENDS).document(targetUid).get().await().exists()) {
            return Result.failure(FriendError("Ya es tu amigo"))
        }
        val targetName = target.getString("username").orEmpty()
        val batch = db.batch()
        batch.set(
            users().document(targetUid).collection(INCOMING).document(me),
            mapOf("username" to myName, "friendCode" to myCode, "ts" to FieldValue.serverTimestamp()),
        )
        batch.set(
            users().document(me).collection(OUTGOING).document(targetUid),
            mapOf("username" to targetName, "friendCode" to code, "ts" to FieldValue.serverTimestamp()),
        )
        return runCatching { batch.commit().await() }.map { }
    }

    /** Acepta la solicitud de [fromUid]: os hacéis amigos mutuos y se limpian las solicitudes. */
    suspend fun accept(fromUid: String) {
        val me = uid()
        val meDoc = users().document(me).get().await()
        val fromDoc = users().document(fromUid).get().await()
        val batch = db.batch()
        batch.set(
            users().document(me).collection(FRIENDS).document(fromUid),
            mapOf(
                "username" to fromDoc.getString("username").orEmpty(),
                "friendCode" to fromDoc.getString("friendCode").orEmpty(),
                "since" to FieldValue.serverTimestamp(),
            ),
        )
        batch.set(
            users().document(fromUid).collection(FRIENDS).document(me),
            mapOf(
                "username" to meDoc.getString("username").orEmpty(),
                "friendCode" to meDoc.getString("friendCode").orEmpty(),
                "since" to FieldValue.serverTimestamp(),
            ),
        )
        batch.delete(users().document(me).collection(INCOMING).document(fromUid))
        batch.delete(users().document(fromUid).collection(OUTGOING).document(me))
        batch.commit().await()
    }

    /** Rechaza la solicitud de [fromUid] (la borra de tu buzón y de sus salientes). */
    suspend fun reject(fromUid: String) {
        val me = uid()
        val batch = db.batch()
        batch.delete(users().document(me).collection(INCOMING).document(fromUid))
        batch.delete(users().document(fromUid).collection(OUTGOING).document(me))
        batch.commit().await()
    }

    /** Cancela tu solicitud enviada a [toUid] (la borra de tus salientes y de su buzón). */
    suspend fun cancelOutgoing(toUid: String) {
        val me = uid()
        val batch = db.batch()
        batch.delete(users().document(me).collection(OUTGOING).document(toUid))
        batch.delete(users().document(toUid).collection(INCOMING).document(me))
        batch.commit().await()
    }

    // ───────── Invitaciones a combate ─────────

    /**
     * Invita a [targetUid] a tu sala [roomCode]: escribe un doc en su buzón `matchInvites`. Su
     * dispositivo lo recibe en vivo (banner + notificación) y, al aceptar, se une a esa sala.
     */
    suspend fun inviteToMatch(targetUid: String, roomCode: String) {
        val me = uid()
        val meDoc = users().document(me).get().await()
        users().document(targetUid).collection(MATCH_INVITES).document(me).set(
            mapOf(
                "fromName" to meDoc.getString("username").orEmpty(),
                "roomCode" to roomCode,
                "ts" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Escucha en vivo tus invitaciones a combate entrantes. */
    fun matchInvites(): Flow<List<MatchInvite>> = subFlow(MATCH_INVITES) {
        MatchInvite(
            fromUid = it.id,
            fromName = it.getString("fromName").orEmpty(),
            roomCode = it.getString("roomCode").orEmpty(),
            ts = it.getTimestamp("ts")?.toDate()?.time ?: 0L,
        )
    }

    /** Descarta la invitación de [fromUid] (al aceptar, rechazar o tras entrar a la sala). */
    suspend fun clearMatchInvite(fromUid: String) {
        users().document(uid()).collection(MATCH_INVITES).document(fromUid).delete().await()
    }

    /** Elimina la amistad con [otherUid] en ambos lados. */
    suspend fun removeFriend(otherUid: String) {
        val me = uid()
        val batch = db.batch()
        batch.delete(users().document(me).collection(FRIENDS).document(otherUid))
        batch.delete(users().document(otherUid).collection(FRIENDS).document(me))
        batch.commit().await()
    }

    private companion object {
        const val USERS = "users"
        const val FRIENDS = "friends"
        const val INCOMING = "incomingRequests"
        const val OUTGOING = "outgoingRequests"
        const val MATCH_INVITES = "matchInvites"
    }
}

/** Error de dominio con mensaje presentable al usuario. */
class FriendError(message: String) : Exception(message)
