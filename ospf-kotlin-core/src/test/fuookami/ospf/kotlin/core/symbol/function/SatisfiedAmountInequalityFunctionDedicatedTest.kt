package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the SatisfiedAmountInequalityFunction symbol. */
class SatisfiedAmountInequalityFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SatisfiedAmountInequalityFunction::class.java))
    }
}
