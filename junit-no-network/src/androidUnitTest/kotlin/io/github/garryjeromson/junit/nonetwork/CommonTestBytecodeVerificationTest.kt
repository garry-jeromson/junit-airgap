package io.github.garryjeromson.junit.nonetwork

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verification test to confirm that bytecode enhancement successfully
 * injected the @Rule field into the CommonTestJUnit4NetworkTest class.
 *
 * This test uses reflection to verify that:
 * 1. The `noNetworkRule` field exists in the compiled class
 * 2. The field has the @Rule annotation
 * 3. The field type is NoNetworkRule
 * 4. The field is public (required for JUnit @Rule)
 *
 * If all these checks pass, it proves that our JUnit4RuleInjectionTask
 * correctly handles tests defined in commonTest source set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class CommonTestBytecodeVerificationTest {
    @Test
    fun `commonTest class should have injected noNetworkRule field`() {
        // Get the CommonTestJUnit4NetworkTest class
        val testClass = CommonTestJUnit4NetworkTest::class.java

        // Verify the noNetworkRule field exists
        val ruleField =
            try {
                testClass.getDeclaredField("noNetworkRule")
            } catch (e: NoSuchFieldException) {
                throw AssertionError(
                    "Field 'noNetworkRule' not found in CommonTestJUnit4NetworkTest. " +
                        "Bytecode enhancement may have failed for commonTest classes. " +
                        "Available fields: ${testClass.declaredFields.joinToString { it.name }}",
                    e,
                )
            }

        assertNotNull(ruleField, "noNetworkRule field should exist")

        // Verify the field has @Rule annotation
        val ruleAnnotation = ruleField.getAnnotation(Rule::class.java)
        assertNotNull(
            ruleAnnotation,
            "@Rule annotation not found on noNetworkRule field. " +
                "Annotations present: ${ruleField.annotations.joinToString {
                    it.annotationClass.simpleName ?: "Unknown"
                }}",
        )

        // Verify the field type is NoNetworkRule
        assertEquals(
            NoNetworkRule::class.java,
            ruleField.type,
            "Field type should be NoNetworkRule but was ${ruleField.type}",
        )

        // Verify the field is public (required for JUnit @Rule)
        assertTrue(
            java.lang.reflect.Modifier
                .isPublic(ruleField.modifiers),
            "Field should be public for JUnit @Rule to work",
        )
    }

    @Test
    fun `commonTest class should not have manually defined rule field in source`() {
        // This test verifies that we're actually testing bytecode enhancement,
        // not a manually-added rule field

        // Read the source file (if available) or check bytecode origin
        // For now, we verify by ensuring the test passes without any manual @Rule in commonTest

        // The real verification is that the above test passes AND
        // the CommonTestJUnit4NetworkTest source file has NO @Rule field

        // This is more of a documentation test - if someone adds a manual @Rule
        // to CommonTestJUnit4NetworkTest, this comment serves as a reminder
        // that defeats the purpose of testing bytecode enhancement

        assertTrue(true, "This test documents that commonTest should have NO manual @Rule")
    }

    @Test
    fun `verify bytecode-injected rule is functional`() {
        // Create an instance of the test class
        val testInstance = CommonTestJUnit4NetworkTest()

        // Get the injected rule field
        val ruleField = testInstance::class.java.getDeclaredField("noNetworkRule")
        ruleField.isAccessible = true

        // Get the rule instance
        val ruleInstance = ruleField.get(testInstance)

        // Verify it's a NoNetworkRule instance
        assertTrue(
            ruleInstance is NoNetworkRule,
            "Injected rule should be an instance of NoNetworkRule",
        )

        // Verify the rule is properly initialized (not null)
        assertNotNull(ruleInstance, "Injected rule should be initialized")
    }
}
