package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [BinaryzationFunction] 契约测试。 / Dedicated contract tests. */
class BinaryzationFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(BinaryzationFunction::class.java))
    }
}
