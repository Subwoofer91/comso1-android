package com.codexremote.store

import android.content.Context
import com.codexremote.domain.ChatItem
import com.codexremote.domain.ChatRole
import com.codexremote.domain.MessageAttachment
import com.codexremote.domain.PhoneLocation
import com.codexremote.domain.ProcessStep
import com.codexremote.domain.ProcessStepKind
import com.codexremote.domain.ThreadRecord
import org.json.JSONArray
import org.json.JSONObject

class PreferencesThreadStore(context: Context) {
    private val prefs = context.getSharedPreferences("codex_remote_threads", Context.MODE_PRIVATE)

    fun list(): List<ThreadRecord> {
        val array = runCatching { JSONArray(prefs.getString(KEY_THREADS, "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id", "")
                if (id.isBlank()) continue
                add(
                    ThreadRecord(
                        id = id,
                        title = item.optString("title", id.take(12)),
                        updatedAt = item.optLong("updatedAt", 0L),
                        workDir = item.optString("workDir", ""),
                        titleGenerated = item.optBoolean("titleGenerated", false),
                        titleLocked = item.optBoolean("titleLocked", false),
                        pinned = item.optBoolean("pinned", false),
                        archived = item.optBoolean("archived", false),
                    ),
                )
            }
        }.sortedForDisplay()
    }

