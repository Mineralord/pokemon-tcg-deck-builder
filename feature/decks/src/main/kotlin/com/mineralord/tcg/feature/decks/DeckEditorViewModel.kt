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
    val imageLarge: String,
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
    val dirty: Boolean = false,                          // hay cambios sin guardar respecto al estado inicial
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

    /** Snapshot de la baraja al entrar al editor (para descartar cambios). */
    private var original: Deck? = null

    val currentFilter: CardFilter get() = filter.value
    val currentSort: CardSort get() = sort.value

    /** Nº de cartas de la colección que cumplirían un filtro (para "VER N CARTAS"). */
    fun countMatching(f: CardFilter): Int = applyFilterSort(ownedCardsCache, f, CardSort()).size

    fun start(id: String) {
        if (deckId.value == id) return
        deckId.value = id
        original = null   // se captura en el primer build de esta baraja
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
        if (original == null) original = deck   // captura el estado inicial al abrir el editor
        val inDeck: Map<String, Int> = deck.entries.associate { it.cardId.raw to it.count }
        // Copias por NOMBRE en el mazo (sumando distintas versiones/expansiones),
        // para aplicar el límite de 4 por nombre — no por cardId. Ver DeckValidation.
        val deckByName: Map<String, Int> = deck.entries
            .mapNotNull { e -> repo[e.cardId]?.let { it.name.en to e.count } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, counts) -> counts.sum() }

        fun toUi(card: Card, ownedCount: Int): EditorCardUi {
            val cur = inDeck[card.id.raw] ?: 0
            // Energía básica: sin límite. Resto: límite de 4 POR NOMBRE (contando
            // las demás versiones ya presentes) y nunca más de las que posees.
            val canAdd = if (card is BasicEnergy) {
                deck.totalCards < DeckValidation.DECK_SIZE
            } else {
                val nameTotal = deckByName[card.name.en] ?: 0
                deck.totalCards < DeckValidation.DECK_SIZE &&
                    cur < ownedCount &&
                    nameTotal < DeckValidation.MAX_COPIES
            }
            return EditorCardUi(
                id = card.id.raw,
                name = card.name.es,
                imageEs = card.artwork.smallEs,
                imageLarge = card.artwork.large(spanish = true),
                supertype = card.supertype,
                inDeck = cur,
                owned = ownedCount,
                canAdd = canAdd,
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
        val orig = original
        val dirty = orig != null && (deck.entries != orig.entries || deck.name != orig.name)

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
            dirty = dirty,
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

    /** Quita todas las cartas del mazo (botón "Quitar todas" del selector). */
    fun clearDeck() = mutate { entries -> entries.clear() }

    /**
     * Autocreación: rellena el mazo hasta [DeckValidation.DECK_SIZE] con energías
     * básicas de los [types] elegidos (1 o 2), repartidas de forma equilibrada
     * (round-robin). No toca las cartas ya presentes; sólo añade energía. Si el mazo
     * ya está lleno o no hay energía básica de esos tipos en el catálogo, no hace nada.
     * Devuelve por [onResult] cuántas energías añadió.
     */
    fun autoComplete(types: Set<EnergyType>, onResult: (Int) -> Unit) {
        val id = state.value.deckId
        viewModelScope.launch {
            val deck = profileRepo.currentDeck(id) ?: run { onResult(0); return@launch }
            val deficit = DeckValidation.DECK_SIZE - deck.totalCards
            if (deficit <= 0 || types.isEmpty()) { onResult(0); return@launch }

            // Una carta de energía básica por tipo elegido (la primera del catálogo).
            val energyByType: Map<EnergyType, CardId> = types.mapNotNull { t ->
                (repo.all.firstOrNull { it is BasicEnergy && it.type == t } as? BasicEnergy)?.let { t to it.id }
            }.toMap()
            if (energyByType.isEmpty()) { onResult(0); return@launch }

            val order = types.filter { it in energyByType }
            val entries = deck.entries.toMutableList()
            var added = 0
            while (added < deficit) {
                val cid = energyByType.getValue(order[added % order.size])
                val idx = entries.indexOfFirst { it.cardId == cid }
                if (idx >= 0) entries[idx] = entries[idx].copy(count = entries[idx].count + 1)
                else entries += DeckEntry(cid, 1)
                added++
            }
            val type = dominantType(entries, repo)
            val headliner = entries.firstOrNull { (repo[it.cardId] as? PokemonCard) != null }?.cardId ?: deck.headliner
            profileRepo.upsertDeck(deck.copy(entries = entries, type = type, headliner = headliner))
            onResult(added)
        }
    }

    fun rename(newName: String) {
        val deckId = state.value.deckId
        viewModelScope.launch {
            val deck = profileRepo.currentDeck(deckId) ?: return@launch
            profileRepo.upsertDeck(deck.copy(name = newName.ifBlank { deck.name }))
        }
    }

    /** Descarta los cambios de la sesión restaurando la baraja a su estado inicial. */
    fun discardChanges(onDone: () -> Unit) {
        val orig = original
        if (orig == null) { onDone(); return }
        viewModelScope.launch {
            profileRepo.upsertDeck(orig)
            onDone()
        }
    }

    /** Crea una copia de la baraja actual (id nuevo, sufijo "(copia)"). */
    fun duplicateDeck() {
        val id = state.value.deckId
        viewModelScope.launch {
            val src = profileRepo.currentDeck(id) ?: return@launch
            profileRepo.upsertDeck(
                src.copy(id = "custom-" + System.currentTimeMillis(), name = src.name + " (copia)", isCustom = true),
            )
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
