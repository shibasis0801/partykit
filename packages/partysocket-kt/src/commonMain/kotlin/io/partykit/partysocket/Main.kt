package io.partykit.partysocket

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.utils.EmptyContent.status
import io.ktor.http.HttpMethod
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.partykit.partysocket.util.generatePartyUrl
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
Take care of closing properly
 */
open class WebSocket(
    val httpClient: HttpClient,
    val url: String,
    val timeout: Duration = 10.seconds,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    channelCapacity: Int = Channel.UNLIMITED,
) {
    val scope = CoroutineScope(dispatcher + SupervisorJob())

    enum class Status {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED
    }

    private val status = atomic(Status.CLOSED)
    var connectionStatus: Status by status
    val frames = MutableSharedFlow<Frame>()
    private val closeChannel = Channel<CloseReason>(Channel.RENDEZVOUS)
    private val sendChannel = Channel<Frame>(channelCapacity)

    init {
        scope.launch { open() }
    }

    suspend fun open() = suspendCancellableCoroutine { continuation ->
        status.value = Status.CONNECTING
        scope.launch {
            val timedOut = withTimeoutOrNull(timeout) {
                httpClient.webSocket(url) {
                    try {
                        status.value = Status.OPEN
                        continuation.resume(true)
                        launch {
                            incoming.consumeEach { frames.emit(it) }
                        }
                        launch {
                            sendChannel.consumeEach { send(it) }
                        }
                        closeChannel.consumeEach { reason ->
                            status.value = Status.CLOSED
                            close(reason)
                        }
                    } catch (e: Throwable) {
                        close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.message ?: ""))
                    }
                }
            }
            if (timedOut == null) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun send(frame: Frame) {
        sendChannel.send(frame)
    }

    suspend fun close(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "")) {
        if (status.compareAndSet(Status.OPEN, Status.CLOSING)) {
            closeChannel.send(reason)
            closeChannel.close()
        }
    }
}

open class PartySocket(
    httpClient: HttpClient,
    host: String,
    room: String
): WebSocket(httpClient, generatePartyUrl(host, room)) {

}
