package io.partykit.partysocket.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.partykit.partysocket.PartySocket
import io.partykit.partysocket.PartySocketOptions
import io.partykit.partysocket.websocket.onTextFrame

@Composable
fun rememberPartySocket(
    httpClient: HttpClient,
    options: PartySocketOptions,
    onConnected: () -> Unit = {},
    onDisconnected: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onMessageReceived: (String) -> Unit = {}
): PartySocket {
    val socket = remember(options.host, options.room) {
        PartySocket(httpClient, options)
    }
    return socket
}
/*

1. allow json encode/decode
2. snapshotstatelist


 */