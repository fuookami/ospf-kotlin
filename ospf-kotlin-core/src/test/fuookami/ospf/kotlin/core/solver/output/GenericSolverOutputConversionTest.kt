@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.output

import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.testing.*

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
        val source = FeasibleSolverOutput(
            obj = Flt64(12.5),
            solution = listOf(Flt64(1.5), Flt64(-2.0)),
            time = 3.seconds,
            possibleBestObj = Flt64(10.0),
            gap = Flt64(0.2),
            bestBound = Flt64(9.75)
        )

        val converted = source.convertTo(numberCase.converter)
        val expectedObjValue = numberCase.converter.intoValue(Flt64(12.5))
        val expectedPossibleBestObjValue = numberCase.converter.intoValue(Flt64(10.0))
        val expectedBestBoundValue = numberCase.converter.intoValue(Flt64(9.75))
        val expectedSolution = source.solution.map { numberCase.converter.intoValue(it) }

        assertEquals(expectedObjValue, converted.objValueOrNull!!, "${numberCase.name}: objValue mismatch")
        assertEquals(expectedPossibleBestObjValue, converted.possibleBestObjValueOrNull!!, "${numberCase.name}: possibleBestObjValue mismatch")
        assertEquals(expectedBestBoundValue, converted.bestBoundValueOrNull!!, "${numberCase.name}: bestBoundValue mismatch")
        assertEquals(expectedSolution, converted.solution, "${numberCase.name}: solution conversion mismatch")
        assertEquals(source.obj, converted.obj, "${numberCase.name}: solver-boundary obj should stay Flt64")
        assertEquals(source.possibleBestObj, converted.possibleBestObj, "${numberCase.name}: solver-boundary possibleBestObj should stay Flt64")
        assertEquals(source.bestBound, converted.bestBound, "${numberCase.name}: solver-boundary bestBound should stay Flt64")
    }
}
