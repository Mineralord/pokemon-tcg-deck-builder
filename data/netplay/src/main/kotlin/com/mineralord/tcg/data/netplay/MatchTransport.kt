package com.mineralord.tcg.data.netplay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Rol en la partida en red. */
enum class MatchRole { HOST, GUEST }

/** Estado del canal de transporte. */
enum class TransportState { IDLE, ADVERTISING, DISCOVERING, CONNECTING, CONNECTED, CLOSED, ERROR }

/**
 * Canal de transporte abstracto entre los dos dispositivos. Permite tener dos
 * backends intercambiables bajo la misma capa de juego (host-autoritativo):
 *   - Firestore (remoto, por Internet) — ver :feature/data en Fase 3.
 *   - Nearby Connections (WiFi/Bluetooth local) — Fase 3.
 *
 * La capa de juego solo conoce esta interfaz: envía/recibe [NetMessage].
 */
interface MatchTransport {
    /** Rol local en esta partida. */
    val role: MatchRole

    /** Estado de conexión observable. */
    val state: StateFlow<TransportState>

    /** Mensajes entrantes del rival. */
    val incoming: Flow<NetMessage>

    /** Envía un mensaje al rival. */
    suspend fun send(message: NetMessage)

    /** Cierra la conexión y libera recursos. */
    suspend fun close()
}

/**
 * Fábrica de transportes. Las implementaciones concretas (Firestore/Nearby) se
 * registrarán en Fase 3 desde el módulo Android correspondiente.
 */
interface MatchTransportFactory {
    /** Crea/host-ea una partida y devuelve un código corto para que el rival se una. */
    suspend fun host(playerName: String): Pair<String, MatchTransport>

    /** Se une a una partida existente por [code]. */
    suspend fun join(code: String, playerName: String): MatchTransport
}

/**
 * Puente para que capas sin acceso directo al módulo `:app` (p. ej. un ViewModel
 * en `:feature:game`) obtengan la fábrica de transporte. La `Application` la
 * implementa y el ViewModel hace `(app as MatchFactoryProvider).matchFactory`.
 */
interface MatchFactoryProvider {
    val matchFactory: MatchTransportFactory
}

/**
 * Idempotencia de entrega para transportes con historial re-entregable: al
 * re-suscribirse tras un corte, un backend durable (p. ej. Firestore) reentrega toda
 * la cola como documentos nuevos. Devuelve true solo si [seq] supera la última
 * secuencia ya entregada ([lastSeen]), evitando reprocesar la ceremonia
 * (Hello/Deal/Coin). Pura → testeable sin backend.
 */
fun shouldForwardSeq(seq: Int, lastSeen: Int): Boolean = seq > lastSeen
