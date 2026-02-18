package com.ecp.jellyseerrremote.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class RemoteMode { CLOUDFLARE, CUSTOM }

class SecurePrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var localUrl: String
        get() = prefs.getString(KEY_LOCAL_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOCAL_URL, value.trim()).apply()

    var remoteEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMOTE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOTE_ENABLED, value).apply()

    var remoteMode: RemoteMode
        get() = when (prefs.getString(KEY_REMOTE_MODE, CLOUDFLARE_VAL) ?: CLOUDFLARE_VAL) {
            CUSTOM_VAL -> RemoteMode.CUSTOM
            else -> RemoteMode.CLOUDFLARE
        }
        set(value) = prefs.edit().putString(KEY_REMOTE_MODE, if (value == RemoteMode.CUSTOM) CUSTOM_VAL else CLOUDFLARE_VAL).apply()

    var tunnelId: String
        get() = prefs.getString(KEY_TUNNEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TUNNEL_ID, value.trim()).apply()

    var customRemoteUrl: String
        get() = prefs.getString(KEY_CUSTOM_REMOTE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_REMOTE_URL, value.trim()).apply()

    var preferLocalFirst: Boolean
        get() = prefs.getBoolean(KEY_PREFER_LOCAL_FIRST, true)
        set(value) = prefs.edit().putBoolean(KEY_PREFER_LOCAL_FIRST, value).apply()

    var cookieHeader: String
        get() = prefs.getString(KEY_COOKIE_HEADER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COOKIE_HEADER, value).apply()

    fun clearAuth() {
        prefs.edit().remove(KEY_COOKIE_HEADER).apply()
    }

    companion object {
        private const val KEY_LOCAL_URL = "local_url"
        private const val KEY_REMOTE_ENABLED = "remote_enabled"
        private const val KEY_REMOTE_MODE = "remote_mode"
        private const val KEY_TUNNEL_ID = "tunnel_id"
        private const val KEY_CUSTOM_REMOTE_URL = "custom_remote_url"
        private const val KEY_PREFER_LOCAL_FIRST = "prefer_local_first"
        private const val KEY_COOKIE_HEADER = "auth_cookie"
        private const val CLOUDFLARE_VAL = "cloudflare"
        private const val CUSTOM_VAL = "custom"
    }
}
