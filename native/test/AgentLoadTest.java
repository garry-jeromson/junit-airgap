/**
 * Test 1: Verify JVMTI agent loads successfully
 *
 * Expected behavior:
 * - Agent prints "JVMTI Agent loaded successfully" to stderr
 * - Program exits with status 0
 *
 * Run with:
 *   javac AgentLoadTest.java
 *   java -agentpath:../build/libjunit-airgap-agent.dylib AgentLoadTest
 */
public class AgentLoadTest {
    public static void main(String[] args) {
        System.out.println("TEST: AgentLoadTest started");
        System.out.println("TEST: Agent should have loaded before main()");
        System.out.println("TEST: If you see this, the agent loaded successfully!");
        System.exit(0);
    }
}
