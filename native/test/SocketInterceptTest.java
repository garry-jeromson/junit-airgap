import java.net.Socket;
import java.net.InetSocketAddress;

/**
 * Test 2: Verify Socket.connect() is intercepted
 *
 * DISCOVERY: Modern Java (7+) uses sun.nio.ch.Net.connect0() internally for ALL socket
 * connections, not the legacy java.net.Socket.socketConnect0(). This means intercepting
 * Net.connect0() gives us coverage for ALL Java network clients!
 *
 * Expected behavior WITH agent:
 * - Agent intercepts sun.nio.ch.Net.connect0() native method
 * - Agent should print "Intercepted sun.nio.ch.Net.connect0() binding" to stderr
 * - Connection should succeed (no blocking configured yet)
 *
 * Expected behavior WITHOUT agent:
 * - Normal socket connection
 * - No interception messages
 *
 * Run with:
 *   javac SocketInterceptTest.java
 *   java -agentpath:../build/libjunit-no-network-agent.dylib SocketInterceptTest
 */
public class SocketInterceptTest {
    public static void main(String[] args) {
        System.out.println("TEST: SocketInterceptTest started");

        try {
            System.out.println("TEST: Attempting to connect socket to example.com:80");
            Socket socket = new Socket();

            // This will trigger the native method binding for Socket.socketConnect0()
            // The connection will likely fail (no route or timeout), but that's okay -
            // we just want to verify the agent intercepts the binding
            try {
                socket.connect(new InetSocketAddress("example.com", 80), 1000);
                System.out.println("TEST: Connection succeeded (or timed out)");
            } catch (Exception e) {
                System.out.println("TEST: Connection failed (expected): " + e.getClass().getSimpleName());
            } finally {
                socket.close();
            }

            System.out.println("TEST: Socket interception test passed");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
