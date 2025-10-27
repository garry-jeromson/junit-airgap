package io.github.garryjeromson.junit.airgap.test

import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.airgap.test.contracts.assertRequestBlocked
import java.net.Socket

actual fun testNetworkBlocking() {
    assertRequestBlocked {
        Socket("example.com", 80).use { }
    }
}

actual fun testNetworkAllowed() {
    assertRequestAllowed {
        Socket("example.com", 80).use { }
    }
}
