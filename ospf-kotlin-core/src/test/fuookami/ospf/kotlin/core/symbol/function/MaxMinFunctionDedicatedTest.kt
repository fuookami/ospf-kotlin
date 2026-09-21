package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [MaxMinFunction] 契约测试。 / Dedicated contract tests. */
class MaxMinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MaxMinFunction::class.java))
    }
}
