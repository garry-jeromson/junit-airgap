package io.github.garryjeromson.junit.nonetwork.test

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

/**
 * JVM implementation of Ktor HTTP client using CIO engine.
 */
actual fun makeKtorRequest(): String =
    runBlocking {
        val client = HttpClient(CIO)
        try {
            client.get("https://example.com").toString()
        } finally {
            client.close()
        }
    }
