package com.mineralord.tcg.feature.game.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.mineralord.tcg.core.designsystem.motion.AnimationCurves
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Side
import kotlin.math.sin

/**
 * "Cue" de animación derivada del stream de [com.mineralord.tcg.engine.events.GameEvent].
 * El [com.mineralord.tcg.feature.game.GameViewModel] reemite estas señales para que la
 * UI reproduzca el FX correspondiente, replicando el feel de TCG Live sin acoplar el
 * motor (puro Kotlin) a Compose.
 */
sealed interface FxCue {
    val side: Side?

    /**
     * Embate del atacante de [side] (se mueve hacia el rival). [attackName] alimenta el rótulo
     * cinematográfico de anuncio de ataque (dirección derivada del lado).
     */
    data class Attack(override val side: Side, val attackName: String = "") : FxCue

    /**
     * Un Pokémon de [side] activó una Habilidad: rótulo de anuncio + aura sobre [pokemon]
     * ([manual] = dorado; pasiva = rojo).
     */
    data class AbilityUse(override val side: Side, val pokemon: CardId, val manual: Boolean) : FxCue

    /** Daño recibido por el Pokémon de [side]: número flotante + sacudida + flash. */
    data class Damage(
        override val side: Side,
        val amount: Int,
        val weakness: Boolean,
        val resistance: Boolean,
    ) : FxCue

    /** Curación del Pokémon de [side]. */
    data class Heal(override val side: Side, val amount: Int) : FxCue

    /** Noqueo del Pokémon de [side]. */
    data class Knockout(override val side: Side) : FxCue

    /** [side] toma [count] premio(s). */
    data class Prize(override val side: Side, val count: Int) : FxCue

    /** Lanzamiento de moneda. */
    data class Coin(override val side: Side?, val heads: Boolean) : FxCue

    /** [side] ha jugado un Estadio [card]: viaja de la mano al slot de Estadio y se asienta con peso. */
    data class StadiumPlaced(override val side: Side, val card: CardId) : FxCue

    /**
     * [side] ha puesto en juego (o evolucionado a) el Pokémon [card]: dispara su GRITO oficial.
     * La UI resuelve la carta → nº de Pokédex nacional → reproduce el .ogg (streaming + caché).
     */
    data class Cry(override val side: Side, val card: CardId) : FxCue
}



/**
 * Sacudida horizontal disparada por [trigger] (cambia su valor para relanzar).
 * Aplica a un Pokémon que recibe daño.
 */
@Composable
fun rememberShake(trigger: Int): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(1f, tween(360, easing = AnimationCurves.Standard))
    }
    // 3 oscilaciones que decaen.
    val t = anim.value
    return sin(t * 18f) * (1f - t) * 10f
}

/**
 * Embate del atacante (estilo TCG Live): el activo avanza hacia el rival y vuelve.
 * Devuelve un desplazamiento vertical en dp. [mine] = lado inferior (avanza hacia
 * arriba, hacia el rival); rival avanza hacia abajo.
 */
@Composable
fun rememberLunge(trigger: Int, mine: Boolean): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(1f, tween(140, easing = AnimationCurves.Standard)) // embate (ida)
        anim.animateTo(0f, tween(240, easing = AnimationCurves.Decelerate)) // retroceso (vuelta)
    }
    val dir = if (mine) -1f else 1f
    return anim.value * 30f * dir
}

/** Estado visual de un noqueo: alfa, escala y rotación de la carta que cae. */
class KoAnim(val alpha: Float, val scale: Float, val rotation: Float)

/**
 * Noqueo (estilo TCG Live): la carta se desvanece, encoge y gira ligeramente al
 * fluir hacia el descarte. Disparado por [trigger].
 */
@Composable
fun rememberKnockout(trigger: Int): KoAnim {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(1f, tween(420, easing = AnimationCurves.Standard))
        anim.snapTo(0f)
    }
    val t = anim.value
    return KoAnim(alpha = 1f - t, scale = 1f - t * 0.35f, rotation = t * 40f)
}

