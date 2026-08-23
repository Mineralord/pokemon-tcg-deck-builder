package com.mineralord.tcg.data.cloud

import com.mineralord.tcg.data.cards.Deck
import com.mineralord.tcg.data.profile.PlayerProfile
import kotlin.math.max

/** Qué hacer tras comparar el perfil local con el de la nube. */
sealed interface Resolution {
    /** Subir el local a la nube (con [snapshot]). */
    data class PushLocal(val snapshot: ProfileSnapshotDto) : Resolution

    /**
     * Fusión aditiva local↔nube: aplicar [profile] en local Y volver a subirlo
     * para que ambos lados converjan al conjunto unido. Nunca pierde cartas.
     */
    data class Merge(val profile: PlayerProfile) : Resolution

    /** Ambos lados ya coinciden; no hacer nada. */
    data object Noop : Resolution
}

/**
 * Resuelve conflictos local↔nube. Función pura y testeable.
 *
 * Preservación Absoluta (Canon 0.23/0.06) + Reversibilidad (0.19): la colección
 * es patrimonio permanente del jugador, por lo que la fusión es **aditiva** y
 * jamás sustituye un conjunto por otro perdiendo cartas.
 *
 * - Sin archivo en la nube → subir el local.
 * - Con archivo en la nube → [Resolution.Merge] con la unión de ambos lados
 *   (ver [merge]). [preferCloudWhenExists] (típico al **iniciar sesión**) solo
 *   decide de qué lado se toma el estado de consumo (límite diario, baraja
 *   activa); la colección siempre se une.
 */
object MergeStrategy {
    fun resolve(
        local: PlayerProfile,
        cloud: ProfileSnapshotDto?,
        preferCloudWhenExists: Boolean,
    ): Resolution {
        if (cloud == null) return Resolution.PushLocal(local.toSnapshot())
        val merged = merge(local, cloud.toProfile(), preferCloudScalars = preferCloudWhenExists)
        // Si la fusión ya coincide con ambos lados, no hay nada que aplicar ni subir.
        if (merged == local && merged == cloud.toProfile()) return Resolution.Noop
        return Resolution.Merge(merged)
    }

    /**
     * Une dos perfiles del MISMO jugador de forma no destructiva:
     * - `owned`: máximo de copias por carta (la colección nunca decrece).
     * - `decks`: unión por id, conservando el de `updatedAt` más reciente.
     * - `favoriteDeckIds`: unión.
     * - `ownedCosmetics`: unión (patrimonio permanente, nunca se pierde un cosmético).
     * - `seeded`: OR lógico.
     * - Estado de consumo (`daily`, `activeDeckId`) y `lastModified`: del lado
     *   más reciente, o de la nube si [preferCloudScalars].
     */
    fun merge(local: PlayerProfile, cloud: PlayerProfile, preferCloudScalars: Boolean): PlayerProfile {
        val ownedUnion = HashMap<String, Int>(local.owned)
        cloud.owned.forEach { (id, n) -> ownedUnion[id] = max(ownedUnion[id] ?: 0, n) }

        val decksById = LinkedHashMap<String, Deck>()
        (local.decks + cloud.decks).forEach { d ->
            val prev = decksById[d.id]
            if (prev == null || d.updatedAt >= prev.updatedAt) decksById[d.id] = d
        }

        val base = if (preferCloudScalars || cloud.lastModified >= local.lastModified) cloud else local
        val activeDeckId = base.activeDeckId?.takeIf { id -> decksById.containsKey(id) }
            ?: decksById.keys.firstOrNull()

        return base.copy(
            owned = ownedUnion,
            decks = decksById.values.toList(),
            favoriteDeckIds = local.favoriteDeckIds + cloud.favoriteDeckIds,
            ownedCosmetics = local.ownedCosmetics + cloud.ownedCosmetics,
            favoriteCosmetics = local.favoriteCosmetics + cloud.favoriteCosmetics,
            seeded = local.seeded || cloud.seeded,
            activeDeckId = activeDeckId,
            lastModified = max(local.lastModified, cloud.lastModified),
        )
    }
}
