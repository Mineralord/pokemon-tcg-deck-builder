package com.mineralord.tcg.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una entrada de la colección para la UI. */
data class OwnedCardUi(
    val name: String,
    val count: Int,
    val rarity: Rarity,
    val setCode: String,
)

data class CollectionUiState(
    val loading: Boolean = true,
    val distinct: Int = 0,
    val total: Int = 0,
    val cards: List<OwnedCardUi> = emptyList(),
)

/** Muestra las cartas poseídas (perfil persistente) resueltas a nombre/rareza. */
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app)
    private lateinit var repo: CardRepository

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo = withContext(Dispatchers.Default) { CardRepository.load() }
            profileRepo.profile.collect { profile ->
                val cards = withContext(Dispatchers.Default) {
                    profile.owned.entries
                        .mapNotNull { (id, count) ->
                            repo[CardId(id)]?.let { c ->
                                OwnedCardUi(c.name.es, count, c.rarity, c.set.code)
                            }
                        }
                        .sortedWith(compareByDescending<OwnedCardUi> { it.rarity.ordinal }.thenBy { it.name })
                }
                _state.value = CollectionUiState(
                    loading = false,
                    distinct = profile.distinctOwned,
                    total = profile.totalOwned,
                    cards = cards,
                )
            }
        }
    }
}
