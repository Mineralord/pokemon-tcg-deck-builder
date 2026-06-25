package com.mineralord.tcg.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

/** El token de Drive caducó o fue revocado (HTTP 401). */
class TokenExpiredException : IOException("Token de acceso a Drive caducado")

/**
 * Cliente mínimo de la REST API de Google Drive v3 contra la carpeta privada
 * `appDataFolder`. Solo cuatro operaciones (buscar/descargar/crear/actualizar)
 * para guardar un único `profile.json`. Llamadas síncronas de OkHttp envueltas
 * en [Dispatchers.IO].
 */
class DriveSyncClient {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }


    /** Devuelve el id del `profile.json` en appDataFolder, o null si no existe. */
    suspend fun findProfileFileId(token: String): String? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val url = "$DRIVE/files?spaces=appDataFolder&fields=files(id,name,modifiedTime)&q=$q"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        client.newCall(req).execute().use { resp ->
            checkResponse(resp.code, "buscar")
            val body = resp.body?.string().orEmpty()
            json.decodeFromString(FileListDto.serializer(), body).files.firstOrNull()?.id
        }
    }

    /** Descarga y deserializa el snapshot, o null si el cuerpo está vacío. */
    suspend fun download(token: String, fileId: String): ProfileSnapshotDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$DRIVE/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            checkResponse(resp.code, "descargar")
            val body = resp.body?.string()
            if (body.isNullOrBlank()) null
            else runCatching { json.decodeFromString(ProfileSnapshotDto.serializer(), body) }.getOrNull()
        }
    }

    /** Crea `profile.json` en appDataFolder (multipart). Devuelve el nuevo id. */
    suspend fun create(token: String, dto: ProfileSnapshotDto): String = withContext(Dispatchers.IO) {
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val mediaJson = json.encodeToString(ProfileSnapshotDto.serializer(), dto)
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody(JSON_MEDIA))
            .addPart(mediaJson.toRequestBody(JSON_MEDIA))
            .build()
        val req = Request.Builder()
            .url("$UPLOAD/files?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            checkResponse(resp.code, "crear")
            val respBody = resp.body?.string().orEmpty()
            json.decodeFromString(FileIdDto.serializer(), respBody).id
        }
    }

    /** Sobrescribe el contenido del archivo existente. */
    suspend fun update(token: String, fileId: String, dto: ProfileSnapshotDto): Unit = withContext(Dispatchers.IO) {
        val mediaJson = json.encodeToString(ProfileSnapshotDto.serializer(), dto)
        val req = Request.Builder()
            .url("$UPLOAD/files/$fileId?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(mediaJson.toRequestBody(JSON_MEDIA))
            .build()
        client.newCall(req).execute().use { resp -> checkResponse(resp.code, "actualizar") }
    }

    private fun checkResponse(code: Int, op: String) {
        if (code == 401) throw TokenExpiredException()
        if (code !in 200..299) throw IOException("Drive: fallo al $op (HTTP $code)")
    }

    @Serializable
    private data class FileListDto(val files: List<DriveFileDto> = emptyList())

    @Serializable
    private data class DriveFileDto(val id: String, val name: String? = null, val modifiedTime: String? = null)

    @Serializable
    private data class FileIdDto(val id: String)

    private companion object {
        const val FILE_NAME = "profile.json"
        const val DRIVE = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        val JSON_MEDIA = "application/json; charset=UTF-8".toMediaType()
    }
}
