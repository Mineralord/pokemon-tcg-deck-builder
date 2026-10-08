package com.mineralord.tcg.data.netfirestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Un amigo confirmado (bidireccional). [uid] = id de Auth del amigo. */
data class FriendEntry(val uid: String, val username: String, val friendCode: String)

/** Una solicitud de amistad (entrante o saliente). [uid] = id de Auth del otro jugador. */
data class FriendRequest(val uid: String, val username: String, val friendCode: String)

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

    /** Publica (o actualiza) tu registro en el directorio para que otros te encuentren por tu ID. */
    suspend fun publishSelf(friendCode: String, username: String) {
        val me = uid()
        users().document(me).set(
            mapOf(
                "friendCode" to friendCode,
                "username" to username,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    fun friends(): Flow<List<FriendEntry>> = subFlow(FRIENDS) {
        FriendEntry(it.id, it.getString("username").orEmpty(), it.getString("friendCode").orEmpty())
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
    }
}

/** Error de dominio con mensaje presentable al usuario. */
class FriendError(message: String) : Exception(message)
