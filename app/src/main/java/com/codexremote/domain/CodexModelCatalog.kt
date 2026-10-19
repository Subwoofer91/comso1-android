package com.codexremote.domain

data class CodexModelOption(
    val id: String,
    val label: String,
    val shortLabel: String,
    val description: String,
    val family: String,
    val supportedEfforts: List<String>,
    val defaultEffort: String = "",
    val isDefault: Boolean = false,
)

data class ReasoningEffortOption(
    val id: String,
    val label: String,
)

object CodexModelCatalog {
    private val defaultEffort = ReasoningEffortOption("", "默认")
    private val knownEfforts = mapOf(
        "none" to ReasoningEffortOption("none", "关闭"),
        "minimal" to ReasoningEffortOption("minimal", "最低"),
        "low" to ReasoningEffortOption("low", "低"),
        "medium" to ReasoningEffortOption("medium", "中"),
        "high" to ReasoningEffortOption("high", "高"),
        "xhigh" to ReasoningEffortOption("xhigh", "超高"),
        "max" to ReasoningEffortOption("max", "最高"),
        "ultra" to ReasoningEffortOption("ultra", "终极"),
    )

    private val standardEfforts = listOf("low", "medium", "high", "xhigh", "max")
    private val lunaEfforts = listOf("low", "medium", "high", "xhigh")

    val models: List<CodexModelOption> = listOf(
        CodexModelOption("", "VPS 默认模型", "VPS 默认", "使用 Codex 服务端配置", "默认", standardEfforts),
        CodexModelOption("gpt-5.6-sol", "GPT-5.6 Sol", "5.6 Sol", "复杂专业任务", "GPT-5.6", standardEfforts),
        CodexModelOption("gpt-5.6-terra", "GPT-5.6 Terra", "5.6 Terra", "质量、速度与成本均衡", "GPT-5.6", standardEfforts),
        CodexModelOption("gpt-5.6-luna", "GPT-5.6 Luna", "5.6 Luna", "快速、轻量的日常任务", "GPT-5.6", lunaEfforts),
        CodexModelOption("gpt-6-astra", "GPT-6 Astra", "6 Astra", "高难度编码与深度推理", "GPT-6", standardEfforts),
        CodexModelOption("gpt-6-sol", "GPT-6 Sol", "6 Sol", "通用编码与日常开发", "GPT-6", standardEfforts),
        CodexModelOption("gpt-6-luna", "GPT-6 Luna", "6 Luna", "低延迟的轻量任务", "GPT-6", lunaEfforts),
    )

    fun model(id: String, availableModels: List<CodexModelOption> = models): CodexModelOption {
        return availableModels.firstOrNull { it.id == id }
            ?: CodexModelOption(id, id.ifBlank { "VPS 默认模型" }, id.ifBlank { "VPS 默认" }, "自定义模型", "其他", standardEfforts)
    }

    fun effortsFor(
        modelId: String,
        availableModels: List<CodexModelOption> = models,
    ): List<ReasoningEffortOption> {
        val supported = model(modelId, availableModels).supportedEfforts.map { effort ->
            knownEfforts[effort] ?: ReasoningEffortOption(effort, effort.replaceFirstChar(Char::uppercase))
        }
        return listOf(defaultEffort) + supported
    }

    fun normalizeEffort(
        modelId: String,
        effortId: String,
        availableModels: List<CodexModelOption> = models,
    ): String {
        return effortId.takeIf { candidate -> effortsFor(modelId, availableModels).any { it.id == candidate } }.orEmpty()
    }
}
