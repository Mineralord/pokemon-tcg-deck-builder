package com.mineralord.tcg.data.gacha

/**
 * Cartera de sobres con REGENERACIÓN acumulable (modelo tipo "energía"), sustituto del tope de
 * calendario. El jugador gana sobres con el tiempo hasta un TOPE, y se acumulan si no entra a la app.
 *
 * Reglas (por defecto): +1 sobre cada 12 h ⇒ **2 sobres al día**, acumulables hasta **6**. Ejemplo:
 * si abres tus 2 sobres hoy a las 14:00, mañana a las 14:00 vuelves a tener 2 (uno a las 02:00 y otro
 * a las 14:00). Si no entras varios días, se acumulan hasta 6 y ahí se detiene el reloj.
 *
 * @property balance sobres disponibles ahora (0..maxPacks).
 * @property lastCreditAt marca de tiempo TRUSTED (ms) del último crédito/anclaje del reloj de regen.
 *   DEBE ser tiempo de confianza (monótono/servidor), NUNCA el reloj de pared del dispositivo, o el
 *   jugador podría adelantar la hora para regenerar gratis. La reconciliación la hace la capa Android.
 */
data class PackWallet(val balance: Int, val lastCreditAt: Long)

/** Resultado de intentar abrir un sobre de la cartera. */
sealed interface PackOpenResult {
    data class Allowed(val wallet: PackWallet, val remaining: Int) : PackOpenResult
    data class Denied(val wallet: PackWallet) : PackOpenResult
}

/**
 * Lógica PURA de regeneración/consumo de la [PackWallet]. Determinista: dada la cartera y un
 * `trustedNow` (ms de confianza), decide el nuevo estado. No conoce persistencia ni Android.
 */
class PackRegen(
    val maxPacks: Int = 6,
    val perPackMs: Long = 12L * 60 * 60 * 1000, // 12 h ⇒ 2/día
) {
    /** Acredita los sobres regenerados desde [PackWallet.lastCreditAt] hasta [trustedNow] (tope incl.). */
    fun credited(wallet: PackWallet, trustedNow: Long): PackWallet {
        // En el tope no se acumula de más: se mantiene el reloj "pegado" a ahora (no genera crédito
        // retroactivo si luego se abre un sobre). El coerce evita crédito por retroceso del reloj.
        if (wallet.balance >= maxPacks) return wallet.copy(lastCreditAt = trustedNow)
        val elapsed = (trustedNow - wallet.lastCreditAt).coerceAtLeast(0)
        val gained = (elapsed / perPackMs).toInt()
        if (gained <= 0) return wallet
        val newBalance = (wallet.balance + gained).coerceAtMost(maxPacks)
        val applied = newBalance - wallet.balance
        // Al llegar al tope, ancla el reloj a ahora; si no, avanza SOLO los incrementos aplicados
        // (conserva el resto de tiempo transcurrido hacia el siguiente sobre).
        val anchor = if (newBalance >= maxPacks) trustedNow else wallet.lastCreditAt + applied * perPackMs
        return PackWallet(newBalance, anchor)
    }

    /** Sobres disponibles AHORA (tras acreditar la regeneración). */
    fun available(wallet: PackWallet, trustedNow: Long): Int = credited(wallet, trustedNow).balance

    /** Intenta consumir un sobre: acredita, y si hay saldo, resta 1 (arrancando el reloj si venía lleno). */
    fun tryOpen(wallet: PackWallet, trustedNow: Long): PackOpenResult {
        val c = credited(wallet, trustedNow)
        if (c.balance <= 0) return PackOpenResult.Denied(c)
        val wasFull = c.balance >= maxPacks
        // Si estaba lleno, el reloj estaba anclado a ahora; al bajar del tope, la regen cuenta desde ahora.
        val anchor = if (wasFull) trustedNow else c.lastCreditAt
        return PackOpenResult.Allowed(PackWallet(c.balance - 1, anchor), c.balance - 1)
    }

    /** Instante (ms trusted) en que llega el PRÓXIMO sobre, o null si ya está en el tope. */
    fun nextPackAt(wallet: PackWallet, trustedNow: Long): Long? {
        val c = credited(wallet, trustedNow)
        return if (c.balance >= maxPacks) null else c.lastCreditAt + perPackMs
    }

    /** Instante (ms trusted) en que la cartera llega al TOPE, o null si ya está llena. */
    fun fullAt(wallet: PackWallet, trustedNow: Long): Long? {
        val c = credited(wallet, trustedNow)
        return if (c.balance >= maxPacks) null else c.lastCreditAt + (maxPacks - c.balance) * perPackMs
    }
}
