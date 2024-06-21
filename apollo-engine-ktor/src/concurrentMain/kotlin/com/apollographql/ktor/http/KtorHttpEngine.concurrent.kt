package com.apollographql.ktor.http

import io.ktor.client.plugins.*

internal actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  this.socketTimeoutMillis = readTimeoutMillis
}
