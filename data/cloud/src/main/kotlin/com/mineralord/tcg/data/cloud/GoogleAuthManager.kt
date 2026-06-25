package com.mineralord.tcg.data.cloud

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/** Estado de autenticación con Google. */
sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val name: String?, val email: String?, val avatarUrl: String?) : AuthState
}

/** Se requiere consentimiento interactivo del usuario (no se puede en silencio). */
class ConsentRequiredException : IOException("Se requiere consentimiento de Google Drive")

/**
 * Maneja el inicio de sesión con Google (Credential Manager) y la autorización
 * del scope `drive.appdata` (Authorization API de Play Services). Separa
 * identidad (quién es) de autorización (token de acceso a Drive), que es el
 * patrón recomendado actual.
 */
class GoogleAuthManager(context: Context) {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)
    private val authClient = Identity.getAuthorizationClient(appContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile private var cachedToken: String? = null
    private var pendingAuth: CompletableDeferred<Intent>? = null

    /** Muestra el selector de cuentas de Google y devuelve la identidad. */
    suspend fun signIn(activity: Activity): AuthState.SignedIn {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = credentialManager.getCredential(activity, request)
        val cred = response.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val google = GoogleIdTokenCredential.createFrom(cred.data)
            val signedIn = AuthState.SignedIn(
                name = google.displayName,
                email = google.id,
                avatarUrl = google.profilePictureUri?.toString(),
            )
            _authState.value = signedIn
            return signedIn
        }
        throw IOException("Credencial de Google inesperada: ${cred.type}")
    }

    /** Restaura el estado de sesión (sin UI) tras reabrir la app. */
    fun restoreSession(state: AuthState.SignedIn) {
        _authState.value = state
    }

    /**
     * Obtiene un token de acceso para el scope `drive.appdata`. Si hace falta
     * consentimiento y [launcher] es null (modo silencioso), lanza
     * [ConsentRequiredException].
     */
    suspend fun authorizeDriveScope(launcher: ((IntentSenderRequest) -> Unit)?): String {
        cachedToken?.let { return it }
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        val result = withContext(Dispatchers.IO) { Tasks.await(authClient.authorize(request)) }
        val token = if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent ?: throw IOException("Falta el PendingIntent de autorización")
            val launch = launcher ?: throw ConsentRequiredException()
            val deferred = CompletableDeferred<Intent>()
            pendingAuth = deferred
            launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            val data = deferred.await()
            authClient.getAuthorizationResultFromIntent(data).accessToken
        } else {
            result.accessToken
        } ?: throw IOException("No se obtuvo token de acceso a Drive")
        cachedToken = token
        return token
    }

    /** Completa el flujo de consentimiento (lo invoca el callback del launcher). */
    fun onAuthorizationResult(data: Intent?) {
        val deferred = pendingAuth ?: return
        pendingAuth = null
        if (data != null) deferred.complete(data)
        else deferred.completeExceptionally(ConsentRequiredException())
    }

    /** Invalida el token en caché (tras un 401). */
    fun invalidateToken() {
        cachedToken = null
    }

    suspend fun signOut() {
        cachedToken = null
        _authState.value = AuthState.SignedOut
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
