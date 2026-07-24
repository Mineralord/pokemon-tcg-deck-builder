package com.mineralord.tcg.core.designsystem.actor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * **Actores visuales: canal neutral para que una animación tome el control temporal de un elemento
 * visual REAL (sin placeholders ni duplicados).**
 *
 * Vive en el núcleo compartido (lo usan el juego y el Studio). Un elemento se marca como actor con
 * [Modifier.actorControlled] pasando su `id`; mientras nadie escriba una transformación para ese id,
 * el elemento se dibuja EXACTAMENTE igual (identidad) → sin cambios de comportamiento. Cuando el
 * dueño de la animación (p. ej. el `AnimationDirector` a través de su puente) publica un
 * [ActorTransform] para ese id, el MISMO Composable se transforma; al limpiarlo, vuelve a su estado
 * original. El traspaso UI↔animación es imperceptible: nunca hay una segunda instancia visual.
 *
 * Es deliberadamente extensible: [ActorTransform] agrupa las propiedades comunes hoy (posición,
 * escala, rotación, opacidad, glow) y admite añadir más en el futuro (con valores por defecto) sin
 * rediseñar el canal ni a sus consumidores.
 */
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
 * Registro observable de transformaciones por actor. Responsabilidad única: almacenar y entregar la
 * transformación viva de cada `id`. No dibuja ni conoce animaciones concretas.
 */
@Stable
class ActorVisualController {
    private val transforms = mutableStateMapOf<String, ActorTransform>()

    /** Publica (toma el control de) la transformación de un actor. */
    fun set(actorId: String, transform: ActorTransform) {
        transforms[actorId] = transform
    }

    /** Libera el actor: vuelve a identidad (lo recupera la UI). */
    fun clear(actorId: String) {
        transforms.remove(actorId)
    }

    /** Transformación actual del actor, o identidad si nadie lo controla. Lectura observable. */
    fun transformOf(actorId: String): ActorTransform = transforms[actorId] ?: ActorTransform.Identity
}

/**
 * Controller activo en composición. Por defecto vacío ⇒ todos los actores en identidad (el juego no
 * provee ninguno, así que sus cartas se dibujan igual que siempre). El Studio provee el suyo.
 */
val LocalActorVisuals = staticCompositionLocalOf { ActorVisualController() }

/**
 * Marca este Composable como **actor visual** [actorId]: aplica la transformación viva del
 * [LocalActorVisuals] actual. La lectura ocurre dentro del `graphicsLayer` (fase de dibujo) para que
 * cada frame sólo re-evalúe la capa —no recomponga— cuando la transformación cambia. Identidad por
 * defecto: coste nulo mientras el actor no esté bajo control.
 */
fun Modifier.actorControlled(actorId: String): Modifier = composed {
    val controller = LocalActorVisuals.current
    graphicsLayer {
        val t = controller.transformOf(actorId)
        translationX = t.translationX
        translationY = t.translationY
        scaleX = t.scaleX
        scaleY = t.scaleY
        rotationZ = t.rotationZ
        alpha = t.alpha
        shadowElevation = t.glow * 28f
    }
}
