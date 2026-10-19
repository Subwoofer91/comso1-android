package com.codexremote.domain

import java.net.InetAddress
import java.net.ServerSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ConversationTitleGeneratorTest {
    private val generator = ConversationTitleGenerator()

    @Test
    fun sanitizesModelFormattingFromTitle() {
        assertEquals("修复安卓 SSH 连接", generator.sanitize("**修复安卓 SSH 连接**。\n说明"))
        assertEquals("VPS 部署排障", generator.sanitize("“VPS 部署排障”"))
    }

    @Test
    fun callsOpenAiCompatibleEndpointAndReturnsSanitizedTitle() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        var authorization = ""
        var requestBody = ""
        val serverThread = Thread {
            server.accept().use { socket ->
                val input = socket.getInputStream()
                val headerBytes = ArrayList<Byte>()
                var delimiter = 0
                while (delimiter < 4) {
                    val value = input.read()
                    if (value < 0) break
                    headerBytes += value.toByte()
                    delimiter = when {
                        delimiter == 0 && value == '\r'.code -> 1
                        delimiter == 1 && value == '\n'.code -> 2
                        delimiter == 2 && value == '\r'.code -> 3
                        delimiter == 3 && value == '\n'.code -> 4
                        value == '\r'.code -> 1
                        else -> 0
                    }
                }
                val headers = headerBytes.toByteArray().toString(Charsets.UTF_8)
                var contentLength = 0
                headers.lineSequence().forEach { line ->
                    when {
                        line.startsWith("Authorization:", ignoreCase = true) -> {
                            authorization = line.substringAfter(':').trim()
                        }
                        line.startsWith("Content-Length:", ignoreCase = true) -> {
                            contentLength = line.substringAfter(':').trim().toInt()
                        }
                    }
                }
                requestBody = input.readNBytes(contentLength).toString(Charsets.UTF_8)
                val response = """{"choices":[{"message":{"content":"**VPS 服务巡检**。"}}]}"""
                    .toByteArray(Charsets.UTF_8)
                socket.getOutputStream().buffered().use { output ->
                    output.write(
                        "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n"
                            .toByteArray(Charsets.UTF_8),
                    )
                    output.write(response)
                }
            }
        }
        serverThread.start()

        try {
            val title = runBlocking {
                generator.generate(
                    TitleApiConfig(
                        enabled = true,
                        baseUrl = "http://127.0.0.1:${server.localPort}",
                        model = "test-model",
                        apiKey = "test-key",
                    ),
                    userMessage = "检查服务",
                    assistantMessage = "服务正常",
                )
            }
            assertEquals("VPS 服务巡检", title)
            assertEquals("Bearer test-key", authorization)
            assertTrue(requestBody.contains("检查服务"))
            assertTrue(requestBody.contains("服务正常"))
            assertTrue(requestBody.contains("\"thinking\":{\"type\":\"disabled\"}"))
            assertTrue(requestBody.contains("\"max_tokens\":128"))
        } finally {
            server.close()
            serverThread.join(2_000)
        }
    }
}
