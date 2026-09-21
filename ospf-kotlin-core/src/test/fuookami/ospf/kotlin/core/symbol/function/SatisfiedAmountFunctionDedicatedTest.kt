package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [SatisfiedAmountFunction] 契约测试。 / Dedicated contract tests. */
class SatisfiedAmountFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SatisfiedAmountFunction::class.java))
    }
}
