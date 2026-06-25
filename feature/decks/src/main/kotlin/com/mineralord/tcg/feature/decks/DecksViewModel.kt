package com.mineralord.tcg.feature.decks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.cards.DeckValidation
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.data.cards.toDeck
import com.mineralord.tcg.data.profile.PlayerProfile
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.Supertype
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una carta de una baraja, resuelta para la UI. */
data class DeckCardUi(val name: String, val count: Int, val imageEs: String?, val supertype: Supertype) {
    val hasSpanish: Boolean get() = imageEs != null
}

/** Una baraja resuelta para la UI (validez, favorito, activa, cartas). */
data class DeckUi(
    val id: String,
    val name: String,
    val type: EnergyType,
    val totalCards: Int,
    val valid: Boolean,
    val invalidReasons: List<String>,
    val isFavorite: Boolean,
    val isActive: Boolean,
    val isCustom: Boolean,
    val updatedAt: Long,
    val cards: List<DeckCardUi>,
)

data class DecksUiState(
    val loading: Boolean = true,
    val decks: List<DeckUi> = emptyList(),
)

/**
 * ViewModel del gestor de barajas. Siembra las 3 barajas starter al primer
 * arranque y observa el perfil persistente. Solo lectura + marcar activa/favorita
 * (la edición llega en la 2ª tanda).
 */
class DecksViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app)
    private lateinit var repo: CardRepository

    private val _state = MutableStateFlow(DecksUiState())
    val state: StateFlow<DecksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo = withContext(Dispatchers.Default) { CardRepository.load() }
            profileRepo.seedDecksOnce(StarterDecks.ALL.map { it.toDeck() })

            profileRepo.profile.collect { profile ->
                val decks = withContext(Dispatchers.Default) {
                    profile.decks.map { it.toUi(profile) }
                }
                _state.value = DecksUiState(loading = false, decks = decks)
            }
        }
    }

    private fun Deck.toUi(profile: PlayerProfile): DeckUi {
        val validity = DeckValidation.validate(this, repo)
        val cards = entries.map { e ->
            val c = repo[e.cardId]
            DeckCardUi(
                name = c?.name?.es ?: e.cardId.raw,
                count = e.count,
                imageEs = c?.artwork?.smallEs,
                supertype = c?.supertype ?: Supertype.TRAINER,
            )
        }
        return DeckUi(
            id = id,
            name = name,
            type = type,
            totalCards = totalCards,
            valid = validity.valid,
            invalidReasons = validity.reasons,
            isFavorite = id in profile.favoriteDeckIds,
            isActive = id == profile.activeDeckId,
            isCustom = isCustom,
            updatedAt = updatedAt,
            cards = cards,
        )
    }

    fun setActive(deckId: String) {
        viewModelScope.launch { profileRepo.setActiveDeck(deckId) }
    }

    fun toggleFavorite(deckId: String) {
        viewModelScope.launch { profileRepo.toggleFavorite(deckId) }
    }

    /** Crea una baraja vacía personalizada y devuelve su id (para abrir el editor). */
    fun createDeck(onCreated: (String) -> Unit) {
        val id = "custom-" + System.currentTimeMillis()
        viewModelScope.launch {
            profileRepo.upsertDeck(
                Deck(
                    id = id,
                    name = "Nueva baraja",
                    headliner = CardId("placeholder"),
                    type = EnergyType.COLORLESS,
                    entries = emptyList(),
                    isCustom = true,
                ),
            )
            onCreated(id)
        }
    }

    /** Duplica una baraja existente con un id nuevo y sufijo "(copia)". */
    fun duplicateDeck(deckId: String) {
        viewModelScope.launch {
            val src = profileRepo.currentDeck(deckId) ?: return@launch
            profileRepo.upsertDeck(
                src.copy(id = "custom-" + System.currentTimeMillis(), name = src.name + " (copia)", isCustom = true),
            )
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch { profileRepo.deleteDeck(deckId) }
    }
}
