@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FeasibleSolverOutputLegacyFallbackGuardTest {
    @Test
    fun defaultFallbackShouldRejectNonFlt64Solution() {
        val converter = GenericNumberCases.rtn64.converter
        val nonFlt64Solution = listOf(converter.intoValue(Flt64.one))

        val error = assertFailsWith<IllegalArgumentException> {
            FeasibleSolverOutput(
                obj = Flt64(12.0),
                solution = nonFlt64Solution,
                time = 1.seconds,
                possibleBestObj = Flt64(11.0),
                gap = Flt64(0.1),
            )
        }

        assertTrue(
            error.message?.contains("Please provide explicit V-typed objValue.") == true
        )
    }

    @Test
    fun explicitTypedObjectiveFieldsShouldAllowNonFlt64Solution() {
        val converter = GenericNumberCases.rtn64.converter
        val nonFlt64Solution = listOf(converter.intoValue(Flt64.one))
        val output = FeasibleSolverOutput(
            obj = Flt64(12.0),
            solution = nonFlt64Solution,
            time = 1.seconds,
            possibleBestObj = Flt64(11.0),
            gap = Flt64(0.1),
            objValue = converter.intoValue(Flt64(12.0)),
            possibleBestObjValue = converter.intoValue(Flt64(11.0)),
            bestBoundValue = null
        )

        assertEquals(converter.intoValue(Flt64(12.0)), output.objValue)
        assertEquals(converter.intoValue(Flt64(11.0)), output.possibleBestObjValue)
    }
}
