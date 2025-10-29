# Kotlin Compiler Plugin Design for Automatic Airgap Injection

## Goal

Automatically inject `installAirgap()` configuration into all `HttpClient(Darwin)` constructor calls in test sources, providing zero-configuration network blocking for iOS tests.

## Architecture

### Project Structure

```
airgap-compiler-plugin/
├── compiler-plugin/           # Core IR transformation logic
│   ├── src/
│   │   └── main/kotlin/
│   │       ├── AirgapComponentRegistrar.kt    # Plugin registration
│   │       ├── AirgapIrGenerationExtension.kt # IR extension hook
│   │       └── HttpClientTransformer.kt       # IR transformation logic
│   └── build.gradle.kts
├── gradle-plugin/             # Gradle integration
│   ├── src/
│   │   └── main/kotlin/
│   │       └── AirgapCompilerGradlePlugin.kt  # Applies compiler plugin
│   └── build.gradle.kts
└── build.gradle.kts
```

## Implementation Plan

### Phase 1: Project Setup

1. **Create new modules** based on kotlin-compiler-plugin-template
   - `airgap-compiler-plugin` - Core IR transformation
   - `airgap-gradle-plugin` - Gradle integration

2. **Dependencies**:
   ```kotlin
   dependencies {
       compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
       implementation("org.jetbrains.kotlin:kotlin-stdlib")
   }
   ```

### Phase 2: IR Transformation Logic

#### Detect HttpClient Constructor Calls

```kotlin
class HttpClientTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner

        // Check if this is HttpClient constructor
        if (isHttpClientConstructor(callee) &&
            isDarwinEngine(expression)) {

            // Transform the call to include installAirgap()
            return transformHttpClientCall(expression)
        }

        return super.visitCall(expression)
    }

    private fun isHttpClientConstructor(function: IrFunction): Boolean {
        return function.name.asString() == "HttpClient" &&
               function.parent.kotlinFqName.asString() == "io.ktor.client"
    }

    private fun isDarwinEngine(call: IrCall): Boolean {
        // Check if first argument is Darwin engine
        val engineArg = call.getValueArgument(0)
        return engineArg?.type?.classFqName?.asString() ==
               "io.ktor.client.engine.darwin.Darwin"
    }
}
```

#### Inject installAirgap() Configuration

```kotlin
private fun transformHttpClientCall(originalCall: IrCall): IrExpression {
    // Get the configuration lambda parameter (2nd argument)
    val configLambda = originalCall.getValueArgument(1) as? IrFunctionExpression

    if (configLambda != null) {
        // Lambda exists - inject installAirgap() call into it
        return injectIntoExistingLambda(originalCall, configLambda)
    } else {
        // No lambda - create one with installAirgap()
        return createLambdaWithAirgap(originalCall)
    }
}

private fun createLambdaWithAirgap(originalCall: IrCall): IrCall {
    val lambdaBuilder = DeclarationIrBuilder(pluginContext, originalCall.symbol)

    // Build: { installAirgap() }
    val lambda = lambdaBuilder.irLambda {
        val installAirgapCall = irCall(
            callee = getInstallAirgapFunction(),
            type = irBuiltIns.unitType
        )
        +irReturn(installAirgapCall)
    }

    // Create new HttpClient call with lambda
    return lambdaBuilder.irCall(originalCall.symbol).apply {
        copyTypeArgumentsFrom(originalCall)
        putValueArgument(0, originalCall.getValueArgument(0)) // Engine
        putValueArgument(1, lambda) // Our new lambda
    }
}

private fun injectIntoExistingLambda(
    originalCall: IrCall,
    configLambda: IrFunctionExpression
): IrCall {
    // Get the lambda body
    val lambdaBody = configLambda.function.body as IrBlockBody

    // Create installAirgap() call
    val installAirgapCall = DeclarationIrBuilder(
        pluginContext,
        configLambda.function.symbol
    ).irCall(
        callee = getInstallAirgapFunction(),
        type = irBuiltIns.unitType
    )

    // Insert at the beginning of lambda body
    lambdaBody.statements.add(0, installAirgapCall)

    return originalCall
}

private fun getInstallAirgapFunction(): IrSimpleFunction {
    // Look up the installAirgap extension function
    return pluginContext.referenceFunctions(
        FqName("io.github.garryjeromson.junit.airgap.installAirgap")
    ).single().owner
}
```

### Phase 3: Conditional Application

Only apply transformation in test source sets:

```kotlin
class AirgapIrGenerationExtension(
    private val isTestSource: Boolean
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (!isTestSource) {
            return // Don't transform production code
        }

        moduleFragment.transform(HttpClientTransformer(pluginContext), null)
    }
}
```

Pass `isTestSource` flag from Gradle plugin:

