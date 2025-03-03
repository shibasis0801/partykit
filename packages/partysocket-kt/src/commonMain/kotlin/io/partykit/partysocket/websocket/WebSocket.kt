package io.partykit.partysocket.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias Logger = (error: Throwable?, message: String) -> Unit
typealias UrlProvider = suspend () -> String

enum class Status {
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED
}

data class WebSocketOptions(
    val minReconnectionDelay: Duration = (1 + Random.nextInt(0, 4)).seconds,
    val maxReconnectionDelay: Duration = 10.seconds,
    val reconnectionDelayGrowFactor: Double = 1.3,
    val minUptime: Duration = 5.seconds,
    val connectionTimeout: Duration = 4.seconds,
    val maxRetries: Int = Int.MAX_VALUE,
    val maxEnqueuedMessages: Int = Int.MAX_VALUE,
    val startClosed: Boolean = false,
    val debug: Boolean = false,
    val logger: Logger = { _, _ -> }
)


open class WebSocket(
    val httpClient: HttpClient,
    protected var options: WebSocketOptions = WebSocketOptions(),
    protected var urlProvider: UrlProvider,
    protected var reconnectionStrategy: ReconnectionStrategy = ExponentialBackoffStrategy(options),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val status = atomic(Status.CLOSED)
    val connectionStatus: Status by status

    val frames = MutableSharedFlow<Frame>()

    private val closeChannel = Channel<CloseReason>(Channel.RENDEZVOUS)
    private val sendChannel = Channel<Frame>(options.maxEnqueuedMessages)

    init {
        scope.launch { open() }
    }

    suspend fun open() = suspendCancellableCoroutine { continuation ->
        status.value = Status.CONNECTING
        scope.launch {
            val url = urlProvider()
            val timedOut = withTimeoutOrNull(options.connectionTimeout) {
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
