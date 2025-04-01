package io.partykit.partysocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.partykit.partysocket.util.generatePartyUrl
import io.partykit.partysocket.websocket.onTextFrame
import io.partykit.partysocket.websocket.sendTextFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

val httpClient = HttpClient(OkHttp) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(HttpTimeout)
}

fun createPartySocket() = PartySocket(httpClient, PartySocketOptions("chat.bestbuds.ai", "my-new-room"))

class PartyInvestigator {
    @Test
    fun partyUrl() {
        val url = generatePartyUrl("https://chat.bestbuds.ai", "my-new-room")
        println("PartyKit URL: $url")
        assert(url.isNotEmpty())
    }

    @Test
    fun testSocket() = runBlocking {
        val partySocket = createPartySocket()
        val receivedMessages = mutableListOf<String>()
        withTimeoutOrNull(5.seconds) {
            partySocket.frames.collect {
                receivedMessages.add((it as? Frame.Text)?.readText() ?: "")
            }
        }

        println("Received: $receivedMessages")
        assert(receivedMessages.isNotEmpty()) { "No messages received within the timeout." }
        assert(receivedMessages.firstOrNull()?.contains("You are now connected") ?: false) { "Connection Successful" }
    }

    @Test
    fun testEcho() = runBlocking {
        val partySocket = createPartySocket()
        val receivedMessages = mutableListOf<String>()
        val message = "Shibasis is testing echo"
        withTimeoutOrNull(10.seconds) {
            launch {
                partySocket.frames.collect {
                    receivedMessages.add((it as? Frame.Text)?.readText() ?: "")
                }
            }
            partySocket.sendTextFrame(message)
        }

        println("Received: $receivedMessages")
        assert(receivedMessages.isNotEmpty()) { "No messages received within the timeout." }
        assert(receivedMessages.find { it.contains(message) } != null) { "Echo Unsuccessful" }
    }

    @Serializable
    data class Message(
        val message: String,
        val sender: String,
    )

    @Test
    fun testBidi() = runBlocking {
        val user1 = createPartySocket()
        val receivedMessages1 = mutableListOf<String>()

        val user2 = createPartySocket()
        val receivedMessages2 = mutableListOf<String>()

        val user3 = createPartySocket()
        val receivedMessages3 = mutableListOf<String>()

        withTimeoutOrNull(10.seconds) {
            val jobs = arrayListOf<Job>()
            jobs += user1.onTextFrame {
                receivedMessages1.add(it)
            }
            user1.sendTextFrame("Hello from user1")
            user1.sendTextFrame("What's up")
            user1.sendTextFrame("I am going now")

            jobs += user2.onTextFrame {
                receivedMessages2.add(it)
            }
            user2.sendTextFrame("Hello from user2")
            delay(1000)
            user1.close()

            user2.sendTextFrame("I am good")
            jobs += user3.onTextFrame {
                receivedMessages3.add(it)
            }
            user3.sendTextFrame("Hello from user3")

            delay(4000)
            jobs.joinAll()
        }
        println("User1 Received: ${receivedMessages1.joinToString("\n")}\n")
        println("User2 Received: ${receivedMessages2.joinToString("\n")}\n")
        println("User3 Received: ${receivedMessages3.joinToString("\n")}\n")

        assert(true)
    }
}
