package com.mineralord.tcg.core.animation

import kotlin.reflect.KClass

/**
 * Ejecutor de UN tipo concreto de paso HOJA (Strategy).
 *
 * Cada acción real futura (Move, Scale, Rotate, PlaySound…) tendrá su propia
 * implementación de esta interfaz. El [AnimationPlayer] NO contiene un `when` gigante:
 * delega cada hoja a su ejecutor registrado. Añadir una acción nueva = crear un nuevo
 * `AnimationStepExecutor` y registrarlo; el player no se toca (Open-Closed).
 *
 * IMPORTANTE: sólo aplica a HOJAS. Los nodos compuestos [AnimationStep.Sequence] y
 * [AnimationStep.Parallel] son control de flujo del árbol y los maneja el propio
 * player por recursión — NO deben tener ejecutor (evita dependencia circular con el
 * player).
 *
 * `suspend`: permite que la acción tarde en el tiempo y sea cancelable
 * cooperativamente. En esta fase no hay implementaciones reales.
 */
interface AnimationStepExecutor<in S : AnimationStep> {
    suspend fun execute(step: S, context: AnimationExecutionContext)
}

/**
 * Registro tipo→ejecutor para las hojas. Aísla el "cómo se despacha una hoja" del
 * recorrido del árbol.
 *
 * Mantiene el mapa por [KClass] (reflexión ligera de stdlib, Kotlin puro; sin
 * librerías externas). Se prefiere a un Visitor porque el Visitor exigiría modificar
 * una interfaz central por cada paso nuevo, violando el requisito de que añadir un
 * paso no toque el player.
 */
class AnimationStepExecutorRegistry {

    private val executors = HashMap<KClass<out AnimationStep>, AnimationStepExecutor<*>>()

    /** Registra el ejecutor de un tipo de paso hoja. */
    fun <S : AnimationStep> register(type: KClass<S>, executor: AnimationStepExecutor<S>) {
        executors[type] = executor
    }

    /**
     * Devuelve el ejecutor para la hoja dada, o `null` si no hay ninguno registrado
     * (en la fase actual eso es lo normal: el player hará no-op seguro).
     */
    @Suppress("UNCHECKED_CAST")
    fun executorFor(step: AnimationStep): AnimationStepExecutor<AnimationStep>? =
        executors[step::class] as AnimationStepExecutor<AnimationStep>?
}
