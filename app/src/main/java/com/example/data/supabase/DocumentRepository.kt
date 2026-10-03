package com.example.data.supabase

import android.util.Log
import com.example.data.model.FileEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DocumentRepository(
    private val config: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "DocumentRepository"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val documentsListType = Types.newParameterizedType(List::class.java, SupabaseDocument::class.java)
    private val documentsAdapter = moshi.adapter<List<SupabaseDocument>>(documentsListType)

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            isoFormat.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                isoFallbackFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (ignored: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    suspend fun fetchDocuments(publishedOnly: Boolean): Result<List<FileEntity>> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val queryParam = if (publishedOnly) "is_published=eq.true&order=created_at.desc" else "order=created_at.desc"
        val endpoint = "$baseUrl/rest/v1/documents?select=*&$queryParam"

        val requestBuilder = Request.Builder().url(endpoint)
        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Failed to load documents: HTTP ${response.code} $err")
                    )
                }

                val bodyStr = response.body?.string().orEmpty()
                val dtoList = documentsAdapter.fromJson(bodyStr).orEmpty()

                val documents = dtoList.map { dto ->
                    FileEntity(
                        id = dto.id,
                        channelId = dto.channelId,
                        name = dto.title.ifEmpty { dto.fileName },
                        description = dto.description ?: "",
                        fileType = dto.fileType ?: "PDF",
                        fileSizeBytes = dto.fileSize ?: 0L,
                        uploadTimestamp = parseTimestamp(dto.createdAt),
                        downloadCount = dto.downloadCount ?: 0,
                        downloadUrl = dto.downloadUrl ?: "",
                        category = dto.category ?: "General",
                        isPublished = dto.isPublished ?: true
                    )
                }
                Result.success(documents)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching documents from Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun createDocument(file: FileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/documents"

        val storagePath = "channels/${file.channelId}/${file.id}/${file.name}"

        val json = JSONObject().apply {
            put("id", file.id)
            put("title", file.name)
            put("description", file.description)
            put("channel_id", file.channelId)
            put("file_name", file.name)
            put("file_path", storagePath)
            put("file_type", file.fileType)
            put("file_size", file.fileSizeBytes)
            put("download_url", file.downloadUrl)
            put("category", file.category)
            put("is_published", file.isPublished)
            put("download_count", file.downloadCount)
        }.toString()

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))

        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException("Failed to save document metadata: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDocument(file: FileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/documents?id=eq.${file.id}"

        val json = JSONObject().apply {
            put("title", file.name)
            put("description", file.description)
            put("channel_id", file.channelId)
            put("file_name", file.name)
            put("file_type", file.fileType)
            put("file_size", file.fileSizeBytes)
            put("category", file.category)
            put("is_published", file.isPublished)
        }.toString()

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Prefer", "return=minimal")
            .patch(json.toRequestBody(JSON_MEDIA_TYPE))

        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException("Failed to update document: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePublishStatus(documentId: String, isPublished: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/documents?id=eq.$documentId"

        val json = JSONObject().apply {
            put("is_published", isPublished)
        }.toString()

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Prefer", "return=minimal")
            .patch(json.toRequestBody(JSON_MEDIA_TYPE))

        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException("Failed to toggle publish status: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/documents?id=eq.$documentId"

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .delete()

        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException("Failed to delete document: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementDownloadCount(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        // Query current count first or update
        val endpoint = "$baseUrl/rest/v1/documents?id=eq.$documentId"

        try {
            val getReq = Request.Builder().url("$endpoint&select=download_count")
            authRepository.getAuthHeaders().forEach { (k, v) -> getReq.addHeader(k, v) }
            val currentCount = httpClient.newCall(getReq.build()).execute().use { res ->
                val body = res.body?.string().orEmpty()
                try {
                    val arr = org.json.JSONArray(body)
                    if (arr.length() > 0) arr.getJSONObject(0).optInt("download_count", 0) else 0
                } catch (ignored: Exception) { 0 }
            }

            val patchBody = JSONObject().put("download_count", currentCount + 1).toString()
            val patchReq = Request.Builder()
                .url(endpoint)
                .patch(patchBody.toRequestBody(JSON_MEDIA_TYPE))
            authRepository.getAuthHeaders().forEach { (k, v) -> patchReq.addHeader(k, v) }
            httpClient.newCall(patchReq.build()).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
