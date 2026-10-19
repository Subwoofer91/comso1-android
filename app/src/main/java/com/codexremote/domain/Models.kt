package com.codexremote.domain

data class VpsConfig(
    val host: String = "",
    val port: Int = 22,
    val username: String = "codex",
    val workDir: String = "/home/codex",
    val codexCommand: String = "codex",
    val model: String = "",
    val reasoningEffort: String = "",
    val privateKey: String = "",
    val privateKeyPassphrase: String = "",
)

data class TitleApiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-flash",
    val apiKey: String = "",
)

data class ThreadRecord(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val workDir: String = "",
    val titleGenerated: Boolean = false,
    val titleLocked: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

data class MessageAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val localPath: String,
    val remotePath: String = "",
)

data class PhoneLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAt: Long,
    val isCached: Boolean = false,
)

enum class ProcessStepKind {
    Commentary,
    Command,
    Tool,
    Error,
}

data class ProcessStep(
    val id: String,
    val kind: ProcessStepKind,
    val title: String,
    val content: String = "",
    val status: String = "",
)

data class ChatItem(
    val id: String,
    val role: ChatRole,
    val text: String,
    val status: String? = null,
    val details: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    val location: PhoneLocation? = null,
    val processSteps: List<ProcessStep> = emptyList(),
)

enum class ChatRole {
    User,
    Agent,
    Process,
    Tool,
    Error,
    System,
}

enum class AgentMessagePhase {
    Commentary,
    FinalAnswer,
    Unknown,
}

sealed interface CodexUiEvent {
    data class AgentText(
        val text: String,
        val phase: AgentMessagePhase = AgentMessagePhase.Unknown,
    ) : CodexUiEvent
    data class ToolCall(
        val title: String,
        val details: String,
        val status: String = "",
    ) : CodexUiEvent
    data class Command(
        val command: String,
        val output: String,
        val exitCode: Int? = null,
        val status: String = "",
    ) : CodexUiEvent
    data class Error(val message: String) : CodexUiEvent
    data class ThreadFound(val threadId: String) : CodexUiEvent
    data class Completed(val threadId: String?) : CodexUiEvent
    data class Raw(val line: String) : CodexUiEvent
}
