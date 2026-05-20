package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Integer
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelingPreparationTest {
    @Test
    fun shouldPrepareVariableDumpingDataWithBoundsNamesAndInitialResults() {
        val variable0 = Variable(
            index = 0,
            lowerBound = Flt64(-3.0),
            upperBound = Flt64(7.0),
            type = Binary,
            origin = null,
            name = "x0",
            initialResult = Flt64(1.0)
        )
        val variable1 = Variable(
            index = 1,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Integer,
            origin = null,
            name = "x1",
            initialResult = null
        )

        val dumpingData = prepareVariableDumpingData(
            variables = listOf(variable0, variable1),
            scopeName = "linear"
        )

        assertContentEquals(doubleArrayOf(-3.0, 0.0), dumpingData.lowerBounds)
        assertContentEquals(doubleArrayOf(7.0, 10.0), dumpingData.upperBounds)
        assertContentEquals(arrayOf("x0", "x1"), dumpingData.names)
        assertEquals(1, dumpingData.initialResults.size)
        assertEquals(0, dumpingData.initialResults[0].first)
        assertEquals(1.0, dumpingData.initialResults[0].second)
    }

    @Test
    fun shouldComputeReasonableConstraintSegmentSize() {
        assertEquals(10, computeConstraintSegmentSize(0, 8))
        assertEquals(10, computeConstraintSegmentSize(8, 8))
        assertEquals(10, computeConstraintSegmentSize(50, 8))
        assertEquals(100, computeConstraintSegmentSize(1000, 8))
        assertTrue(computeConstraintSegmentSize(50000, 8) >= 1000)
    }
}
