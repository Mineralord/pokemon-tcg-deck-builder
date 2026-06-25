package com.mineralord.tcg.app

import android.app.Application
import com.mineralord.tcg.data.cloud.CloudSyncRepository
import com.mineralord.tcg.data.cloud.DriveSyncClient
import com.mineralord.tcg.data.cloud.GoogleAuthManager
import com.mineralord.tcg.data.cloud.SyncMetadataStore
import com.mineralord.tcg.data.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application que aloja el singleton de sincronización en la nube. Sin framework
 * de DI, es el único portador natural de una instancia de proceso. Nota: este
 * [ProfileRepository] y los que crean los ViewModels comparten el mismo archivo
 * DataStore (el delegate es singleton por proceso), así que la auto-subida ve
 * los cambios hechos por cualquier pantalla.
 */
class TcgApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var cloudSync: CloudSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        cloudSync = CloudSyncRepository(
            profileRepo = ProfileRepository(this),
            auth = GoogleAuthManager(this),
            drive = DriveSyncClient(),
            metadata = SyncMetadataStore(this),
            scope = appScope,
        )
        cloudSync.start()
    }
}
