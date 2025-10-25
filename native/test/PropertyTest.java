/**
 * Test to verify the agent sets junit.nonetwork.jvmti.loaded system property.
 *
 * Run with:
 *   javac PropertyTest.java
 *   java -agentpath:../build/libjunit-no-network-agent.dylib PropertyTest
 */
public class PropertyTest {
    public static void main(String[] args) {
        System.out.println("TEST: Checking if agent set system property...");

        String loaded = System.getProperty("junit.nonetwork.jvmti.loaded");
        System.out.println("TEST: junit.nonetwork.jvmti.loaded = " + loaded);

        if ("true".equals(loaded)) {
            System.out.println("TEST: ✅ Property test passed!");
            System.exit(0);
        } else {
            System.err.println("TEST: ❌ Property test failed - expected 'true', got: " + loaded);
            System.exit(1);
        }
    }
}
