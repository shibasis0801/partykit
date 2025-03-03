package io.partykit.partysocket.util

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
    val normalizedHost = host.replace(Regex("^(http[s]?://|ws[s]?://)"), "").trimEnd('/')
    val protocol = if (normalizedHost.startsWith("localhost") ||
        normalizedHost.startsWith("127.0.0.1")
    ) "ws" else "wss"
    val basePath = prefix ?: "parties/$party/$room"
    val path = if (additionalPath.isNotEmpty()) "/$additionalPath" else ""

    val clientId = Uuid.random().toString()
    return "$protocol://$normalizedHost/$basePath$path?_pk=$clientId"
}
