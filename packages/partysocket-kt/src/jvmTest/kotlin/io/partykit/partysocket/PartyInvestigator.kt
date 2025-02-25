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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun openSocket() {
        val url = generatePartyUrl("chat.shibasis.dev", "my-new-room")
        val receivedMessages = mutableListOf<String>()
        val maxMessages = 5  // Or however many you want to quickly check
        val timeoutDuration = 10.seconds // Timeout after 10 seconds
        val sendMessages = false // flag for sending message

        runBlocking {
            val job = launch {
                try {
                    httpClient.webSocket(url) {
                        println("Connected!")

                        // Use a conflated flow to only process the *latest* incoming message.
                        // conflate is deprecated for now, but it is a quick solution
                        @OptIn(kotlinx.coroutines.FlowPreview::class)
                        val incomingFlow = incoming.consumeAsFlow()
                            .map { (it as? Frame.Text)?.readText() ?: "" }
                            .conflate()


                        // Collect messages, adding them to the list, until we hit our max.
                        incomingFlow.take(maxMessages).collect { message ->
                            println("Received: $message")
                            receivedMessages.add(message)
                        }
                        println("Closing connection after receiving $maxMessages messages.")
                        close(CloseReason(CloseReason.Codes.NORMAL, "Collected enough messages"))
                    }
                } catch (e: Exception) {
                    println("Error: $e")
                }
            }

            // Add a timeout to the whole operation.
            withTimeout(timeoutDuration) {
                job.join()
            }

            // Assert that we received *some* messages (optional, but good for testing)
            assert(receivedMessages.isNotEmpty()) { "No messages received within the timeout." }
        }
    }
}