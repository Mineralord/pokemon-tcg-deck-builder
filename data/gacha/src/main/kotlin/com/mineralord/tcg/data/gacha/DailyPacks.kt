package com.mineralord.tcg.data.gacha

/**
 * Estado persistido del límite diario de sobres COMPRADOS con Cristales (Fase 2 §7.4 /
 * Fase 3 §4.6). `dayId` identifica el día (p.ej. días epoch UTC); `openedToday` cuántos
 * sobres se han comprado en ese día.
 *
 * Nota histórica: originalmente contaba sobres GRATIS por día; los gratuitos pasaron al
 * monedero con regeneración ([PackWallet]) y este contador se reutiliza ahora para el
 * límite de COMPRA, que sí es diario. Es un dato puro y serializable: la persistencia real
 * (DataStore) vive en `:data:profile`, pero la LÓGICA del límite es pura y se testea aquí.
 */
data class DailyPackState(val dayId: Long = 0, val openedToday: Int = 0)

/** Resultado de intentar comprar un sobre bajo el límite diario. */
sealed interface OpenAttempt {
    /** Permitido: nuevo estado tras consumir una compra, y cuántas quedan hoy. */
    data class Allowed(val newState: DailyPackState, val remainingToday: Int) : OpenAttempt
    /** Denegado: se alcanzó el tope diario de compras. */
    data class Denied(val state: DailyPackState) : OpenAttempt
}

/**
 * Aplica el tope de [maxPerDay] sobres COMPRADOS por día. Función pura: dado el estado
 * persistido y el día actual, decide y devuelve el nuevo estado. El reset es implícito:
 * si cambia el día, el contador vuelve a 0.
 */
class DailyPackLimiter(val maxPerDay: Int = 10) {

    fun remaining(state: DailyPackState, today: Long): Int =
        if (state.dayId != today) maxPerDay else (maxPerDay - state.openedToday).coerceAtLeast(0)

    fun tryOpen(state: DailyPackState, today: Long): OpenAttempt {
        val normalized = if (state.dayId != today) DailyPackState(today, 0) else state
        if (normalized.openedToday >= maxPerDay) return OpenAttempt.Denied(normalized)
        val updated = normalized.copy(openedToday = normalized.openedToday + 1)
        return OpenAttempt.Allowed(updated, maxPerDay - updated.openedToday)
    }
}
