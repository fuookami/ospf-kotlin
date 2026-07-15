package fuookami.ospf.kotlin.example.linear_function

import org.junit.jupiter.api.Test

/** Tests the balance-ternaryzation linear function via smoke assertions. */
class BTerTest {
    @Test
    fun smoke() {
        LinearFunctionSmokeAssertions.assertBalanceTernaryzationFunctionWorks()
    }
}
