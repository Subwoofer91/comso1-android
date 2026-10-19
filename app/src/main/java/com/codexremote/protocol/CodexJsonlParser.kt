package com.codexremote.protocol

import com.codexremote.domain.CodexUiEvent
import com.codexremote.domain.AgentMessagePhase
import org.json.JSONArray
import org.json.JSONObject

class CodexJsonlParser {
    fun parse(line: String): List<CodexUiEvent> {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return emptyList()

        val json = runCatching { JSONObject(trimmed) }.getOrNull()
            ?: return listOf(CodexUiEvent.Raw(trimmed))
        val type = json.optString("type", json.optString("event", "")).lowercase()

        return when {
            type == "thread.started" || type == "thread.created" -> {
                findThreadId(json)?.let { listOf(CodexUiEvent.ThreadFound(it)) }
                    ?: listOf(CodexUiEvent.Raw(trimmed))
            }
            type == "turn.completed" || type == "turn.done" || type == "task_complete" -> {
                listOf(CodexUiEvent.Completed(findThreadId(json)))
            }
            type == "turn.failed" || type == "turn.error" || type == "error" || json.has("error") -> {
                listOf(CodexUiEvent.Error(extractError(json)))
            }
            type == "item.completed" -> parseItem(json, trimmed, completed = true)
            type == "item.started" || type == "item.updated" -> parseItem(json, trimmed, completed = false)
            else -> parseLegacyEvent(json, type, trimmed)
        }
    }

    private fun parseItem(json: JSONObject, raw: String, completed: Boolean): List<CodexUiEvent> {
        val item = json.optJSONObject("item") ?: return listOf(CodexUiEvent.Raw(raw))
        return when (normalizedType(item.optString("type", ""))) {
            "agentmessage", "assistantmessage", "message" -> {
                if (!completed) return emptyList()
                extractMessageText(item)?.let {
                    listOf(CodexUiEvent.AgentText(it, parsePhase(item.optString("phase", ""))))
                }
                    ?: listOf(CodexUiEvent.Raw(raw))
            }
            "commandexecution", "command" -> {
                val command = firstString(item, "command", "cmd") ?: "命令执行"
                val output = buildCommandOutput(item)
                listOf(
                    CodexUiEvent.Command(
                        command = command,
                        output = output,
                        exitCode = item.optIntOrNull("exit_code") ?: item.optIntOrNull("exitCode"),
                        status = item.optString("status", if (completed) "completed" else "running"),
                    ),
                )
            }
            "mcptoolcall", "toolcall", "functioncall" -> {
                val server = firstString(item, "server", "server_name")
                val tool = firstString(item, "tool", "tool_name", "name") ?: "工具调用"
                val title = listOfNotNull(server, tool).joinToString(" / ")
                listOf(
                    CodexUiEvent.ToolCall(
                        title = title,
                        details = buildToolDetails(item),
                        status = item.optString("status", if (completed) "completed" else "running"),
                    ),
                )
            }
            "filechange", "websearch", "extension" -> {
                val title = firstString(item, "query", "path", "name")
                    ?: when (normalizedType(item.optString("type", ""))) {
                        "websearch" -> "网页搜索"
                        "filechange" -> "文件修改"
                        else -> "工具调用"
                    }
                listOf(
                    CodexUiEvent.ToolCall(
                        title = title,
                        details = buildToolDetails(item),
                        status = item.optString("status", if (completed) "completed" else "running"),
                    ),
                )
            }
            "error" -> listOf(CodexUiEvent.Error(extractError(item)))
            "reasoning" -> emptyList()
            else -> listOf(CodexUiEvent.Raw(raw))
        }
    }

    private fun parseLegacyEvent(json: JSONObject, type: String, raw: String): List<CodexUiEvent> {
        val events = mutableListOf<CodexUiEvent>()
        findThreadId(json)?.let { events += CodexUiEvent.ThreadFound(it) }

        extractLegacyText(json, type)?.let {
            events += CodexUiEvent.AgentText(it, parsePhase(json.optString("phase", "")))
        }
        extractLegacyCommand(json, type)?.let { events += CodexUiEvent.Command(it.first, it.second) }
        extractLegacyTool(json, type)?.let { events += CodexUiEvent.ToolCall(it.first, it.second) }

        if (events.isEmpty()) events += CodexUiEvent.Raw(raw)
        return events
    }

