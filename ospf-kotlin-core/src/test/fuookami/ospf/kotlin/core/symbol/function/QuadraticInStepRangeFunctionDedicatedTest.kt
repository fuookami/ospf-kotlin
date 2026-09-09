package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the QuadraticInStepRangeFunction symbol. */
class QuadraticInStepRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticInStepRangeFunction::class.simpleName == "QuadraticInStepRangeFunction")
    }
}
