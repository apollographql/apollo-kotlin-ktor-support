package com.apollographql.ktor.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.http.HttpEngine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import okio.Buffer
import kotlin.coroutines.cancellation.CancellationException

class KtorHttpEngine(
    private val client: HttpClient,
): HttpEngine {

  private var disposed = false

  /**
   * @param timeoutMillis: The timeout in milliseconds used both for the connection and socket read.
   */
  constructor(timeoutMillis: Long = 60_000) : this(timeoutMillis, timeoutMillis)

  /**
   * @param connectTimeoutMillis The connection timeout in milliseconds. The connection timeout is the time period in which a client should establish a connection with a server.
   * @param readTimeoutMillis The socket read timeout in milliseconds. On JVM and Apple this maps to [HttpTimeout.HttpTimeoutCapabilityConfiguration.socketTimeoutMillis], on JS
   * this maps to [HttpTimeout.HttpTimeoutCapabilityConfiguration.requestTimeoutMillis]
   */
  constructor(connectTimeoutMillis: Long, readTimeoutMillis: Long) : this(
      HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
          this.connectTimeoutMillis = connectTimeoutMillis
          setReadTimeout(readTimeoutMillis)
        }
      }
  )

  override suspend fun execute(request: HttpRequest): HttpResponse {
    try {
      val response = client.request(request.url) {
        method = when (request.method) {
          HttpMethod.Get -> io.ktor.http.HttpMethod.Get
          HttpMethod.Post -> io.ktor.http.HttpMethod.Post
        }
        request.headers.forEach {
          header(it.name, it.value)
        }
        request.body?.let {
          header(HttpHeaders.ContentType, it.contentType)
          val buffer = Buffer()
          it.writeTo(buffer)
          setBody(buffer.readUtf8())
        }
      }
      val responseByteArray: ByteArray = response.body()
      val responseBufferedSource = Buffer().write(responseByteArray)
      return HttpResponse.Builder(statusCode = response.status.value)
          .body(responseBufferedSource)
          .addHeaders(response.headers.flattenEntries().map { HttpHeader(it.first, it.second) })
          .build()
    } catch (e: CancellationException) {
      // Cancellation Exception is passthrough
      throw e
    } catch (t: Throwable) {
      throw ApolloNetworkException(t.message, t)
    }
  }

  override fun close() {
    if (!disposed) {
      client.close()
      disposed = true
    }
  }
}

internal expect fun HttpTimeoutConfig.setReadTimeout(readTimeoutMillis: Long)
