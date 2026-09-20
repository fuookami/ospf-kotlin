package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import fuookami.ospf.kotlin.core.model.intermediate.SemiStructure
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

class GurobiNativeSemiTest {
    private val result = RealVar("native_semi_result")
    private val indicator = BinVar("native_semi_indicator")

    @Test
    fun prepareConvertsPositiveFiniteBounds() {
        val prepared = prepareGurobiNativeSemi(structure())

        assertTrue(prepared is Ok)
        assertEquals(result.key, prepared.value.resultKey)
        assertEquals(2.0, prepared.value.lowerBound)
        assertEquals(5.0, prepared.value.upperBound)
    }

    @Test
    fun rejectsZeroLowerBoundAndReversedBounds() {
        assertTrue(prepareGurobiNativeSemi(structure(lowerBound = Flt64.zero)) is Failed)
        assertTrue(prepareGurobiNativeSemi(structure(lowerBound = Flt64(6.0))) is Failed)
    }

    private fun structure(
        lowerBound: Flt64 = Flt64(2.0),
        upperBound: Flt64 = Flt64(5.0)
    ): SemiStructure<Flt64> = SemiStructure(
        lowerBound = lowerBound,
        upperBound = upperBound,
        resultVariable = result,
        indicatorVariable = indicator,
        converter = IntoValue.Identity,
        name = "native-semi"
    )
}
