package com.codexremote.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexRemoteSessionTest {
    @Test
    fun modelRefreshTimeoutIsNonFatal() {
        assertTrue(
            isNonFatalCodexStderr(
                "2026-10-09T16:04:10Z ERROR codex_models_manager::manager: failed to refresh available models: request timed out",
            ),
        )
        assertFalse(isNonFatalCodexStderr("authentication failed"))
    }
}
