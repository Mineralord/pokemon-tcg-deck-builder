package com.mineralord.tcg.data.netfirestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Identidad ESTABLE del jugador en Firebase. El login anónimo da un uid nuevo en cada reinstalación
 * (o borrado de datos); como el ID de amigo se restaura de la nube, el mismo jugador acababa con
 * VARIOS documentos `users/{uid}` (y regalos/amigos repartidos entre ellos). Vinculando la sesión a
 * la cuenta de Google, el uid queda fijo para siempre en cualquier dispositivo.
 *
 * - Primera vez: [linkGoogle] convierte el usuario anónimo actual en usuario Google (mismo uid).
 * - Si esa cuenta Google ya tenía uid (otro dispositivo / instalación previa): se migra lo del uid
 *   anónimo actual (amigos y solicitudes enviadas) al uid Google y se borra el documento anónimo.
 */
class FirebaseIdentity private constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    constructor() : this(Firebase.firestore, Firebase.auth)

    /** true si la sesión de Firebase ya está ligada a una cuenta de Google. */
    fun isGoogleLinked(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

    /** Vincula la sesión a Google con el [idToken] de Credential Manager. Devuelve el uid final. */
    suspend fun linkGoogle(idToken: String): String {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.currentUser ?: return auth.signInWithCredential(cred).await().user!!.uid
        if (isGoogleLinked()) return user.uid
        return try {
            user.linkWithCredential(cred).await().user!!.uid
        } catch (e: FirebaseAuthUserCollisionException) {
            // La cuenta Google ya tiene su propio uid: llévate lo del anónimo y cámbiate a él.
            val carried = exportAndDetach(user.uid)
            val newUid = auth.signInWithCredential(e.updatedCredential ?: cred).await().user!!.uid
            importInto(newUid, carried)
            newUid
        }
    }

    private data class Carried(
        val friends: Map<String, Map<String, Any?>>,
        val outgoing: Map<String, Map<String, Any?>>,
    )

    /**
     * Como uid ANÓNIMO (las reglas solo dejan tocar estos enlaces a su dueño): lee tus amigos y
     * solicitudes enviadas, quita tus enlaces recíprocos en las listas de los demás y borra tu
     * documento de directorio. Las solicitudes RECIBIDAS se descartan (el emisor puede reenviarlas).
     */
    private suspend fun exportAndDetach(old: String): Carried {
        val me = users().document(old)
        val friends = me.collection(FRIENDS).get().await().documents.associate { it.id to it.data.orEmpty() }
        val outgoing = me.collection(OUTGOING).get().await().documents.associate { it.id to it.data.orEmpty() }
        val incoming = me.collection(INCOMING).get().await().documents.map { it.id }
        val batch = db.batch()
        friends.keys.forEach { f ->
            batch.delete(users().document(f).collection(FRIENDS).document(old))
            batch.delete(me.collection(FRIENDS).document(f))
        }
        outgoing.keys.forEach { t ->
            batch.delete(users().document(t).collection(INCOMING).document(old))
            batch.delete(me.collection(OUTGOING).document(t))
        }
        incoming.forEach { f ->
            batch.delete(users().document(f).collection(OUTGOING).document(old))
            batch.delete(me.collection(INCOMING).document(f))
        }
        batch.delete(me)
        runCatching { batch.commit().await() }
        return Carried(friends, outgoing)
    }

    /** Como uid GOOGLE: recrea amistades (ambos lados) y solicitudes enviadas. */
    private suspend fun importInto(newUid: String, carried: Carried) {
        if (carried.friends.isEmpty() && carried.outgoing.isEmpty()) return
        val meDoc = users().document(newUid).get().await()
        val myName = meDoc.getString("username").orEmpty()
        val myCode = meDoc.getString("friendCode").orEmpty()
        val batch = db.batch()
        carried.friends.forEach { (f, data) ->
            if (f == newUid) return@forEach
            batch.set(users().document(newUid).collection(FRIENDS).document(f), data)
            batch.set(
                users().document(f).collection(FRIENDS).document(newUid),
                mapOf("username" to myName, "friendCode" to myCode, "since" to FieldValue.serverTimestamp()),
            )
        }
        carried.outgoing.forEach { (t, data) ->
            if (t == newUid) return@forEach
            batch.set(users().document(newUid).collection(OUTGOING).document(t), data)
            batch.set(
                users().document(t).collection(INCOMING).document(newUid),
                mapOf("username" to myName, "friendCode" to myCode, "ts" to FieldValue.serverTimestamp()),
            )
        }
        runCatching { batch.commit().await() }
    }

    private fun users() = db.collection(FirestoreFriendsRepository.USERS)

    private companion object {
        const val FRIENDS = FirestoreFriendsRepository.FRIENDS
        const val INCOMING = FirestoreFriendsRepository.INCOMING
        const val OUTGOING = FirestoreFriendsRepository.OUTGOING
    }
}
