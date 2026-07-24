package com.mineralord.tcg.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.EvolveRenderNode
import com.mineralord.tcg.core.animationcompose.RenderLayer
import com.mineralord.tcg.core.animationcompose.evolveArc
import com.mineralord.tcg.core.designsystem.actor.ActorTransform
import com.mineralord.tcg.core.designsystem.actor.ActorVFX
import com.mineralord.tcg.core.designsystem.actor.ActorVisualController
import com.mineralord.tcg.core.designsystem.actor.BurstVFX
import com.mineralord.tcg.core.designsystem.actor.RingVFX
import kotlin.math.PI
import kotlin.math.cos

/**
 * **Puente actor↔animación (Studio).**
 *
 * Reutiliza la MISMA línea de tiempo del `EvolveExecutor` (el nodo [EvolveRenderNode] que ya emite el
 * pipeline canónico con `progress` + `visual`) pero, en lugar de dibujar el placeholder de vuelo, la
 * traduce a un [ActorTransform] y toma el control TEMPORAL de la carta REAL (el Activo, [actorId]) a
 * través del [ActorVisualController]. Al terminar la animación (el nodo desaparece), libera el actor
 * y la carta vuelve a su estado original. No dibuja nada ni crea copias: hay un único actor visual.
 *
 * Es genérico por diseño: cualquier animación que produzca una transformación para un `id` de actor
 * usará este mismo canal; hoy se cablea la evolución como primer caso.
 */
@Composable
fun EvolveActorBridge(renderState: AnimationRenderState, actors: ActorVisualController, actorId: String?) {
    val node = renderState.nodesOf(RenderLayer.Flight).filterIsInstance<EvolveRenderNode>().firstOrNull()
    if (actorId == null) return

    if (node != null) {
        val transform = node.toActorTransform()
        val vfx = node.toActorVfx()
        // El nodo cambia cada frame ⇒ esta recomposición mínima publica transform + VFX vivos, que
        // viajan con la carta real (se dibujan dentro de su capa transformada).
        SideEffect { actors.set(actorId, transform, vfx) }
    } else {
        // Sin nodo: la animación terminó (o no hay ninguna) ⇒ devolvemos el control a la CombatScreen.
        SideEffect { actors.clear(actorId) }
    }
}

/**
 * Traduce el estado del nodo de evolución a una transformación RELATIVA del actor (la carta en su
 * posición de reposo): ascenso, bombeo, giro, volteo y destello (glow). Es el mismo cálculo que hacía
 * el `EvolveNodeRenderer` sobre el placeholder, ahora aplicado a la carta real vía `graphicsLayer`.
 */
private fun EvolveRenderNode.toActorTransform(): ActorTransform {
    val v = visual
    val p = progress
    val arc = evolveArc(p)
    val flipX = if (v.flip > 0f) cos(v.flip * 2f * PI.toFloat() * p) else 1f
    val pulse = 1f + v.pulse * arc
    return ActorTransform(
        translationY = -v.rise * bounds.height * arc,
        scaleX = pulse * flipX,
        scaleY = pulse,
        rotationZ = v.spin * 360f * p,
        glow = (v.flash * arc).coerceIn(0f, 1f),
    )
}

/**
 * VFX de la evolución ANCLADOS a la carta (anillo expansivo del matiz de la variante + destello),
 * derivados del mismo estado del nodo. Al viajar dentro de la capa del actor, siguen a la carta.
 */
private fun EvolveRenderNode.toActorVfx(): List<ActorVFX> {
    val v = visual
    val p = progress
    val arc = evolveArc(p)
    val accent = Color.hsv(v.hue.coerceIn(0f, 360f), 0.7f, 1f)
    val effects = ArrayList<ActorVFX>(2)
    if (v.ring > 0f && p > 0f) {
        effects += RingVFX(color = accent, progress = p, maxRadiusFrac = v.ring, alpha = (1f - p) * 0.6f)
    }
    if (v.flash > 0f) {
        effects += BurstVFX(color = Color.White, alpha = v.flash * arc, radiusFrac = 0.7f + p * 0.9f)
    }
    return effects
}
