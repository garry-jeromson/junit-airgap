package io.github.garryjeromson.junit.nonetwork.gradle

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.SuperMethodCall
import net.bytebuddy.matcher.ElementMatchers
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
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
     * Name of the Test task to get classpath from (resolved at execution time)
     */
    @get:Input
    @get:Optional
    abstract val testTaskName: Property<String>

    init {
        group = "verification"
        description = "Inject @Rule field into JUnit 4 test classes for zero-configuration network blocking"
    }

    @TaskAction
    fun injectRules() {
        val classesDir = testClassesDir.get().asFile
        logger.lifecycle("JUnit 4 Rule Injection Task - checking directory: ${classesDir.absolutePath}")

        if (!classesDir.exists()) {
            logger.lifecycle(
                "Test classes directory does not exist, skipping rule injection: ${classesDir.absolutePath}",
            )
            return
        }

        val debugMode = debug.get()
        logger.lifecycle("Scanning for JUnit 4 test classes in: ${classesDir.absolutePath}")
        if (debugMode) {
            logger.lifecycle("Debug mode enabled")
        }

        // Find all .class files
        val classFiles =
            classesDir
                .walk()
                .filter { it.isFile && it.extension == "class" }
                .filter { !it.name.contains("$") } // Skip inner classes for now
                .toList()

        if (debugMode) {
            logger.lifecycle("Found ${classFiles.size} class files to analyze")
        }

        var enhancedCount = 0
        var skippedCount = 0

        // Create classloader with test classpath
        // Include test classes directory + full test runtime classpath (for ByteBuddy TypePool)
        val urls = mutableListOf(classesDir.toURI().toURL())

        // Resolve test task's classpath at execution time
        if (testTaskName.isPresent) {
            val taskName = testTaskName.get()
            val testTask = project.tasks.findByName(taskName) as? org.gradle.api.tasks.testing.Test
            if (testTask != null) {
                testTask.classpath.files.forEach { file ->
                    urls.add(file.toURI().toURL())
                }
                if (debugMode) {
                    logger.lifecycle("Added ${testTask.classpath.files.size} files from test task '$taskName' classpath")
                }
            } else {
                logger.warn("Test task '$taskName' not found or not a Test task")
            }
        } else {
            logger.warn("Test task name not provided - bytecode injection may fail")
        }

        if (debugMode) {
            logger.lifecycle("Using test classes directory for injection: ${classesDir.absolutePath}")
            logger.lifecycle("Total classpath URLs: ${urls.size}")
        }

        // Use plugin's classloader as parent (includes JUnit for annotation detection)
        val classLoader = URLClassLoader(urls.toTypedArray(), javaClass.classLoader)

        classFiles.forEach { classFile ->
            val className = getClassName(classesDir, classFile)

            try {
                if (debugMode) {
                    logger.lifecycle("Loading class: $className")
                }
                val clazz = classLoader.loadClass(className)

                if (debugMode) {
                    logger.lifecycle("Checking if $className is JUnit 4 test class...")
                }

                if (isJUnit4TestClass(clazz)) {
                    if (debugMode) {
                        logger.lifecycle("  ✓ $className IS a JUnit 4 test class")
                    }
                    if (hasNoNetworkRule(clazz)) {
                        if (debugMode) {
                            logger.info("  Skipping $className - already has noNetworkRule field")
                        }
                        skippedCount++
                    } else {
                        injectRule(clazz, classFile, classLoader)
                        enhancedCount++
                        if (debugMode) {
                            logger.lifecycle("  Enhanced: $className")
                        }
                    }
                } else {
                    if (debugMode) {
                        logger.lifecycle("  ✗ $className is NOT a JUnit 4 test class")
                    }
                }
            } catch (e: ClassNotFoundException) {
                if (debugMode) {
                    logger.lifecycle("Could not load class $className: ${e.message}")
                }
            } catch (e: NoClassDefFoundError) {
                if (debugMode) {
                    logger.lifecycle("Could not load class $className (missing dependency): ${e.message}")
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
        val debugMode = debug.get()

        if (debugMode) {
            logger.lifecycle("  Checking if ${clazz.name} is a JUnit 4 test class")
            logger.lifecycle("    Found ${clazz.declaredMethods.size} methods")
        }

        // Check for @Test annotation from org.junit (JUnit 4)
        val hasJUnit4Test =
            clazz.declaredMethods.any { method ->
                if (debugMode) {
                    logger.lifecycle("    Method: ${method.name}, annotations: ${method.annotations.size}")
                }
                val hasTest =
                    method.annotations.any { annotation ->
                        // Use Kotlin reflection to get the annotation type
                        val annotationTypeName = annotation.annotationClass.java.name
                        if (debugMode) {
                            logger.lifecycle("      Annotation: $annotationTypeName")
                        }
                        annotationTypeName == "org.junit.Test"
                    }
                if (debugMode && hasTest) {
                    logger.lifecycle("      ✓ Method ${method.name} has @Test annotation")
                }
                hasTest
            }

        // Also check if it uses @RunWith (another JUnit 4 indicator)
        val hasRunWith =
            clazz.annotations.any { annotation ->
                val annotationTypeName = annotation.annotationClass.java.name
                if (debugMode) {
                    logger.lifecycle("    Class annotation: $annotationTypeName")
                }
                annotationTypeName == "org.junit.runner.RunWith"
            }

        val isJUnit4 = hasJUnit4Test || hasRunWith
        if (debugMode) {
            logger.lifecycle("    Result: isJUnit4Test=$hasJUnit4Test, hasRunWith=$hasRunWith, final=$isJUnit4")
        }

        return isJUnit4
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
     * Note: This implementation uses ByteBuddy's type pool to reference NoNetworkRule by name,
     * avoiding the need to have it in the gradle plugin's classpath.
     */
    private fun injectRule(
        clazz: Class<*>,
        classFile: File,
        classLoader: ClassLoader,
    ) {
        val buddy = ByteBuddy()

        // Reference the NoNetworkRule class by name using ByteBuddy's type pool
        // This doesn't require the class to be in the gradle plugin's classpath
        val ruleClassName = "io.github.garryjeromson.junit.nonetwork.NoNetworkRule"

        // Use ByteBuddy's type pool to get a type description without loading the class
        val typePool =
            net.bytebuddy.pool.TypePool.Default
                .of(classLoader)
        val ruleType = typePool.describe(ruleClassName).resolve()

        // Create annotation for @Rule
        val ruleAnnotation = AnnotationDescription.Builder.ofType(Rule::class.java).build()

        // For Kotlin classes, we need @JvmField as well
        val isKotlinClass =
            clazz.annotations.any {
                it.annotationClass.java.name
                    .startsWith("kotlin.")
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
        val fieldBuilder =
            buddy
                .rebase(clazz)
                .defineField("noNetworkRule", ruleType, Visibility.PUBLIC)
                .annotateField(annotations)

        // Inject field initialization into all constructors
        // For each constructor: call super(), then initialize noNetworkRule = new NoNetworkRule()
        // Find the no-arg constructor
        val constructor =
            ruleType
                .getDeclaredMethods()
                .filter(ElementMatchers.isConstructor<net.bytebuddy.description.method.MethodDescription>())
                .filter(ElementMatchers.takesArguments(0))
                .first()

        val unloaded =
            fieldBuilder
                .constructor(ElementMatchers.any())
                .intercept(
                    SuperMethodCall.INSTANCE.andThen(
                        MethodCall
                            .construct(constructor)
                            .setsField(ElementMatchers.named("noNetworkRule")),
                    ),
                ).make()

        // Get the bytecode
        val bytes = unloaded.bytes

        // Write the modified bytecode back to the original class file
        classFile.writeBytes(bytes)

        if (debug.get()) {
            logger.info("Successfully injected @Rule field into ${classFile.name}")
        }
    }
}
