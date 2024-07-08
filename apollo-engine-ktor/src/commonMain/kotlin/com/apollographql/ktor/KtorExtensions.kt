package com.apollographql.ktor

import com.apollographql.apollo.ApolloClient
import com.apollographql.ktor.http.KtorHttpEngine
import com.apollographql.ktor.ws.KtorWebSocketEngine
import io.ktor.client.*

/**
 * Configures the [ApolloClient] to use the Ktor [HttpClient] for network requests.
 * The [HttpClient] will be used for both HTTP and WebSocket requests.
 *
 * See also [ApolloClient.Builder.httpEngine] and [ApolloClient.Builder.webSocketEngine]
 */
fun ApolloClient.Builder.ktorClient(httpClient: HttpClient) = apply {
  httpEngine(KtorHttpEngine(httpClient))
  webSocketEngine(KtorWebSocketEngine(httpClient))
}
