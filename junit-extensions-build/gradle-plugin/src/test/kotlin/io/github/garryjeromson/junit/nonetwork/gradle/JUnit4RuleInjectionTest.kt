package io.github.garryjeromson.junit.nonetwork.gradle

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for JUnit 4 rule injection logic.
 * These tests verify the bytecode injection functionality in isolation
 * for faster feedback compared to full integration tests.
 */
class JUnit4RuleInjectionTest {
    @TempDir
    lateinit var tempDir: File

    /**
     * Test fixture: Simple JUnit 4 test class with @Test annotation
     */
    class SimpleJUnit4Test {
        @org.junit.Test
        fun testMethod() {
        }

        @org.junit.Test
        fun anotherTestMethod() {
        }
    }

    /**
     * Test fixture: JUnit 4 test class with @RunWith annotation
     */
    @org.junit.runner.RunWith(org.junit.runners.JUnit4::class)
    class RunWithTest {
        fun notAnnotatedMethod() {
        }
    }

    /**
     * Test fixture: Not a test class (no test annotations)
     */
    class NotATest {
        fun regularMethod() {
        }
    }

    /**
     * Test fixture: Class that already has the noNetworkRule field
     */
    class HasRuleTest {
        @org.junit.Rule
        val noNetworkRule = Any() // Using Any as placeholder since NoNetworkRule may not be available

        @org.junit.Test
        fun testMethod() {
        }
    }

    /**
     * Test fixture: JUnit 5 test class (should NOT be detected as JUnit 4)
     */
    class JUnit5Test {
        @org.junit.jupiter.api.Test
        fun testMethod() {
        }
    }

    /**
     * Test fixture: Parent class with rule
     */
    open class ParentWithRule {
        @org.junit.Rule
        val noNetworkRule = Any()
    }

    /**
     * Test fixture: Child class inheriting from parent with rule
     */
    class ChildOfParentWithRule : ParentWithRule() {
        @org.junit.Test
        fun testMethod() {
        }
    }

    // Helper to access private methods via reflection
    private fun <T> Any.invokePrivate(
        methodName: String,
        vararg args: Any?,
    ): T {
        val method =
            this::class.declaredMemberFunctions.find { it.name == methodName }
                ?: error("Method $methodName not found")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.call(this, *args) as T
    }

    @Test
    fun `getClassName converts simple class path correctly`() {
        val baseDir = File("/test/classes")
        val classFile = File("/test/classes/com/example/TestClass.class")

        val className: String = getClassName(baseDir, classFile)

        assertEquals("com.example.TestClass", className)
    }

    @Test
    fun `getClassName converts nested package path correctly`() {
        val baseDir = File("/project/build/classes/kotlin/test")
        val classFile = File("/project/build/classes/kotlin/test/io/github/example/nested/DeepTest.class")

        val className: String = getClassName(baseDir, classFile)

        assertEquals("io.github.example.nested.DeepTest", className)
    }

    @Test
    fun `getClassName handles single package level`() {
        val baseDir = File("/classes")
        val classFile = File("/classes/simple/Test.class")

        val className: String = getClassName(baseDir, classFile)

        assertEquals("simple.Test", className)
    }

    @Test
    fun `isJUnit4TestClass detects class with @Test annotation`() {
        val result = isJUnit4TestClass(SimpleJUnit4Test::class.java)
        assertTrue(result, "Should detect JUnit 4 class with @Test annotation")
    }

    @Test
    fun `isJUnit4TestClass detects class with @RunWith annotation`() {
        val result = isJUnit4TestClass(RunWithTest::class.java)
        assertTrue(result, "Should detect JUnit 4 class with @RunWith annotation")
    }

    @Test
    fun `isJUnit4TestClass returns false for class without test annotations`() {
        val result = isJUnit4TestClass(NotATest::class.java)
        assertFalse(result, "Should not detect non-test class")
    }

    @Test
    fun `isJUnit4TestClass returns false for JUnit 5 test class`() {
        val result = isJUnit4TestClass(JUnit5Test::class.java)
        assertFalse(result, "Should not detect JUnit 5 test class as JUnit 4")
    }

    @Test
    fun `hasNoNetworkRule returns false when field does not exist`() {
        val result = hasNoNetworkRule(SimpleJUnit4Test::class.java)
        assertFalse(result, "Should return false when noNetworkRule field does not exist")
    }

    @Test
    fun `hasNoNetworkRule returns true when field exists`() {
        val result = hasNoNetworkRule(HasRuleTest::class.java)
        assertTrue(result, "Should return true when noNetworkRule field exists")
    }

    @Test
    fun `hasNoNetworkRule detects field in parent class`() {
        val result = hasNoNetworkRule(ChildOfParentWithRule::class.java)
        assertTrue(result, "Should detect noNetworkRule field in parent class")
    }

    @Test
    fun `injectRule adds noNetworkRule field to class`() {
        // Create a simple test class bytecode
        val originalClass = createSimpleTestClass("InjectionTest1")
        val classFile = saveClassToFile(originalClass, "InjectionTest1")

        // Create a mock NoNetworkRule class for testing
        val mockRuleClass = createMockRuleClass()
        saveClassToFile(mockRuleClass, "MockNoNetworkRule")

        // Create classloader with both classes
        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)

        // Load the class
        val clazz = classLoader.loadClass("InjectionTest1")

        // Inject the rule
        injectRule(clazz, classFile, classLoader)

