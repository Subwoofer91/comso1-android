package com.codexremote.store

import android.content.Context
import com.codexremote.domain.TitleApiConfig

class PreferencesTitleConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("codex_remote_title_api", Context.MODE_PRIVATE)
    private val secure = SecureStringStore()

    fun load(): TitleApiConfig = TitleApiConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
        model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
        apiKey = secure.decrypt(prefs.getString(KEY_API_KEY, "")),
    )

    fun hasApiKey(): Boolean = !prefs.getString(KEY_API_KEY, "").isNullOrBlank()

    fun save(config: TitleApiConfig, apiKeyInput: String) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_BASE_URL, config.baseUrl.trim().ifBlank { DEFAULT_BASE_URL })
            .putString(KEY_MODEL, config.model.trim().ifBlank { DEFAULT_MODEL })
            .apply()
        if (apiKeyInput.isNotBlank()) {
            prefs.edit().putString(KEY_API_KEY, secure.encrypt(apiKeyInput.trim())).apply()
        }
    }

    private companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-flash"
        const val KEY_ENABLED = "enabled"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key"
    }
}
