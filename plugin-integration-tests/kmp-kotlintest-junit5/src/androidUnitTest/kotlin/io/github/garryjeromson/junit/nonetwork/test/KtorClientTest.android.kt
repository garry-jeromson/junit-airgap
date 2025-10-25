package io.github.garryjeromson.junit.nonetwork.test

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

/**
 * Android implementation of Ktor HTTP client using OkHttp engine.
 */
actual fun makeKtorRequest(): String = runBlocking {
    val client = HttpClient(OkHttp)
    try {
        client.get("https://example.com").toString()
    } finally {
        client.close()
    }
}