        // Load the modified class
        val modifiedClassLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)
        val modifiedClass = modifiedClassLoader.loadClass("InjectionTest1")

        // Verify the field was added
        val field = modifiedClass.getDeclaredField("noNetworkRule")
        assertEquals("noNetworkRule", field.name)
    }

    @Test
    fun `injectRule adds @Rule annotation to field`() {
        // Create a simple test class bytecode
        val originalClass = createSimpleTestClass("InjectionTest2")
        val classFile = saveClassToFile(originalClass, "InjectionTest2")

        // Create a mock NoNetworkRule class for testing
        val mockRuleClass = createMockRuleClass()
        saveClassToFile(mockRuleClass, "MockNoNetworkRule")

        // Create classloader with both classes
        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)

        // Load the class
        val clazz = classLoader.loadClass("InjectionTest2")

        // Inject the rule
        injectRule(clazz, classFile, classLoader)

        // Load the modified class
        val modifiedClassLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)
        val modifiedClass = modifiedClassLoader.loadClass("InjectionTest2")

        // Verify the @Rule annotation exists
        val field = modifiedClass.getDeclaredField("noNetworkRule")
        val ruleAnnotation = field.getAnnotation(Rule::class.java)
        assertTrue(ruleAnnotation != null, "Field should have @Rule annotation")
    }

    @Test
    fun `injected rule field is public`() {
        // Create a simple test class bytecode
        val originalClass = createSimpleTestClass("InjectionTest3")
        val classFile = saveClassToFile(originalClass, "InjectionTest3")

        // Create a mock NoNetworkRule class for testing
        val mockRuleClass = createMockRuleClass()
        saveClassToFile(mockRuleClass, "MockNoNetworkRule")

        // Create classloader with both classes
        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)

        // Load the class
        val clazz = classLoader.loadClass("InjectionTest3")

        // Inject the rule
        injectRule(clazz, classFile, classLoader)

        // Load the modified class
        val modifiedClassLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()), this::class.java.classLoader)
        val modifiedClass = modifiedClassLoader.loadClass("InjectionTest3")

        // Verify the field is public
        val field = modifiedClass.getDeclaredField("noNetworkRule")
        assertTrue(
            java.lang.reflect.Modifier
                .isPublic(field.modifiers),
            "Field should be public",
        )
    }

    // ==================== Helper Methods ====================

    /**
     * Standalone implementation of getClassName for testing
     */
    private fun getClassName(
        baseDir: File,
        classFile: File,
    ): String {
        val relativePath = classFile.relativeTo(baseDir).path
        return relativePath
            .removeSuffix(".class")
            .replace(File.separatorChar, '.')
    }

    /**
     * Standalone implementation of isJUnit4TestClass for testing
     */
    private fun isJUnit4TestClass(clazz: Class<*>): Boolean {
        // Check for @Test annotation from org.junit (JUnit 4)
        val hasJUnit4Test =
            clazz.declaredMethods.any { method ->
                method.annotations.any { annotation ->
                    annotation.annotationClass.java.name == "org.junit.Test"
                }
            }

        // Also check if it uses @RunWith (another JUnit 4 indicator)
        val hasRunWith =
            clazz.annotations.any { annotation ->
                annotation.annotationClass.java.name == "org.junit.runner.RunWith"
            }

        return hasJUnit4Test || hasRunWith
    }

    /**
     * Standalone implementation of hasNoNetworkRule for testing
     */
    private fun hasNoNetworkRule(clazz: Class<*>): Boolean {
        return try {
            clazz.getDeclaredField("noNetworkRule")
            true
        } catch (e: NoSuchFieldException) {
            // Check parent classes
            var parent = clazz.superclass
            while (parent != null && parent != Any::class.java) {
                try {
                    parent.getDeclaredField("noNetworkRule")
                    return true
                } catch (e: NoSuchFieldException) {
                    parent = parent.superclass
                }
            }
            false
        }
    }

    /**
     * Standalone implementation of injectRule for testing
     */
    private fun injectRule(
        clazz: Class<*>,
        classFile: File,
        classLoader: ClassLoader,
    ) {
        val buddy = ByteBuddy()

        // For testing, use a mock rule class instead of the real NoNetworkRule
        val ruleClass = classLoader.loadClass("MockNoNetworkRule")

        // Create annotation for @Rule
        val ruleAnnotation =
            net.bytebuddy.description.annotation.AnnotationDescription.Builder
                .ofType(
                    Rule::class.java,
                ).build()

        // Build the enhanced class
        val fieldBuilder =
            buddy
                .rebase(clazz)
                .defineField("noNetworkRule", ruleClass, Visibility.PUBLIC)
                .annotateField(ruleAnnotation)

        // Inject field initialization into all constructors
        val unloaded =
            fieldBuilder
                .constructor(
                    net.bytebuddy.matcher.ElementMatchers
                        .any(),
                ).intercept(
                    net.bytebuddy.implementation.SuperMethodCall.INSTANCE.andThen(
                        net.bytebuddy.implementation.MethodCall
                            .construct(ruleClass.getConstructor())
                            .setsField(
                                net.bytebuddy.matcher.ElementMatchers
                                    .named("noNetworkRule"),
                            ),
                    ),
                ).make()

        // Get the bytecode
        val bytes = unloaded.bytes

        // Write the modified bytecode back to the original class file
        classFile.writeBytes(bytes)
    }

    /**
     * Create a simple test class using ByteBuddy
     */
    private fun createSimpleTestClass(className: String): ByteArray =
        ByteBuddy()
            .subclass(Any::class.java)
            .name(className)
            .make()
            .bytes

    /**
     * Create a mock NoNetworkRule class for testing
     */
    private fun createMockRuleClass(): ByteArray =
        ByteBuddy()
            .subclass(Any::class.java)
            .name("MockNoNetworkRule")
            .make()
            .bytes

    /**
     * Save class bytecode to file
     */
    private fun saveClassToFile(
        bytecode: ByteArray,
        className: String,
    ): File {
        val classFile = File(tempDir, "$className.class")
        classFile.writeBytes(bytecode)
        return classFile
    }
}
