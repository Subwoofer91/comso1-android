package com.codexremote.store

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.codexremote.domain.MessageAttachment
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAttachmentStore(private val context: Context) {
    suspend fun importImages(uris: List<Uri>): List<MessageAttachment> = withContext(Dispatchers.IO) {
        uris.take(MAX_ATTACHMENTS).map { uri -> importImage(uri) }
    }

    private fun importImage(uri: Uri): MessageAttachment {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/*" }
        require(mimeType.startsWith("image/")) { "只能添加图片" }
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            ?.takeIf { it.isNotBlank() }
            ?: "image"
        val extension = extensionFor(mimeType, displayName)
        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "message_attachments").apply { mkdirs() }
        val target = File(directory, "$id$extension")
        try {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取所选图片" }
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "单张图片不能超过 20 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        return MessageAttachment(
            id = id,
            name = displayName.take(120),
            mimeType = mimeType,
            localPath = target.absolutePath,
        )
    }

    private fun extensionFor(mimeType: String, name: String): String {
        val fromName = name.substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
        if (fromName != null) return ".$fromName"
        return when (mimeType) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> ".jpg"
        }
    }

    private companion object {
        const val MAX_ATTACHMENTS = 4
        const val MAX_BYTES = 20L * 1024L * 1024L
    }
}
