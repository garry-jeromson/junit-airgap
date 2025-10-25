package io.github.garryjeromson.junit.nonetwork

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.Socket
import kotlin.test.assertFailsWith

/**
 * Tests for system property configuration.
 * Tests the -Djunit.nonetwork.applyToAllTests=true system property.
 */
class SystemPropertyConfigurationTest {
    private var originalPropertyValue: String? = null

    @BeforeEach
    fun setUp() {
        // Save original property value
        originalPropertyValue = System.getProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
    }

    @AfterEach
    fun tearDown() {
        // Restore original property value
        if (originalPropertyValue != null) {
            System.setProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY, originalPropertyValue!!)
        } else {
            System.clearProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
        }
    }

    /**
     * Test 1: System property enabled
     */
    @Nested
    class WithSystemPropertyEnabled {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension()

        companion object {
            @JvmStatic
            @BeforeAll
            fun enableSystemProperty() {
                System.setProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY, "true")
            }

            @JvmStatic
            @AfterAll
            fun cleanup() {
                System.clearProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
            }
        }

        @Test
        fun `should block network when system property is true`() {
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }

        @Test
        @AllowNetworkRequests
        fun `should allow network with AllowNetwork even when system property is true`() {
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("@AllowNetworkRequests should override system property", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }

    /**
     * Test 2: System property disabled (default)
     */
    @Nested
    class WithSystemPropertyDisabled {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension()

        companion object {
            @JvmStatic
            @BeforeAll
            fun disableSystemProperty() {
                System.clearProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
            }
        }

        @Test
        fun `should NOT block network when system property is not set`() {
            // Existing behavior - no blocking without annotation
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Should not block when system property is not set", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }

        @Test
        @BlockNetworkRequests
        fun `should still block with NoNetworkTest when system property is not set`() {
            // Existing @BlockNetworkRequests behavior should still work
            assertFailsWith<NetworkRequestAttemptedException> {
                Socket("example.com", 80)
            }
        }
    }

    /**
     * Test 3: Priority - Constructor parameter should override system property
     */
    @Nested
    class ConstructorOverridesSystemProperty {
        @JvmField
        @RegisterExtension
        val extension = NoNetworkExtension(applyToAllTests = false)

        companion object {
            @JvmStatic
            @BeforeAll
            fun enableSystemProperty() {
                System.setProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY, "true")
            }

            @JvmStatic
            @AfterAll
            fun cleanup() {
                System.clearProperty(ExtensionConfiguration.APPLY_TO_ALL_TESTS_PROPERTY)
            }
        }

        @Test
        fun `constructor parameter false should override system property true`() {
            // Constructor says false, system property says true -> constructor wins
            try {
                Socket("example.com", 80)
            } catch (e: NetworkRequestAttemptedException) {
                throw AssertionError("Constructor parameter should override system property", e)
            } catch (e: Exception) {
                // Other network errors are fine
            }
        }
    }
}
