package com.codexremote.domain

import com.codexremote.ssh.JschCodexAppServerClient
import com.codexremote.store.PreferencesConfigStore
import org.json.JSONObject

data class CodexModelDiscoveryResult(
    val models: List<CodexModelOption>,
    val defaultModelId: String,
)

class CodexModelDiscovery(
    private val configStore: PreferencesConfigStore,
    private val appServerClient: JschCodexAppServerClient,
) {
    suspend fun discover(): CodexModelDiscoveryResult {
        val config = configStore.load()
        val exchange = appServerClient.requestModelList(
            config = config,
            codexCommand = config.codexCommand,
        )
        val response = parseJsonLine(exchange.response)
        if (response == null) {
            val detail = exchange.stderr.asReversed()
                .map(::stripAnsi)
                .firstOrNull { it.isNotBlank() && !it.contains("bubblewrap", ignoreCase = true) }
                ?: "Codex app-server 未返回 model/list"
            error("模型目录读取失败：$detail")
        }
        response.optJSONObject("error")?.let { error ->
            error("模型目录读取失败：${error.optString("message", error.toString())}")
        }
        val data = response.optJSONObject("result")?.optJSONArray("data")
            ?: error("Codex app-server 返回了无法识别的模型目录")
        val discovered = buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                if (item.optBoolean("hidden", false)) continue
                val id = item.optString("model").ifBlank { item.optString("id") }
                if (id.isBlank()) continue
                val displayName = item.optString("displayName").ifBlank { id }
                val effortsJson = item.optJSONArray("supportedReasoningEfforts")
                val efforts = buildList {
                    if (effortsJson != null) {
                        for (effortIndex in 0 until effortsJson.length()) {
                            val effort = effortsJson.optJSONObject(effortIndex)
                                ?.optString("reasoningEffort")
                                .orEmpty()
                            if (effort.isNotBlank() && effort !in this) add(effort)
                        }
                    }
                }
                add(
                    CodexModelOption(
                        id = id,
                        label = displayName,
                        shortLabel = shortLabel(displayName, id),
                        description = item.optString("description").ifBlank { "由 VPS 上的 Codex 提供" },
                        family = familyLabel(displayName, id),
                        supportedEfforts = efforts,
                        defaultEffort = item.optString("defaultReasoningEffort"),
                        isDefault = item.optBoolean("isDefault", false),
                    ),
                )
            }
        }
        if (discovered.isEmpty()) error("Codex app-server 返回了空模型目录")
        val defaultModel = discovered.firstOrNull { it.isDefault }?.id.orEmpty()
        return CodexModelDiscoveryResult(
            models = listOf(CodexModelCatalog.models.first()) + discovered,
            defaultModelId = defaultModel,
        )
    }

    private fun shortLabel(displayName: String, id: String): String {
        return displayName
            .replace(Regex("^GPT[- ]?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?<=\\d)-(?=[A-Za-z])"), " ")
            .ifBlank { id }
    }

    private fun familyLabel(displayName: String, id: String): String {
        val source = "$displayName $id"
        val version = Regex("(?:gpt[- ]?)?(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
            .find(source)
            ?.groupValues
            ?.getOrNull(1)
        return version?.let { "GPT-$it" } ?: "VPS 模型"
    }

    private fun parseJsonLine(raw: String): JSONObject? {
        val value = stripAnsi(raw).trim()
        val start = value.indexOf('{')
        if (start < 0) return null
        return runCatching { JSONObject(value.substring(start)) }.getOrNull()
    }

    private fun stripAnsi(value: String): String {
        return value.replace(ANSI_ESCAPE, "")
    }

    private companion object {
        val ANSI_ESCAPE = Regex("\\u001B\\[[;?0-9]*[ -/]*[@-~]")
    }
}
