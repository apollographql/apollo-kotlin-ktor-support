package com.apollographql.ktor.http

import io.ktor.client.plugins.*

internal actual fun HttpTimeoutConfig.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
