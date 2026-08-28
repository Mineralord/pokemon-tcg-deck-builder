package com.mineralord.tcg.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.profile.CurrencyKind
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.CardId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estadísticas reales del perfil derivadas de la colección + monedas. */
data class ProfileStats(
    val monedas: Int = 0,
    val cristales: Int = 0,
    val fichas: Int = 0,
    val totalOwned: Int = 0,      // cartas en colección (con copias)
    val distinctOwned: Int = 0,   // cartas diferentes
    val expansions: Int = 0,      // expansiones distintas poseídas
    val indexPct: Float = 0f,     // índice de colección (% de cartas distintas del catálogo)
    val friendCode: String = "",  // código de amigo único (12 dígitos)
    val username: String = "",    // Nombre de Usuario editable (se propaga a toda la app)
    val loading: Boolean = true,
)

/** Puente entre la pantalla de Perfil y el [CloudSyncRepository] del proceso. */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val cloud = (app as TcgApplication).cloudSync
    private val profileRepo = ProfileRepository(app)

    val authState = cloud.authState
    val syncStatus = cloud.syncStatus
    val lastSynced = cloud.lastSynced

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    init {
        // Genera el código de amigo la primera vez (idempotente).
        viewModelScope.launch { profileRepo.ensureFriendCode() }
        viewModelScope.launch {
            val repo = withContext(Dispatchers.Default) { CardRepository.load() }
            val catalogTotal = repo.all.size
            profileRepo.profile.collect { p ->
                val stats = withContext(Dispatchers.Default) {
                    val ids = p.owned.keys
                    val distinct = ids.count { repo[CardId(it)] != null }
                    val exps = ids.mapNotNull { repo[CardId(it)]?.set?.code }.distinct().size
                    val idx = if (catalogTotal > 0) distinct.toFloat() / catalogTotal * 100f else 0f
                    ProfileStats(
                        monedas = p.balanceOf(CurrencyKind.MONEDAS),
                        cristales = p.balanceOf(CurrencyKind.CRISTALES),
                        fichas = p.balanceOf(CurrencyKind.FICHAS),
                        totalOwned = p.totalOwned,
                        distinctOwned = distinct,
                        expansions = exps,
                        indexPct = idx,
                        friendCode = p.friendCode,
                        username = p.username,
                        loading = false,
                    )
                }
                _stats.value = stats
            }
        }
    }

    /**
     * Cambia el Nombre de Usuario. Se persiste en el perfil y, al observarse el [Flow], se
     * propaga a toda la app (menú, perfil, combate) y viaja a la nube en la próxima sync.
     */
    fun setUsername(name: String) {
        viewModelScope.launch { profileRepo.setUsername(name) }
    }

    fun signIn(activity: Activity, launch: (IntentSenderRequest) -> Unit) {
        viewModelScope.launch { cloud.signIn(activity, launch) }
    }

    /** Lo invoca el callback del launcher de consentimiento de Drive. */
    fun onAuthorizationResult(data: Intent?) = cloud.onAuthorizationResult(data)

    fun signOut() {
        viewModelScope.launch { cloud.signOut() }
    }

    fun retry() {
        viewModelScope.launch { cloud.syncNow(preferCloud = false) }
    }
}
