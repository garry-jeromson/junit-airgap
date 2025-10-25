package io.github.garryjeromson.junit.nonetwork.integrationtest.shared

import io.ktor.client.statement.HttpResponse

/**
 * Repository demonstrating business logic in commonMain.
 * Uses ApiClient for network operations, keeping platform-specific
 * HTTP implementation details abstracted away.
 */
class UserRepository(
    private val apiClient: ApiClient = DefaultApiClient()
) {
    /**
     * Fetches user data by ID.
     * In a real app, this would parse the response and return a User model.
     */
    suspend fun getUser(userId: Int): HttpResponse {
        return apiClient.fetchUser(userId)
    }

    /**
     * Fetches all posts.
     * In a real app, this would parse and return List<Post>.
     */
    suspend fun getAllPosts(): HttpResponse {
        return apiClient.fetchPosts()
    }

    /**
     * Checks if a user exists by attempting to fetch them.
     * This demonstrates business logic that uses the API.
     */
    suspend fun userExists(userId: Int): Boolean {
        return try {
            val response = apiClient.fetchUser(userId)
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cleanup resources.
     */
    fun close() {
        apiClient.close()
    }
}
