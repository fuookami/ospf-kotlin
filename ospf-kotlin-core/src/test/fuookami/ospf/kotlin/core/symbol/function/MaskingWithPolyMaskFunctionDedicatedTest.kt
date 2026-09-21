package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [MaskingWithPolyMaskFunction] 契约测试。 / Dedicated contract tests. */
class MaskingWithPolyMaskFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MaskingWithPolyMaskFunction::class.java))
    }
}
