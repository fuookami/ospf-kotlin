package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [MinMaxFunction] 契约测试。 / Dedicated contract tests. */
class MinMaxFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MinMaxFunction::class.java))
    }
}
