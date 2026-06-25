package com.mineralord.tcg.feature.decks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardFilter
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.CardSort
import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.cards.DeckEntry
import com.mineralord.tcg.data.cards.DeckValidation
import com.mineralord.tcg.data.cards.applyFilterSort
import com.mineralord.tcg.data.cards.dominantType
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Supertype
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Carta resuelta para las rejillas del editor (mazo o colección). */
data class EditorCardUi(
    val id: String,
    val name: String,
    val imageEs: String?,
    val supertype: Supertype,
    val inDeck: Int,
    val owned: Int,
    val canAdd: Boolean,
) {
    val hasSpanish: Boolean get() = imageEs != null
}

data class EditorUiState(
    val loading: Boolean = true,
    val exists: Boolean = true,
    val deckId: String = "",
    val name: String = "",
    val type: EnergyType = EnergyType.COLORLESS,
    val total: Int = 0,
    val valid: Boolean = false,
    val reasons: List<String> = emptyList(),
    val pokemonCount: Int = 0,
    val trainerCount: Int = 0,
    val energyCount: Int = 0,
    val deckCards: List<EditorCardUi> = emptyList(),     // cartas en el mazo (inDeck>0)
    val collection: List<EditorCardUi> = emptyList(),    // colección poseída, filtrada/ordenada
    val availableExpansions: List<String> = emptyList(), // códigos (= nombres) presentes en la colección
)

/**
 * ViewModel del editor de una baraja. Observa el perfil persistente y la
 * colección poseída; cada add/remove recalcula el mazo y lo persiste vía
 * [ProfileRepository.upsertDeck]. La validez y los conteos se derivan en vivo.
 */
class DeckEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app)
    private lateinit var repo: CardRepository

    private val deckId = MutableStateFlow("")
    private val filter = MutableStateFlow(CardFilter())
    private val sort = MutableStateFlow(CardSort())

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var ownedCardsCache: List<Card> = emptyList()

    val currentFilter: CardFilter get() = filter.value
    val currentSort: CardSort get() = sort.value

    /** Nº de cartas de la colección que cumplirían un filtro (para "VER N CARTAS"). */
    fun countMatching(f: CardFilter): Int = applyFilterSort(ownedCardsCache, f, CardSort()).size

    fun start(id: String) {
        if (deckId.value == id) return
        deckId.value = id
        viewModelScope.launch {
            if (!::repo.isInitialized) repo = withContext(Dispatchers.Default) { CardRepository.load() }
            combine(profileRepo.profile, deckId, filter, sort) { profile, id2, f, s ->
                Quad(profile, id2, f, s)
            }.collect { (profile, id2, f, s) ->
                val deck = profile.decks.firstOrNull { it.id == id2 }
                if (deck == null) {
                    _state.value = EditorUiState(loading = false, exists = false, deckId = id2)
                    return@collect
                }
                _state.value = withContext(Dispatchers.Default) { build(deck, profile.owned, f, s) }
            }
        }
    }

    private fun build(deck: Deck, owned: Map<String, Int>, f: CardFilter, s: CardSort): EditorUiState {
        val inDeck: Map<String, Int> = deck.entries.associate { it.cardId.raw to it.count }

        fun toUi(card: Card, ownedCount: Int): EditorCardUi {
            val cur = inDeck[card.id.raw] ?: 0
            val cap = if (card is BasicEnergy) Int.MAX_VALUE else minOf(DeckValidation.MAX_COPIES, ownedCount)
            return EditorCardUi(
                id = card.id.raw,
                name = card.name.es,
                imageEs = card.artwork.smallEs,
                supertype = card.supertype,
                inDeck = cur,
                owned = ownedCount,
                canAdd = deck.totalCards < DeckValidation.DECK_SIZE && cur < cap,
            )
        }

        // Cartas del mazo (en orden de entries).
        val deckCards = deck.entries.mapNotNull { e -> repo[e.cardId]?.let { toUi(it, owned[e.cardId.raw] ?: 0) } }

        // Colección poseída resuelta y filtrada/ordenada.
        val ownedCards = owned.entries.mapNotNull { (id, n) -> repo[CardId(id)]?.let { it to n } }
        ownedCardsCache = ownedCards.map { it.first }
        val filteredSorted = applyFilterSort(ownedCards.map { it.first }, f, s)
        val ownedCountById = ownedCards.associate { it.first.id.raw to it.second }
        val collection = filteredSorted.map { toUi(it, ownedCountById[it.id.raw] ?: 0) }

        val validity = DeckValidation.validate(deck, repo)
        val expansions = ownedCards.map { it.first.set.code }.distinct().sorted()

        return EditorUiState(
            loading = false,
            exists = true,
            deckId = deck.id,
            name = deck.name,
            type = deck.type,
            total = deck.totalCards,
            valid = validity.valid,
            reasons = validity.reasons,
            pokemonCount = countBy(deck, Supertype.POKEMON),
            trainerCount = countBy(deck, Supertype.TRAINER),
            energyCount = countBy(deck, Supertype.ENERGY),
            deckCards = deckCards,
            collection = collection,
            availableExpansions = expansions,
        )
    }

    private fun countBy(deck: Deck, st: Supertype): Int =
        deck.entries.filter { repo[it.cardId]?.supertype == st }.sumOf { it.count }

    // --- Mutaciones (persisten vía upsert; el Flow re-emite y refresca la UI) ---

    fun addCard(cardId: String) = mutate { entries ->
        val i = entries.indexOfFirst { it.cardId.raw == cardId }
        if (i >= 0) entries[i] = entries[i].copy(count = entries[i].count + 1)
        else entries += DeckEntry(CardId(cardId), 1)
    }

    fun removeCard(cardId: String) = mutate { entries ->
        val i = entries.indexOfFirst { it.cardId.raw == cardId }
        if (i >= 0) {
            val c = entries[i].count - 1
            if (c <= 0) entries.removeAt(i) else entries[i] = entries[i].copy(count = c)
        }
    }

    fun rename(newName: String) {
        val deckId = state.value.deckId
        viewModelScope.launch {
            val deck = profileRepo.currentDeck(deckId) ?: return@launch
            profileRepo.upsertDeck(deck.copy(name = newName.ifBlank { deck.name }))
        }
    }

    fun deleteDeck(onDone: () -> Unit) {
        val id = state.value.deckId
        viewModelScope.launch {
            profileRepo.deleteDeck(id)
            onDone()
        }
    }

    private fun mutate(block: (MutableList<DeckEntry>) -> Unit) {
        val id = state.value.deckId
        viewModelScope.launch {
            val deck = profileRepo.currentDeck(id) ?: return@launch
            val entries = deck.entries.toMutableList()
            block(entries)
            val type = dominantType(entries, repo)
            val headliner = entries.firstOrNull { (repo[it.cardId] as? PokemonCard) != null }?.cardId ?: deck.headliner
            profileRepo.upsertDeck(deck.copy(entries = entries, type = type, headliner = headliner))
        }
    }

    fun setFilter(f: CardFilter) { filter.value = f }
    fun setSort(s: CardSort) { sort.value = s }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
