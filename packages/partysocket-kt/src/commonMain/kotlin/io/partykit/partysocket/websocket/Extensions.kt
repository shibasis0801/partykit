package io.partykit.partysocket.websocket

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

suspend fun WebSocket.sendText(message: String) = send(Frame.Text(message))
suspend fun WebSocket.sendBinary(message: ByteArray) = send(Frame.Binary(true, message))


suspend fun WebSocket.textFrames() =
    frames
        .filter { it is Frame.Text }
        .map { (it as Frame.Text).readText() }

suspend inline fun WebSocket.onTextMessage(crossinline collector: (String) -> Unit) =
    textFrames().collect {
        collector(it)
    }

suspend fun WebSocket.binaryFrames() =
    frames
        .filter { it is Frame.Binary }
        .map { it.data }

//suspend inline fun <reified T> WebSocket.sendObject(data: T) =
//    runCatching {
//        session.value?.sendSerialized(data, typeInfo<T>()) ?: error("Session is null")
//    }
//
//suspend inline fun <reified T> WebSocket.objectFrames(): Flow<T> {
//    val converter = session.value?.converter ?: return flowOf()
//    return frames
//        .map {
//            runCatching {
//                converter.deserialize(Charsets.UTF_8, typeInfo<T>(), it)
//            }
//        }
//        .filter { it.isSuccess && it.getOrNull() != null }
//        .map { it.getOrNull() as T }
//}