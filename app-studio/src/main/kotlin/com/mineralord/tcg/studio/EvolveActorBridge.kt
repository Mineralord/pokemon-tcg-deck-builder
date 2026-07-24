package com.mineralord.tcg.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.mineralord.tcg.core.animationcompose.AnimationRenderState
import com.mineralord.tcg.core.animationcompose.EvolveRenderNode
import com.mineralord.tcg.core.animationcompose.RenderLayer
import com.mineralord.tcg.core.animationcompose.evolveArc
import com.mineralord.tcg.core.designsystem.actor.ActorTransform
import com.mineralord.tcg.core.designsystem.actor.ActorVisualController
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
        // El nodo cambia cada frame ⇒ esta recomposición mínima publica la transformación viva.
        SideEffect { actors.set(actorId, transform) }
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
