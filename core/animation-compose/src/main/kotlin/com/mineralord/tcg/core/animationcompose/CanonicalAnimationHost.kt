package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mineralord.tcg.core.animation.AnimationDirector
import com.mineralord.tcg.core.animation.AnimationStep
import com.mineralord.tcg.core.animation.AnimationStepExecutorRegistry
import com.mineralord.tcg.core.animation.DefaultAnimationDirector

/**
 * **Composition root ÚNICO del motor de animaciones (compartido por juego y Studio).**
 *
 * Este es el único lugar donde se ensambla el pipeline real. Cualquier anfitrión —la partida del
 * juego o el Studio— obtiene su [AnimationDirector] desde aquí, garantizando que **existe un único
 * pipeline**: un único conjunto de contribuidores ([CanonicalAnimationContributors]), un único
 * conjunto de ejecutores ([canonicalStepExecutors]), un único `ComposeAnimationRunner`, un único
 * `Scheduler` y un único `AnimationDirector`. No hay versiones específicas por app: el anfitrión sólo
 * provee su `CoordinateRegistry` y su `AnimationRenderState` (dónde se dibuja) y recibe el motor real.
 */

/**
 * Todos los contribuidores de recetas canónicas del proyecto. Añadir una animación canónica nueva =
 * añadir su contribuidor a esta lista (y su executor a [canonicalStepExecutors]); el juego y el
 * Studio la reproducen sin más cambios.
 */
val CanonicalAnimationContributors = listOf(
    DrawCardAnimations,
    MoveAnimations,
    EvolveAnimations,
    BannerAnimations,
    AbilityGlowAnimations,
    StadiumPlaceAnimations,
)

/**
 * Registro de ejecutores canónicos, ligados al [coordinates]/[renderState] del anfitrión. Es el
 * único conjunto de `Executors` del sistema; los mismos que usará el juego.
 */
fun canonicalStepExecutors(
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
): AnimationStepExecutorRegistry = AnimationStepExecutorRegistry().apply {
    register(AnimationStep.DrawCard::class, DrawCardExecutor(coordinates, renderState))
    register(AnimationStep.Move::class, MoveExecutor(coordinates, renderState))
    register(AnimationStep.Evolve::class, EvolveExecutor(coordinates, renderState))
    register(AnimationStep.Banner::class, BannerExecutor(renderState))
    register(AnimationStep.AbilityGlow::class, AbilityGlowExecutor(coordinates, renderState))
    register(AnimationStep.StadiumPlace::class, StadiumPlaceExecutor(coordinates, renderState))
}

/**
 * Ensambla y recuerda el [AnimationDirector] real para el anfitrión actual. Es el punto de entrada
 * que tanto el juego como el Studio usan para hospedar el motor.
 *
 * @param coordinates registro de ranuras del anfitrión (el mismo que consume su `AnimationStage`).
 * @param renderState estado de render del anfitrión (el mismo que consume su `AnimationStage`).
 */
@Composable
fun rememberCanonicalAnimationDirector(
    coordinates: CoordinateRegistry,
    renderState: AnimationRenderState,
): AnimationDirector {
    val scope = rememberCoroutineScope()
    return remember(coordinates, renderState, scope) {
        val executors = canonicalStepExecutors(coordinates, renderState)
        val definitions = AnimationDefinitionRegistry.from(CanonicalAnimationContributors)
        val runner = ComposeAnimationRunner.create(definitions, executors, scope)
        DefaultAnimationDirector.create(runner = runner, scope = scope)
    }
}
