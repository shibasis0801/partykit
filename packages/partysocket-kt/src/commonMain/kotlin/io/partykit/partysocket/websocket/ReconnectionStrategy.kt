package io.partykit.partysocket.websocket

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ReconnectionStrategy {
    suspend fun shouldReconnect(): Boolean
    suspend fun wait()
}

class ExponentialBackoffStrategy(
    val minDelay: Duration = (1 + Random.nextInt(0, 4)).seconds,
    val maxDelay: Duration = 10.seconds,
    val growFactor: Double = 1.3,
    val maxRetries: Int = 10,
) : ReconnectionStrategy {
    private var waitTime = minDelay
    private var retries = 0

    override suspend fun shouldReconnect(): Boolean {
        if (waitTime < maxDelay) {
            waitTime = (waitTime * growFactor)
                .coerceAtMost(maxDelay)
        }
        retries++
        return retries < maxRetries
    }

    override suspend fun wait() {
        delay(waitTime)
    }
}
