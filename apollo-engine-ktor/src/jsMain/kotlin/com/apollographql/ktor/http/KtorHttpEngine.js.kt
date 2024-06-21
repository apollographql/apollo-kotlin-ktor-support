package com.apollographql.ktor.http

import io.ktor.client.plugins.*

internal actual fun HttpTimeout.HttpTimeoutCapabilityConfiguration.setReadTimeout(readTimeoutMillis: Long) {
  // Cannot use socketTimeoutMillis on JS - https://youtrack.jetbrains.com/issue/KTOR-6211
  this.requestTimeoutMillis = readTimeoutMillis
}
