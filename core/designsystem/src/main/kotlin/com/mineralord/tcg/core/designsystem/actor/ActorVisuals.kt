package com.mineralord.tcg.core.designsystem.actor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/**
 * **Actores visuales: una animación toma el control temporal de un elemento REAL — transformaciones
 * Y sus VFX — sin placeholders, copias ni capas independientes.**
 *
 * Vive en el núcleo compartido (juego + Studio). Un elemento se marca actor con [actorControlled];
 * mientras nadie lo controle se dibuja idéntico (identidad, sin VFX). Cuando el dueño de la animación
 * publica un [ActorState] para su id, el MISMO Composable se transforma y sus [ActorVFX] se dibujan
 * DENTRO de su capa transformada ⇒ **los efectos viajan con el actor** (si la carta sube, gira o
 * escala, sus auras/anillos/partículas la acompañan). Al liberar el actor, transform y VFX desaparecen
 * juntos. Un único ciclo de vida: Actor + Transformaciones + VFX.
 */

/** Transformación viva del actor. Extensible: añadir campos con default no rompe a los consumidores. */
@Immutable
data class ActorTransform(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationZ: Float = 0f,
    val alpha: Float = 1f,
    /** Halo/brillo 0..1 (aproximado como elevación de sombra hasta que existan shaders). */
    val glow: Float = 0f,
) {
    companion object {
        val Identity = ActorTransform()
    }
}

/**
 * **Efecto visual anclado al actor.** Contrato ABIERTO y autodibujable: cada efecto sabe pintarse a sí
 * mismo relativo al contenido del actor (el [DrawScope] recibido ya está DENTRO de la capa
 * transformada del actor: `size` = tamaño del actor, `center` = su centro). Añadir un efecto nuevo
 * (partículas, hojas, fuego, trails, shaders…) = una nueva implementación de [ActorVFX]; **no se
 * modifica la arquitectura**.
 */
interface ActorVFX {
    /** true = se dibuja SOBRE el actor; false = por DETRÁS (aura/anillo). */
    val over: Boolean get() = false

    /** Pinta el efecto. Se invoca dentro de la capa transformada del actor (viaja con él). */
    fun DrawScope.drawVfx()
}

/** Estado completo de un actor bajo control: su transformación + sus efectos. */
@Immutable
data class ActorState(
    val transform: ActorTransform = ActorTransform.Identity,
    val vfx: List<ActorVFX> = emptyList(),
) {
    companion object {
        val Idle = ActorState()
    }
}

/**
 * Registro observable del estado por actor. Responsabilidad única: almacenar y entregar el estado vivo
 * (transform + VFX) de cada `id`. No dibuja ni conoce animaciones concretas.
 */
@Stable
class ActorVisualController {
    private val states = mutableStateMapOf<String, ActorState>()

    /** Toma el control del actor: publica su transformación y sus VFX. */
    fun set(actorId: String, transform: ActorTransform, vfx: List<ActorVFX> = emptyList()) {
        states[actorId] = ActorState(transform, vfx)
    }

    /** Libera el actor: vuelve a identidad y sin VFX (lo recupera la UI). */
    fun clear(actorId: String) {
        states.remove(actorId)
    }

    /** Estado actual del actor, o [ActorState.Idle] si nadie lo controla. Lectura observable. */
    fun stateOf(actorId: String): ActorState = states[actorId] ?: ActorState.Idle
}

/** Controller activo en composición. Por defecto vacío ⇒ todos los actores en identidad y sin VFX. */
val LocalActorVisuals = staticCompositionLocalOf { ActorVisualController() }

/**
 * Marca este Composable como **actor visual** [actorId]: aplica su transformación viva y dibuja sus
 * VFX dentro de la MISMA capa (`graphicsLayer` + `drawWithContent`), de modo que los efectos heredan
 * posición/escala/rotación del actor. Las lecturas ocurren en fase de dibujo ⇒ sólo se re-evalúa la
 * capa (no recompone). Identidad y sin efectos por defecto: coste nulo mientras nadie lo controle.
 */
fun Modifier.actorControlled(actorId: String): Modifier = composed {
    val controller = LocalActorVisuals.current
    this
        .graphicsLayer {
            val t = controller.stateOf(actorId).transform
            translationX = t.translationX
            translationY = t.translationY
            scaleX = t.scaleX
            scaleY = t.scaleY
            rotationZ = t.rotationZ
            alpha = t.alpha
            shadowElevation = t.glow * 28f
        }
        .drawWithContent {
            val vfx = controller.stateOf(actorId).vfx
            vfx.forEach { if (!it.over) with(it) { drawVfx() } } // aura/anillos detrás
            drawContent()                                        // el actor real
            vfx.forEach { if (it.over) with(it) { drawVfx() } }  // destellos/partículas delante
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// VFX reutilizables incorporados (genéricos, no específicos de ninguna animación).
// Nuevos efectos = nuevas implementaciones de ActorVFX, sin tocar el canal.
// ─────────────────────────────────────────────────────────────────────────────

/** Aura/halo suave centrado en el actor. */
@Immutable
data class HaloVFX(val color: Color, val intensity: Float) : ActorVFX {
    override fun DrawScope.drawVfx() {
        if (intensity <= 0f) return
        drawCircle(color.copy(alpha = intensity.coerceIn(0f, 1f) * 0.5f), radius = size.minDimension * 0.75f, center = center)
    }
}

/** Anillo expansivo (borde) que crece con [progress] y se desvanece. */
@Immutable
data class RingVFX(val color: Color, val progress: Float, val maxRadiusFrac: Float, val alpha: Float) : ActorVFX {
    override fun DrawScope.drawVfx() {
        if (alpha <= 0f || maxRadiusFrac <= 0f) return
        val radius = size.width * maxRadiusFrac * progress.coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius,
            center = center,
            style = Stroke(width = (size.width * 0.06f).coerceAtLeast(2f)),
        )
    }
}

/** Destello/estallido relleno (por delante del actor). */
@Immutable
data class BurstVFX(val color: Color, val alpha: Float, val radiusFrac: Float) : ActorVFX {
    override val over: Boolean get() = true
    override fun DrawScope.drawVfx() {
        if (alpha <= 0f) return
        drawCircle(color.copy(alpha = alpha.coerceIn(0f, 1f)), radius = size.width * radiusFrac, center = center)
    }
}
