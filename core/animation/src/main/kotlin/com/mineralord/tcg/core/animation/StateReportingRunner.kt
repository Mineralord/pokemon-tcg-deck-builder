package com.mineralord.tcg.core.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Decorador de [AnimationRunner] que reporta el [AnimationState] al Facade.
 *
 * Es el mecanismo que permite al [DefaultAnimationDirector] exponer estado observable
 * **sin modificar el [AnimationScheduler]**: el scheduler recibe este runner como si
 * fuera el real; aquí interceptamos el arranque y el fin de cada animación para
 * actualizar el `StateFlow`, y delegamos la ejecución en [delegate].
 *
 * Alcance del reporte (primera versión, honesto):
 * - `phase` = Playing mientras haya alguna animación activa; Idle cuando no queda
 *   ninguna. Se lleva la cuenta con un contador atómico porque varias animaciones
 *   paralelas entran/salen concurrentemente.
 * - `current` = la última solicitud arrancada.
 * - `pending` NO se reporta (queda en 0): la profundidad real de la cola vive en el
 *   scheduler, que no modificamos; políticas como Ignore/Replace impedirían un conteo
 *   fiable desde aquí. Se reportará cuando el scheduler exponga eventos (fase futura).
 */
internal class StateReportingRunner(
    private val delegate: AnimationRunner,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<AnimationState>,
) : AnimationRunner {

    private val active = AtomicInteger(0)

    override fun start(request: AnimationRequest): AnimationHandle {
        val handle = delegate.start(request)

        active.incrementAndGet()
        state.update { it.copy(current = request, phase = AnimationState.Phase.Playing) }

        scope.launch {
            runCatching { handle.await() }
            if (active.decrementAndGet() <= 0) {
                state.value = AnimationState.Idle
            }
        }
        return handle
    }
}
