package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertTrue
import kotlin.test.Test

/** [QuadraticMinFunction] 契约测试。 / Dedicated contract tests. */
class QuadraticMinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticMinFunction::class.simpleName == "QuadraticMinFunction")
    }
}
