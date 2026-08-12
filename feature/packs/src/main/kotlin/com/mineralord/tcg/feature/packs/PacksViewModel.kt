package com.mineralord.tcg.feature.packs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.gacha.PackOpener
import com.mineralord.tcg.data.gacha.PackPool
import com.mineralord.tcg.data.gacha.RarityWeights
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** Copias poseídas tras añadir esta carta (para "COPIAS EN LA COLECCIÓN n/tope"). */
    val copiesOwned: Int = 0,
    /** Tope de copias de esta carta (4 normal · 30 por variante de Energía Básica). */
    val cap: Int = 4,
    // Datos para abrir el MISMO visor de la colección (holo + manipulación con el dedo) al tocarla.
    val imageLarge: String? = null,
    val cardNumber: Int? = null,
    val setCode: String = "sv3pt5",
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
    /** Segundos (tiempo CONFIABLE) hasta el próximo sobre; null si está en el tope. */
    val secondsToNext: Long? = null,
    /** Progreso de colección del set (cartas distintas poseídas / total del set) — como TCG Pocket. */
    val ownedInSet: Int = 0,
    val totalInSet: Int = 0,
)

/**
 * ViewModel del slice de sobres, ahora con perfil persistente ([ProfileRepository]).
 * Al primer arranque siembra las cartas de los 3 mazos desbloqueados; el tope
 * diario y la colección sobreviven al cierre de la app.
 */
class PacksViewModel(app: Application) : AndroidViewModel(app) {

    private val regen = com.mineralord.tcg.data.gacha.PackRegen()
    private val clock = com.mineralord.tcg.data.profile.TrustedClock(app)
    private val opener = PackOpener()
    private val profileRepo = ProfileRepository(app)

    private lateinit var repo: CardRepository
    private lateinit var pool: PackPool
    private var energyIds: List<com.mineralord.tcg.engine.model.CardId> = emptyList()
    private var wallet: com.mineralord.tcg.data.gacha.PackWallet? = null
    private var owned: Map<String, Int> = emptyMap()
    private var totalInSet: Int = 0

    private val _state = MutableStateFlow(PacksUiState())
    val state: StateFlow<PacksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                val r = CardRepository.load()
                // Bolsa de rarezas: SOLO las cartas numeradas del 151 (sin energías, que van por su
                // propio slot). Energías Básicas: todas las variantes del catálogo (una por tipo).
                val pool151 = r.all.filter {
                    it.id.raw.startsWith(SET_151_PREFIX) && !ProfileRepository.isEnergyId(it.id.raw)
                }
                val energies = r.all.filter { ProfileRepository.isEnergyId(it.id.raw) }.map { it.id }
                Triple(r, PackPool.from(pool151), energies)
            }
            repo = loaded.first
            pool = loaded.second
            energyIds = loaded.third
            totalInSet = repo.all.count { it.id.raw.startsWith(SET_151_PREFIX) }

            // Siembra la colección inicial con las cartas de los 3 mazos.
            val seed = StarterDecks.ALL.flatMap { it.expandedCardIds() }.map { it.raw }
            profileRepo.seedOnce(seed)
            // Siembra el monedero de sobres en el primer uso: lleno, anclado al tiempo CONFIABLE.
            if (profileRepo.wallet.first() == null) {
                profileRepo.saveWallet(com.mineralord.tcg.data.gacha.PackWallet(regen.maxPacks, clock.nowMs()))
            }

            // Observa colección y monedero persistentes.
            launch { profileRepo.profile.collect { owned = it.owned; refreshUi() } }
            launch { profileRepo.wallet.collect { w -> wallet = w; refreshUi() } }
        }
    }

    /** Recalcula el saldo disponible (con la regeneración acreditada al tiempo confiable). */
    private suspend fun refreshUi() {
        val w = wallet ?: return
        val now = clock.nowMs()
        val credited = regen.credited(w, now)
        val nextAt = regen.nextPackAt(credited, now)
        _state.value = _state.value.copy(
            loading = false,
            remainingToday = credited.balance,
            maxPerDay = regen.maxPacks,
            totalCards = pool.totalCards,
            ownedDistinct = owned.size,
            secondsToNext = nextAt?.let { ((it - now) / 1000).coerceAtLeast(0) },
            ownedInSet = owned.keys.count { it.startsWith(SET_151_PREFIX) },
            totalInSet = totalInSet,
        )
    }

    /** Recalcula el saldo/cuenta atrás (la UI lo llama al agotarse el temporizador local). */
    fun refresh() {
        viewModelScope.launch { refreshUi() }
    }

    fun openPack() {
        if (_state.value.loading) return
        viewModelScope.launch {
            val w = wallet ?: return@launch
            when (val attempt = regen.tryOpen(w, clock.nowMs())) {
                is com.mineralord.tcg.data.gacha.PackOpenResult.Denied -> {
                    _state.value = _state.value.copy(
                        deniedMessage = "No tienes sobres disponibles ahora. Regeneras 1 cada 12 h (máx. ${regen.maxPacks}).",
                    )
                }
                is com.mineralord.tcg.data.gacha.PackOpenResult.Allowed -> {
                    // Persiste el monedero (dispara la reprogramación del aviso vía el flujo).
                    profileRepo.saveWallet(attempt.wallet)
                    wallet = attempt.wallet
                    // Sobre FIEL de 151 (10 cartas: 9 numeradas + 1 Energía Básica). Energías por su slot.
                    val opened = opener.open(
                        RarityWeights.templateFor(SET_151_CODE), pool, Random(System.nanoTime()), energyIds,
                    )
                    // Recuento acumulado para "n/tope" e "isNew", respetando el tope por carta (4 · 30).
                    val running = HashMap<String, Int>()
                    val revealed = opened.map { oc ->
                        val card = repo[oc.id]
                        val cap = ProfileRepository.capFor(oc.id.raw)
                        val before = ((owned[oc.id.raw] ?: 0) + (running[oc.id.raw] ?: 0)).coerceAtMost(cap)
                        running[oc.id.raw] = (running[oc.id.raw] ?: 0) + 1
                        val idRaw = oc.id.raw
                        RevealedCard(
                            name = card?.name?.es ?: idRaw,
                            rarity = oc.rarity,
                            imageUrl = card?.artwork?.smallEs,   // solo español; null -> punto rojo
                            isNew = before == 0,
                            copiesOwned = (before + 1).coerceAtMost(cap),
                            cap = cap,
                            imageLarge = card?.artwork?.large(true),
                            cardNumber = idRaw.substringAfterLast('-').toIntOrNull(),
                            setCode = if (idRaw.startsWith("energy")) "energy" else idRaw.substringBeforeLast('-'),
                        )
                    }
                    _state.value = _state.value.copy(
                        remainingToday = attempt.remaining,
                        revealed = revealed,
                        deniedMessage = null,
                        opening = true,
                    )
                    profileRepo.addCards(opened.map { it.id.raw })
                }
            }
        }
    }

    /** Cierra la animación de apertura y deja las cartas en la rejilla. */
    fun dismissOpening() {
        _state.value = _state.value.copy(opening = false)
    }

    private companion object {
        const val SET_151_PREFIX = "sv3pt5-"
        const val SET_151_CODE = "sv3pt5"
    }
}
