package com.mineralord.tcg.core.animation

import kotlin.time.Duration

/**
 * Un nodo en el árbol de una [AnimationDefinition].
 *
 * Decisión arquitectónica clave (Composite pattern):
 * - Un `AnimationStep` NO es sólo una acción hoja: puede ser también un **contenedor**
 *   ([Sequence], [Parallel]). Así una `List<AnimationStep>` en la raíz de una
 *   definición puede describir desde una animación trivial hasta una secuencia
 *   arbitrariamente compleja (paralelo dentro de secuencia dentro de paralelo…)
 *   SIN que el reproductor ni el [AnimationDirector] necesiten cambiar. La complejidad
 *   se expresa como más nodos, no como más código.
 *
 * Fase actual: SÓLO estructura. Las hojas declaran su intención como DATOS inmutables
 * (no ejecutan nada); el futuro reproductor las interpretará. Ninguna hoja conoce
 * Compose, Android ni el engine — los objetivos se referencian por `String` a
 * propósito para no acoplar este módulo a modelos externos.
 *
 * Los ejemplos de hoja incluidos (mover, escalar, rotar, esperar, sonido, partículas,
 * texto) son un punto de partida abierto: añadir una acción nueva = añadir una
 * subclase, sin tocar nada más (Open-Closed).
 */
sealed interface AnimationStep {

    // ---------------------------------------------------------------------------
    // Nodos compuestos (Composite): permiten árboles arbitrarios.
    // ---------------------------------------------------------------------------

    /** Ejecuta sus hijos uno tras otro, en orden. */
    data class Sequence(val children: List<AnimationStep>) : AnimationStep

    /** Ejecuta todos sus hijos a la vez; termina cuando termina el último. */
    data class Parallel(val children: List<AnimationStep>) : AnimationStep

    // ---------------------------------------------------------------------------
    // Ejemplos de hoja (sólo estructura; sin comportamiento).
    // ---------------------------------------------------------------------------

    /**
     * Mueve el elemento de una ranura de origen a una ranura de destino.
     *
     * Las ranuras se referencian por `String` (no por `SlotId` de Compose) para no
     * acoplar este módulo puro al mundo Compose: el ejecutor traducirá cada id a su
     * rectángulo real vía el registro de coordenadas. [duration] es la duración de la
     * traslación; la interpola el ejecutor (trayectoria lineal en esta fase).
     *
     * Forma actual = necesidad REAL de la primera prueba vertical (mover de A a B). No
     * se añaden campos "por si acaso"; el modelo crece cuando surja una necesidad real.
     */
    data class Move(
        val fromSlotId: String,
        val toSlotId: String,
        val duration: Duration,
    ) : AnimationStep

    /**
     * Robo de carta: la carta viaja de la ranura del mazo a la de la mano describiendo
     * un ARCO leve, con escala/rotación de asentado (la "piel" del robo la aporta su
     * ejecutor/renderer; ver `DrawCardExecutor`/`DrawCardNodeRenderer`). Se separa de
     * [Move] a propósito: es una animación con identidad propia (curva + overshoot +
     * escala), no una traslación lineal. Añadir esta hoja NO toca el `AnimationPlayer`
     * (Open-Closed): el registro de ejecutores la enruta a su ejecutor.
     *
     * Ranuras por `String` (no `SlotId` de Compose) para no acoplar el módulo puro.
     * [duration] gobierna el tempo; el "feel" (altura del arco, rango de escala, easing)
     * vive en el ejecutor/renderer como identidad de esta animación, no como datos aquí.
     */
    data class DrawCard(
        val fromSlotId: String,
        val toSlotId: String,
        val duration: Duration,
    ) : AnimationStep