    fun messages(threadId: String): List<ChatItem> {
        val array = runCatching { JSONArray(prefs.getString(messagesKey(threadId), "[]")) }.getOrElse { JSONArray() }
        val decoded = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val role = runCatching { ChatRole.valueOf(item.optString("role", ChatRole.System.name)) }
                    .getOrDefault(ChatRole.System)
                val text = item.optString("text", "")
                if (role == ChatRole.Tool && text == "原始 JSONL 事件") continue
                add(
                    ChatItem(
                        id = item.optString("id", "$threadId-$index"),
                        role = role,
                        text = text,
                        status = item.optString("status", "").ifBlank { null },
                        details = item.optString("details", "").ifBlank { null },
                        attachments = decodeAttachments(item.optJSONArray("attachments")),
                        location = decodeLocation(item.optJSONObject("location")),
                        processSteps = decodeProcessSteps(item.optJSONArray("processSteps")),
                    ),
                )
            }
        }
        return compactLegacyToolItems(decoded)
    }

    fun saveMessages(threadId: String, messages: List<ChatItem>) {
        val array = JSONArray()
        messages.takeLast(400).forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("role", item.role.name)
                    .put("text", item.text)
                    .put("status", item.status ?: "")
                    .put("details", item.details ?: "")
                    .put("attachments", encodeAttachments(item.attachments))
                    .put("location", encodeLocation(item.location))
                    .put("processSteps", encodeProcessSteps(item.processSteps)),
            )
        }
        prefs.edit().putString(messagesKey(threadId), array.toString()).apply()
    }

    private fun compactLegacyToolItems(items: List<ChatItem>): List<ChatItem> = buildList {
        var index = 0
        while (index < items.size) {
            val item = items[index]
            if (!item.isLegacyProcessItem(items, index)) {
                add(item)
                index += 1
                continue
            }

            val legacyTools = mutableListOf<ChatItem>()
            while (index < items.size && items[index].isLegacyProcessItem(items, index)) {
                legacyTools += items[index]
                index += 1
            }
            add(
                ChatItem(
                    id = "legacy-process-${legacyTools.first().id}",
                    role = ChatRole.Process,
                    text = "",
                    status = "completed",
                    processSteps = legacyTools.map { legacy ->
                        ProcessStep(
                            id = legacy.id,
                            kind = if (legacy.role == ChatRole.Error) ProcessStepKind.Error else ProcessStepKind.Tool,
                            title = if (legacy.role == ChatRole.Error) "运行期错误" else legacy.text.ifBlank { "工具调用" },
                            content = if (legacy.role == ChatRole.Error) legacy.text else legacy.details.orEmpty(),
                            status = if (legacy.role == ChatRole.Error) "error" else "completed",
                        )
                    },
                ),
            )
        }
    }

    private fun ChatItem.isLegacyProcessItem(items: List<ChatItem>, index: Int): Boolean {
        if (role == ChatRole.Tool) return true
        if (role != ChatRole.Error) return false
        val turnEnd = (index + 1 until items.size)
            .firstOrNull { items[it].role == ChatRole.User }
            ?: items.size
        return (index + 1 until turnEnd).any { items[it].role == ChatRole.Agent }
    }

    fun upsert(record: ThreadRecord) {
        val next = (list().filterNot { it.id == record.id } + record)
            .sortedForDisplay()
            .take(100)
        val array = JSONArray()
        next.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("updatedAt", it.updatedAt)
                    .put("workDir", it.workDir)
                    .put("titleGenerated", it.titleGenerated)
                    .put("titleLocked", it.titleLocked)
                    .put("pinned", it.pinned)
                    .put("archived", it.archived),
            )
        }
        prefs.edit().putString(KEY_THREADS, array.toString()).apply()
    }

    fun rename(threadId: String, title: String, lock: Boolean = true) {
        val current = list().firstOrNull { it.id == threadId } ?: return
        upsert(
            current.copy(
                title = title.trim().take(80).ifBlank { current.id.take(12) },
                updatedAt = System.currentTimeMillis(),
                titleGenerated = current.titleGenerated || !lock,
                titleLocked = lock,
            ),
        )
    }

    fun setPinned(threadId: String, pinned: Boolean) {
        val current = list().firstOrNull { it.id == threadId } ?: return
        upsert(current.copy(pinned = pinned))
    }

    fun setArchived(threadId: String, archived: Boolean) {
        val current = list().firstOrNull { it.id == threadId } ?: return
        upsert(current.copy(archived = archived, pinned = if (archived) false else current.pinned))
    }

    private fun encodeAttachments(items: List<MessageAttachment>): JSONArray = JSONArray().also { array ->
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("mimeType", item.mimeType)
                    .put("localPath", item.localPath)
                    .put("remotePath", item.remotePath),
            )
        }
    }

    private fun decodeAttachments(array: JSONArray?): List<MessageAttachment> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id", "")
            val localPath = item.optString("localPath", "")
            if (id.isBlank() || localPath.isBlank()) continue
            add(
                MessageAttachment(
                    id = id,
                    name = item.optString("name", "图片"),
                    mimeType = item.optString("mimeType", "image/*"),
                    localPath = localPath,
                    remotePath = item.optString("remotePath", ""),
                ),
            )
        }
    }

    private fun encodeLocation(location: PhoneLocation?): Any = location?.let {
        JSONObject()
            .put("latitude", it.latitude)
            .put("longitude", it.longitude)
            .put("accuracyMeters", it.accuracyMeters.toDouble())
            .put("capturedAt", it.capturedAt)
            .put("isCached", it.isCached)
    } ?: JSONObject.NULL

    private fun decodeLocation(item: JSONObject?): PhoneLocation? {
        if (item == null || !item.has("latitude") || !item.has("longitude")) return null
        return PhoneLocation(
            latitude = item.optDouble("latitude"),
            longitude = item.optDouble("longitude"),
            accuracyMeters = item.optDouble("accuracyMeters", 0.0).toFloat(),
            capturedAt = item.optLong("capturedAt", 0L),
            isCached = item.optBoolean("isCached", false),
        )
    }

    private fun List<ThreadRecord>.sortedForDisplay(): List<ThreadRecord> = sortedWith(
        compareBy<ThreadRecord> { it.archived }
            .thenByDescending { it.pinned }
            .thenByDescending { it.updatedAt },
    )

    private fun encodeProcessSteps(items: List<ProcessStep>): JSONArray = JSONArray().also { array ->
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("kind", item.kind.name)
                    .put("title", item.title)
                    .put("content", item.content)
                    .put("status", item.status),
            )
        }
    }

    private fun decodeProcessSteps(array: JSONArray?): List<ProcessStep> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            add(
                ProcessStep(
                    id = item.optString("id", "step-$index"),
                    kind = runCatching {
                        ProcessStepKind.valueOf(item.optString("kind", ProcessStepKind.Tool.name))
                    }.getOrDefault(ProcessStepKind.Tool),
                    title = item.optString("title", "工作步骤"),
                    content = item.optString("content", ""),
                    status = item.optString("status", ""),
                ),
            )
        }
    }

    private fun messagesKey(threadId: String): String = "messages_$threadId"

    private companion object {
        const val KEY_THREADS = "threads"
    }
}
