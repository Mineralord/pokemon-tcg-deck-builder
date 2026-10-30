package com.mineralord.tcg.data.netfirestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mineralord.tcg.data.netplay.MatchRole
import com.mineralord.tcg.data.netplay.MatchTransport
import com.mineralord.tcg.data.netplay.MatchTransportFactory
import com.mineralord.tcg.data.netplay.NetMessage
import com.mineralord.tcg.data.netplay.TransportState
import com.mineralord.tcg.data.netplay.decodeNetMessage
import com.mineralord.tcg.data.netplay.encode
import com.mineralord.tcg.data.netplay.shouldForwardSeq
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Transporte host-autoritativo sobre Firestore. Un "match" es un documento
 * `matches/{code}`; cada PARTIDA abre una sesión nueva `matches/{code}/sessions/{session}` (el id va
 * en el campo `session` del match) con dos subcolecciones-cola de mensajes:
 *   - `h2g` (host → guest) y `g2h` (guest → host).
 * La sesión por partida es imprescindible en las salas de amigos, que REUTILIZAN el código: sin ella,
 * los mensajes de la partida anterior (p. ej. el `Hello` con la baraja de quien fue invitado entonces)
 * se leían en la nueva y el rival jugaba con la baraja equivocada.
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

    /** Última `seq` ya ENTREGADA aguas arriba. Evita reprocesar el historial que
     *  Firestore reentrega como ADDED al re-suscribirse tras un corte. */
    private val lastSeen = AtomicInteger(-1)

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
                    val docSeq = change.document.getLong("seq")?.toInt() ?: continue
                    // Dedup idempotente: al re-suscribirse tras un corte, Firestore
                    // reentrega toda la cola; solo pasamos lo que aún no entregamos.
                    if (!shouldForwardSeq(docSeq, lastSeen.get())) continue
                    val payload = change.document.getString("payload") ?: continue
                    val msg = runCatching { decodeNetMessage(payload) }.getOrNull() ?: continue
                    if (trySend(msg).isSuccess) lastSeen.set(docSeq)
                }
            }
        awaitClose { registration.remove() }
    }.buffer(Channel.UNLIMITED)

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
        val session = newSession()
        matchDoc.set(
            mapOf(
                "status" to "waiting",
                "session" to session,
                "hostName" to playerName,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return code to hostTransport(matchDoc, session)
    }

    override suspend fun host(code: String, playerName: String): MatchTransport {
        ensureSignedIn()
        val normalized = code.trim().uppercase()
        val matchDoc = db.collection(MATCHES).document(normalized)
        // MERGE: conserva el estado del lobby (miembros/lobbyStatus) creado antes en la sala de amigos.
        // Sesión NUEVA en cada partida: la sala reutiliza el código y no debe ver mensajes viejos.
        val session = newSession()
        matchDoc.set(
            mapOf(
                "status" to "waiting",
                "session" to session,
                "hostName" to playerName,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
        return hostTransport(matchDoc, session)
    }

    override suspend fun join(code: String, playerName: String): MatchTransport {
        ensureSignedIn()
        val normalized = code.trim().uppercase()
        val matchDoc = db.collection(MATCHES).document(normalized)
        // Espera a que el anfitrión abra la sesión de ESTA partida (status "waiting"). En la sala de
        // amigos ambos arrancan a la vez, y el documento puede conservar la sesión de la partida
        // anterior (ya "connected"): unirse a esa dejaría a cada uno hablando en una cola distinta.
        val session = withTimeoutOrNull(JOIN_TIMEOUT_MS) { awaitOpenSession(matchDoc) }
            ?: throw IOException("No existe una partida con el código $normalized")
        matchDoc.update(mapOf("status" to "connected", "guestName" to playerName)).await()
        val sessionDoc = matchDoc.collection(SESSIONS).document(session)
        return FirestoreMatchTransport(
            role = MatchRole.GUEST,
            matchDoc = matchDoc,
            outgoingCol = sessionDoc.collection(G2H),
            incomingCol = sessionDoc.collection(H2G),
        )
    }

    private fun hostTransport(matchDoc: DocumentReference, session: String): FirestoreMatchTransport {
        val sessionDoc = matchDoc.collection(SESSIONS).document(session)
        return FirestoreMatchTransport(
            role = MatchRole.HOST,
            matchDoc = matchDoc,
            outgoingCol = sessionDoc.collection(H2G),
            incomingCol = sessionDoc.collection(G2H),
        )
    }

    private fun newSession(): String = java.util.UUID.randomUUID().toString()

    /** Suspende hasta que el match tenga una sesión abierta (status "waiting") y devuelve su id. */
    private suspend fun awaitOpenSession(matchDoc: DocumentReference): String =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            var registration: com.google.firebase.firestore.ListenerRegistration? = null
            registration = matchDoc.addSnapshotListener { snap, error ->
                if (error != null) {
                    registration?.remove()
                    if (cont.isActive) cont.resumeWith(Result.failure(error))
                    return@addSnapshotListener
                }
                val session = snap?.getString("session")
                if (snap?.getString("status") == "waiting" && session != null && cont.isActive) {
                    registration?.remove()
                    cont.resumeWith(Result.success(session))
                }
            }
            cont.invokeOnCancellation { registration?.remove() }
        }

    /** Código corto legible (5 chars, sin caracteres ambiguos: sin O/0/I/1). */
    private fun randomCode(): String = (1..CODE_LEN)
        .map { CODE_ALPHABET.random() }
        .joinToString("")

    private companion object {
        const val MATCHES = "matches"
        const val H2G = "h2g"
        const val G2H = "g2h"
        const val SESSIONS = "sessions"
        /** Cuánto espera el invitado a que el anfitrión abra la partida. */
        const val JOIN_TIMEOUT_MS = 30_000L
        const val CODE_LEN = 5
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
