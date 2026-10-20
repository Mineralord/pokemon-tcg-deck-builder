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
            if (gift.cristales > 0) profileRepo.credit(CurrencyKind.CRISTALES, gift.cristales)
            if (gift.monedas > 0) profileRepo.credit(CurrencyKind.MONEDAS, gift.monedas)
            if (gift.fichas > 0) profileRepo.credit(CurrencyKind.FICHAS, gift.fichas)
            repo.claimGift(gift.id)
        }.onSuccess {
            _message.value = buildString {
                append("¡Reclamado!")
                if (gift.cristales > 0) append(" +${"%,d".format(gift.cristales)} cristales")
                if (gift.monedas > 0) append(" +${"%,d".format(gift.monedas)} monedas")
                if (gift.fichas > 0) append(" +${"%,d".format(gift.fichas)} fichas")
            }
        }.onFailure { _message.value = "No se pudo reclamar el regalo" }
    }

    fun clearMessage() { _message.value = null }
}
