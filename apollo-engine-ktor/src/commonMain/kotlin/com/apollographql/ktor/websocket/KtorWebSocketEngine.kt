package com.apollographql.ktor.websocket

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.network.websocket.WebSocket
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class KtorWebSocketEngine(
  private val client: HttpClient,
) : WebSocketEngine {

  constructor() : this(
    HttpClient {
      install(WebSockets)
    }
  )

  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val sendFrameChannel = Channel<Frame>(Channel.UNLIMITED)


  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    val newUrl = URLBuilder(url).apply {
      protocol = when (protocol) {
        URLProtocol.HTTPS -> URLProtocol.WSS
        URLProtocol.HTTP -> URLProtocol.WS
        URLProtocol.WS, URLProtocol.WSS -> protocol
        /* URLProtocol.SOCKS */else -> throw UnsupportedOperationException("SOCKS is not a supported protocol")
      }
    }.build()

    coroutineScope.launch {
      try {
        client.webSocket(
          request = {
            headers {
              headers.forEach {
                append(it.name, it.value)
              }
            }
            url(newUrl)
          },
        ) {
          listener.onOpen()
          coroutineScope {
            launch {
              sendFrames(this@webSocket)
            }
            try {
              receiveFrames(incoming, listener)
            } catch (e: Throwable) {
              val closeReason = closeReasonOrNull()
              val apolloException = if (closeReason != null) {
                ApolloWebSocketClosedException(
                  code = closeReason.code.toInt(),
                  reason = closeReason.message,
                  cause = e
                )
              } else {
                ApolloNetworkException(
                  message = "Web socket communication error",
                  platformCause = e
                )
              }
              listener.onError(apolloException)
            }
          }
        }
      } catch (e: Throwable) {
        listener.onError(ApolloNetworkException(message = "Web socket communication error", platformCause = e))
      } finally {
        // Not 100% sure this can happen. Better safe than sorry. close() is idempotent so it shouldn't hurt
        listener.onError(ApolloNetworkException(message = "Web socket communication error", platformCause = null))
      }
    }

    return object : WebSocket {
      override fun send(data: ByteArray) {
        sendFrameChannel.trySend(Frame.Binary(true, data))
      }

      override fun send(text: String) {
        sendFrameChannel.trySend(Frame.Text(text))
      }

      override fun close(code: Int, reason: String) {
        sendFrameChannel.trySend(Frame.Close(CloseReason(code.toShort(), reason)))
      }
    }
  }

  private suspend fun DefaultClientWebSocketSession.closeReasonOrNull(): CloseReason? {
    return try {
      closeReason.await()
    } catch (t: Throwable) {
      if (t is CancellationException) {
        throw t
      }
      null
    }
  }

  private suspend fun sendFrames(session: DefaultClientWebSocketSession) {
    while (true) {
      val frame = sendFrameChannel.receive()
      session.send(frame)
    }
  }

  private suspend fun receiveFrames(incoming: ReceiveChannel<Frame>, listener: WebSocketListener) {
    while (true) {
      when (val frame = incoming.receive()) {
        is Frame.Text -> listener.onMessage(frame.readText())
        is Frame.Binary -> listener.onMessage(frame.data)
        is Frame.Close -> {
          val reason = frame.readReason() ?: return

          listener.onClosed(reason.code.toInt(), reason.message)
        }

        else -> error("unknown frame type")
      }
    }
  }

  override fun close() = client.close()
}
