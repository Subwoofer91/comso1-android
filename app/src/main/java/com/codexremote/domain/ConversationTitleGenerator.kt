package com.codexremote.domain

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ConversationTitleGenerator {
    suspend fun generate(
        config: TitleApiConfig,
        userMessage: String,
        assistantMessage: String,
    ): String = withContext(Dispatchers.IO) {
        require(config.enabled) { "标题生成未启用" }
        require(config.apiKey.isNotBlank()) { "标题 API Key 未配置" }

        val endpoint = config.baseUrl.trimEnd('/').let {
            if (it.endsWith("/chat/completions")) it else "$it/chat/completions"
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("stream", false)
            .put("max_tokens", 128)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                "根据首次对话生成一个简洁的中文会话标题。只输出标题，不要引号、标点或解释；建议 6 到 18 个汉字。",
                            ),
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "content",
                                "用户消息：\n${userMessage.take(2400)}\n\nCodex 回复：\n${assistantMessage.take(2400)}",
                            ),
                    ),
            )

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 40_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) error("标题服务请求失败（HTTP $code）")
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val content = JSONObject(response)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            sanitize(content).ifBlank { error("标题服务返回了空标题") }
        } finally {
            connection.disconnect()
        }
    }

    internal fun sanitize(value: String): String = value
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trim()
        .removeSurrounding("\"")
        .removeSurrounding("“", "”")
        .replace(Regex("^[#*`\\s]+|[#*`。！？：:\\s]+$"), "")
        .take(36)
}
