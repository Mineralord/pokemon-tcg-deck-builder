package com.mineralord.tcg.core.animation

/**
 * Describe cómo una animación se comporta **respecto a las demás**.
 *
 * Decisión arquitectónica: en lugar de un único enum gigante con combinaciones como
 * `ParaleloCancelableReemplaza`, se modela como una `data class` con varios ejes
 * ORTOGONALES. Los seis comportamientos que se pidieron (paralelo, bloquea la cola,
 * cancelable, debe terminar, ignora nuevas, reemplaza) son en realidad tres
 * dimensiones independientes. Separarlas:
 * - evita la explosión combinatoria de constantes,
 * - respeta SRP (cada eje decide una sola cosa),
 * - y permite combinaciones futuras gratis, sin tocar el modelo (Open-Closed).
 *
 * En esta fase NO hay lógica que interprete estas políticas; sólo el contrato. La
 * cola/scheduler las consumirán en la fase del reproductor.
 */
data class AnimationPolicy(
    /** ¿Convive con otras animaciones o exige la escena en exclusiva? */
    val concurrency: Concurrency = Concurrency.Exclusive,

    /** ¿Puede abortarse a mitad, o debe completarse sí o sí? */
    val interruptibility: Interruptibility = Interruptibility.Cancellable,

    /** Qué hacer cuando llega otra solicitud que compite por el mismo objetivo. */
    val conflict: Conflict = Conflict.Enqueue,
) {
    /** Eje de concurrencia. */
    enum class Concurrency {
        /** Puede ejecutarse a la vez que otras. */
        Parallel,
        /** Bloquea la cola: nada más se ejecuta hasta que termina. */
        Exclusive,
    }

    /** Eje de interrumpibilidad. */
    enum class Interruptibility {
        /** Puede cancelarse en cualquier momento. */
        Cancellable,
        /** Debe terminar antes de continuar; no admite cancelación. */
        MustFinish,
    }

    /** Eje de resolución de conflictos frente a nuevas solicitudes. */
    enum class Conflict {
        /** La nueva solicitud espera su turno en la cola. */
        Enqueue,
        /** La nueva solicitud se descarta mientras ésta esté activa. */
        Ignore,
        /** La nueva solicitud reemplaza (interrumpe) a ésta. */
        Replace,
    }

    companion object {
        /** Política por defecto sensata: exclusiva, cancelable, encola. */
        val Default: AnimationPolicy = AnimationPolicy()
    }
}
