package com.codexremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SandboxDenialTest {
    @Test
    fun detectsReadOnlyProtectedConfigWrite() {
        val output = "/bin/bash: line 1: /home/codex/.codex/config.toml: Read-only file system"
        val denial = detectSandboxDenial("/bin/bash -lc 'printf x > config.toml'", output)

        assertEquals("/home/codex/.codex/config.toml", denial?.path)
        assertEquals("/home/codex/.codex", denial?.parentDir)
        assertEquals("/bin/bash -lc 'printf x > config.toml'", denial?.command)
    }

    @Test
    fun detectsPermissionDeniedOutsideWorkspace() {
        val denial = detectSandboxDenial("cmd", "bash: /etc/nginx/nginx.conf: Permission denied")
        assertEquals("/etc/nginx/nginx.conf", denial?.path)
        assertEquals("/etc/nginx", denial?.parentDir)
    }

    @Test
    fun ignoresUnrelatedOutput() {
        assertNull(detectSandboxDenial("cmd", "hello world"))
        assertNull(detectSandboxDenial("cmd", "exit code 1"))
    }
}
