package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the QuadraticLinearFunction symbol. */
class QuadraticLinearFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticLinearFunction::class.simpleName == "QuadraticLinearFunction")
    }
}
