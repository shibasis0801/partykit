package io.partykit.partysocket

import io.ktor.client.*
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.invoke
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generatePartyUrl(
    host: String,
    room: String,
    party: String = "main",
    prefix: String? = null,
    additionalPath: String = ""
): String {
    // Remove protocol if present and trailing slash if any.
    val normalizedHost = host.replace(Regex("^(http[s]?://|ws[s]?://)"), "").trimEnd('/')
    // Determine protocol; here we default to secure for production.
    val protocol = if (normalizedHost.startsWith("localhost") ||
        normalizedHost.startsWith("127.0.0.1")
    ) "ws" else "wss"
    // Construct the base path.
    val basePath = prefix ?: "parties/$party/$room"
    // Optionally append any additional path.
    val path = if (additionalPath.isNotEmpty()) "/$additionalPath" else ""
    // Optionally add a query parameter for a unique client ID.
    val clientId = Uuid.random().toString()
    return "$protocol://$normalizedHost/$basePath$path?_pk=$clientId"
}

val webSocketFlow = MutableStateFlow<String>("")
suspend fun HttpClient.partySocket(url: String) {
    webSocket(url) {
        println("Connected!")
        while(true) {
            val message = incoming.receive() as? Frame.Text ?: continue
            webSocketFlow.value = message.readText()
        }
    }
}