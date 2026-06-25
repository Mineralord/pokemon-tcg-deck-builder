package com.mineralord.tcg.data.cloud

import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import com.mineralord.tcg.data.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Estado de la sincronización con la nube (para la UI). */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val atMillis: Long) : SyncStatus
    data class Error(val cause: Throwable) : SyncStatus
}

/**
 * Coordina autenticación + Drive + estrategia de fusión. Singleton de proceso
 * (lo crea [TcgApplication]). Observa el perfil y sube cambios con debounce;
 * baja/restaura al iniciar sesión o al reabrir la app.
 */
class CloudSyncRepository(
    private val profileRepo: ProfileRepository,
    private val auth: GoogleAuthManager,
    private val drive: DriveSyncClient,
    private val metadata: SyncMetadataStore,
    private val scope: CoroutineScope,
) {

    val authState: StateFlow<AuthState> = auth.authState
    val lastSynced get() = metadata.lastSynced

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    @Volatile private var applyingRemote = false
    @Volatile private var currentToken: String? = null

    @OptIn(FlowPreview::class)
    fun start() {
        // 1) Restaura una sesión previa en silencio y resincroniza.
        scope.launch {
            val session = metadata.getSession() ?: return@launch
            runCatching {
                currentToken = auth.authorizeDriveScope(launcher = null)
                auth.restoreSession(AuthState.SignedIn(session.name, session.email, session.avatarUrl))
                syncNow(preferCloud = false)
            }.onFailure {
                // Token revocado o sin conexión: el usuario reiniciará sesión manualmente.
                _syncStatus.value = SyncStatus.Idle
            }
        }
        // 2) Auto-subida (con debounce) ante cambios del perfil.
        scope.launch {
            profileRepo.profile
                .drop(1)
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { profile ->
                    if (auth.authState.value is AuthState.SignedIn && !applyingRemote) {
                        uploadSafe(profile.toSnapshot())
                    }
                }
        }
    }

    /** Inicia sesión, autoriza Drive y restaura datos de la nube si existen. */
    suspend fun signIn(activity: Activity, launcher: (IntentSenderRequest) -> Unit) {
        _syncStatus.value = SyncStatus.Syncing
        runCatching {
            val account = auth.signIn(activity)
            currentToken = auth.authorizeDriveScope(launcher)
            metadata.setSession(account.email, account.name, account.avatarUrl)
            syncNow(preferCloud = true)
        }.onFailure { _syncStatus.value = SyncStatus.Error(it) }
    }

    /** Reenvía el resultado del consentimiento de Drive al gestor de auth. */
    fun onAuthorizationResult(data: android.content.Intent?) = auth.onAuthorizationResult(data)

    suspend fun signOut() {
        auth.signOut()
        currentToken = null
        metadata.clear()
        _syncStatus.value = SyncStatus.Idle
    }

    /** Sincronización completa: descarga, resuelve conflicto y aplica/sube. */
    suspend fun syncNow(preferCloud: Boolean) {
        val token = currentToken ?: runCatching { auth.authorizeDriveScope(null) }
            .getOrElse { _syncStatus.value = SyncStatus.Error(it); return }
            .also { currentToken = it }
        _syncStatus.value = SyncStatus.Syncing
        try {
            val local = profileRepo.profile.first()
            val fileId = metadata.getFileId() ?: drive.findProfileFileId(token)?.also { metadata.setFileId(it) }
            val cloud = if (fileId != null) drive.download(token, fileId) else null
            when (val r = MergeStrategy.resolve(local, cloud, preferCloud)) {
                is Resolution.ImportCloud -> {
                    applyingRemote = true
                    try { profileRepo.importSnapshot(r.profile) } finally { applyingRemote = false }
                }
                is Resolution.PushLocal -> uploadSnapshot(token, r.snapshot)
                Resolution.Noop -> {}
            }
            markSynced()
        } catch (e: TokenExpiredException) {
            auth.invalidateToken(); currentToken = null
            _syncStatus.value = SyncStatus.Error(e)
        } catch (e: Throwable) {
            _syncStatus.value = SyncStatus.Error(e)
        }
    }

    private suspend fun uploadSafe(snapshot: ProfileSnapshotDto) {
        val token = currentToken ?: return
        _syncStatus.value = SyncStatus.Syncing
        runCatching { uploadSnapshot(token, snapshot) }
            .onSuccess { markSynced() }
            .onFailure {
                if (it is TokenExpiredException) { auth.invalidateToken(); currentToken = null }
                _syncStatus.value = SyncStatus.Error(it)
            }
    }

    private suspend fun uploadSnapshot(token: String, snapshot: ProfileSnapshotDto) {
        val fileId = metadata.getFileId() ?: drive.findProfileFileId(token)
        if (fileId == null) {
            metadata.setFileId(drive.create(token, snapshot))
        } else {
            drive.update(token, fileId, snapshot)
            metadata.setFileId(fileId)
        }
    }

    private suspend fun markSynced() {
        val now = System.currentTimeMillis()
        metadata.setLastSynced(now)
        _syncStatus.value = SyncStatus.Synced(now)
    }

    private companion object {
        const val DEBOUNCE_MS = 2_000L
    }
}