    /**
     * Evolución de un Pokémon EN una ranura (no una traslación): la carta asciende, gira,
     * bombea, destella y proyecta un anillo según sus perillas [visual]. Se separa de
     * [Move]/[DrawCard] a propósito: es una transformación in-situ con identidad propia.
     * Añadir esta hoja NO toca el `AnimationPlayer` (Open-Closed): el registro de ejecutores
     * la enruta a `EvolveExecutor`.
     *
     * La ranura se referencia por `String` (no `SlotId`) para no acoplar el módulo puro. El
     * "feel" concreto de cada variante son DATOS ([visual]), no ramas de código: cinco
     * evoluciones distintas = cinco [EvolveVisual] distintos, un único ejecutor/renderer.
     */
    data class Evolve(
        val slotId: String,
        val duration: Duration,
        val visual: EvolveVisual,
    ) : AnimationStep

    /**
     * Rótulo/banner AAA a pantalla completa que anuncia un ataque o una habilidad (lower-third
     * tipo Marvel Snap / Legends of Runeterra). NO viaja por ranuras: es un overlay que barre,
     * sostiene y sale. Su identidad (dirección, tinte, énfasis) son DATOS ([visual]); el texto
     * ([kicker]/[title]) también. Separado de [ShowText] a propósito: [ShowText] es texto flotante
     * anclado a un objetivo; esto es un rótulo cinematográfico de anuncio con placa y barrido de luz.
     *
     * Añadir esta hoja NO toca el `AnimationPlayer` (Open-Closed): su ejecutor la enruta a
     * `BannerExecutor`, que la publica como nodo de la capa de overlay.
     */
    data class Banner(
        val kicker: String,
        val title: String,
        val duration: Duration,
        val visual: BannerVisual,
    ) : AnimationStep

    /**
     * Aura de habilidad EN una ranura (banca o activo): bloom radial por debajo de la carta + rim-glow
     * en los bordes, con respiración/shimmer. Comunica que el Pokémon está ejerciendo una habilidad.
     * El COLOR y el carácter (rojo estable = pasiva; dorado invitador = manual) son DATOS ([visual]);
     * un único ejecutor/renderer sirve ambos modos. La ranura se referencia por `String` (no `SlotId`)
     * para no acoplar el módulo puro. Enrutada a `AbilityGlowExecutor` (Open-Closed).
     */
    data class AbilityGlow(
        val slotId: String,
        val duration: Duration,
        val visual: GlowVisual,
    ) : AnimationStep

    /**
     * Colocación de un Estadio: la carta viaja de la ranura de la mano a la del slot de Estadio
     * describiendo un ARCO y se ASIENTA con peso (overshoot + destello de aterrizaje). Se separa de
     * [DrawCard]/[Move] a propósito: su identidad visual es "objeto pesado que aterriza" (escala
     * 0.9→1.0, overshoot ~12%, flash radial al tocar), no un robo ni una traslación lineal. El "feel"
     * (altura de arco, overshoot, flash) vive en `StadiumPlaceExecutor`/`StadiumPlaceNodeRenderer`.
     * Ranuras por `String` para no acoplar el módulo puro. Enrutada a su ejecutor (Open-Closed).
     */
    data class StadiumPlace(
        val fromSlotId: String,
        val toSlotId: String,
        val duration: Duration,
    ) : AnimationStep

    /** Escala un objetivo hasta un factor. */
    data class Scale(val targetId: String, val factor: Float) : AnimationStep

    /** Rota un objetivo un número de grados. */
    data class Rotate(val targetId: String, val degrees: Float) : AnimationStep

    /** Pausa la línea temporal sin efecto visual. */
    data class Wait(val duration: Duration) : AnimationStep

    /** Reproduce un efecto de sonido identificado por clave (sin acoplar a assets). */
    data class PlaySound(val soundKey: String) : AnimationStep

    /** Emite un sistema de partículas en un objetivo. */
    data class SpawnParticles(val targetId: String, val effectKey: String) : AnimationStep

    /** Muestra un texto flotante (p. ej. daño, "¡Debilidad!"). */
    data class ShowText(val targetId: String, val text: String) : AnimationStep
}
