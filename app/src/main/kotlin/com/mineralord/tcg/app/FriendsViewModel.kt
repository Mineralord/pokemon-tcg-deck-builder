package com.mineralord.tcg.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineralord.tcg.data.netfirestore.FirestoreFriendsRepository
import com.mineralord.tcg.data.netfirestore.FriendEntry
import com.mineralord.tcg.data.netfirestore.FriendRequest
import com.mineralord.tcg.data.netfirestore.MatchInvite
import com.mineralord.tcg.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del Hub de Amigos: puente entre la UI y el [FirestoreFriendsRepository] (backend real),
 * usando la identidad del jugador (friendCode + username) de [ProfileRepository]. Publica tu perfil
 * en el directorio al arrancar y expone en vivo tus Amigos / Solicitudes enviadas / recibidas.
 */
class FriendsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirestoreFriendsRepository()
    private val profileRepo = ProfileRepository(app)

    val friends: StateFlow<List<FriendEntry>> =
        repo.friends().catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val incoming: StateFlow<List<FriendRequest>> =
        repo.incoming().catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val outgoing: StateFlow<List<FriendRequest>> =
        repo.outgoing().catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    /** Invitaciones a combate entrantes (en vivo). La UI muestra banner + notificación. */
    val matchInvites: StateFlow<List<MatchInvite>> =
        repo.matchInvites().catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _myCode = MutableStateFlow("")
    val myCode: StateFlow<String> = _myCode.asStateFlow()
    private val _myName = MutableStateFlow("")
    val myName: StateFlow<String> = _myName.asStateFlow()

    /** Mensaje transitorio para la UI (resultado de enviar solicitud, errores…). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // La publicación en el directorio (publishSelf) se hace de forma GLOBAL en AppShell, así el
        // nombre se refleja estés donde estés. Aquí solo leemos la identidad para mostrarla en el Hub.
        viewModelScope.launch {
            profileRepo.ensureFriendCode()
            profileRepo.profile.collect { p ->
                _myCode.value = p.friendCode
                _myName.value = p.username
            }
        }
    }

    fun send(code: String) = viewModelScope.launch {
        val r = repo.sendRequest(code)
        _message.value = r.fold({ "Solicitud enviada" }, { it.message ?: "No se pudo enviar la solicitud" })
    }

    fun accept(uid: String) = viewModelScope.launch {
        runCatching { repo.accept(uid) }.onFailure { _message.value = "No se pudo aceptar" }
    }

    fun reject(uid: String) = viewModelScope.launch { runCatching { repo.reject(uid) } }

    fun cancel(uid: String) = viewModelScope.launch { runCatching { repo.cancelOutgoing(uid) } }

    fun remove(uid: String) = viewModelScope.launch { runCatching { repo.removeFriend(uid) } }

    fun clearAllIncoming() = viewModelScope.launch {
        incoming.value.forEach { runCatching { repo.reject(it.uid) } }
    }

    fun clearAllOutgoing() = viewModelScope.launch {
        outgoing.value.forEach { runCatching { repo.cancelOutgoing(it.uid) } }
    }

    /** Invita a [uid] a tu sala [roomCode] (le llega notificación + banner). */
    fun inviteToMatch(uid: String, roomCode: String) = viewModelScope.launch {
        runCatching { repo.inviteToMatch(uid, roomCode) }
            .onSuccess { _message.value = "Invitación enviada" }
            .onFailure { _message.value = "No se pudo invitar" }
    }

    /** Descarta la invitación a combate de [fromUid] (al aceptar, rechazar o entrar a la sala). */
    fun clearMatchInvite(fromUid: String) = viewModelScope.launch {
        runCatching { repo.clearMatchInvite(fromUid) }
    }

    fun clearMessage() { _message.value = null }
}
