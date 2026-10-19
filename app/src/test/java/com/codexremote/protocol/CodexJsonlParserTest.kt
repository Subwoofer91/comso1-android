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

        assertTrue(fresh.contains("exec --json --skip-git-repo-check"))
        assertTrue(resumed.contains("exec resume --json --skip-git-repo-check"))
        assertTrue(fresh.contains("&& exec"))
        assertTrue(!fresh.contains("model_reasoning_effort="))
        assertTrue(!resumed.contains("model_reasoning_effort="))
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

        assertTrue(fresh.contains("exec --json --skip-git-repo-check --model"))
        assertTrue(resumed.contains("exec resume --json --skip-git-repo-check --model"))
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
        val command = ShellCommand.codexModelList("/home/codex/.local/bin/codex")

        assertTrue(command.contains("app-server --listen stdio://"))
        assertTrue(command.contains("model/list"))
        assertTrue(command.contains("initialized"))
        assertTrue(command.contains("timeout 20s"))
    }
}
