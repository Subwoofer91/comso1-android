package com.codexremote.ssh

import com.codexremote.domain.VpsConfig
import com.codexremote.protocol.ShellCommand
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.OutputStream
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JschPtyTerminal(
    private val hostKeyRepository: HostKeyRepository,
) {
    @Volatile private var activeChannel: ChannelShell? = null
    @Volatile private var activeSession: Session? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var disconnectRequested = false
    @Volatile private var lastPtySize: PtySize? = null

    suspend fun run(
        config: VpsConfig,
        workDir: String,
        initialColumns: Int,
        initialRows: Int,
        onConnected: suspend () -> Unit,
        onData: suspend (ByteArray) -> Unit,
    ) = withContext(Dispatchers.IO) {
        disconnectRequested = false
        lastPtySize = null
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
        lastPtySize = PtySize(initialColumns.coerceAtLeast(20), initialRows.coerceAtLeast(5))
        runCatching { channel.setEnv("TERM", "xterm-256color") }
        activeSession = session
        activeChannel = channel
        try {
            val input = channel.inputStream
            val terminalOutput = channel.outputStream
            output = terminalOutput
            channel.connect(10_000)
            val readyMarker = "\u001ECOMSO1_READY\u001F".toByteArray(StandardCharsets.UTF_8)
            terminalOutput.write(
                (
                    "cd -- ${ShellCommand.quote(workDir)} && printf '\\033[2J\\033[H'; " +
                        "printf '\\036COMSO1_READY\\037'\r"
                ).toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            terminalOutput.flush()

            val buffer = ByteArray(16 * 1024)
            val startupBuffer = ByteArrayOutputStream()
            var ready = false
            while (channel.isConnected && !channel.isClosed) {
                val count = try {
                    input.read(buffer)
                } catch (error: Throwable) {
                    if (disconnectRequested) break
                    throw IOException("SSH 数据通道读取失败：${error.message ?: error::class.java.simpleName}", error)
                }
                if (count < 0) break
                if (count <= 0) continue
                if (ready) {
                    onData(buffer.copyOf(count))
                    continue
                }

                startupBuffer.write(buffer, 0, count)
                val startupBytes = startupBuffer.toByteArray()
                val markerIndex = startupBytes.indexOf(readyMarker)
                if (markerIndex >= 0) {
                    ready = true
                    onConnected()
                    val contentStart = markerIndex + readyMarker.size
                    if (contentStart < startupBytes.size) {
                        onData(startupBytes.copyOfRange(contentStart, startupBytes.size))
                    }
                } else if (startupBytes.size > 256 * 1024) {
                    throw IOException("SSH 终端初始化输出过长，未收到就绪标记")
                }
            }
            if (!disconnectRequested) {
                val exitStatus = channel.exitStatus.takeIf { it >= 0 }?.let { "，退出码 $it" }.orEmpty()
                throw IOException("SSH 终端通道被远端关闭$exitStatus")
            }
        } finally {
            output = null
            activeChannel = null
            activeSession = null
            lastPtySize = null
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
        val next = PtySize(columns, rows)
        synchronized(this) {
            if (next == lastPtySize) return
            val channel = activeChannel ?: return
            if (!channel.isConnected || channel.isClosed) return
            runCatching {
                channel.setPtySize(next.columns, next.rows, widthPx.coerceAtLeast(0), heightPx.coerceAtLeast(0))
            }.onSuccess {
                lastPtySize = next
            }
        }
    }

    fun disconnect() {
        disconnectRequested = true
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
            setServerAliveInterval(15_000)
            setServerAliveCountMax(3)
        }
    }

    private fun normalizePrivateKey(value: String): String {
        return value.replace("\\n", "\n").trim().plus("\n")
    }

    private data class PtySize(
        val columns: Int,
        val rows: Int,
    )

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        if (size < needle.size) return -1
        for (start in 0..size - needle.size) {
            var matched = true
            for (offset in needle.indices) {
                if (this[start + offset] != needle[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return start
        }
        return -1
    }
}
