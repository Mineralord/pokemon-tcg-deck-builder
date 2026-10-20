package com.mineralord.tcg.data.netfirestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Información de la ÚLTIMA versión descargable publicada, leída del documento de configuración.
 *
 * [latestVersionCode] es el número interno autoritativo: la app se compara contra ÉL (no contra el
 * texto) para decidir si hay actualización. [versionName] y [message] son solo para mostrar.
 * [downloadUrl] es el enlace de descarga (p. ej. tu carpeta/archivo de Google Drive).
 */
data class AppVersionInfo(
    val latestVersionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val message: String = "",
)

/**
 * Fuente de verdad de "cuál es la última versión descargable para todos". La CONTROLAS TÚ a mano:
 * el aviso de actualización solo aparece cuando editas este documento en la consola de Firebase y
 * subes `latestVersionCode` por encima del `versionCode` instalado. Nada se publica con los commits.
 *
 * Modelo de datos (un único documento):
 *   config/appVersion → {
 *     latestVersionCode: Number,   // número interno; súbelo en cada versión que repartas
 *     versionName:       String,   // texto visible, p. ej. "0.2.0"
 *     downloadUrl:       String,   // enlace de Google Drive a la APK
 *     message:           String,   // (opcional) novedades a mostrar en el aviso
 *   }
 *
 * Reutiliza el mismo patrón que [FirestoreFriendsRepository]: `Firebase.firestore` + `await()` y una
 * API pública que NO expone tipos de Firebase (son `implementation` y no viajan al classpath de `app`).
 */
class FirestoreAppVersionRepository private constructor(
    private val db: FirebaseFirestore,
) {
    constructor() : this(Firebase.firestore)

    /**
     * Lee la última versión publicada (lectura puntual al arrancar). Devuelve `null` si el documento
     * aún no existe o no tiene los campos mínimos: en ese caso NUNCA se molesta al usuario. Esta
     * lectura NO requiere login (las reglas deben permitir leer `config/appVersion` a cualquiera).
     */
    suspend fun fetchLatest(): AppVersionInfo? = runCatching {
        val doc = db.collection(CONFIG).document(APP_VERSION).get().await()
        if (!doc.exists()) return null
        val code = doc.getLong("latestVersionCode")?.toInt() ?: return null
        val url = doc.getString("downloadUrl").orEmpty()
        if (url.isBlank()) return null
        AppVersionInfo(
            latestVersionCode = code,
            versionName = doc.getString("versionName").orEmpty(),
            downloadUrl = url,
            message = doc.getString("message").orEmpty(),
        )
    }.getOrNull()

    private companion object {
        const val CONFIG = "config"
        const val APP_VERSION = "appVersion"
    }
}
