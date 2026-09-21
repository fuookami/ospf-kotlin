package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [AnyFunction] 契约测试。 / Dedicated contract tests. */
class AnyFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(AnyFunction::class.java))
    }
}
