package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [SinFunction] 契约测试。 / Dedicated contract tests. */
class SinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SinFunction::class.java))
    }
}
