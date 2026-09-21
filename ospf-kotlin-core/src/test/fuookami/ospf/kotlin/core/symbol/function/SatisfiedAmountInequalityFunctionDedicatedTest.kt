package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [SatisfiedAmountInequalityFunction] 契约测试。 / Dedicated contract tests. */
class SatisfiedAmountInequalityFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SatisfiedAmountInequalityFunction::class.java))
    }
}