```kotlin
class AirgapCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {

        val isTestSource = kotlinCompilation.name.contains("test", ignoreCase = true)

        return project.provider {
            listOf(
                SubpluginOption(key = "isTestSource", value = isTestSource.toString())
            )
        }
    }
}
```

### Phase 4: Integration with Gradle Plugin

Update existing Gradle plugin to apply compiler plugin:

```kotlin
// In gradle-plugin/build.gradle.kts
dependencies {
    implementation(project(":airgap-compiler-plugin"))
}

// In JunitAirgapPlugin.kt
class JunitAirgapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // ... existing code ...

        // Apply compiler plugin for iOS
        project.plugins.apply(AirgapCompilerGradlePlugin::class.java)
    }
}
```

## Transformation Examples

### Before (User Code)

```kotlin
// User writes this:
val client = HttpClient(Darwin)

// Or this:
val client = HttpClient(Darwin) {
    install(Logging)
}
```

### After (Compiler Transformation)

```kotlin
// Transformed to:
val client = HttpClient(Darwin) {
    installAirgap()
}

// Or:
val client = HttpClient(Darwin) {
    installAirgap()  // ← Injected at the beginning
    install(Logging)
}
```

## Testing Strategy

### Unit Tests (compiler-plugin module)

```kotlin
class HttpClientTransformerTest {
    @Test
    fun `transforms simple HttpClient Darwin call`() {
        val result = compile("""
            import io.ktor.client.*
            import io.ktor.client.engine.darwin.*

            fun test() {
                val client = HttpClient(Darwin)
            }
        """)

        result.assertContains("installAirgap()")
    }

    @Test
    fun `injects into existing configuration lambda`() {
        val result = compile("""
            import io.ktor.client.*
            import io.ktor.client.engine.darwin.*
            import io.ktor.client.plugins.logging.*

            fun test() {
                val client = HttpClient(Darwin) {
                    install(Logging)
                }
            }
        """)

        result.assertContains("installAirgap()")
        result.assertContains("install(Logging)")
    }

    @Test
    fun `does not transform non-Darwin engines`() {
        val result = compile("""
            import io.ktor.client.*
            import io.ktor.client.engine.cio.*

            fun test() {
                val client = HttpClient(CIO)
            }
        """)

        result.assertDoesNotContain("installAirgap()")
    }
}
```

### Integration Tests (gradle-plugin module)

```kotlin
class AirgapCompilerPluginIntegrationTest {
    @Test
    fun `transforms HttpClient in test sources`() {
        val project = setupTestProject()

        val result = project.gradle("compileTestKotlin")

        assertSuccess(result)
        assertTransformed(project.testOutput)
    }

    @Test
    fun `does not transform HttpClient in main sources`() {
        val project = setupTestProject()

        val result = project.gradle("compileKotlin")

        assertSuccess(result)
        assertNotTransformed(project.mainOutput)
    }
}
```

## Edge Cases to Handle

1. **HttpClient created with factory function**:
   ```kotlin
   val client = createHttpClient(Darwin)  // Won't be caught
   ```
   → Document limitation, focus on direct constructor calls

2. **Dynamically determined engine**:
   ```kotlin
   val engine = if (iOS) Darwin else CIO
   val client = HttpClient(engine)  // Hard to detect
   ```
   → Document limitation

3. **HttpClient in dependency code**:
   ```kotlin
   // In third-party library (pre-compiled)
   val client = HttpClient(Darwin)  // Can't transform
   ```
   → This is a platform limitation - document clearly

4. **Multiple NetworkBlocker installations**:
   ```kotlin
   HttpClient(Darwin) {
       installAirgap()  // User added
       // Compiler also injects → duplicate call
   }
   ```
   → Make `installAirgap()` idempotent (check if already installed)

## Implementation Timeline

- **Week 1**: Project setup, basic IR transformation skeleton
- **Week 2**: Implement transformation logic, unit tests
- **Week 3**: Gradle integration, make conditional on test sources
- **Week 4**: Integration tests, edge case handling, documentation

## Benefits

✅ **Zero-configuration** - Users write normal Ktor code
✅ **Catches 95%+ of cases** - All direct HttpClient(Darwin) calls in user test code
✅ **Automatic** - Works without any setup
✅ **Type-safe** - Compile-time transformation

## Limitations

❌ **Pre-compiled dependencies** - Can't transform third-party libraries
❌ **Dynamic engine selection** - Can't detect runtime engine choices
❌ **Factory patterns** - Only catches direct constructor calls
❌ **No global interception** - Unlike JVM's JVMTI socket-level blocking

These limitations are documented and inherent to the iOS platform architecture.

## Next Steps

1. Create `airgap-compiler-plugin` module
2. Implement basic IR transformer
3. Test with simple HttpClient(Darwin) calls
4. Add configuration lambda injection
5. Integrate with Gradle plugin
6. Comprehensive testing
7. Documentation and examples
