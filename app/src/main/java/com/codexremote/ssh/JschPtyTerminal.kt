package com.codexremote.ssh

import android.util.Log
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JschPtyTerminal(
    private val hostKeyRepository: HostKeyRepository,
) {
    @Volatile private var activeChannel: ChannelShell? = null
    @Volatile private var activeSession: Session? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var disconnectRequested = false
    @Volatile private var lastPtySize: PtySize? = null
    @Volatile private var desiredPtySize: PtyRequest? = null

    // Only one shell session may exist at a time. Reconnects must wait for the
    // previous read loop to clean up, otherwise its finally block clears the new
    // connection's channel and the PTY silently stops accepting resizes.
    private val runMutex = Mutex()

    suspend fun run(
        config: VpsConfig,
        workDir: String,
        initialColumns: Int,
        initialRows: Int,
        onConnected: suspend () -> Unit,
        onData: suspend (ByteArray) -> Unit,
    ) = runMutex.withLock {
        runSession(config, workDir, initialColumns, initialRows, onConnected, onData)
    }

    private suspend fun runSession(
        config: VpsConfig,
        workDir: String,
        initialColumns: Int,
        initialRows: Int,
        onConnected: suspend () -> Unit,
        onData: suspend (ByteArray) -> Unit,
    ) = withContext(Dispatchers.IO) {
        disconnectRequested = false
        lastPtySize = null
        desiredPtySize = null
        val session = createSession(config)
        session.connect(15_000)
        // Recording the latest grid before the PTY is created avoids a stale initial
        // size when the view is resized (for example by the keyboard) while SSH connects.
        val startup = synchronized(this@JschPtyTerminal) { desiredPtySize }
            ?: PtyRequest(initialColumns, initialRows, 0, 0)
        val startupColumns = startup.columns.coerceAtLeast(2)
        val startupRows = startup.rows.coerceAtLeast(2)
        val channel = session.openChannel("shell") as ChannelShell
        channel.setPty(true)
        channel.setPtyType(
            "xterm-256color",
            startupColumns,
            startupRows,
            startup.widthPx,
            startup.heightPx,
        )
        lastPtySize = PtySize(startupColumns, startupRows)
        Log.d(TAG, "pty created cols=$startupColumns rows=$startupRows")
        runCatching { channel.setEnv("TERM", "xterm-256color") }
        activeSession = session
        activeChannel = channel
        try {
            val input = channel.inputStream
            val terminalOutput = channel.outputStream
            output = terminalOutput
            channel.connect(10_000)
            synchronized(this@JschPtyTerminal) {
                desiredPtySize?.let { applyPtySizeLocked(channel, it) }
            }
            Log.d(TAG, "pty connected cols=${lastPtySize?.columns} rows=${lastPtySize?.rows}")
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
        val request = PtyRequest(
            columns = columns,
            rows = rows,
            widthPx = widthPx.coerceAtLeast(0),
            heightPx = heightPx.coerceAtLeast(0),
        )
        synchronized(this) {
            val gridChanged = desiredPtySize?.let {
                it.columns != request.columns || it.rows != request.rows
            } ?: true
            // Always remember the newest grid, even before the shell channel is ready.
            // The run loop applies it once the channel connects, so the PTY can never
            // stay stuck at the size it was created with.
            desiredPtySize = request
            val channel = activeChannel
            if (channel == null) {
                if (gridChanged) Log.d(TAG, "resize deferred (no channel) cols=$columns rows=$rows")
                return
            }
            if (!channel.isConnected || channel.isClosed) {
                if (gridChanged) Log.d(TAG, "resize deferred (channel not connected) cols=$columns rows=$rows")
                return
            }
            applyPtySizeLocked(channel, request)
        }
    }

    private fun applyPtySizeLocked(channel: ChannelShell, request: PtyRequest): Boolean {
        val previous = lastPtySize
        if (previous != null && previous.columns == request.columns && previous.rows == request.rows) {
            return true
        }
        return runCatching {
            channel.setPtySize(request.columns, request.rows, request.widthPx, request.heightPx)
        }.onSuccess {
            lastPtySize = PtySize(request.columns, request.rows)
            Log.d(TAG, "pty size applied cols=${request.columns} rows=${request.rows}")
        }.onFailure {
            Log.w(TAG, "pty size failed cols=${request.columns} rows=${request.rows}: $it")
        }.isSuccess
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

    private data class PtyRequest(
        val columns: Int,
        val rows: Int,
        val widthPx: Int,
        val heightPx: Int,
    )

    private companion object {
        private const val TAG = "ComsoTerminal"
    }

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
