package com.mineralord.tcg.feature.game.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.engine.model.Side
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Cue" de animación derivada del stream de [com.mineralord.tcg.engine.events.GameEvent].
 * El [com.mineralord.tcg.feature.game.GameViewModel] reemite estas señales para que la
 * UI reproduzca el FX correspondiente, replicando el feel de TCG Live sin acoplar el
 * motor (puro Kotlin) a Compose.
 */
sealed interface FxCue {
    val side: Side?

    /** Embate del atacante de [side] (se mueve hacia el rival). */
    data class Attack(override val side: Side) : FxCue

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
}

/** Estado vivo de un número flotante (daño o curación). */
class FloatingNumber(
    val id: Long,
    val side: Side,
    val amount: Int,
    val weakness: Boolean,
    val resistance: Boolean,
    /** true → curación (verde, "+N"); false → daño ("-N"). */
    val heal: Boolean = false,
)

/** Número de daño/curación que sube y se desvanece (estilo TCG Live). */
@Composable
fun FloatingDamage(num: FloatingNumber, onDone: (Long) -> Unit) {
    val rise = remember(num.id) { Animatable(0f) }
    val alpha = remember(num.id) { Animatable(1f) }
    LaunchedEffect(num.id) {
        rise.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
        onDone(num.id)
    }
    LaunchedEffect(num.id) {
        alpha.animateTo(0f, tween(900, delayMillis = 300))
    }
    val color = when {
        num.heal -> Color(0xFF66BB6A)
        num.weakness -> Color(0xFFFF5252)
        num.resistance -> Color(0xFF80D8FF)
        else -> Color(0xFFFFFFFF)
    }
    Text(
        text = (if (num.heal) "+${num.amount}" else "-${num.amount}") + if (num.weakness) " ×2" else "",
        color = color,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        modifier = Modifier
            .graphicsLayer {
                translationY = -rise.value * 90f
                scaleX = 1f + (1f - rise.value) * 0.3f
                scaleY = scaleX
            }
            .alpha(alpha.value),
    )
}

/** Genera un id único para cada número flotante. */
fun newFxId(): Long = System.nanoTime() + Random.nextInt(1000)

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
        anim.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
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
        anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing)) // embate (ida)
        anim.animateTo(0f, tween(240, easing = LinearOutSlowInEasing)) // retroceso (vuelta)
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
        anim.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        anim.snapTo(0f)
    }
    val t = anim.value
    return KoAnim(alpha = 1f - t, scale = 1f - t * 0.35f, rotation = t * 40f)
}

/** Moneda girando (cara/cruz) que aparece, gira y se desvanece. */
@Composable
fun CoinFlipFx(heads: Boolean, id: Long, onDone: () -> Unit) {
    val spin = remember(id) { Animatable(0f) }
    val alpha = remember(id) { Animatable(1f) }
    LaunchedEffect(id) {
        spin.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        alpha.animateTo(0f, tween(300))
        onDone()
    }
    Box(
        Modifier
            .size(72.dp)
            .graphicsLayer {
                rotationX = spin.value * 1440f
            }
            .alpha(alpha.value)
            .clip(CircleShape)
            .background(if (heads) Color(0xFFE4AB5E) else Color(0xFF7F8C8D)),
    ) {
        Text(
            if (heads) "C" else "X",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            modifier = Modifier.scale(1f).align(androidx.compose.ui.Alignment.Center),
        )
    }
}
