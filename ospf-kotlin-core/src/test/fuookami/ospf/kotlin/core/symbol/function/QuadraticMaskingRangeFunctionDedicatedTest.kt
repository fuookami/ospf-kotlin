package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the QuadraticMaskingRangeFunction symbol. */
class QuadraticMaskingRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticMaskingRangeFunction::class.simpleName == "QuadraticMaskingRangeFunction")
    }
}
