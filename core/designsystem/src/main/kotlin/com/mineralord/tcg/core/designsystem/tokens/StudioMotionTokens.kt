package com.mineralord.tcg.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Motion tokens del **ecosistema Pokémon TCG** (Studio, Launcher, Card Viewer, herramientas)
 * — Fase 9.6. Implementa las **cinco frases del movimiento** del Visual Grammar (VG-MOV). El
 * sistema **limita** el movimiento, no lo multiplica.
 *
 * NO confundir con el framework de animación del *juego* (paquete `..designsystem.motion`).
 *
 * Fuente de verdad: `docs/developer-studio/tokens/tokens.motion.json`. Regenerar desde el JSON.
 *
 * Reglas duras (VG-MOV / VL / Emil): quietud por defecto — lo de **alta frecuencia o iniciado por
 * teclado NO se anima**; solo se animan `translate/scale/opacity` (nunca layout); transiciones
 * **interrumpibles** (no keyframes); nunca `scale(0)` (entra desde 0.97 + opacity); **salida más
 * rápida que entrada**. Prioridad absoluta: reducir la fatiga en jornadas de 8 h; si una animación
 * no aporta significado, se elimina. Ver Anexo 9.6.
 */
object StudioMotionTokens {

    /** Primitivos. Escala mínima (4 duraciones, 3 easings). NO referenciar desde componentes (TD-2). */
    object Foundation {
        /** Duraciones en ms. */
        object Duration {
            const val Instant: Int = 0    // alta frecuencia / teclado
            const val Fast: Int = 120     // confirmación / salida
            const val Base: Int = 180     // entrada / continuidad
            const val Slow: Int = 240     // transformación / morph
        }

        /** Curvas. Nunca ease-in en UI (arranca lento, se siente perezoso). */
        object Easing {
            /** ease-out fuerte: entra/sale con respuesta inmediata. */
            val Standard: androidx.compose.animation.core.Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
            /** ease-in-out: movimiento/morph en pantalla. */
            val Movement: androidx.compose.animation.core.Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
            /** movimiento constante (progreso indeterminado). */
            val Linear: androidx.compose.animation.core.Easing = LinearEasing
        }

        /** Desplazamiento de entrada (corto; nunca desde lejos). */
        object Distance {
            val Sm: Dp = 4.dp
            val Md: Dp = 8.dp
        }

        /** Escala de entrada/pulsación (jamás desde 0). */
        object Scale {
            const val EnterFrom: Float = 0.97f
            const val Press: Float = 0.97f
        }

        /** Opacidad de entrada. */
        object Opacity {
            const val FadeFrom: Float = 0f
        }

        /** Cascada (stagger) corta y decorativa; nunca bloquea la interacción. */
        object Stagger {
            const val StepMs: Int = 40
            const val MaxMs: Int = 50
        }
    }

    /** Intensidad del movimiento (amplitud). Muy pocas opciones. */
    enum class Intensity { Subtle, Moderate }

    /** Prioridad / precedencia de interrupción. */
    enum class Priority { Normal, Immediate }

    /**
     * Las cinco frases del movimiento como **roles** oficiales (VG-MOV). Un componente elige un rol
     * por lo que la transición *significa*; si no dice ninguna de las cinco frases, no se anima.
     *
     * @property durationMillis duración de entrada/efecto principal.
     * @property exitMillis duración de salida (asimétrica: más rápida que la entrada).
     * @property easing curva de la frase.
     * @property intensity amplitud del movimiento.
     * @property priority precedencia de interrupción.
     */
    enum class Role(
        val durationMillis: Int,
        val exitMillis: Int,
        val easing: androidx.compose.animation.core.Easing,
        val intensity: Intensity,
        val priority: Priority,
    ) {
        /** Algo se mueve manteniendo su identidad (reordenar, reposicionar). */
        Continuity(Foundation.Duration.Base, Foundation.Duration.Base, Foundation.Easing.Movement, Intensity.Subtle, Priority.Normal),

        /** Algo entra o sale de escena. Salida más rápida que entrada. */
        Change(Foundation.Duration.Base, Foundation.Duration.Fast, Foundation.Easing.Standard, Intensity.Subtle, Priority.Normal),

        /** Un objeto se convierte en otro estado de sí mismo (expandir a detalle). */
        Transformation(Foundation.Duration.Slow, Foundation.Duration.Fast, Foundation.Easing.Movement, Intensity.Moderate, Priority.Normal),

        /** Respuesta breve a una acción del usuario (pulsación). */
        Confirmation(Foundation.Duration.Fast, Foundation.Duration.Fast, Foundation.Easing.Standard, Intensity.Subtle, Priority.Immediate),

        /** Algo no puede completarse / requiere atención. Rarísimo (VL Alerta). */
        Error(Foundation.Duration.Base, Foundation.Duration.Fast, Foundation.Easing.Standard, Intensity.Moderate, Priority.Immediate);

        /** Spec de entrada lista para Compose (`animate*AsState`, `updateTransition`, etc.). */
        fun <T> enterSpec(): TweenSpec<T> = tween(durationMillis = durationMillis, easing = easing)

        /** Spec de salida (asimétrica, más rápida). */
        fun <T> exitSpec(): TweenSpec<T> = tween(durationMillis = exitMillis, easing = easing)
    }

    /**
     * Accesibilidad: con `prefers-reduced-motion` activo, se retira el transform y se conserva un
     * fade breve. Devuelve el spec reducido a aplicar en lugar del de rol. El lugar de uso decide
     * (a partir del ajuste del sistema) si llama a esto en vez de a [Role.enterSpec].
     */
    fun <T> reducedMotionSpec(): TweenSpec<T> =
        tween(durationMillis = Foundation.Duration.Fast, easing = Foundation.Easing.Standard)
}
