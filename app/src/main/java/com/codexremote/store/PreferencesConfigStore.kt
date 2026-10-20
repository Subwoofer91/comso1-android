package com.codexremote.store

import android.content.Context
import com.codexremote.domain.CodexModelDiscoveryResult
import com.codexremote.domain.CodexModelOption
import com.codexremote.domain.VpsConfig
import org.json.JSONArray
import org.json.JSONObject

data class ModelRefreshCache(
    val checkedAtMillis: Long,
    val successful: Boolean,
    val models: List<CodexModelOption>,
    val defaultModelId: String,
    val errorMessage: String?,
)

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

    fun loadModelRefreshCache(): ModelRefreshCache? {
        val checkedAt = prefs.getLong(KEY_MODEL_REFRESHED_AT, 0L)
        if (checkedAt <= 0L) return null
        val models = runCatching {
            val values = JSONArray(prefs.getString(KEY_MODEL_CATALOG, "[]") ?: "[]")
            buildList {
                for (index in 0 until values.length()) {
                    val item = values.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val efforts = item.optJSONArray("supportedEfforts")
                    add(
                        CodexModelOption(
                            id = id,
                            label = item.optString("label", id),
                            shortLabel = item.optString("shortLabel", id),
                            description = item.optString("description"),
                            family = item.optString("family", "VPS 模型"),
                            supportedEfforts = buildList {
                                if (efforts != null) {
                                    for (effortIndex in 0 until efforts.length()) {
                                        efforts.optString(effortIndex).takeIf(String::isNotBlank)?.let(::add)
                                    }
                                }
                            },
                            defaultEffort = item.optString("defaultEffort"),
                            isDefault = item.optBoolean("isDefault", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        return ModelRefreshCache(
            checkedAtMillis = checkedAt,
            successful = prefs.getBoolean(KEY_MODEL_REFRESH_SUCCESS, false),
            models = models,
            defaultModelId = prefs.getString(KEY_MODEL_DEFAULT, "").orEmpty(),
            errorMessage = prefs.getString(KEY_MODEL_REFRESH_ERROR, null),
        )
    }

    fun saveModelRefreshSuccess(result: CodexModelDiscoveryResult, checkedAtMillis: Long) {
        val modelsJson = JSONArray().apply {
            result.models.forEach { model ->
                put(
                    JSONObject()
                        .put("id", model.id)
                        .put("label", model.label)
                        .put("shortLabel", model.shortLabel)
                        .put("description", model.description)
                        .put("family", model.family)
                        .put("supportedEfforts", JSONArray(model.supportedEfforts))
                        .put("defaultEffort", model.defaultEffort)
                        .put("isDefault", model.isDefault),
                )
            }
        }
        prefs.edit()
            .putLong(KEY_MODEL_REFRESHED_AT, checkedAtMillis)
            .putBoolean(KEY_MODEL_REFRESH_SUCCESS, true)
            .putString(KEY_MODEL_CATALOG, modelsJson.toString())
            .putString(KEY_MODEL_DEFAULT, result.defaultModelId)
            .remove(KEY_MODEL_REFRESH_ERROR)
            .apply()
    }

    fun saveModelRefreshFailure(message: String, checkedAtMillis: Long) {
        prefs.edit()
            .putLong(KEY_MODEL_REFRESHED_AT, checkedAtMillis)
            .putBoolean(KEY_MODEL_REFRESH_SUCCESS, false)
            .putString(KEY_MODEL_REFRESH_ERROR, message)
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
        const val KEY_MODEL_REFRESHED_AT = "model_refreshed_at"
        const val KEY_MODEL_REFRESH_SUCCESS = "model_refresh_success"
        const val KEY_MODEL_REFRESH_ERROR = "model_refresh_error"
        const val KEY_MODEL_CATALOG = "model_catalog"
        const val KEY_MODEL_DEFAULT = "model_default"
    }
}
