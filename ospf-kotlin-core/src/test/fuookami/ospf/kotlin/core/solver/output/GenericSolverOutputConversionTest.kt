@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.testing.GenericNumberCase
import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class GenericSolverOutputConversionTest {
    @Test
    fun convertToShouldConvertObjectiveViewsForAllNumberTypes() {
        assertCase(GenericNumberCases.flt64)
        assertCase(GenericNumberCases.rtn64)
        assertCase(GenericNumberCases.fltX)
        assertCase(GenericNumberCases.rtnX)
    }

    @Suppress("DEPRECATION")
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

        assertEquals(expectedObjValue, converted.objValue, "${numberCase.name}: objValue mismatch")
        assertEquals(expectedPossibleBestObjValue, converted.possibleBestObjValue, "${numberCase.name}: possibleBestObjValue mismatch")
        assertEquals(expectedBestBoundValue, converted.bestBoundValue, "${numberCase.name}: bestBoundValue mismatch")
        assertEquals(expectedSolution, converted.solution, "${numberCase.name}: solution conversion mismatch")
        assertEquals(source.obj, converted.obj, "${numberCase.name}: legacy obj should stay Flt64")
        assertEquals(source.possibleBestObj, converted.possibleBestObj, "${numberCase.name}: legacy possibleBestObj should stay Flt64")
        assertEquals(source.bestBound, converted.bestBound, "${numberCase.name}: legacy bestBound should stay Flt64")
    }
}
