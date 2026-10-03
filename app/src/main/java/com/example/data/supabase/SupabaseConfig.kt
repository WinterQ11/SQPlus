package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class SupabaseConfig(private val context: Context) {

    companion object {
        const val DEFAULT_STORAGE_BUCKET = "sqplus-documents"
        const val AUTHORIZED_ADMIN_EMAIL = "hollowfaith1001@gmail.com"
        const val ADMIN_USERNAME = "Winter"
        private const val PREFS_NAME = "sqplus_supabase_prefs"
        private const val KEY_CUSTOM_URL = "custom_supabase_url"
        private const val KEY_CUSTOM_KEY = "custom_supabase_key"
        private const val KEY_ACCESS_TOKEN = "supabase_access_token"
        private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
        private const val KEY_USER_EMAIL = "supabase_user_email"
        private const val KEY_USER_ID = "supabase_user_id"
        private const val KEY_USER_ROLE = "supabase_user_role"

        // Default project configuration fallback
        private const val FALLBACK_URL = "https://sqplus-1b0e9.supabase.co"
        private const val FALLBACK_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.anon"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSupabaseUrl(): String {
        val custom = prefs.getString(KEY_CUSTOM_URL, null)
        if (!custom.isNullOrBlank()) return custom.trimEnd('/')

        val buildConfigUrl = try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            field.get(null) as? String
        } catch (ignored: Exception) {
            null
        }

        return if (!buildConfigUrl.isNullOrBlank() && !buildConfigUrl.contains("your-project-id")) {
            buildConfigUrl.trimEnd('/')
        } else {
            FALLBACK_URL
        }
    }

    fun getAnonKey(): String {
        val custom = prefs.getString(KEY_CUSTOM_KEY, null)
        if (!custom.isNullOrBlank()) return custom.trim()

        val buildConfigKey = try {
            val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
            field.get(null) as? String
        } catch (ignored: Exception) {
            null
        }

        return if (!buildConfigKey.isNullOrBlank() && !buildConfigKey.contains("your-supabase-anon-key")) {
            buildConfigKey.trim()
        } else {
            FALLBACK_ANON_KEY
        }
    }

    fun setCustomConfig(url: String, apiKey: String) {
        prefs.edit()
            .putString(KEY_CUSTOM_URL, url.trim().trimEnd('/'))
            .putString(KEY_CUSTOM_KEY, apiKey.trim())
            .apply()
    }

    // Session Management
    fun saveSession(accessToken: String, refreshToken: String?, userId: String, email: String, role: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    fun hasValidSession(): Boolean = !getAccessToken().isNullOrBlank()

    fun getStorageBucket(): String = DEFAULT_STORAGE_BUCKET
}
