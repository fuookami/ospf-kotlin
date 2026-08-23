@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class GenericSolverOutputConversionTest {
    @Test
    fun convertToShouldConvertObjectiveViewsForAllNumberTypes() {
        assertCase(GenericNumberCases.flt64)
        assertCase(GenericNumberCases.rtn64)
        assertCase(GenericNumberCases.fltX)
        assertCase(GenericNumberCases.rtnX)
    }
    private fun <V> assertCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val source = SolverStatus.Feasible.toSolveReport(
            objective = Flt64(12.5),
            values = listOf(Flt64(1.5), Flt64(-2.0)),
            solveTime = 3.seconds,
            bestBound = Flt64(9.75),
            gap = Flt64(0.2)
        )

        val converted = source.convertTo(numberCase.converter)
        val expectedObjValue = numberCase.converter.intoValue(Flt64(12.5))
        val expectedBestBound = Flt64(9.75)
        val expectedSolution = source.values.map { numberCase.converter.intoValue(it) }

        assertEquals(expectedObjValue, converted.solution?.objective, "${numberCase.name}: objective mismatch")
        assertEquals(expectedBestBound, converted.statistics.bestBound, "${numberCase.name}: bestBound mismatch")
        assertEquals(expectedSolution, converted.values, "${numberCase.name}: solution conversion mismatch")
        assertEquals(Flt64(12.5), source.solution?.objective, "${numberCase.name}: source objective mismatch")
        assertEquals(expectedBestBound, converted.statistics.bestBound, "${numberCase.name}: converted statistics mismatch")
    }
}