    private fun findThreadId(json: JSONObject): String? {
        for (key in listOf("thread_id", "threadId", "session_id", "sessionId", "conversation_id", "conversationId")) {
            json.optString(key, "").takeIf { it.isNotBlank() }?.let { return it }
        }
        for (key in listOf("session", "thread", "conversation", "data")) {
            json.optJSONObject(key)?.let { nested -> findThreadId(nested)?.let { return it } }
        }
        return null
    }

    private fun extractMessageText(json: JSONObject): String? {
        firstString(json, "text", "delta")?.let { return it }
        val content = json.opt("content")
        return when (content) {
            is String -> content.takeIf { it.isNotBlank() }
            is JSONArray -> extractContentArray(content)
            else -> null
        }
    }

    private fun extractLegacyText(json: JSONObject, type: String): String? {
        val role = json.optString("role", "").lowercase()
        val isAssistant = role == "assistant" || listOf("assistant", "agent", "message", "delta", "response")
            .any(type::contains)
        if (!isAssistant) return null

        extractMessageText(json)?.let { return it }
        for (key in listOf("message", "item", "data", "output")) {
            json.optJSONObject(key)?.let { extractMessageText(it)?.let { text -> return text } }
        }
        return null
    }

    private fun extractLegacyCommand(json: JSONObject, type: String): Pair<String, String>? {
        if (!type.contains("command") && !type.contains("exec") && !json.has("command")) return null
        val command = firstString(json, "command", "cmd", "shell", "argv") ?: return null
        return command to buildCommandOutput(json)
    }

    private fun extractLegacyTool(json: JSONObject, type: String): Pair<String, String>? {
        if (!type.contains("tool") && !type.contains("function") && !json.has("tool")) return null
        val name = firstString(json, "tool", "tool_name", "name", "function") ?: "工具调用"
        return name to buildToolDetails(json)
    }

    private fun extractError(json: JSONObject): String {
        return when (val error = json.opt("error")) {
            is JSONObject -> firstString(error, "message", "detail", "text") ?: error.toString(2)
            is String -> error
            else -> firstString(json, "message", "detail", "text") ?: json.toString(2)
        }
    }

    private fun firstString(json: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            when (val value = json.opt(key)) {
                is String -> if (value.isNotBlank()) return value
                is JSONArray -> if (value.length() > 0) return value.toString()
            }
        }
        return null
    }

    private fun extractContentArray(array: JSONArray): String? {
        val parts = buildList {
            for (index in 0 until array.length()) {
                when (val item = array.opt(index)) {
                    is String -> add(item)
                    is JSONObject -> firstString(item, "text", "content", "delta")?.let(::add)
                }
            }
        }
        return parts.joinToString("").takeIf { it.isNotBlank() }
    }

    private fun parsePhase(value: String): AgentMessagePhase = when (value.lowercase()) {
        "commentary", "analysis", "progress" -> AgentMessagePhase.Commentary
        "final_answer", "final", "answer" -> AgentMessagePhase.FinalAnswer
        else -> AgentMessagePhase.Unknown
    }

    private fun normalizedType(value: String): String = value
        .lowercase()
        .replace("_", "")
        .replace("-", "")

    private fun buildCommandOutput(item: JSONObject): String {
        val stdout = firstString(item, "aggregated_output", "output", "stdout", "result").orEmpty()
        val stderr = firstString(item, "stderr").orEmpty()
        return buildString {
            if (stdout.isNotBlank()) append(stdout.trimEnd())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(stderr.trimEnd())
            }
        }
    }

    private fun buildToolDetails(item: JSONObject): String {
        val lines = mutableListOf<String>()
        firstString(item, "query")?.let { lines += "查询：$it" }
        firstString(item, "path")?.let { lines += "路径：$it" }
        firstString(item, "input", "arguments")?.let { lines += it }
        firstString(item, "output", "result")?.let { lines += it }

        val results = item.optJSONArray("results")
        if (results != null) {
            for (index in 0 until minOf(results.length(), 8)) {
                val result = results.optJSONObject(index) ?: continue
                val title = firstString(result, "title", "name") ?: continue
                val url = firstString(result, "url")
                lines += if (url == null) title else "$title\n$url"
            }
        }
        return lines.joinToString("\n\n").ifBlank {
            item.optString("status", "").takeIf { it.isNotBlank() } ?: "已完成"
        }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }
}
