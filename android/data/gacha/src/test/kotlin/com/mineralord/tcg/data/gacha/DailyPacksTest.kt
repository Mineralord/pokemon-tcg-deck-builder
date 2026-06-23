package com.mineralord.tcg.data.gacha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyPacksTest {

    private val limiter = DailyPackLimiter(maxPerDay = 2)

    @Test
    fun `permite dos sobres y deniega el tercero el mismo dia`() {
        var state = DailyPackState()
        val day = 20_000L

        val first = limiter.tryOpen(state, day)
        assertTrue(first is OpenAttempt.Allowed)
        state = (first as OpenAttempt.Allowed).newState
        assertEquals(1, first.remainingToday)

        val second = limiter.tryOpen(state, day)
        assertTrue(second is OpenAttempt.Allowed)
        state = (second as OpenAttempt.Allowed).newState
        assertEquals(0, second.remainingToday)

        val third = limiter.tryOpen(state, day)
        assertTrue(third is OpenAttempt.Denied)
    }

    @Test
    fun `el contador se reinicia al cambiar de dia`() {
        val agotado = DailyPackState(dayId = 20_000L, openedToday = 2)
        assertEquals(0, limiter.remaining(agotado, 20_000L))
        assertEquals(2, limiter.remaining(agotado, 20_001L))

        val nextDay = limiter.tryOpen(agotado, 20_001L)
        assertTrue(nextDay is OpenAttempt.Allowed)
        assertEquals(20_001L, (nextDay as OpenAttempt.Allowed).newState.dayId)
        assertEquals(1, nextDay.newState.openedToday)
    }
}
