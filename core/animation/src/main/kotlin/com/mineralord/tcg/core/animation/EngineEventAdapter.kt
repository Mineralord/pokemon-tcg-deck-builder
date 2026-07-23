package com.mineralord.tcg.core.animation

/**
 * Frontera de traducción entre el dominio y el sistema visual.
 *
 * Posición en el pipeline:
 * `EngineEvent → **EngineEventAdapter** → AnimationRequest → AnimationQueue → …`.
 *
 * Responsabilidad ÚNICA (SRP):
 * - Recibir un evento del motor (`EngineEvent`) y traducirlo a cero, una o varias
 *   [AnimationRequest]. Nada más.
 *
 * Lo que este adaptador **NO** hace (fronteras explícitas):
 * - NO reproduce animaciones.
 * - NO conoce Jetpack Compose.
 * - NO depende del [AnimationDirector] ni de la [AnimationQueue]: sólo produce
 *   solicitudes y las devuelve; quien las encola/sirve es otra pieza. Esto lo hace
 *   una función pura y trivialmente testeable.
 *
 * Nota sobre el tipo `EngineEvent`:
 * - `EngineEvent` vive en el módulo `engine:events`. Importarlo aquí obligaría a
 *   `core:animation` a depender del motor, violando Clean Architecture (el subsistema
 *   visual quedaría acoplado al dominio). Por eso, en esta fase se declara SÓLO la
 *   interfaz genérica y la traducción concreta queda PENDIENTE.
 * - La implementación real vivirá en un módulo de composición (p. ej. `feature:game`)
 *   que sí puede depender tanto de `engine:events` como de `core:animation`, y allí
 *   `E` se fijará a `EngineEvent`. Así la dependencia motor→visual nunca se produce;
 *   es el módulo de arriba quien conoce a ambos (Dependency Inversion).
 *
 * @param E tipo del evento de dominio a traducir. Genérico a propósito para no
 *          acoplar `core:animation` al tipo real `EngineEvent`.
 */
fun interface EngineEventAdapter<in E> {

    /**
     * Traduce un evento de dominio en la lista ordenada de solicitudes de animación
     * que representa. Puede devolver una lista vacía si el evento no tiene
     * representación visual.
     *
     * Contrato puro: misma entrada ⇒ misma salida; sin efectos secundarios.
     */
    fun adapt(event: E): List<AnimationRequest>
}
