package com.mineralord.tcg.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un hueco del binder (carta del set, poseída o no). */
data class SlotUi(
    val number: Int,
    val name: String,
    val rarity: Rarity,
    val owned: Boolean,
    val count: Int,
    val typeLabel: String?,
    /** Código de set de la app (p. ej. "sv1", "svp") para resolver el foil real. */
    val setCode: String,
    /** Imagen en español (null si la carta no tiene versión ES -> punto rojo). */
    val imageEs: String?,
    /** Imagen HD grande para el visor de detalle (fallback a inglés). */
    val imageLarge: String,
) {
    val hasSpanish: Boolean get() = imageEs != null
}

/** Una pestaña del binder: un set (o las energías) con su nombre y serie. */
data class SetTab(val code: String, val name: String, val series: String)

data class CollectionUiState(
    val loading: Boolean = true,
    val tabs: List<SetTab> = emptyList(),
    val selectedTab: Int = 0,
    val ownedInSet: Int = 0,
    val totalInSet: Int = 0,
    val slots: List<SlotUi> = emptyList(),
) {
    val setName: String get() = tabs.getOrNull(selectedTab)?.name ?: ""
    /** El binder del 151 usa su logo dedicado; el resto, texto. */
    val isSet151: Boolean get() = tabs.getOrNull(selectedTab)?.code == "sv3pt5"
}

/**
 * Cartadex multi-set: un binder por cada set presente en las tres barajas de la Academia de
 * Combate 2024 (151 + SV1–SV4 + Promos + Energías). Las cartas de las barajas quedan marcadas
 * como poseídas en su set correspondiente; el resto del set se ve como huecos por conseguir.
 */
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private lateinit var repo: CardRepository
    private val profileRepo = ProfileRepository(app)

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    // Todas las cartas del catálogo, indexadas por set → lista ordenada.
    private var bySet: Map<String, List<Card>> = emptyMap()
    // Colección REAL del jugador (id -> copias), observada del perfil persistente.
    private var owned: Map<String, Int> = emptyMap()

    init {
        viewModelScope.launch {
            repo = withContext(Dispatchers.Default) { CardRepository.load() }
            withContext(Dispatchers.Default) {
                // Agrupa TODAS las cartas del catálogo por su prefijo de set (para pintar el binder
                // completo; solo se marcan como poseídas las que el jugador tiene en su colección).
                bySet = repo.all.groupBy { setCodeOf(it.id.raw) }
            }
            // Siembra idempotente de la colección inicial (por si se abre la Cartadex antes que Sobres).
            profileRepo.seedOnce(StarterDecks.ALL.flatMap { it.expandedCardIds() }.map { it.raw })
            // Observa la colección real: cada cambio (abrir sobres) repinta el binder.
            profileRepo.profile.collect { profile ->
                owned = profile.owned
                rebuild(_state.value.selectedTab)
            }
        }
    }

    /** Cambia el set mostrado. */
    fun selectSet(index: Int) {
        if (index != _state.value.selectedTab) rebuild(index)
    }

    private fun rebuild(index: Int) {
        viewModelScope.launch {
            val (tabs, slots) = withContext(Dispatchers.Default) {
                val tabs = ORDERED_SETS.mapNotNull { code ->
                    val cards = bySet[code].orEmpty()
                    if (cards.isEmpty()) return@mapNotNull null
                    val sample = cards.first()
                    SetTab(code = code, name = sample.set.name.es, series = sample.set.series)
                }
                val sel = index.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
                val code = tabs.getOrNull(sel)?.code
                val slots = bySet[code].orEmpty()
                    .sortedBy { numberOf(it.id.raw) }
                    .map { c -> c.toSlot() }
                tabs to (sel to slots)
            }
            _state.value = CollectionUiState(
                loading = false,
                tabs = tabs,
                selectedTab = slots.first,
                ownedInSet = slots.second.count { it.owned },
                totalInSet = slots.second.size,
                slots = slots.second,
            )
        }
    }

    private fun Card.toSlot(): SlotUi {
        // Una carta se posee si está en la colección REAL del jugador (starters sembrados + sobres).
        val copies = owned[id.raw] ?: 0
        return SlotUi(
            number = numberOf(id.raw),
            name = name.es,
            rarity = rarity,
            owned = copies > 0,
            count = copies,
            typeLabel = (this as? PokemonCard)?.types?.firstOrNull()?.name,
            // OJO: `set.code` guarda el NOMBRE del set (no un código); el código que entiende
            // malie sale del prefijo del id impreso ("svp-106" → "svp").
            setCode = setCodeOf(id.raw),
            imageEs = artwork.smallEs,
            imageLarge = artwork.large(spanish = true),
        )
    }

    private fun numberOf(id: String): Int = id.substringAfterLast('-').toIntOrNull() ?: 0

    /** Prefijo de set de un id impreso: "sv1-164" → "sv1", "energy-basic-…" → "energy". */
    private fun setCodeOf(id: String): String =
        if (id.startsWith("energy")) "energy" else id.substringBeforeLast('-')

    private companion object {
        // Orden de las pestañas: 151 primero, luego las expansiones y promos, energías al final.
        val ORDERED_SETS = listOf("sv3pt5", "sv1", "sv2", "sv3", "sv4", "svp", "energy")
    }
}
