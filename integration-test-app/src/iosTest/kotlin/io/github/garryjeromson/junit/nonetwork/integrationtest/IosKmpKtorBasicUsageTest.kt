package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * iOS tests for basic KMP Ktor usage with shared client.
 * Note: iOS implementation provides API structure only - network requests
 * are NOT actively blocked on iOS (requires Objective-C bridge).
 * These tests demonstrate the proper KMP API structure.
 */
class IosKmpKtorBasicUsageTest {

    @Test
    fun testSharedApiClientStructure() = runTest {
        val apiClient = DefaultApiClient()
        try {
            // This demonstrates the API structure works on iOS
            // Network will not be blocked (API structure only)
            apiClient.fetchUser(1)
        } catch (e: Exception) {
            // Expected - network request may fail
        } finally {
            apiClient.close()
        }
    }

    @Test
    fun testSharedApiClientFetchPostsStructure() = runTest {
        val apiClient = DefaultApiClient()
        try {
            apiClient.fetchPosts()
        } catch (e: Exception) {
            // Expected
        } finally {
            apiClient.close()
        }
    }

    @Test
    fun testUserRepositoryStructure() = runTest {
        val repository = UserRepository()
        try {
            repository.getUser(1)
        } catch (e: Exception) {
            // Expected
        } finally {
            repository.close()
        }
    }

    @Test
    fun testApiClientCanBeCreated() {
        val apiClient = DefaultApiClient()
        // Verify we can create the client on iOS
        apiClient.close()
    }
}
