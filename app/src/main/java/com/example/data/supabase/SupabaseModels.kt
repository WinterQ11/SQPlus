package com.example.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseChannel(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = true,
    @Json(name = "file_count") val fileCount: Int? = 0,
    @Json(name = "is_featured") val isFeatured: Boolean? = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseDocument(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "channel_id") val channelId: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "file_type") val fileType: String? = "PDF",
    @Json(name = "file_size") val fileSize: Long? = 0L,
    @Json(name = "download_url") val downloadUrl: String? = null,
    @Json(name = "category") val category: String? = "General",
    @Json(name = "is_published") val isPublished: Boolean? = true,
    @Json(name = "download_count") val downloadCount: Int? = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "user") val user: SupabaseUserDto? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "app_metadata") val appMetadata: Map<String, Any>? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseErrorResponse(
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "code") val code: String? = null
)
