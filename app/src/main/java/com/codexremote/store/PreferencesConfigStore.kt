package com.codexremote.store

import android.content.Context
import com.codexremote.domain.VpsConfig

class PreferencesConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("codex_remote_config", Context.MODE_PRIVATE)
    private val secure = SecureStringStore()

    fun load(): VpsConfig {
        return VpsConfig(
            host = prefs.getString(KEY_HOST, "") ?: "",
            port = prefs.getInt(KEY_PORT, 22),
            username = prefs.getString(KEY_USER, "codex") ?: "codex",
            workDir = prefs.getString(KEY_WORKDIR, "/home/codex") ?: "/home/codex",
            codexCommand = prefs.getString(KEY_CODEX_COMMAND, "codex") ?: "codex",
            model = prefs.getString(KEY_MODEL, "") ?: "",
            reasoningEffort = prefs.getString(KEY_REASONING_EFFORT, "") ?: "",
            privateKey = secure.decrypt(prefs.getString(KEY_PRIVATE_KEY, "")),
            privateKeyPassphrase = secure.decrypt(prefs.getString(KEY_PRIVATE_KEY_PASSPHRASE, "")),
        )
    }

    fun hasPrivateKey(): Boolean = !prefs.getString(KEY_PRIVATE_KEY, "").isNullOrBlank()

    fun save(config: VpsConfig, privateKeyInput: String) {
        prefs.edit()
            .putString(KEY_HOST, config.host.trim())
            .putInt(KEY_PORT, config.port.coerceIn(1, 65_535))
            .putString(KEY_USER, config.username.trim().ifBlank { "codex" })
            .putString(KEY_WORKDIR, config.workDir.trim().ifBlank { "/home/codex" })
            .putString(KEY_CODEX_COMMAND, config.codexCommand.trim().ifBlank { "codex" })
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_REASONING_EFFORT, config.reasoningEffort.trim())
            .apply()

        if (privateKeyInput.isNotBlank()) {
            prefs.edit().putString(KEY_PRIVATE_KEY, secure.encrypt(privateKeyInput)).apply()
        }
        if (config.privateKeyPassphrase.isNotBlank()) {
            prefs.edit().putString(KEY_PRIVATE_KEY_PASSPHRASE, secure.encrypt(config.privateKeyPassphrase)).apply()
        }
    }

    fun saveModelSelection(model: String, reasoningEffort: String) {
        prefs.edit()
            .putString(KEY_MODEL, model.trim())
            .putString(KEY_REASONING_EFFORT, reasoningEffort.trim())
            .apply()
    }

    private companion object {
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_USER = "username"
        const val KEY_WORKDIR = "workdir"
        const val KEY_CODEX_COMMAND = "codex_command"
        const val KEY_MODEL = "model"
        const val KEY_REASONING_EFFORT = "reasoning_effort"
        const val KEY_PRIVATE_KEY = "private_key"
        const val KEY_PRIVATE_KEY_PASSPHRASE = "private_key_passphrase"
    }
}
