package io.github.garryjeromson.junit.nonetwork.gradle

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.SuperMethodCall
import net.bytebuddy.matcher.ElementMatchers
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.junit.Rule
import java.io.File
import java.net.URLClassLoader

/**
 * Gradle task that injects a @Rule field into JUnit 4 test classes using ByteBuddy.
 *
 * This task:
 * - Scans compiled test class files
 * - Detects JUnit 4 test classes (classes with @Test methods from org.junit)
 * - Injects a public Rule field: `@Rule public NoNetworkRule noNetworkRule = new NoNetworkRule()`
 * - Skips classes that already have the rule
 * - Handles both Java and Kotlin classes
 *
 * This enables zero-configuration network blocking for JUnit 4 without requiring manual @Rule setup.
 */
abstract class JUnit4RuleInjectionTask : DefaultTask() {
    /**
     * Directory containing compiled test classes
     */
    @get:InputDirectory
    abstract val testClassesDir: DirectoryProperty

    /**
     * Whether debug logging is enabled
     */
    @get:Input
    abstract val debug: Property<Boolean>

    /**
     * Test classpath for loading classes
     */
    @get:Input
    abstract val testClasspath: Property<String>

    init {
        group = "verification"
        description = "Inject @Rule field into JUnit 4 test classes for zero-configuration network blocking"
    }

    @TaskAction
    fun injectRules() {
        val classesDir = testClassesDir.get().asFile
        if (!classesDir.exists()) {
            if (debug.get()) {
                logger.info("Test classes directory does not exist, skipping rule injection: ${classesDir.absolutePath}")
            }
            return
        }

        val debugMode = debug.get()
        if (debugMode) {
            logger.lifecycle("Scanning for JUnit 4 test classes in: ${classesDir.absolutePath}")
        }

        // Find all .class files
        val classFiles = classesDir.walk()
            .filter { it.isFile && it.extension == "class" }
            .filter { !it.name.contains("$") } // Skip inner classes for now
            .toList()

        if (debugMode) {
            logger.lifecycle("Found ${classFiles.size} class files to analyze")
        }

        var enhancedCount = 0
        var skippedCount = 0

        // Create classloader with test classpath
        val classpathUrls = testClasspath.get().split(File.pathSeparator)
            .filter { it.isNotEmpty() }
            .map { File(it).toURI().toURL() }
            .toTypedArray()

        // Use parent classloader as fallback (includes Gradle's classpath with JUnit, etc.)
        val classLoader = if (classpathUrls.isEmpty()) {
            javaClass.classLoader
        } else {
            URLClassLoader(classpathUrls, javaClass.classLoader)
        }

        classFiles.forEach { classFile ->
            val className = getClassName(classesDir, classFile)

            try {
                val clazz = classLoader.loadClass(className)

                if (isJUnit4TestClass(clazz)) {
                    if (hasNoNetworkRule(clazz)) {
                        if (debugMode) {
                            logger.info("  Skipping $className - already has noNetworkRule field")
                        }
                        skippedCount++
                    } else {
                        injectRule(clazz, classFile)
                        enhancedCount++
                        if (debugMode) {
                            logger.lifecycle("  Enhanced: $className")
                        }
                    }
                }
            } catch (e: ClassNotFoundException) {
                if (debugMode) {
                    logger.debug("Could not load class $className: ${e.message}")
                }
            } catch (e: NoClassDefFoundError) {
                if (debugMode) {
                    logger.debug("Could not load class $className (missing dependency): ${e.message}")
                }
            } catch (e: Exception) {
                logger.warn("Error processing class $className: ${e.message}")
                if (debugMode) {
                    logger.warn("Stack trace:", e)
                }
            }
        }

        if (enhancedCount > 0 || debugMode) {
            logger.lifecycle("JUnit 4 Rule Injection: enhanced $enhancedCount classes, skipped $skippedCount classes")
        }
    }

    /**
     * Get fully qualified class name from file path
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
     * Check if class is a JUnit 4 test class
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
        val hasRunWith = clazz.annotations.any { it.annotationClass.java.name == "org.junit.runner.RunWith" }

        return hasJUnit4Test || hasRunWith
    }

    /**
     * Check if class already has a noNetworkRule field
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
     * Inject @Rule field into class using ByteBuddy
     *
     * Note: This is a simplified implementation that adds the field declaration.
     * Field initialization is handled by adding a default constructor call.
     */
    private fun injectRule(
        clazz: Class<*>,
        classFile: File,
    ) {
        val buddy = ByteBuddy()

        // Load the NoNetworkRule class
        val ruleClass = Class.forName("io.github.garryjeromson.junit.nonetwork.NoNetworkRule")

        // Create annotation for @Rule
        val ruleAnnotation = AnnotationDescription.Builder.ofType(Rule::class.java).build()

        // For Kotlin classes, we need @JvmField as well
        val isKotlinClass = clazz.annotations.any {
            it.annotationClass.java.name.startsWith("kotlin.")
        }

        // Build list of annotations to add
        val annotations = mutableListOf(ruleAnnotation)
        if (isKotlinClass) {
            try {
                @Suppress("UNCHECKED_CAST")
                val jvmFieldClass = Class.forName("kotlin.jvm.JvmField") as Class<out Annotation>
                val jvmFieldAnnotation = AnnotationDescription.Builder.ofType(jvmFieldClass).build()
                annotations.add(jvmFieldAnnotation)
            } catch (e: ClassNotFoundException) {
                // kotlin.jvm.JvmField not available, skip
            } catch (e: ClassCastException) {
                // kotlin.jvm.JvmField is not an annotation, skip
            }
        }

        // Build the enhanced class using rebase (not redefine) to add new members
        // Define the field with all annotations at once
        val fieldBuilder = buddy.rebase(clazz)
            .defineField("noNetworkRule", ruleClass, Visibility.PUBLIC)
            .annotateField(annotations)

        // Inject field initialization into all constructors
        // For each constructor: call super(), then initialize noNetworkRule = new NoNetworkRule()
        val unloaded = fieldBuilder
            .constructor(ElementMatchers.any())
            .intercept(
                SuperMethodCall.INSTANCE.andThen(
                    FieldAccessor.ofField("noNetworkRule")
                        .setsValue(
                            MethodCall.construct(ruleClass.getConstructor())
                        )
                )
            )
            .make()

        // Get the bytecode
        val bytes = unloaded.bytes

        // Write the modified bytecode back to the original class file
        classFile.writeBytes(bytes)

        if (debug.get()) {
            logger.info("Successfully injected @Rule field into ${classFile.name}")
        }
    }
}
