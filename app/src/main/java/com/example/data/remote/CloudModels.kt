package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudChannel(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "category") val category: String,
    @Json(name = "icon_name") val iconName: String,
    @Json(name = "file_count") val fileCount: Int = 0,
    @Json(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
    @Json(name = "is_featured") val isFeatured: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CloudFile(
    @Json(name = "id") val id: String,
    @Json(name = "channel_id") val channelId: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "file_type") val fileType: String,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long,
    @Json(name = "upload_timestamp") val uploadTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "download_count") val downloadCount: Int = 0,
    @Json(name = "download_url") val downloadUrl: String = "",
    @Json(name = "category") val category: String = "General"
)

data class CloudBackendConfig(
    val providerName: String = "Supabase Cloud Platform",
    val endpointUrl: String = "https://sqplus-1b0e9.supabase.co",
    val apiKey: String = "",
    val storageBucket: String = "sqplus-documents",
    val isCustomConfigured: Boolean = false
)

enum class NetworkStatus {
    ONLINE,
    OFFLINE,
    SYNCING
}
