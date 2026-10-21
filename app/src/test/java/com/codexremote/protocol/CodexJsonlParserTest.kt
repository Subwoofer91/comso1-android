package com.codexremote.protocol

import com.codexremote.domain.CodexUiEvent
import com.codexremote.domain.AgentMessagePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexJsonlParserTest {
    private val parser = CodexJsonlParser()

    @Test
    fun parsesThreadStarted() {
        val events = parser.parse("""{"type":"thread.started","thread_id":"abc-123"}""")
        assertEquals(listOf(CodexUiEvent.ThreadFound("abc-123")), events)
    }

    @Test
    fun parsesCompletedAgentMessageWithoutCompletingTurn() {
        val events = parser.parse(
            """{"type":"item.completed","item":{"id":"item_0","type":"agent_message","text":"你好"}}""",
        )
        assertEquals(listOf(CodexUiEvent.AgentText("你好", AgentMessagePhase.Unknown)), events)
        assertTrue(events.none { it is CodexUiEvent.Completed })
    }

    @Test
    fun parsesCompletedCommand() {
        val events = parser.parse(
            """{"type":"item.completed","item":{"type":"command_execution","command":"pwd","aggregated_output":"/home/codex","status":"completed"}}""",
        )
        val command = events.single() as CodexUiEvent.Command
        assertEquals("pwd", command.command)
        assertEquals("/home/codex", command.output)
        assertEquals("completed", command.status)
    }

    @Test
    fun separatesCommentaryFromFinalAnswer() {
        val commentary = parser.parse(
            """{"type":"item.completed","item":{"type":"agent_message","phase":"commentary","text":"正在检查"}}""",
        ).single() as CodexUiEvent.AgentText
        val finalAnswer = parser.parse(
            """{"type":"item.completed","item":{"type":"agent_message","phase":"final_answer","text":"检查完成"}}""",
        ).single() as CodexUiEvent.AgentText

        assertEquals(AgentMessagePhase.Commentary, commentary.phase)
        assertEquals(AgentMessagePhase.FinalAnswer, finalAnswer.phase)
    }

    @Test
    fun onlyTurnCompletedCompletesTheTurn() {
        assertEquals(
            listOf(CodexUiEvent.Completed(null)),
            parser.parse("""{"type":"turn.completed","usage":{"input_tokens":1}}"""),
        )
    }

    @Test
    fun parsesTurnFailure() {
        val events = parser.parse(
            """{"type":"turn.failed","error":{"message":"request failed"}}""",
        )
        assertEquals(listOf(CodexUiEvent.Error("request failed")), events)
    }

    @Test
    fun newAndResumeCommandsSkipRepositoryCheck() {
        val fresh = ShellCommand.codexExec("/home/codex", "codex", "hello", null)
        val resumed = ShellCommand.codexExec("/home/codex/project", "codex", "again", "abc-123")

        assertTrue(fresh.contains("--json --skip-git-repo-check"))
        assertTrue(resumed.contains("exec resume "))
        assertTrue(resumed.contains("--json --skip-git-repo-check"))
        assertTrue(fresh.contains("&& exec"))
        assertTrue(!fresh.contains("model_reasoning_effort="))
        assertTrue(!resumed.contains("model_reasoning_effort="))
    }

    @Test
    fun newSessionRequestsWorkspaceWriteProfile() {
        val fresh = ShellCommand.codexExec("/home/codex", "codex", "hello", null)
        val resumed = ShellCommand.codexExec("/home/codex/project", "codex", "again", "abc-123")

        assertTrue(fresh.contains("--approve-for-me"))
        // --approve-for-me implies workspace-write and is mutually exclusive with
        // --sandbox, so the app must not send both.
        assertTrue(!fresh.contains("--sandbox"))
        assertTrue(!fresh.contains("-s workspace-write"))
        assertTrue(fresh.contains("sandbox_workspace_write.network_access=true"))
        assertTrue(
            fresh,
            fresh.contains("""sandbox_workspace_write.writable_roots=["/home/codex","/srv/agent"]"""),
        )
        // `codex exec resume` rejects --approve-for-me/--sandbox, so resume uses the
        // equivalent config overrides instead of silently falling back to read-only.
        assertTrue(!resumed.contains("--approve-for-me"))
        assertTrue(!resumed.contains("--sandbox"))
        assertTrue(resumed.contains("sandbox_mode=workspace-write"))
        assertTrue(resumed.contains("approval_policy=on-request"))
        assertTrue(resumed.contains("approvals_reviewer=auto_review"))
        assertTrue(resumed.contains("sandbox_workspace_write.network_access=true"))
        assertTrue(
            resumed,
            resumed.contains(
                """sandbox_workspace_write.writable_roots=["/home/codex/project","/srv/agent"]""",
            ),
        )
    }

    @Test
    fun extraWritableRootsAreMergedForSandboxEscalation() {
        val resumed = ShellCommand.codexExec(
            workDir = "/home/codex",
            codexCommand = "codex",
            message = "retry",
            threadId = "abc-123",
            extraWritableRoots = listOf("/home/codex/.codex"),
        )

        assertTrue(
            resumed,
            resumed.contains(
                """sandbox_workspace_write.writable_roots=["/home/codex","/home/codex/.codex","/srv/agent"]""",
            ),
        )
    }

    @Test
    fun modelAndReasoningOptionsApplyToNewAndResumedThreads() {
        val fresh = ShellCommand.codexExec(
            "/home/codex",
            "codex",
            "hello",
            null,
            model = "gpt-6-sol",
            reasoningEffort = "high",
        )
        val resumed = ShellCommand.codexExec(
            "/home/codex",
            "codex",
            "again",
            "abc-123",
            model = "gpt-6-sol",
            reasoningEffort = "high",
        )

        assertTrue(fresh.contains("--json --skip-git-repo-check"))
        assertTrue(resumed.contains("exec resume "))
        assertTrue(resumed.contains("--json --skip-git-repo-check"))
        assertTrue(fresh.contains("--model"))
        assertTrue(resumed.contains("--model"))
        assertTrue(fresh.contains("gpt-6-sol"))
        assertTrue(resumed.contains("gpt-6-sol"))
        assertTrue(fresh.contains("model_reasoning_effort=high"))
        assertTrue(resumed.contains("model_reasoning_effort=high"))
    }

    @Test
    fun imagePathsArePassedAsQuotedImageOptions() {
        val command = ShellCommand.codexExec(
            workDir = "/home/codex",
            codexCommand = "codex",
            message = "看看图片",
            threadId = null,
            imagePaths = listOf("/home/codex/.codex-remote/uploads/a/photo one.png"),
        )

        assertTrue(command.contains("--image"))
        assertTrue(command.contains("photo one.png"))
    }

    @Test
    fun modelListCommandUsesInitializedAppServerProtocol() {
        val command = ShellCommand.codexAppServer("/home/codex/.local/bin/codex")
        val initialize = ShellCommand.codexInitializeRequest().decodeToString()
        val request = ShellCommand.codexModelListRequest().decodeToString()

        assertTrue(command.contains("app-server --listen stdio://"))
        assertTrue(command.contains("timeout 20s"))
        assertTrue(initialize.contains("initialize"))
        assertTrue(initialize.contains("\"id\":1"))
        assertTrue(request.contains("model/list"))
        assertTrue(request.contains("initialized"))
    }
}
