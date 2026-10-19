package com.codexremote.ssh

import com.codexremote.domain.VpsConfig
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JschSshTransport(
    private val hostKeyRepository: HostKeyRepository,
) : SshTransport {
    @Volatile private var activeChannel: Channel? = null
    @Volatile private var activeSession: Session? = null

    override suspend fun execute(
        config: VpsConfig,
        command: String,
        onStdoutLine: suspend (String) -> Unit,
        onStderrLine: suspend (String) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        require(config.host.isNotBlank()) { "SSH host is required" }
        require(config.username.isNotBlank()) { "SSH username is required" }
        require(config.privateKey.isNotBlank()) { "SSH private key is required" }

        val session = createSession(config)
        session.connect(15_000)

        val channel = session.openChannel("exec") as ChannelExec
        activeSession = session
        activeChannel = channel
        try {
            channel.setCommand(command)
            channel.setInputStream(ByteArrayInputStream(ByteArray(0)))
            val stdout = BufferedReader(InputStreamReader(channel.inputStream, StandardCharsets.UTF_8))
            val stderr = BufferedReader(InputStreamReader(channel.errStream, StandardCharsets.UTF_8))
            channel.connect(10_000)

            coroutineScope {
                val stdoutJob = launch(Dispatchers.IO) {
                    stdout.lineSequence().forEach { onStdoutLine(it) }
                }
                val stderrJob = launch(Dispatchers.IO) {
                    stderr.lineSequence().forEach { onStderrLine(it) }
                }
                while (!channel.isClosed) delay(100)
                stdoutJob.join()
                stderrJob.join()
            }
            channel.exitStatus
        } finally {
            activeChannel = null
            activeSession = null
            runCatching { channel.disconnect() }
            runCatching { session.disconnect() }
        }
    }

    override suspend fun upload(
        config: VpsConfig,
        files: List<UploadPayload>,
    ): List<String> = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext emptyList()
        val session = createSession(config)
        session.connect(15_000)
        val channel = session.openChannel("sftp") as ChannelSftp
        activeSession = session
        activeChannel = channel
        try {
            channel.connect(10_000)
            val base = channel.pwd().trimEnd('/')
            val relativeDir = ".codex-remote/uploads/${UUID.randomUUID()}"
            ensureDirectory(channel, relativeDir)
            files.mapIndexed { index, file ->
                val safeName = sanitizeFileName(file.name, index)
                val relativePath = "$relativeDir/$safeName"
                ByteArrayInputStream(file.bytes).use { channel.put(it, relativePath) }
                "$base/$relativePath"
            }
        } finally {
            activeChannel = null
            activeSession = null
            runCatching { channel.disconnect() }
            runCatching { session.disconnect() }
        }
    }

    override fun cancel() {
        runCatching { activeChannel?.disconnect() }
        runCatching { activeSession?.disconnect() }
    }

    private fun createSession(config: VpsConfig): Session {
        require(config.host.isNotBlank()) { "SSH host is required" }
        require(config.username.isNotBlank()) { "SSH username is required" }
        require(config.privateKey.isNotBlank()) { "SSH private key is required" }

        JSch.setConfig("keypairgen.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        JSch.setConfig("keypairgen_fromprivate.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        JSch.setConfig("ssh-ed25519", "com.jcraft.jsch.bc.SignatureEd25519")
        JSch.setConfig("ssh-ed448", "com.jcraft.jsch.bc.SignatureEd448")

        val passphrase = config.privateKeyPassphrase
            .takeIf { it.isNotBlank() }
            ?.toByteArray(StandardCharsets.UTF_8)
        val jsch = JSch()
        jsch.setHostKeyRepository(hostKeyRepository)
        jsch.addIdentity(
            "codex-remote-key",
            normalizePrivateKey(config.privateKey).toByteArray(StandardCharsets.UTF_8),
            null,
            passphrase,
        )
        return jsch.getSession(config.username, config.host, config.port).apply {
            setConfig("PreferredAuthentications", "publickey")
            setConfig("StrictHostKeyChecking", "yes")
        }
    }

    private fun ensureDirectory(channel: ChannelSftp, relativePath: String) {
        var current = ""
        relativePath.split('/').filter { it.isNotBlank() }.forEach { part ->
            current = if (current.isBlank()) part else "$current/$part"
            runCatching { channel.stat(current) }.getOrElse { channel.mkdir(current) }
        }
    }

    private fun sanitizeFileName(value: String, index: Int): String {
        val normalized = value.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_')
            .take(100)
        return normalized.ifBlank { "image_$index.jpg" }
    }

    private fun normalizePrivateKey(value: String): String {
        return value
            .replace("\\n", "\n")
            .trim()
            .plus("\n")
    }
}
