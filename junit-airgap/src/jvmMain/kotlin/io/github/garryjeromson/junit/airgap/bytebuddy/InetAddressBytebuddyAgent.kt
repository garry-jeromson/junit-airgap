package io.github.garryjeromson.junit.airgap.bytebuddy

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.lang.instrument.Instrumentation

/**
 * ByteBuddy Java agent for intercepting DNS resolution at the Java API layer.
 *
 * This agent transforms java.net.InetAddress to intercept DNS lookups before
 * they reach the native layer. This ensures interception works even when:
 * - DNS classes are loaded before JVMTI agent initialization
 * - Native methods are bound before NativeMethodBindCallback fires
 * - Multiple JVM instances are spawned (e.g., IDE test runners)
 *
 * ## Architecture
 *
 * This agent works alongside the JVMTI native agent, providing two layers of defense:
 *
 * Layer 1 (This agent): Intercepts at Java API level (InetAddress.getAllByName)
 * - Always works, regardless of class loading order
 * - Slightly higher overhead but more reliable
 *
 * Layer 2 (JVMTI): Intercepts at native level (Inet6AddressImpl.lookupAllHostAddr)
 * - Lower overhead when it works
 * - May miss early DNS calls in some JVM configurations
 *
 * Both layers call the same NetworkBlockerContext.checkConnection() logic.
 *
 * ## Usage
 *
 * This agent is automatically loaded by the Gradle plugin via -javaagent flag.
 * No manual configuration required.
 */
object InetAddressBytebuddyAgent {
    /**
     * Java agent entry point called by JVM.
     *
     * Usage: -javaagent:path/to/junit-airgap-bytebuddy-agent.jar
     *
     * @param agentArgs Agent arguments (unused)
     * @param instrumentation JVM instrumentation interface
     */
    @JvmStatic
    fun premain(
        agentArgs: String?,
        instrumentation: Instrumentation,
    ) {
        val debugMode = System.getProperty("junit.airgap.debug") == "true"

        if (debugMode) {
            System.err.println("[junit-airgap-bytebuddy] ByteBuddy DNS agent starting...")
        }

        try {
            AgentBuilder
                .Default()
                .disableClassFormatChanges() // Don't modify class format version
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION) // Support retransformation
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE) // Don't initialize classes
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE) // Allow redefinition
                .with(createListener(debugMode)) // Add logging listener
                .ignore(ElementMatchers.none()) // Don't ignore any types
                .type(ElementMatchers.named("java.net.InetAddress")) // Target InetAddress
                .transform { builder, typeDescription, classLoader, module, _ ->
                    transformInetAddress(builder, typeDescription, classLoader, module)
                }
                .installOn(instrumentation)

            if (debugMode) {
                System.err.println("[junit-airgap-bytebuddy] ByteBuddy DNS agent installed successfully")
            }
        } catch (e: Exception) {
            System.err.println("[junit-airgap-bytebuddy] ERROR: Failed to install ByteBuddy DNS agent")
            System.err.println("[junit-airgap-bytebuddy] ${e.javaClass.name}: ${e.message}")
            e.printStackTrace(System.err)
        }
    }

    /**
     * Transform InetAddress class to inject DNS interception advice.
     *
     * This adds advice to getAllByName and getByName methods to check
     * network configuration before allowing DNS lookups.
     */
    private fun transformInetAddress(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?,
    ): DynamicType.Builder<*> {
        val debugMode = System.getProperty("junit.airgap.debug") == "true"

        if (debugMode) {
            System.err.println("[junit-airgap-bytebuddy] Transforming java.net.InetAddress")
        }

        val getAllByNameMatcher =
            ElementMatchers.named<net.bytebuddy.description.method.MethodDescription>("getAllByName")
                .and(ElementMatchers.takesArguments(1))
        val getByNameMatcher =
            ElementMatchers.named<net.bytebuddy.description.method.MethodDescription>("getByName")
                .and(ElementMatchers.takesArguments(1))

        return builder
            // Intercept getAllByName(String) - primary DNS lookup method
            .visit(
                Advice
                    .to(InetAddressGetAllByNameAdvice.Companion::class.java)
                    .on(getAllByNameMatcher),
            )
            // Intercept getByName(String) - convenience method
            .visit(
                Advice
                    .to(InetAddressGetByNameAdvice.Companion::class.java)
                    .on(getByNameMatcher),
            )
    }

    /**
     * Create listener for logging transformation events (debug mode only).
     */
    private fun createListener(debugMode: Boolean): AgentBuilder.Listener {
        return if (debugMode) {
            object : AgentBuilder.Listener {
                override fun onDiscovery(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    if (typeName == "java.net.InetAddress") {
                        System.err.println("[junit-airgap-bytebuddy] Discovered InetAddress class (loaded=$loaded)")
                    }
                }

                override fun onTransformation(
                    typeDescription: TypeDescription,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                    dynamicType: DynamicType,
                ) {
                    System.err.println("[junit-airgap-bytebuddy] Transformed ${typeDescription.name}")
                }

                override fun onIgnored(
                    typeDescription: TypeDescription,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    // Ignore - too noisy
                }

                override fun onError(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                    throwable: Throwable,
                ) {
                    System.err.println("[junit-airgap-bytebuddy] ERROR transforming $typeName:")
                    System.err.println("[junit-airgap-bytebuddy] ${throwable.javaClass.name}: ${throwable.message}")
                    throwable.printStackTrace(System.err)
                }

                override fun onComplete(
                    typeName: String,
                    classLoader: ClassLoader?,
                    module: JavaModule?,
                    loaded: Boolean,
                ) {
                    if (typeName == "java.net.InetAddress") {
                        System.err.println("[junit-airgap-bytebuddy] Transformation complete for InetAddress")
                    }
                }
            }
        } else {
            AgentBuilder.Listener.NoOp.INSTANCE
        }
    }
}
