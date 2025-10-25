package io.github.garryjeromson.junit.nonetwork.integrationtest

import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.DefaultApiClient
import io.github.garryjeromson.junit.nonetwork.integrationtest.shared.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS tests for default blocking structure with KMP shared client.
 * Note: Default blocking is NOT enforced on iOS (API structure only).
 * These tests validate the KMP API structure works correctly on iOS.
 */
class IosKmpKtorDefaultBlockingTest {

    @Test
    fun testSharedClientStructureWithRepository() = runTest {
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
    fun testApiClientCustomConfiguration() = runTest {
        val client = DefaultApiClient(baseUrl = "https://api.example.com")
        try {
            client.fetchUser(1)
        } catch (e: Exception) {
            // Expected
        } finally {
            client.close()
        }
    }

    @Test
    fun testUserRepositoryBusinessLogic() = runTest {
        val repository = UserRepository()
        try {
            // Test the business logic method
            val exists = repository.userExists(1)
            // Result will depend on network connectivity
        } catch (e: Exception) {
            // Expected
        } finally {
            repository.close()
        }
    }

    @Test
    fun testMultipleRepositoriesCanExist() {
        val repo1 = UserRepository()
        val repo2 = UserRepository()
        assertNotNull(repo1)
        assertNotNull(repo2)
        repo1.close()
        repo2.close()
    }
}
