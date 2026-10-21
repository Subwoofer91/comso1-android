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
        extraWritableRoots: List<String> = emptyList(),
    ): String {
        val launcher = commandLauncher(codexCommand.ifBlank { "codex" })
        val preflight = if (codexCommand.isBlank() || codexCommand == "codex") {
            "command -v codex >/dev/null 2>&1 || { echo 'codex not found in non-interactive SSH shell. Set Codex command path in Settings or add codex to PATH.' >&2; echo \"PATH=${'$'}PATH\" >&2; exit 127; }; "
        } else {
            ""
        }
        val writableRoots = buildList {
            if (workDir.isNotBlank()) add(workDir)
            addAll(extraWritableRoots.filter { it.isNotBlank() })
            if (workDir != "/srv/agent") add("/srv/agent")
        }.distinct().joinToString(separator = ",", prefix = "[", postfix = "]") { root ->
            "\"" + root.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
        val permissionOptions = listOf(
            // Codex defaults non-interactive exec sessions to read-only. Give a new
            // session an explicit workspace-write profile with automatic approval
            // review and network access, instead of depending on whatever
            // ~/.codex/config.toml or the app-server happens to carry.
            // --approve-for-me already selects the workspace-write sandbox; passing
            // --sandbox alongside it is rejected by Codex CLI.
            "--approve-for-me",
            "-c sandbox_workspace_write.network_access=true",
            "-c ${quote("sandbox_workspace_write.writable_roots=$writableRoots")}",
        ).joinToString(" ")
        // `codex exec resume` rejects --approve-for-me/--sandbox, but it accepts the
        // same settings as config overrides. Without them a resumed turn silently
        // falls back to read-only + approval_policy=never.
        val resumePermissionOptions = listOf(
            "-c sandbox_mode=workspace-write",
            "-c approval_policy=on-request",
            "-c approvals_reviewer=auto_review",
            "-c sandbox_workspace_write.network_access=true",
            "-c ${quote("sandbox_workspace_write.writable_roots=$writableRoots")}",
        ).joinToString(" ")
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
            "$launcher exec $permissionOptions $options ${quote(message)}"
        } else {
            "$launcher exec resume $resumePermissionOptions $options ${quote(threadId)} ${quote(message)}"
        }
        return loginShell("${preflight}cd ${quote(workDir)} && exec $codex </dev/null")
    }

    fun codexAppServer(codexCommand: String): String {
        val launcher = commandLauncher(codexCommand.ifBlank { "codex" })
        return loginShell("exec timeout 20s $launcher app-server --listen stdio://")
    }

    fun codexInitializeRequest(): ByteArray {
        return "{\"method\":\"initialize\",\"id\":1,\"params\":{\"clientInfo\":{\"name\":\"comso1\",\"title\":\"Comso1\",\"version\":\"0.4.1\"}}}\n"
            .toByteArray(Charsets.UTF_8)
    }

    fun codexModelListRequest(): ByteArray {
        return listOf(
            "{\"method\":\"initialized\",\"params\":{}}",
            "{\"method\":\"model/list\",\"id\":2,\"params\":{\"limit\":100,\"includeHidden\":false}}",
        ).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
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
