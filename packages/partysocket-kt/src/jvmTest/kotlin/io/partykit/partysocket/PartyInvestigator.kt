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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

val httpClient = HttpClient(OkHttp) {
    install(WebSockets)
    install(HttpTimeout)
}

class PartyInvestigator {
    @Test
    fun workingNetwork() {
        val response = runBlocking {
            httpClient.get("https://docs.partykit.io/reference/partysocket-api/")
        }
        assertEquals(response.status, HttpStatusCode.OK)
    }

    @Test
    fun partyUrl() {
        val url = generatePartyUrl("https://chat.shibasis.dev", "my-new-room")
        println("PartyKit URL: $url")
        assert(url.length > 0)
    }

    @Test
    fun testSocket() = runBlocking {
        val partySocket = PartySocket(httpClient, "chat.shibasis.dev", "my-new-room")
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
}
