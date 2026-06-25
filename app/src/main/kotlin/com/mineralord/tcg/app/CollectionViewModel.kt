package com.mineralord.tcg.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.profile.ProfileRepository
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
    /** Imagen en español (null si la carta no tiene versión ES -> punto rojo). */
    val imageEs: String?,
) {
    val hasSpanish: Boolean get() = imageEs != null
}

data class CollectionUiState(
    val loading: Boolean = true,
    val setName: String = "ESCARLATA Y PÚRPURA",
    val setLabel: String = "151",
    val ownedInSet: Int = 0,
    val totalInSet: Int = 0,
    val slots: List<SlotUi> = emptyList(),
)

/** Cartadex: binder del set 151 con las cartas poseídas marcadas. */
class CollectionViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app)
    private lateinit var repo: CardRepository

    private val _state = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo = withContext(Dispatchers.Default) { CardRepository.load() }
            val set151 = withContext(Dispatchers.Default) {
                repo.all.filter { it.id.raw.startsWith(SET_151_PREFIX) }
                    .sortedBy { numberOf(it.id.raw) }
            }
            profileRepo.profile.collect { profile ->
                val slots = withContext(Dispatchers.Default) {
                    set151.map { c ->
                        val count = profile.owned[c.id.raw] ?: 0
                        SlotUi(
                            number = numberOf(c.id.raw),
                            name = c.name.es,
                            rarity = c.rarity,
                            owned = count > 0,
                            count = count,
                            typeLabel = (c as? PokemonCard)?.types?.firstOrNull()?.name,
                            imageEs = c.artwork.smallEs,
                        )
                    }
                }
                _state.value = CollectionUiState(
                    loading = false,
                    ownedInSet = slots.count { it.owned },
                    totalInSet = slots.size,
                    slots = slots,
                )
            }
        }
    }

    private fun numberOf(id: String): Int = id.substringAfterLast('-').toIntOrNull() ?: 0

    private companion object {
        const val SET_151_PREFIX = "sv3pt5-"
    }
}
