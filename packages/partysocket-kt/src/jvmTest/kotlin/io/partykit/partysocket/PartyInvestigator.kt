package io.partykit.partysocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.partykit.partysocket.util.generatePartyUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.internal.sse.ServerSentEventReader.Companion.options
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

val httpClient = HttpClient(OkHttp) {
    install(WebSockets)
    install(HttpTimeout)
}

fun createPartySocket() = PartySocket(httpClient, PartySocketOptions("chat.shibasis.dev", "my-new-room"))

class PartyInvestigator {
    @Test
    fun partyUrl() {
        val url = generatePartyUrl("https://chat.shibasis.dev", "my-new-room")
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
            partySocket.send(Frame.Text(message))
        }

        println("Received: $receivedMessages")
        assert(receivedMessages.isNotEmpty()) { "No messages received within the timeout." }
        assert(receivedMessages.find { it.contains(message) } != null) { "Echo Unsuccessful" }
    }
}
