package com.codexremote.ssh

import com.codexremote.domain.VpsConfig

interface SshTransport {
    suspend fun execute(
        config: VpsConfig,
        command: String,
        onStdoutLine: suspend (String) -> Unit,
        onStderrLine: suspend (String) -> Unit,
    ): Int

    suspend fun upload(
        config: VpsConfig,
        files: List<UploadPayload>,
    ): List<String>

    fun cancel()
}

data class UploadPayload(
    val name: String,
    val bytes: ByteArray,
)
