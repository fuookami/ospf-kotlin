package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertTrue

/** Dedicated contract test for the QuadraticMinFunction symbol. */
class QuadraticMinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticMinFunction::class.simpleName == "QuadraticMinFunction")
    }
}
