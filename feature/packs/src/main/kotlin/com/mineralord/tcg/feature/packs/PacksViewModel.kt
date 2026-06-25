package com.mineralord.tcg.feature.packs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.gacha.DailyPackLimiter
import com.mineralord.tcg.data.gacha.DailyPackState
import com.mineralord.tcg.data.gacha.OpenAttempt
import com.mineralord.tcg.data.gacha.PackOpener
import com.mineralord.tcg.data.gacha.PackPool
import com.mineralord.tcg.data.gacha.RarityWeights
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Carta revelada para la UI (ya resuelta a nombre + rareza). */
data class RevealedCard(
    val name: String,
    val rarity: Rarity,
    val imageUrl: String?,
    /** Si la carta no estaba en la colección antes de este sobre. */
    val isNew: Boolean = false,
    /** Copias poseídas tras añadir esta carta (para "COPIAS EN LA COLECCIÓN n/4"). */
    val copiesOwned: Int = 0,
)

/** Estado de la pantalla de sobres (MVI). */
data class PacksUiState(
    val loading: Boolean = true,
    val remainingToday: Int = 0,
    val maxPerDay: Int = 2,
    val revealed: List<RevealedCard> = emptyList(),
    val deniedMessage: String? = null,
    val totalCards: Int = 0,
    val opening: Boolean = false,
    val ownedDistinct: Int = 0,
    val setLabel: String = "Escarlata y Púrpura · 151",
)

/**
 * ViewModel del slice de sobres, ahora con perfil persistente ([ProfileRepository]).
 * Al primer arranque siembra las cartas de los 3 mazos desbloqueados; el tope
 * diario y la colección sobreviven al cierre de la app.
 */
class PacksViewModel(app: Application) : AndroidViewModel(app) {

    private val limiter = DailyPackLimiter(maxPerDay = 2)
    private val opener = PackOpener()
    private val profileRepo = ProfileRepository(app)

    private lateinit var repo: CardRepository
    private lateinit var pool: PackPool
    private var daily: DailyPackState = DailyPackState()
    private var owned: Map<String, Int> = emptyMap()

    private val _state = MutableStateFlow(PacksUiState())
    val state: StateFlow<PacksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                val r = CardRepository.load()
                val pool151 = r.all.filter { it.id.raw.startsWith(SET_151_PREFIX) }
                r to PackPool.from(pool151)
            }
            repo = loaded.first
            pool = loaded.second

            // Siembra la colección inicial con las cartas de los 3 mazos.
            val seed = StarterDecks.ALL.flatMap { it.expandedCardIds() }.map { it.raw }
            profileRepo.seedOnce(seed)

            // Observa el perfil persistente.
            launch {
                profileRepo.profile.collect { profile ->
                    daily = profile.daily
                    owned = profile.owned
                    _state.value = _state.value.copy(
                        loading = false,
                        remainingToday = limiter.remaining(profile.daily, today()),
                        maxPerDay = limiter.maxPerDay,
                        totalCards = pool.totalCards,
                        ownedDistinct = profile.distinctOwned,
                    )
                }
            }
        }
    }

    fun openPack() {
        if (_state.value.loading) return
        when (val attempt = limiter.tryOpen(daily, today())) {
            is OpenAttempt.Denied -> {
                _state.value = _state.value.copy(
                    deniedMessage = "Ya abriste tus ${limiter.maxPerDay} sobres de hoy. Vuelve mañana.",
                )
            }
            is OpenAttempt.Allowed -> {
                daily = attempt.newState
                val opened = opener.open(RarityWeights.STANDARD_PACK, pool, Random(System.nanoTime()))
                // Recuento acumulado para "n/4" e "isNew", contando duplicados del mismo sobre.
                val running = HashMap<String, Int>()
                val revealed = opened.map { oc ->
                    val card = repo[oc.id]
                    val before = (owned[oc.id.raw] ?: 0) + (running[oc.id.raw] ?: 0)
                    running[oc.id.raw] = (running[oc.id.raw] ?: 0) + 1
                    RevealedCard(
                        name = card?.name?.es ?: oc.id.raw,
                        rarity = oc.rarity,
                        imageUrl = card?.artwork?.smallEs,   // solo español; null -> punto rojo
                        isNew = before == 0,
                        copiesOwned = before + 1,
                    )
                }
                _state.value = _state.value.copy(
                    remainingToday = attempt.remainingToday,
                    revealed = revealed,
                    deniedMessage = null,
                    opening = true,
                )
                // Persiste el tope diario y añade las cartas a la colección.
                viewModelScope.launch {
                    profileRepo.setDaily(attempt.newState)
                    profileRepo.addCards(opened.map { it.id.raw })
                }
            }
        }
    }

    /** Cierra la animación de apertura y deja las cartas en la rejilla. */
    fun dismissOpening() {
        _state.value = _state.value.copy(opening = false)
    }

    private fun today(): Long = System.currentTimeMillis() / 86_400_000L

    private companion object {
        const val SET_151_PREFIX = "sv3pt5-"
    }
}
