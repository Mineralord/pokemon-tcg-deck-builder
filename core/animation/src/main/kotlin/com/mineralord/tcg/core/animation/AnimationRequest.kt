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

    /**
     * Se ha jugado un Estadio: la carta viaja de la mano al slot de Estadio (lateral, compartido) y
     * se ASIENTA boca arriba con peso (overshoot + destello de aterrizaje). [playerId] identifica al
     * jugador que lo coloca (origen = su mano); [stadiumId] la carta. La receta resuelve la
     * [AnimationDefinition] con el paso [AnimationStep.StadiumPlace].
     */
    data class StadiumPlaced(
        val playerId: String,
        val stadiumId: String,
    ) : AnimationRequest {
        override val priority: AnimationPriority get() = AnimationPriority.High
    }

    /**
     * Un Pokémon ha evolucionado. [variantId] selecciona QUÉ variante visual de evolución
     * reproducir (p. ej. "EVO_003"); el contribuidor de recetas resuelve la
     * [AnimationDefinition] concreta a partir de ella. En el juego bastará con una variante
     * canónica; en el Studio se prueban todas las variantes por su id.
     */
    data class Evolved(
        val playerId: String,
        val pokemonId: String,
        val variantId: String,
    ) : AnimationRequest

    /**
     * Comienza la secuencia de ataque de un Pokémon. [incoming] codifica la DIRECCIÓN para el
     * rótulo: `true` = ataque del RIVAL hacia mí (barre entrante, tinte cálido); `false` = ataque
     * MÍO hacia el rival (barre saliente, tinte frío). El contribuidor resuelve el `BannerVisual`.
     */
    data class AttackStarted(
        val attackerId: String,
        val attackName: String,
        val incoming: Boolean = false,
    ) : AnimationRequest {
        override val priority: AnimationPriority get() = AnimationPriority.High
    }

    /**
     * Se activa una habilidad. El rótulo muestra como texto principal el NOMBRE DEL POKÉMON
     * ([pokemonName], p. ej. "Venusaur"), no el de la habilidad. [manual] distingue el tono:
     * `true` = habilidad manual (dorada, accionable); `false` = pasiva/disparada (carmesí, ambiental).
     * [abilityName] se conserva para logs/depuración aunque el rótulo no lo muestre.
     */
    data class AbilityActivated(
        val sourcePokemonId: String,
        val pokemonName: String,
        val abilityName: String,
        val manual: Boolean = false,
    ) : AnimationRequest

    /**
     * Un Pokémon (en banca o activo) está EJERCIENDO una habilidad: enciende el aura bajo su carta.
     * [manual] elige el carácter: `true` = dorado invitador (puede activarse manualmente);
     * `false` = rojo estable (pasiva en curso). [slotId] es la ranura donde se dibuja el aura.
     */
    data class AbilityGlowRequested(
        val sourcePokemonId: String,
        val slotId: String,
        val manual: Boolean = false,
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
