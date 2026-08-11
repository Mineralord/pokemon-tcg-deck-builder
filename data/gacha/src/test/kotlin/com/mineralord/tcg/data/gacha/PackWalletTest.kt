package com.mineralord.tcg.data.gacha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackWalletTest {

    private val h12 = 12L * 60 * 60 * 1000
    private val regen = PackRegen(maxPacks = 6, perPackMs = h12)

    @Test
    fun `abrir 2 a las 2pm, manana a las 2pm hay 2 de nuevo`() {
        val t0 = 1_000_000_000_000L // "hoy 2pm"
        var w = PackWallet(balance = 2, lastCreditAt = t0)
        // Abre los 2.
        val a = regen.tryOpen(w, t0); assertTrue(a is PackOpenResult.Allowed); w = (a as PackOpenResult.Allowed).wallet
        val b = regen.tryOpen(w, t0); assertTrue(b is PackOpenResult.Allowed); w = (b as PackOpenResult.Allowed).wallet
        assertEquals(0, w.balance)
        assertTrue(regen.tryOpen(w, t0) is PackOpenResult.Denied)
        // +12h ⇒ 1; +24h ⇒ 2.
        assertEquals(1, regen.available(w, t0 + h12))
        assertEquals(2, regen.available(w, t0 + 2 * h12))
    }

    @Test
    fun `acumula hasta 6 si no se entra varios dias, sin pasar del tope`() {
        val t0 = 2_000_000_000_000L
        val w = PackWallet(balance = 0, lastCreditAt = t0)
        assertEquals(6, regen.available(w, t0 + 3L * 24 * 60 * 60 * 1000))   // 3 días = 6
        assertEquals(6, regen.available(w, t0 + 30L * 24 * 60 * 60 * 1000))  // mucho tiempo = sigue 6
        assertNull(regen.nextPackAt(w, t0 + 3L * 24 * 60 * 60 * 1000))       // lleno: sin próximo
    }

    @Test
    fun `adelantar el reloj hacia atras no regala sobres`() {
        val t0 = 3_000_000_000_000L
        val w = PackWallet(balance = 1, lastCreditAt = t0)
        // Un "ahora" ANTERIOR al ancla (retroceso de reloj) no acredita nada.
        assertEquals(1, regen.available(w, t0 - 5 * h12))
    }

    @Test
    fun `al bajar del tope arranca el reloj desde ahora`() {
        val t0 = 4_000_000_000_000L
        val full = PackWallet(balance = 6, lastCreditAt = t0 - 100 * h12) // ancla vieja, pero lleno
        val r = regen.tryOpen(full, t0)
        assertTrue(r is PackOpenResult.Allowed)
        val w = (r as PackOpenResult.Allowed).wallet
        assertEquals(5, w.balance)
        // El siguiente sobre llega 12h DESPUÉS de ahora (no crédito retroactivo por el ancla vieja).
        assertEquals(t0 + h12, regen.nextPackAt(w, t0))
    }
}
