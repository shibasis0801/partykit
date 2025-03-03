package io.partykit.partysocket.websocket

import kotlinx.coroutines.delay

interface ReconnectionStrategy {
    suspend fun shouldReconnect(): Boolean
    suspend fun wait()
}

class ExponentialBackoffStrategy(
    private val webSocketOptions: WebSocketOptions
) : ReconnectionStrategy {
    private var waitTime = webSocketOptions.minReconnectionDelay
    private var retries = 0

    override suspend fun shouldReconnect(): Boolean {
        if (waitTime < webSocketOptions.maxReconnectionDelay) {
            waitTime = (waitTime * webSocketOptions.reconnectionDelayGrowFactor)
                .coerceAtMost(webSocketOptions.maxReconnectionDelay)
        }
        retries++
        return retries < webSocketOptions.maxRetries
    }

    override suspend fun wait() {
        delay(waitTime)
    }
}
