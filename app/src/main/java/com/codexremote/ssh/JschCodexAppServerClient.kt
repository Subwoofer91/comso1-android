package com.codexremote.ssh

import com.codexremote.domain.VpsConfig
import com.codexremote.protocol.ShellCommand
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AppServerModelListExchange(
    val response: String,
    val stderr: List<String>,
)

class JschCodexAppServerClient(
    private val hostKeyRepository: HostKeyRepository,
) {
    suspend fun requestModelList(
        config: VpsConfig,
        codexCommand: String,
    ): AppServerModelListExchange = withContext(Dispatchers.IO) {
        val session = createSession(config)
        session.connect(15_000)
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(ShellCommand.codexAppServer(codexCommand))
        val stdout = channel.inputStream.bufferedReader(StandardCharsets.UTF_8)
        val stderr = channel.errStream.bufferedReader(StandardCharsets.UTF_8)
        val errors = mutableListOf<String>()
        try {
            val input = channel.outputStream
            channel.connect(10_000)
            input.write(ShellCommand.codexInitializeRequest())
            input.flush()

            var followUpSent = false
            var modelResponse: String? = null
            withTimeout(25_000) {
                while (modelResponse == null && !channel.isClosed) {
                    while (stderr.ready()) stderr.readLine()?.let(errors::add)
                    if (!stdout.ready()) {
                        delay(20)
                        continue
                    }
                    val line = stdout.readLine() ?: break
                    val json = parseJsonLine(line) ?: continue
                    when (json.optInt("id", -1)) {
                        INITIALIZE_REQUEST_ID -> if (!followUpSent) {
                            json.optJSONObject("error")?.let { error ->
                                throw IllegalStateException(error.optString("message", error.toString()))
                            }
                            input.write(ShellCommand.codexModelListRequest())
                            input.flush()
                            followUpSent = true
                        }
                        MODEL_LIST_REQUEST_ID -> modelResponse = line
                    }
                }
            }
            while (stderr.ready()) stderr.readLine()?.let(errors::add)
            AppServerModelListExchange(
                response = modelResponse ?: error("Codex app-server 未返回 model/list"),
                stderr = errors,
            )
        } finally {
            runCatching { channel.disconnect() }
            runCatching { session.disconnect() }
        }
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
            "comso1-model-catalog-key",
            normalizePrivateKey(config.privateKey).toByteArray(StandardCharsets.UTF_8),
            null,
            passphrase,
        )
        return jsch.getSession(config.username, config.host, config.port).apply {
            setConfig("PreferredAuthentications", "publickey")
            setConfig("StrictHostKeyChecking", "yes")
        }
    }

    private fun parseJsonLine(raw: String): JSONObject? {
        val clean = raw.replace(ANSI_ESCAPE, "").trim()
        val start = clean.indexOf('{')
        if (start < 0) return null
        return runCatching { JSONObject(clean.substring(start)) }.getOrNull()
    }

    private fun normalizePrivateKey(value: String): String {
        return value.replace("\\n", "\n").trim().plus("\n")
    }

    private companion object {
        const val INITIALIZE_REQUEST_ID = 1
        const val MODEL_LIST_REQUEST_ID = 2
        val ANSI_ESCAPE = Regex("\\u001B\\[[;?0-9]*[ -/]*[@-~]")
    }
}
