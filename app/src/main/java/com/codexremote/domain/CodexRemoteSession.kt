package com.codexremote.domain

import com.codexremote.protocol.CodexJsonlParser
import com.codexremote.protocol.ShellCommand
import com.codexremote.ssh.SshTransport
import com.codexremote.ssh.UploadPayload
import com.codexremote.store.PreferencesConfigStore
import com.codexremote.store.PreferencesThreadStore
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodexRemoteSession(
    private val configStore: PreferencesConfigStore,
    private val threadStore: PreferencesThreadStore,
    private val transport: SshTransport,
) {
    private val parser = CodexJsonlParser()

    suspend fun send(
        currentThreadId: String?,
        workDirOverride: String?,
        message: String,
        attachments: List<MessageAttachment> = emptyList(),
        location: PhoneLocation? = null,
        onEvent: suspend (CodexUiEvent) -> Unit,
    ) {
        val config = configStore.load().let { saved ->
            workDirOverride?.takeIf { it.isNotBlank() }?.let { saved.copy(workDir = it) } ?: saved
        }
        val remoteImages = if (attachments.isEmpty()) {
            emptyList()
        } else {
            val payloads = withContext(Dispatchers.IO) {
                attachments.map { attachment ->
                    UploadPayload(
                        name = attachment.name,
                        bytes = File(attachment.localPath).readBytes(),
                    )
                }
            }
            transport.upload(
                config,
                payloads,
            )
        }
        val prompt = buildPrompt(message, location)
        val command = ShellCommand.codexExec(
            workDir = config.workDir,
            codexCommand = config.codexCommand,
            message = prompt,
            threadId = currentThreadId,
            model = config.model,
            reasoningEffort = config.reasoningEffort,
            imagePaths = remoteImages,
        )
        var seenThreadId = currentThreadId
        var errorReported = false
        var turnCompleted = false

        val exitCode = transport.execute(
            config = config,
            command = command,
            onStdoutLine = { line ->
                parser.parse(line).forEach { event ->
                    if (event is CodexUiEvent.Error) errorReported = true
                    if (event is CodexUiEvent.ThreadFound) seenThreadId = event.threadId
                    if (event is CodexUiEvent.Completed && event.threadId != null) {
                        seenThreadId = event.threadId
                    }
                    if (event is CodexUiEvent.Completed) turnCompleted = true
                    onEvent(event)
                }
            },
            onStderrLine = { line ->
                if (line.isNotBlank() && !isNonFatalCodexStderr(line)) {
                    errorReported = true
                    onEvent(CodexUiEvent.Error(line))
                }
            },
        )

        if (exitCode != 0) {
            if (!errorReported) {
                onEvent(CodexUiEvent.Error("远端命令退出，状态码 $exitCode"))
            }
            return
        }

        if (errorReported) return

        seenThreadId?.takeIf { it.isNotBlank() }?.let {
            val existing = threadStore.list().firstOrNull { record -> record.id == it }
            threadStore.upsert(
                existing?.copy(
                    updatedAt = System.currentTimeMillis(),
                    workDir = config.workDir,
                ) ?: ThreadRecord(
                    id = it,
                    title = message.lineSequence().firstOrNull().orEmpty().take(48).ifBlank { it.take(12) },
                    updatedAt = System.currentTimeMillis(),
                    workDir = config.workDir,
                ),
            )
        }
        if (!turnCompleted) onEvent(CodexUiEvent.Completed(seenThreadId))
    }

    suspend fun runTerminal(
        workDirOverride: String?,
        commandText: String,
        onLine: suspend (String) -> Unit,
    ): Int {
        val config = configStore.load().let { saved ->
            workDirOverride?.takeIf { it.isNotBlank() }?.let { saved.copy(workDir = it) } ?: saved
        }
        val command = ShellCommand.terminal(config.workDir, commandText)
        return transport.execute(
            config = config,
            command = command,
            onStdoutLine = { onLine(it) },
            onStderrLine = { onLine("ERR: $it") },
        )
    }

    fun cancel() {
        transport.cancel()
    }

    private fun buildPrompt(message: String, location: PhoneLocation?): String {
        if (location == null) return message
        return buildString {
            append(message)
            append("\n\n<phone_location>")
            append("\nlatitude: ${location.latitude}")
            append("\nlongitude: ${location.longitude}")
            append("\naccuracy_meters: ${location.accuracyMeters}")
            append("\ncaptured_at: ${Instant.ofEpochMilli(location.capturedAt)}")
            append("\nsource: ${if (location.isCached) "user_explicit_share_last_known" else "user_explicit_share_current"}")
            append("\n</phone_location>")
        }
    }
}

internal fun isNonFatalCodexStderr(line: String): Boolean {
    val normalized = line.trim()
    return normalized == "Reading additional input from stdin..." ||
        normalized.contains("codex_models_manager::manager: failed to refresh available models:")
}
