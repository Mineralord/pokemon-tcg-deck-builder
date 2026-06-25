package com.mineralord.tcg.data.cloud

import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.cards.DeckEntry
import com.mineralord.tcg.data.gacha.DailyPackState
import com.mineralord.tcg.data.profile.PlayerProfile
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EnergyType
import kotlinx.serialization.Serializable

/**
 * Representación serializable del [PlayerProfile] completo: es el JSON que se
 * sube/baja de la carpeta privada (appDataFolder) del Google Drive del usuario.
 * Reutiliza el patrón DTO de [com.mineralord.tcg.data.profile.ProfileRepository]
 * (ids y enums como String) para evitar acoplarse a sus tipos privados.
 */
@Serializable
data class ProfileSnapshotDto(
    val schemaVersion: Int = 1,
    val lastModified: Long = 0L,
    val owned: Map<String, Int> = emptyMap(),
    val dailyDayId: Long = 0L,
    val dailyOpenedToday: Int = 0,
    val seeded: Boolean = false,
    val activeDeckId: String? = null,
    val favoriteDeckIds: List<String> = emptyList(),
    val decks: List<DeckSnapshotDto> = emptyList(),
)

@Serializable
data class DeckSnapshotDto(
    val id: String,
    val name: String,
    val headliner: String,
    val type: String,
    val entries: List<EntrySnapshotDto> = emptyList(),
    val isCustom: Boolean = false,
    val updatedAt: Long = 0L,
)

@Serializable
data class EntrySnapshotDto(val cardId: String, val count: Int)

fun PlayerProfile.toSnapshot(): ProfileSnapshotDto = ProfileSnapshotDto(
    lastModified = lastModified,
    owned = owned,
    dailyDayId = daily.dayId,
    dailyOpenedToday = daily.openedToday,
    seeded = seeded,
    activeDeckId = activeDeckId,
    favoriteDeckIds = favoriteDeckIds.toList(),
    decks = decks.map { d ->
        DeckSnapshotDto(
            id = d.id,
            name = d.name,
            headliner = d.headliner.raw,
            type = d.type.name,
            entries = d.entries.map { EntrySnapshotDto(it.cardId.raw, it.count) },
            isCustom = d.isCustom,
            updatedAt = d.updatedAt,
        )
    },
)

fun ProfileSnapshotDto.toProfile(): PlayerProfile = PlayerProfile(
    owned = owned,
    daily = DailyPackState(dayId = dailyDayId, openedToday = dailyOpenedToday),
    seeded = seeded,
    decks = decks.map { d ->
        Deck(
            id = d.id,
            name = d.name,
            headliner = CardId(d.headliner),
            type = EnergyType.valueOf(d.type),
            entries = d.entries.map { DeckEntry(CardId(it.cardId), it.count) },
            isCustom = d.isCustom,
            updatedAt = d.updatedAt,
        )
    },
    activeDeckId = activeDeckId,
    favoriteDeckIds = favoriteDeckIds.toSet(),
    lastModified = lastModified,
)
