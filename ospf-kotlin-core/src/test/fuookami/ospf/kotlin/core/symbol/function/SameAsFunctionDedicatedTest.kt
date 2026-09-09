package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the SameAsFunction symbol. */
class SameAsFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(SameAsFunction::class.java))
    }
}
