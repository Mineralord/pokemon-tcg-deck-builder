package com.mineralord.tcg.data.cloud

import com.mineralord.tcg.data.profile.PlayerProfile

/** Qué hacer tras comparar el perfil local con el de la nube. */
sealed interface Resolution {
    /** Aplicar la nube en local (sobrescribe con [profile]). */
    data class ImportCloud(val profile: PlayerProfile) : Resolution

    /** Subir el local a la nube (con [snapshot]). */
    data class PushLocal(val snapshot: ProfileSnapshotDto) : Resolution

    /** Ambos lados ya coinciden; no hacer nada. */
    data object Noop : Resolution
}

/**
 * Resuelve conflictos local↔nube. Función pura y testeable.
 *
 * - Sin archivo en la nube → subir el local.
 * - [preferCloudWhenExists] (típico al **iniciar sesión**): si hay datos en la
 *   nube se importan siempre (restauración autoritativa); evita que el sembrado
 *   recién hecho en un dispositivo nuevo pise los datos reales de la nube.
 * - En otro caso → last-write-wins por [PlayerProfile.lastModified].
 */
object MergeStrategy {
    fun resolve(
        local: PlayerProfile,
        cloud: ProfileSnapshotDto?,
        preferCloudWhenExists: Boolean,
    ): Resolution {
        if (cloud == null) return Resolution.PushLocal(local.toSnapshot())
        if (preferCloudWhenExists) return Resolution.ImportCloud(cloud.toProfile())
        return when {
            cloud.lastModified > local.lastModified -> Resolution.ImportCloud(cloud.toProfile())
            local.lastModified > cloud.lastModified -> Resolution.PushLocal(local.toSnapshot())
            else -> Resolution.Noop
        }
    }
}
