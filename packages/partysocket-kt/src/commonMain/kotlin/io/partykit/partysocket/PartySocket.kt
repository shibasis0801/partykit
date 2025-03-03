@file:OptIn(ExperimentalUuidApi::class)
package io.partykit.partysocket

import io.ktor.client.*
import io.partykit.partysocket.util.PartyInfo
import io.partykit.partysocket.util.getPartyInfo
import io.partykit.partysocket.websocket.WebSocketOptions
import io.partykit.partysocket.websocket.WebSocket
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

typealias QueryProvider = suspend () -> Map<String, String>

data class PartySocketOptions(
    val host: String,
    val room: String,
    val party: String = "main",
    val prefix: String? = null,
    val path: String? = null,
    val protocol: String? = null,
    val queryProvider: QueryProvider = { emptyMap() },
    val webSocketOptions: WebSocketOptions = WebSocketOptions(),
    val id: String = Uuid.random().toString(),
)

/**
 * Differences from the TypeScript version,
 * 1. No subprotocol support yet, need to build
 */

open class PartySocket(
    httpClient: HttpClient,
    options: PartySocketOptions,
    private var partyInfo: PartyInfo = options.getPartyInfo("ws", mapOf("_pk" to options.id))
): WebSocket(httpClient, options.webSocketOptions,partyInfo.urlProvider) {

}
