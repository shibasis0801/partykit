package io.partykit.partysocket.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface Logger {
    operator fun invoke(message: String, error: Throwable? = null)
}

typealias UrlProvider = suspend () -> String

enum class Status {
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED
}

data class WebSocketOptions(
    val minUptime: Duration = 5.seconds,
    val connectionTimeout: Duration = 4.seconds,
    val maxEnqueuedMessages: Int = Int.MAX_VALUE,
    val startClosed: Boolean = false,
    val debug: Boolean = false,
    val logger: Logger? = null
)


open class WebSocket(
    val httpClient: HttpClient,
    protected var options: WebSocketOptions = WebSocketOptions(),
    protected var urlProvider: UrlProvider,
    protected var reconnectionStrategy: ReconnectionStrategy = ExponentialBackoffStrategy(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val status = atomic(Status.CLOSED)
    val connectionStatus: Status by status

    val frames = MutableSharedFlow<Frame>()
    val session = MutableStateFlow<DefaultClientWebSocketSession?>(null)

    private var closeChannel = Channel<CloseReason>(Channel.RENDEZVOUS)
    private var sendChannel = Channel<Frame>(options.maxEnqueuedMessages)


    init {
        scope.launch { open() }
    }

    private fun tearDown() {
        scope.cancel()
        closeChannel.close()
        sendChannel.close()
    }

    suspend fun open() = suspendCancellableCoroutine { continuation ->
        status.value = Status.CONNECTING
        scope.launch {
            try {
                withTimeout(options.connectionTimeout) {
                    val url = urlProvider()
                    httpClient.webSocket(url) {
                        session.value = this
                        status.value = Status.OPEN
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }

                        launch {
                            incoming.consumeEach { frames.emit(it) }
                        }
                        launch {
                            sendChannel.consumeEach { send(it) }
                        }
                        closeChannel.consumeEach { reason ->
                            session.value = null
                            status.value = Status.CLOSED
                            close(reason)
                        }
                    }
                }
            } catch (e: Throwable) {
                status.value = Status.CLOSED
                if (reconnectionStrategy.shouldReconnect()) {
                    // reconnect ?
                }
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


