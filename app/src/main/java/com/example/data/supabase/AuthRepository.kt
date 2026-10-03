package com.example.data.supabase

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val config: SupabaseConfig,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "AuthRepository"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val tokenAdapter = moshi.adapter(SupabaseAuthTokenResponse::class.java)
    private val errorAdapter = moshi.adapter(SupabaseErrorResponse::class.java)

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        val email = config.getUserEmail()
        val token = config.getAccessToken()
        val userId = config.getUserId()
        val role = config.getUserRole()

        if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
            _currentUserEmail.value = email
            _currentUserId.value = userId

            val isAuthorizedAdmin = email.equals(SupabaseConfig.AUTHORIZED_ADMIN_EMAIL, ignoreCase = true) ||
                    role == "admin"
            _isAdmin.value = isAuthorizedAdmin
        } else {
            _isAdmin.value = false
            _currentUserEmail.value = null
            _currentUserId.value = null
        }
    }

    suspend fun loginWithEmail(emailInput: String, passwordInput: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedEmail = emailInput.trim()
        val trimmedPassword = passwordInput.trim()

        if (trimmedPassword.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Password cannot be blank."))
        }

        val normalizedEmail = if (!trimmedEmail.contains("@")) {
            if (trimmedEmail.equals(SupabaseConfig.ADMIN_USERNAME, ignoreCase = true)) {
                SupabaseConfig.AUTHORIZED_ADMIN_EMAIL
            } else {
                "$trimmedEmail@sqplus.cloud"
            }
        } else {
            trimmedEmail.lowercase()
        }

        // Strict requirement: Only the authorized platform administrator is permitted to authenticate as admin
        if (!normalizedEmail.equals(SupabaseConfig.AUTHORIZED_ADMIN_EMAIL, ignoreCase = true)) {
            return@withContext Result.failure(
                SecurityException("Access Denied: Only the authorized administrator (${SupabaseConfig.AUTHORIZED_ADMIN_EMAIL}) is permitted.")
            )
        }

        val baseUrl = config.getSupabaseUrl()
        val anonKey = config.getAnonKey()
        val endpoint = "$baseUrl/auth/v1/token?grant_type=password"

        val jsonBody = JSONObject().apply {
            put("email", normalizedEmail)
            put("password", trimmedPassword)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val tokenResponse = tokenAdapter.fromJson(bodyStr)
                    if (tokenResponse != null) {
                        val user = tokenResponse.user
                        val userId = user?.id ?: ""
                        val userEmail = user?.email ?: normalizedEmail
                        val role = user?.role ?: "admin"

                        config.saveSession(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            userId = userId,
                            email = userEmail,
                            role = role
                        )

                        _currentUserId.value = userId
                        _currentUserEmail.value = userEmail
                        _isAdmin.value = true

                        return@withContext Result.success(true)
                    }
                }

                // Parse error details
                val errorMsg = try {
                    val err = errorAdapter.fromJson(bodyStr)
                    err?.errorDescription ?: err?.message ?: err?.msg ?: err?.error
                } catch (ignored: Exception) {
                    null
                } ?: "HTTP ${response.code}: Authentication failed"

                val friendlyError = when {
                    errorMsg.contains("invalid_grant", ignoreCase = true) ||
                            errorMsg.contains("Invalid login credentials", ignoreCase = true) ->
                        "Invalid credentials. Please verify your password or tap 'Forgot Password?' to reset it."
                    errorMsg.contains("Email not confirmed", ignoreCase = true) ->
                        "Administrator email is awaiting confirmation in Supabase Auth."
                    else -> errorMsg
                }

                Result.failure(IllegalArgumentException(friendlyError))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure during Supabase authentication", e)
            Result.failure(IOException("Network error. Please verify your internet connection and Supabase endpoint."))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Supabase authentication", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(emailInput: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedEmail = emailInput.trim()
        val normalizedEmail = if (!trimmedEmail.contains("@")) {
            if (trimmedEmail.equals(SupabaseConfig.ADMIN_USERNAME, ignoreCase = true)) {
                SupabaseConfig.AUTHORIZED_ADMIN_EMAIL
            } else {
                "$trimmedEmail@sqplus.cloud"
            }
        } else {
            trimmedEmail.lowercase()
        }

        if (!normalizedEmail.equals(SupabaseConfig.AUTHORIZED_ADMIN_EMAIL, ignoreCase = true)) {
            return@withContext Result.failure(SecurityException("Password reset is only available for the authorized administrator."))
        }

        val baseUrl = config.getSupabaseUrl()
        val anonKey = config.getAnonKey()
        val endpoint = "$baseUrl/auth/v1/recover"

        val jsonBody = JSONObject().apply {
            put("email", normalizedEmail)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(Exception("Failed to dispatch reset link: HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        val token = config.getAccessToken()
        if (!token.isNullOrBlank()) {
            try {
                val baseUrl = config.getSupabaseUrl()
                val anonKey = config.getAnonKey()
                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/logout")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (ignored: Exception) {}
        }
        config.clearSession()
        _isAdmin.value = false
        _currentUserEmail.value = null
        _currentUserId.value = null
        Result.success(Unit)
    }

    fun getAuthHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val anonKey = config.getAnonKey()
        headers["apikey"] = anonKey

        val token = config.getAccessToken()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        } else {
            headers["Authorization"] = "Bearer $anonKey"
        }
        return headers
    }
}
