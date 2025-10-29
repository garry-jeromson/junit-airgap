package io.github.garryjeromson.junit.airgap

import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.Socket
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AirgapRuleTest {
    @get:Rule
    val noNetworkRule = AirgapRule()

    @Test
    @BlockNetworkRequests
    fun shouldBlockNetworkRequestsWhenAnnotated() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("example.com", 80)
        }
    }

    @Test
    @BlockNetworkRequests
    @AllowRequestsToHosts(hosts = ["*"])
    @BlockRequestsToHosts(hosts = ["evil.com"])
    fun shouldRespectBlockedHostsAnnotation() {
        assertFailsWith<NetworkRequestAttemptedException> {
            Socket("evil.com", 80)
        }
    }

    @Test
    fun shouldNotBlockWhenAnnotationIsAbsent() {
        // This test doesn't have @BlockNetworkRequests, so blocking should not occur
        // The actual connection may fail, but it should not throw NetworkRequestAttemptedException
        try {
            Socket("example.com", 80)
        } catch (e: NetworkRequestAttemptedException) {
            throw AssertionError("Should not block without @BlockNetworkRequests")
        } catch (e: Exception) {
            // Other exceptions are fine
        }
    }

    @Test
    fun `base statement is executed when blocking is enabled`() {
        var baseStatementExecuted = false
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    baseStatementExecuted = true
                }
            }

        val rule = AirgapRule()
        val description =
            Description.createTestDescription(
                this::class.java,
                "testMethod",
                BlockNetworkRequests(),
            )

        val wrappedStatement = rule.apply(baseStatement, description)
        wrappedStatement.evaluate()

        assertTrue(baseStatementExecuted, "Base statement should be executed when blocking is enabled")
    }

    @Test
    fun `base statement is executed when blocking is disabled`() {
        var baseStatementExecuted = false
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    baseStatementExecuted = true
                }
            }

        val rule = AirgapRule()
        val description = Description.createTestDescription(this::class.java, "testMethod")

        val wrappedStatement = rule.apply(baseStatement, description)
        wrappedStatement.evaluate()

        assertTrue(baseStatementExecuted, "Base statement should be executed when blocking is disabled")
    }

    @Test
    fun `base statement is executed when AllowNetworkRequests annotation is present`() {
        var baseStatementExecuted = false
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    baseStatementExecuted = true
                }
            }

        val rule = AirgapRule()
        val description =
            Description.createTestDescription(
                this::class.java,
                "testMethod",
                AllowNetworkRequests(),
            )

        val wrappedStatement = rule.apply(baseStatement, description)
        wrappedStatement.evaluate()

        assertTrue(
            baseStatementExecuted,
            "Base statement should be executed when @AllowNetworkRequests is present",
        )
    }
}
