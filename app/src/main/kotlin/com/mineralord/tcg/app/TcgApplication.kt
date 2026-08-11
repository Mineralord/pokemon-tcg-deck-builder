package com.mineralord.tcg.app

import android.app.Application
import com.mineralord.tcg.data.cloud.CloudSyncRepository
import com.mineralord.tcg.data.cloud.DriveSyncClient
import com.mineralord.tcg.data.cloud.GoogleAuthManager
import com.mineralord.tcg.data.cloud.SyncMetadataStore
import com.mineralord.tcg.data.netfirestore.FirestoreMatchTransportFactory
import com.mineralord.tcg.data.netplay.MatchFactoryProvider
import com.mineralord.tcg.data.netplay.MatchTransportFactory
import com.mineralord.tcg.data.gacha.PackRegen
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.data.profile.TrustedClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Application que aloja el singleton de sincronización en la nube. Sin framework
 * de DI, es el único portador natural de una instancia de proceso. Nota: este
 * [ProfileRepository] y los que crean los ViewModels comparten el mismo archivo
 * DataStore (el delegate es singleton por proceso), así que la auto-subida ve
 * los cambios hechos por cualquier pantalla.
 */
class TcgApplication : Application(), MatchFactoryProvider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var cloudSync: CloudSyncRepository
        private set

    /** Fábrica de transporte para partidas online (Firestore). Singleton de proceso.
     *  PEREZOSO a propósito: crear la fábrica toca `Firebase.firestore`, que exige que
     *  el FirebaseApp por defecto ya esté inicializado (lo hace el ContentProvider de
     *  Firebase tras el constructor de la Application). Si se inicializara aquí como
     *  campo, correría en el constructor —antes de Firebase— y la app crashearía al abrir. */
    override val matchFactory: MatchTransportFactory by lazy { FirestoreMatchTransportFactory() }

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

        // Notificaciones de sobres: canal + reprogramación del aviso del PRÓXIMO sobre según el
        // monedero y el TIEMPO CONFIABLE (no el reloj del teléfono). Cada vez que cambia el monedero
        // (abrir/regenerar), se recalcula el aviso del siguiente sobre.
        PackNotifications.ensureChannel(this)
        val packRepo = ProfileRepository(this)
        val clock = TrustedClock(this)
        val regen = PackRegen()
        appScope.launch {
            clock.sync() // ancla de tiempo real (best-effort; sin red no cambia nada)
            packRepo.wallet.collect {
                PackNotifications.refreshSchedule(this@TcgApplication, packRepo, clock, regen)
            }
        }
    }
}
