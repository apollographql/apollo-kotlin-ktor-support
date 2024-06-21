package com.apollographql.ktor.ws

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.CLOSE_NORMAL
import com.apollographql.apollo3.network.ws.WebSocketConnection
import com.apollographql.apollo3.network.ws.WebSocketEngine
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString

class KtorWebSocketEngine(
    private val client: HttpClient,
) : WebSocketEngine {

  constructor() : this(
      HttpClient {
        install(WebSockets)
      }
  )

  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val receiveMessageChannel = Channel<String>(Channel.UNLIMITED)
  private val sendFrameChannel = Channel<Frame>(Channel.UNLIMITED)

  override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection = open(Url(url), headers)

  private suspend fun open(url: Url, headers: List<HttpHeader>): WebSocketConnection {
    val newUrl = URLBuilder(url).apply {
      protocol = when (url.protocol) {
        URLProtocol.HTTPS -> URLProtocol.WSS
        URLProtocol.HTTP -> URLProtocol.WS
        URLProtocol.WS, URLProtocol.WSS -> url.protocol
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
          coroutineScope {
            launch {
              sendFrames(this@webSocket)
            }
            try {
              receiveFrames(incoming)
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
              receiveMessageChannel.close(apolloException)
              throw e
            }
          }
        }
      } catch (e: Throwable) {
        receiveMessageChannel.close(ApolloNetworkException(message = "Web socket communication error", platformCause = e))
      } finally {
        // Not 100% sure this can happen. Better safe than sorry. close() is idempotent so it shouldn't hurt
        receiveMessageChannel.close(ApolloNetworkException(message = "Web socket communication error", platformCause = null))
      }
    }
    return object : WebSocketConnection {
      override suspend fun receive(): String {
        return receiveMessageChannel.receive()
      }

      override fun send(data: ByteString) {
        sendFrameChannel.trySend(Frame.Binary(true, data.toByteArray()))
      }

      override fun send(string: String) {
        sendFrameChannel.trySend(Frame.Text(string))
      }

      override fun close() {
        sendFrameChannel.trySend(Frame.Close(CloseReason(CLOSE_NORMAL.toShort(), "")))
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
      if (frame is Frame.Close) {
        // normal termination
        receiveMessageChannel.close()
      }
    }
  }

  private suspend fun receiveFrames(incoming: ReceiveChannel<Frame>) {
    while (true) {
      val frame = incoming.receive()
      when (frame) {
        is Frame.Text -> {
          receiveMessageChannel.trySend(frame.readText())
        }

        is Frame.Binary -> {
          receiveMessageChannel.trySend(frame.data.decodeToString())
        }

        else -> error("unknown frame type")
      }
    }
  }
}
