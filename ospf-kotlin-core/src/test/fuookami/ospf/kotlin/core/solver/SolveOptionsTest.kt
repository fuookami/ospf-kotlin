package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy

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
