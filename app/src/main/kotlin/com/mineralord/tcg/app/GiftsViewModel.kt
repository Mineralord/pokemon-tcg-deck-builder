package com.mineralord.tcg.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.netfirestore.FirestoreFriendsRepository
import com.mineralord.tcg.data.netfirestore.Gift
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de la bandeja de regalos: expone en vivo tus regalos (`users/{me}/gifts`) y los reclama,
 * acreditando las monedas en el [ProfileRepository] (que luego suben a tu nube) y borrando el regalo.
 * Los regalos solo se conceden server-side (Admin SDK); el cliente nunca los crea.
 */
class GiftsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirestoreFriendsRepository()
    private val profileRepo = ProfileRepository(app)

    val gifts: StateFlow<List<Gift>> =
        repo.gifts().catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mensaje transitorio para la UI (p. ej. "¡+50.000 cristales!"). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Reclama [gift]: acredita cada moneda y luego lo borra de la bandeja. */
    fun claim(gift: Gift) = viewModelScope.launch {
        runCatching {
            apply(CurrencyKind.CRISTALES, gift.cristales)
            apply(CurrencyKind.MONEDAS, gift.monedas)
            apply(CurrencyKind.FICHAS, gift.fichas)
            repo.claimGift(gift.id)
        }.onSuccess {
            _message.value = buildString {
                append("¡Reclamado!")
                giftRewardParts(gift).forEach { append(" ").append(it) }
            }
        }.onFailure { _message.value = "No se pudo reclamar el regalo" }
    }

    /** Importe positivo = abono; negativo = retirada administrativa (el saldo no baja de 0). */
    private suspend fun apply(kind: CurrencyKind, amount: Int) {
        if (amount > 0) profileRepo.credit(kind, amount)
        else if (amount < 0) profileRepo.debitClamped(kind, (-amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    fun clearMessage() { _message.value = null }
}

/** Texto de cada importe del regalo ("+50.000 cristales", "−todos los cristales"). */
internal fun giftRewardParts(gift: Gift): List<String> = buildList {
    fun part(n: Int, name: String) {
        when {
            n > 0 -> add("+${"%,d".format(n)} $name")
            n == Int.MIN_VALUE || n <= -1_000_000_000 -> add("−todos los $name")
            n < 0 -> add("−${"%,d".format(-n.toLong())} $name")
        }
    }
    part(gift.cristales, "cristales")
    part(gift.monedas, "monedas")
    part(gift.fichas, "fichas")
}
