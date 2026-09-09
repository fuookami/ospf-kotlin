package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the UnivariateLinearPiecewiseFunction symbol. */
class UnivariateLinearPiecewiseFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(UnivariateLinearPiecewiseFunction::class.java))
    }
}
