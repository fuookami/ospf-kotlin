package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the MaskingWithPolyMaskFunction symbol. */
class MaskingWithPolyMaskFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MaskingWithPolyMaskFunction::class.java))
    }
}
