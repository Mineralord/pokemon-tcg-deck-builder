package com.mineralord.tcg.data.netfirestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mineralord.tcg.data.netplay.MatchRole
import com.mineralord.tcg.data.netplay.MatchTransport
import com.mineralord.tcg.data.netplay.MatchTransportFactory
import com.mineralord.tcg.data.netplay.NetMessage
import com.mineralord.tcg.data.netplay.TransportState
import com.mineralord.tcg.data.netplay.decodeNetMessage
import com.mineralord.tcg.data.netplay.encode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Transporte host-autoritativo sobre Firestore. Un "match" es un documento
 * `matches/{code}` con dos subcolecciones-cola de mensajes:
 *   - `h2g` (host → guest) y `g2h` (guest → host).
 * Cada lado ESCRIBE en su cola saliente y ESCUCHA su cola entrante (ordenada por
 * `seq`), emitiendo en orden los documentos nuevos ([DocumentChange.Type.ADDED]).
 * Solo hay un escritor por cola, así que un contador local basta para el orden.
 */
class FirestoreMatchTransport internal constructor(
    override val role: MatchRole,
    private val matchDoc: DocumentReference,
    private val outgoingCol: CollectionReference,
    private val incomingCol: CollectionReference,
) : MatchTransport {

    private val _state = MutableStateFlow(TransportState.CONNECTED)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val seq = AtomicInteger(0)

    override val incoming: Flow<NetMessage> = callbackFlow {
        val registration = incomingCol
            .orderBy("seq", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.value = TransportState.ERROR
                    close(error)
                    return@addSnapshotListener
                }
                val snap = snapshot ?: return@addSnapshotListener
                for (change in snap.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val payload = change.document.getString("payload") ?: continue
                    runCatching { decodeNetMessage(payload) }.getOrNull()?.let { trySend(it) }
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun send(message: NetMessage) {
        outgoingCol.add(
            mapOf(
                "seq" to seq.getAndIncrement(),
                "payload" to message.encode(),
                "at" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun close() {
        _state.value = TransportState.CLOSED
        // Mejor esfuerzo: el host retira el documento del match (las subcolecciones
        // quedan huérfanas pero son diminutas; aceptable para el MVP).
        if (role == MatchRole.HOST) runCatching { matchDoc.delete().await() }
    }
}

/**
 * Fábrica de transportes Firestore. Firma una sesión ANÓNIMA (invisible) antes de
 * hostear/unirse, para satisfacer las reglas `request.auth != null` de la colección
 * de partidas (MVP familiar; partidas efímeras). No hay pantalla de login.
 */
class FirestoreMatchTransportFactory private constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : MatchTransportFactory {

    // Constructor público SIN tipos de Firebase en la firma: así `:app` puede
    // instanciarlo sin necesitar el SDK de Firebase en su classpath (queda
    // encapsulado en este módulo).
    constructor() : this(Firebase.firestore, Firebase.auth)

    /** Asegura una sesión (anónima) para que las reglas `request.auth != null` pasen. */
    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    override suspend fun host(playerName: String): Pair<String, MatchTransport> {
        ensureSignedIn()
        val code = randomCode()
        val matchDoc = db.collection(MATCHES).document(code)
        matchDoc.set(
            mapOf(
                "status" to "waiting",
                "hostName" to playerName,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        val transport = FirestoreMatchTransport(
            role = MatchRole.HOST,
            matchDoc = matchDoc,
            outgoingCol = matchDoc.collection(H2G),
            incomingCol = matchDoc.collection(G2H),
        )
        return code to transport
    }

    override suspend fun join(code: String, playerName: String): MatchTransport {
        ensureSignedIn()
        val normalized = code.trim().uppercase()
        val matchDoc = db.collection(MATCHES).document(normalized)
        val snap = matchDoc.get().await()
        if (!snap.exists()) throw IOException("No existe una partida con el código $normalized")
        matchDoc.update(mapOf("status" to "connected", "guestName" to playerName)).await()
        return FirestoreMatchTransport(
            role = MatchRole.GUEST,
            matchDoc = matchDoc,
            outgoingCol = matchDoc.collection(G2H),
            incomingCol = matchDoc.collection(H2G),
        )
    }

    /** Código corto legible (5 chars, sin caracteres ambiguos: sin O/0/I/1). */
    private fun randomCode(): String = (1..CODE_LEN)
        .map { CODE_ALPHABET.random() }
        .joinToString("")

    private companion object {
        const val MATCHES = "matches"
        const val H2G = "h2g"
        const val G2H = "g2h"
        const val CODE_LEN = 5
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
