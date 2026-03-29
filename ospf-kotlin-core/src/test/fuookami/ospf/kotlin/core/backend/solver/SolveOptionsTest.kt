package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.core.backend.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SolveOptionsTest {
    @Test
    fun solveOptionsShouldUseNullAsDefaultForOptionalFields() {
        val options = SolveOptions()

        assertNull(options.solutionAmount)
        assertNull(options.modelBuildingStatusCallBack)
        assertNull(options.solvingStatusCallBack)
        assertNull(options.valueConversionPolicy)
    }

    @Test
    fun solveOptionsBuilderShouldBuildConfiguredValues() {
        val options = SolveOptions.build {
            solutionAmount = UInt64(3)
            modelBuildingStatusCallBack = { ok }
            solvingStatusCallBack = { ok }
            valueConversionPolicy = SolveValueConversionPolicy.Strict
        }

        assertEquals(UInt64(3), options.solutionAmount)
        assertEquals(SolveValueConversionPolicy.Strict, options.valueConversionPolicy)
    }

    @Test
    fun solveOptionsShouldUseAllowRoundingAsEffectiveDefaultPolicy() {
        val options = SolveOptions()

        assertEquals(SolveValueConversionPolicy.AllowRounding, options.effectiveValueConversionPolicy)
    }
}
