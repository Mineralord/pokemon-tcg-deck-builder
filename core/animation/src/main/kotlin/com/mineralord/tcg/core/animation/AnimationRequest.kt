package com.mineralord.tcg.core.animation

/**
 * Contrato base para **toda** solicitud de animación del sistema visual.
 *
 * ¿Por qué `Request` y no `Event`?
 * - En `engine:events` ya existe `EngineEvent`, que modela hechos de DOMINIO. Un
 *   [AnimationRequest] no es un hecho del juego: es una **solicitud** dirigida al
 *   subsistema visual ("reproduce esto"). Nombrarlo `Request` evita la colisión
 *   conceptual con `EngineEvent` y deja explícito el sentido del flujo:
 *   `EngineEvent` (dominio) → [EngineEventAdapter] → [AnimationRequest] (presentación).
 * - El motor NUNCA conoce este tipo. La traducción vive fuera de `core:animation`.
 *
 * Responsabilidad:
 * - Ser el lenguaje común del subsistema de animaciones, como datos inmutables.
 *   El [AnimationDirector] decide *cómo* y *cuándo* servir cada solicitud.
 *
 * Principios SOLID:
 * - Open-Closed / Interface Segregation: `sealed interface`; añadir una animación =
 *   añadir una subclase, sin tocar director, cola ni UI.
 * - Liskov: cualquier subtipo es intercambiable donde se espere un request.
 *
 * En esta fase NO se define comportamiento (duración, curvas, assets…). Sólo
 * ESTRUCTURA. Los identificadores son `String` a propósito para no acoplar este
 * módulo a los modelos del engine.
 */
sealed interface AnimationRequest {

    /**
     * Prioridad relativa. Preparada para que la [AnimationQueue] y el
     * [AnimationScheduler] puedan ordenar o descartar solicitudes. Sin lógica de
     * prioridad todavía; sólo se declara el punto de extensión.
     */
    val priority: AnimationPriority
        get() = AnimationPriority.Normal

    // ---------------------------------------------------------------------------
    // Ejemplos de solicitudes (sólo estructura, sin comportamiento).
    // ---------------------------------------------------------------------------

    /** Se ha robado una carta del mazo a la mano. */
    data class CardDrawn(
        val playerId: String,
        val cardId: String,
    ) : AnimationRequest

    /** Un Pokémon se ha puesto en juego (banca o activo). */
    data class PokemonPlayed(
        val playerId: String,
        val pokemonId: String,
    ) : AnimationRequest

    /** Comienza la secuencia de ataque de un Pokémon. */
    data class AttackStarted(
        val attackerId: String,
        val attackName: String,
    ) : AnimationRequest {
        override val priority: AnimationPriority get() = AnimationPriority.High
    }

    /** Se activa una habilidad (manual o disparada). */
    data class AbilityActivated(
        val sourcePokemonId: String,
        val abilityName: String,
    ) : AnimationRequest

    /** Se aplica daño a un Pokémon objetivo. */
    data class DamageApplied(
        val targetPokemonId: String,
        val amount: Int,
    ) : AnimationRequest {
        override val priority: AnimationPriority get() = AnimationPriority.High
    }

    /** Un Pokémon queda fuera de combate (KO). */
    data class PokemonKnockedOut(
        val pokemonId: String,
    ) : AnimationRequest {
        override val priority: AnimationPriority get() = AnimationPriority.Critical
    }
}

/**
 * Niveles de prioridad para futura ordenación/coalescencia en la cola/scheduler.
 * Declarado ahora para dejar el punto de extensión listo; sin lógica asociada aún.
 */
enum class AnimationPriority { Low, Normal, High, Critical }
