package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the BivariateLinearPiecewiseFunction symbol. */
class BivariateLinearPiecewiseFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(BivariateLinearPiecewiseFunction::class.java))
    }
}
