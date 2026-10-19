package com.codexremote.protocol

object ShellCommand {
    fun codexExec(
        workDir: String,
        codexCommand: String,
        message: String,
        threadId: String?,
        model: String = "",
        reasoningEffort: String = "",
        imagePaths: List<String> = emptyList(),
    ): String {
        val launcher = commandLauncher(codexCommand.ifBlank { "codex" })
        val preflight = if (codexCommand.isBlank() || codexCommand == "codex") {
            "command -v codex >/dev/null 2>&1 || { echo 'codex not found in non-interactive SSH shell. Set Codex command path in Settings or add codex to PATH.' >&2; echo \"PATH=${'$'}PATH\" >&2; exit 127; }; "
        } else {
            ""
        }
        val options = buildList {
            add("--json")
            add("--skip-git-repo-check")
            if (model.isNotBlank()) add("--model ${quote(model)}")
            if (reasoningEffort.isNotBlank()) {
                add("--config ${quote("model_reasoning_effort=$reasoningEffort")}")
            }
            imagePaths.forEach { add("--image ${quote(it)}") }
        }.joinToString(" ")
        val codex = if (threadId.isNullOrBlank()) {
            "$launcher exec $options ${quote(message)}"
        } else {
            "$launcher exec resume $options ${quote(threadId)} ${quote(message)}"
        }
        return loginShell("${preflight}cd ${quote(workDir)} && exec $codex </dev/null")
    }

    fun codexModelList(codexCommand: String): String {
        val launcher = commandLauncher(codexCommand.ifBlank { "codex" })
        val requests = listOf(
            "{\"method\":\"initialize\",\"id\":1,\"params\":{\"clientInfo\":{\"name\":\"comso1\",\"title\":\"Comso1\",\"version\":\"0.4.0\"}}}",
            "{\"method\":\"initialized\",\"params\":{}}",
            "{\"method\":\"model/list\",\"id\":2,\"params\":{\"limit\":100,\"includeHidden\":false}}",
        ).joinToString("\\n", postfix = "\\n")
        val requestPipeline = "printf %b ${quote(requests)} | $launcher app-server --listen stdio://"
        val command = "timeout 20s bash -c ${quote(requestPipeline)}"
        return loginShell(command)
    }

    fun quote(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    private fun loginShell(command: String): String {
        val bootstrap = listOf(
            "export PATH=\"${'$'}HOME/.local/bin:${'$'}HOME/bin:${'$'}HOME/.npm-global/bin:${'$'}HOME/.npm-packages/bin:${'$'}HOME/.bun/bin:${'$'}HOME/.cargo/bin:/usr/local/bin:/usr/bin:/bin:${'$'}PATH\"",
            "[ -s \"${'$'}HOME/.profile\" ] && . \"${'$'}HOME/.profile\" >/dev/null 2>&1 || true",
            "[ -s \"${'$'}HOME/.bash_profile\" ] && . \"${'$'}HOME/.bash_profile\" >/dev/null 2>&1 || true",
            "[ -s \"${'$'}HOME/.bashrc\" ] && . \"${'$'}HOME/.bashrc\" >/dev/null 2>&1 || true",
            "[ -s \"${'$'}HOME/.nvm/nvm.sh\" ] && . \"${'$'}HOME/.nvm/nvm.sh\" >/dev/null 2>&1 && nvm use --silent default >/dev/null 2>&1 || true",
            "for d in \"${'$'}HOME\"/.nvm/versions/node/*/bin; do [ -d \"${'$'}d\" ] && export PATH=\"${'$'}d:${'$'}PATH\"; done",
            "export PATH=\"${'$'}HOME/.local/bin:${'$'}HOME/bin:${'$'}HOME/.npm-global/bin:${'$'}HOME/.npm-packages/bin:${'$'}HOME/.bun/bin:${'$'}HOME/.cargo/bin:/usr/local/bin:/usr/bin:/bin:${'$'}PATH\"",
            command,
        ).joinToString("; ")
        return "bash -lc ${quote(bootstrap)}"
    }

    private fun commandLauncher(command: String): String {
        return if (command.any(Char::isWhitespace)) command else quote(command)
    }
}
