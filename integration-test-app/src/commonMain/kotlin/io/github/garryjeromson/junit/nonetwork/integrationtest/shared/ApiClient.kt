package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

/**
 * Shared API client interface demonstrating KMP networking pattern.
 * Uses platform-specific HttpClient under the hood.
 */
interface ApiClient {
    /**
     * Fetches a user by ID from the API.
     */
    suspend fun fetchUser(userId: Int): HttpResponse

    /**
     * Fetches list of posts from the API.
     */
    suspend fun fetchPosts(): HttpResponse

    /**
     * Makes a request to a custom URL.
     */
    suspend fun fetchUrl(url: String): HttpResponse

    /**
     * Closes the underlying HTTP client.
     */
    fun close()
}

/**
 * Default implementation of ApiClient using Ktor HttpClient.
 * This is shared code that works across all platforms.
 */
class DefaultApiClient(
    private val baseUrl: String = "https://jsonplaceholder.typicode.com",
    private val httpClient: HttpClient = HttpClientFactory.create()
) : ApiClient {

    override suspend fun fetchUser(userId: Int): HttpResponse {
        return httpClient.get("$baseUrl/users/$userId")
    }

    override suspend fun fetchPosts(): HttpResponse {
        return httpClient.get("$baseUrl/posts")
    }

    override suspend fun fetchUrl(url: String): HttpResponse {
        return httpClient.get(url)
    }

    override fun close() {
        httpClient.close()
    }
}
