package io.github.garryjeromson.junit.nonetwork.test

import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestAllowed
import io.github.garryjeromson.junit.nonetwork.test.contracts.assertRequestBlocked
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
