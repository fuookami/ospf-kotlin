package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [SigmoidFunction] 契约测试。 / Dedicated contract tests. */
class SigmoidFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SigmoidFunction::class.java))
    }
}
