package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [CosFunction] 契约测试。 / Dedicated contract tests. */
class CosFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(CosFunction::class.java))
    }
}
