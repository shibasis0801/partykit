@file:OptIn(ExperimentalUuidApi::class)
package io.partykit.partysocket.util

import io.partykit.partysocket.PartySocketOptions
import io.partykit.partysocket.websocket.UrlProvider
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private fun String.isInternal(): Boolean {
    return (startsWith("localhost:") ||
            startsWith("127.0.0.1:") ||
            startsWith("192.168.") ||
            startsWith("10.") ||
            (startsWith("172.") &&
                    split(".").getOrNull(1)?.toIntOrNull()?.let { it >= 16 && it <= 31 } == true
            ) ||
            startsWith("[::ffff:7f00:1]:")
    )
}

data class PartyInfo(
    val host: String,
    val path: String,
    val room: String?,
    val name: String,
    val protocol: String,
    val partyUrl: String,
    val urlProvider: UrlProvider
)

fun PartySocketOptions.getPartyInfo(
    defaultProtocol: String,
    defaultParams: Map<String, String> = emptyMap()
): PartyInfo {
    val normalizedHost = host.replace(Regex("^(http|https|ws|wss)://"), "").trimEnd('/')
    if (path?.startsWith("/") == true) {
        throw IllegalArgumentException("path must not start with a slash")
    }

    val normalizedPath = path?.let { "/$it" } ?: ""

    val finalProtocol = protocol ?: (
            if (normalizedHost.isInternal()) defaultProtocol else "${defaultProtocol}s"
    )

    val baseUrl = "$finalProtocol://$normalizedHost/${prefix ?: "parties/$party/$room"}$normalizedPath"

    val urlProvider = suspend {
        val query = queryProvider()

        // todo not encodeUriComponent, fix later
        val queryString = (defaultParams + query).entries
            .joinToString("&") { (key, value) -> "$key=$value" }

        "$baseUrl?$queryString"
    }

    return PartyInfo(
        host = normalizedHost,
        path = normalizedPath,
        room = room,
        name = party,
        protocol = finalProtocol,
        partyUrl = baseUrl,
        urlProvider = urlProvider,
    )
}

fun generatePartyUrl(
    host: String,
    room: String,
    party: String = "main",
    prefix: String? = null,
    additionalPath: String = ""
): String {
    val normalizedHost = host.replace(Regex("^(http[s]?://|ws[s]?://)"), "").trimEnd('/')
    val protocol = if (normalizedHost.isInternal()) "ws" else "wss"
    val basePath = prefix ?: "parties/$party/$room"
    val path = if (additionalPath.isNotEmpty()) "/$additionalPath" else ""

    val clientId = Uuid.random().toString()
    return "$protocol://$normalizedHost/$basePath$path?_pk=$clientId"
}
