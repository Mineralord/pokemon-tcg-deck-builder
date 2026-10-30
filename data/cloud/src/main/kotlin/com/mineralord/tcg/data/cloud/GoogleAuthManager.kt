package com.mineralord.tcg.data.cloud

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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

    /** ID token de Google del último inicio de sesión interactivo (para vincular Firebase Auth). */
    @Volatile var lastIdToken: String? = null
        private set
    private var pendingAuth: CompletableDeferred<Intent>? = null

    /**
     * Muestra el selector de cuentas de Google y devuelve la identidad. Intenta primero el flujo
     * normal (`GetGoogleIdOption`); si no hay credencial disponible (p. ej. ninguna cuenta
     * previamente autorizada, caso típico en la 1ª vez), reintenta con el flujo EXPLÍCITO
     * "Sign in with Google" (`GetSignInWithGoogleOption`), que siempre abre el selector / añadir cuenta.
     */
    suspend fun signIn(activity: Activity): AuthState.SignedIn {
        val serverId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
        val idOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val response = try {
            credentialManager.getCredential(
                activity,
                GetCredentialRequest.Builder().addCredentialOption(idOption).build(),
            )
        } catch (e: NoCredentialException) {
            // Respaldo: botón explícito de Google (fuerza el selector / añadir cuenta).
            val signInOption = GetSignInWithGoogleOption.Builder(serverId).build()
            credentialManager.getCredential(
                activity,
                GetCredentialRequest.Builder().addCredentialOption(signInOption).build(),
            )
        }
        val cred = response.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val google = GoogleIdTokenCredential.createFrom(cred.data)
            lastIdToken = google.idToken
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

    /**
     * ID token de Google SIN selector (solo cuentas ya autorizadas, auto-selección): para vincular
     * Firebase Auth en usuarios que iniciaron sesión antes de existir ese vínculo. null si no se puede.
     */
    suspend fun silentIdToken(activity: Activity): String? = runCatching {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val cred = credentialManager.getCredential(
            activity, GetCredentialRequest.Builder().addCredentialOption(option).build(),
        ).credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            GoogleIdTokenCredential.createFrom(cred.data).idToken.also { lastIdToken = it }
        else null
    }.getOrNull()

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
