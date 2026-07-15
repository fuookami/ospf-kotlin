package fuookami.ospf.kotlin.core.solver.value

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class SolveValueConversionContextTest {
    @Test
    fun strictPolicyShouldRejectInfinityDuringConcreteConversion() {
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                withSolveValueConversionPolicy(SolveValueConversionPolicy.Strict) {
                    Flt64.infinity.toSolverDouble("field")
                }
            }
        }
    }

    @Test
    fun allowRoundingPolicyShouldKeepInfinityStable() = runBlocking {
        val value = withSolveValueConversionPolicy(SolveValueConversionPolicy.AllowRounding) {
            Flt64.infinity.toSolverDouble("field")
        }

        assertEquals(Double.POSITIVE_INFINITY, value)
    }

    @Test
    fun conversionPolicyShouldRestoreAfterScopeExit() = runBlocking {
        withSolveValueConversionPolicy(SolveValueConversionPolicy.Strict) {
            assertFailsWith<IllegalArgumentException> {
                Flt64.nan.toSolverDouble("inner")
            }
        }

        val value = Flt64.nan.toSolverDouble("outer")
        assertEquals(true, value.isNaN())
    }
}
