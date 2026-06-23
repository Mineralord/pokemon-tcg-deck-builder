package com.mineralord.tcg.feature.packs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.gacha.DailyPackLimiter
import com.mineralord.tcg.data.gacha.DailyPackState
import com.mineralord.tcg.data.gacha.OpenAttempt
import com.mineralord.tcg.data.gacha.PackOpener
import com.mineralord.tcg.data.gacha.PackPool
import com.mineralord.tcg.data.gacha.RarityWeights
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Carta revelada para la UI (ya resuelta a nombre + rareza). */
data class RevealedCard(val name: String, val rarity: Rarity, val imageUrl: String?)

/** Estado de la pantalla de sobres (MVI). */
data class PacksUiState(
    val loading: Boolean = true,
    val remainingToday: Int = 0,
    val maxPerDay: Int = 2,
    val revealed: List<RevealedCard> = emptyList(),
    val deniedMessage: String? = null,
    val totalCards: Int = 0,
    /** true mientras debe mostrarse la animación de apertura a pantalla completa. */
    val opening: Boolean = false,
)

/**
 * ViewModel del slice vertical "abrir sobre". Conecta la UI con la lógica pura:
 * carga el catálogo ([CardRepository]), construye el [PackPool], aplica el tope
 * diario ([DailyPackLimiter]) y abre con [PackOpener].
 *
 * Persistencia: de momento el estado diario vive en memoria; se moverá a
 * DataStore en `:data:profile`. El RNG usa System.nanoTime por apertura (en
 * producción se sembrará desde el perfil/servidor para reproducibilidad).
 */
class PacksViewModel : ViewModel() {

    private val limiter = DailyPackLimiter(maxPerDay = 2)
    private var dailyState = DailyPackState()
    private val opener = PackOpener()

    private lateinit var repo: CardRepository
    private lateinit var pool: PackPool

    private val _state = MutableStateFlow(PacksUiState())
    val state: StateFlow<PacksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                val r = CardRepository.load()
                r to PackPool.from(r.all)
            }
            repo = loaded.first
            pool = loaded.second
            _state.value = PacksUiState(
                loading = false,
                remainingToday = limiter.remaining(dailyState, today()),
                maxPerDay = limiter.maxPerDay,
                totalCards = repo.size,
            )
        }
    }

    fun openPack() {
        if (_state.value.loading) return
        when (val attempt = limiter.tryOpen(dailyState, today())) {
            is OpenAttempt.Denied -> {
                _state.value = _state.value.copy(
                    deniedMessage = "Ya abriste tus ${limiter.maxPerDay} sobres de hoy. Vuelve mañana.",
                )
            }
            is OpenAttempt.Allowed -> {
                dailyState = attempt.newState
                val opened = opener.open(RarityWeights.STANDARD_PACK, pool, Random(System.nanoTime()))
                val revealed = opened.map { oc ->
                    val card = repo[oc.id]
                    RevealedCard(
                        name = card?.name?.es ?: oc.id.raw,
                        rarity = oc.rarity,
                        imageUrl = card?.artwork?.small(spanish = true),
                    )
                }
                _state.value = _state.value.copy(
                    remainingToday = attempt.remainingToday,
                    revealed = revealed,
                    deniedMessage = null,
                    opening = true,
                )
            }
        }
    }

    /** Cierra la animación de apertura y deja las cartas en la rejilla. */
    fun dismissOpening() {
        _state.value = _state.value.copy(opening = false)
    }

    /** Día actual como días epoch UTC. */
    private fun today(): Long = System.currentTimeMillis() / 86_400_000L
}
