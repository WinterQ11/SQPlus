package com.example.data.supabase

import android.util.Log
import com.example.data.model.ChannelEntity
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

class ChannelRepository(
    private val config: SupabaseConfig,
    private val authRepository: AuthRepository,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "ChannelRepository"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val channelsListType = Types.newParameterizedType(List::class.java, SupabaseChannel::class.java)
    private val channelsAdapter = moshi.adapter<List<SupabaseChannel>>(channelsListType)

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

    suspend fun fetchChannels(): Result<List<ChannelEntity>> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/channels?select=*&order=is_featured.desc,updated_at.desc"

        val requestBuilder = Request.Builder().url(endpoint)
        authRepository.getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Failed to load channels: HTTP ${response.code} $errorBody")
                    )
                }

                val bodyStr = response.body?.string().orEmpty()
                val dtoList = channelsAdapter.fromJson(bodyStr).orEmpty()

                val channels = dtoList.map { dto ->
                    ChannelEntity(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        category = dto.category ?: "General",
                        iconName = dto.icon ?: "folder",
                        fileCount = dto.fileCount ?: 0,
                        lastUpdated = parseTimestamp(dto.updatedAt ?: dto.createdAt),
                        isFeatured = dto.isFeatured ?: false
                    )
                }
                Result.success(channels)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching channels from Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun createChannel(channel: ChannelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/channels"

        val json = JSONObject().apply {
            put("id", channel.id)
            put("name", channel.name)
            put("description", channel.description)
            put("category", channel.category)
            put("icon", channel.iconName)
            put("is_active", true)
            put("file_count", channel.fileCount)
            put("is_featured", channel.isFeatured)
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
                    Result.failure(IllegalStateException("Failed to create channel: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChannel(channel: ChannelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/channels?id=eq.${channel.id}"

        val json = JSONObject().apply {
            put("name", channel.name)
            put("description", channel.description)
            put("category", channel.category)
            put("icon", channel.iconName)
            put("is_featured", channel.isFeatured)
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
                    Result.failure(IllegalStateException("Failed to update channel: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChannel(channelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.getSupabaseUrl()
        val endpoint = "$baseUrl/rest/v1/channels?id=eq.$channelId"

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
                    Result.failure(IllegalStateException("Failed to delete channel: HTTP ${response.code} $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
