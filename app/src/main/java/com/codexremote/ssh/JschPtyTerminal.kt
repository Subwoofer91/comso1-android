package com.codexremote.ssh

import com.codexremote.domain.VpsConfig
import com.codexremote.protocol.ShellCommand
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JschPtyTerminal(
    private val hostKeyRepository: HostKeyRepository,
) {
    @Volatile private var activeChannel: ChannelShell? = null
    @Volatile private var activeSession: Session? = null
    @Volatile private var output: OutputStream? = null

    suspend fun run(
        config: VpsConfig,
        workDir: String,
        initialColumns: Int,
        initialRows: Int,
        onConnected: suspend () -> Unit,
        onData: suspend (ByteArray) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val session = createSession(config)
        session.connect(15_000)
        val channel = session.openChannel("shell") as ChannelShell
        channel.setPty(true)
        channel.setPtyType(
            "xterm-256color",
            initialColumns.coerceAtLeast(20),
            initialRows.coerceAtLeast(5),
            0,
            0,
        )
        runCatching { channel.setEnv("TERM", "xterm-256color") }

        activeSession = session
        activeChannel = channel
        try {
            val input = channel.inputStream
            val terminalOutput = channel.outputStream
            output = terminalOutput
            channel.connect(10_000)
            terminalOutput.write(
                "cd -- ${ShellCommand.quote(workDir)}\r".toByteArray(StandardCharsets.UTF_8),
            )
            terminalOutput.flush()
            onConnected()

            val buffer = ByteArray(16 * 1024)
            while (channel.isConnected && !channel.isClosed) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) onData(buffer.copyOf(count))
            }
        } finally {
            output = null
            activeChannel = null
            activeSession = null
            runCatching { channel.disconnect() }
            runCatching { session.disconnect() }
        }
    }

    fun write(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val current = output ?: return
        synchronized(this) {
            runCatching {
                current.write(bytes)
                current.flush()
            }
        }
    }

    fun resize(columns: Int, rows: Int, widthPx: Int, heightPx: Int) {
        if (columns < 2 || rows < 2) return
        runCatching {
            activeChannel?.setPtySize(
                columns,
                rows,
                widthPx.coerceAtLeast(0),
                heightPx.coerceAtLeast(0),
            )
        }
    }

    fun disconnect() {
        output = null
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
            "comso1-terminal-key",
            normalizePrivateKey(config.privateKey).toByteArray(StandardCharsets.UTF_8),
            null,
            passphrase,
        )
        return jsch.getSession(config.username, config.host, config.port).apply {
            setConfig("PreferredAuthentications", "publickey")
            setConfig("StrictHostKeyChecking", "yes")
        }
    }

    private fun normalizePrivateKey(value: String): String {
        return value.replace("\\n", "\n").trim().plus("\n")
    }
}
