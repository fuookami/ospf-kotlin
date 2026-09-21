package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [IfInFunction] 契约测试。 / Dedicated contract tests. */
class IfInFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(IfInFunction::class.java))
    }
}
