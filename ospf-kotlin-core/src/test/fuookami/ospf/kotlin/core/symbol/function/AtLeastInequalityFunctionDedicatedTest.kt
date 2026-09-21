package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [AtLeastInequalityFunction] 契约测试。 / Dedicated contract tests. */
class AtLeastInequalityFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(AtLeastInequalityFunction::class.java))
    }
}
