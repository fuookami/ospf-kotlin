package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Failed

class IntervalVariableTest {
    @Test
    fun intervalShouldValidateEndAndOptionalPresence() {
        val valid = IntervalVariable.create(
            id = "fixed",
            start = ConstraintProgrammingExpression.Constant(Int64.zero),
            size = ConstraintProgrammingExpression.Constant(Int64(3)),
            end = ConstraintProgrammingExpression.Constant(Int64(3))
        ).value!!
        assertEquals(true, valid.evaluate(emptyMap()).value!!.present)

        val invalid = IntervalVariable.create(
            id = "invalid",
            start = ConstraintProgrammingExpression.Constant(Int64.zero),
            size = ConstraintProgrammingExpression.Constant(Int64(3)),
            end = ConstraintProgrammingExpression.Constant(Int64(4))
        ).value!!
        assertIs<Failed<*, *, *>>(invalid.evaluate(emptyMap()))

        val optional = IntervalVariable.create(
            id = "optional",
            start = ConstraintProgrammingExpression.Constant(Int64(0)),
            size = ConstraintProgrammingExpression.Constant(Int64(100)),
            end = ConstraintProgrammingExpression.Constant(Int64(100)),
            presence = BooleanLiteral.False
        ).value!!
        assertEquals(false, optional.evaluate(emptyMap()).value!!.present)
    }

    @Test
    fun noOverlapShouldRespectHalfOpenIntervalsAndOptionalIntervals() {
        val first = interval("first", 0, 3, 3)
        val second = interval("second", 3, 2, 5)
        val overlap = interval("overlap", 2, 2, 4)
        val noOverlap = NoOverlap.create(listOf(first, second)).value!!
        assertEquals(true, noOverlap.isSatisfied(emptyMap()).value)
        assertEquals(false, NoOverlap.create(listOf(first, overlap)).value!!.isSatisfied(emptyMap()).value)

        val optional = IntervalVariable.create(
            id = "disabled",
            start = ConstraintProgrammingExpression.Constant(Int64(1)),
            size = ConstraintProgrammingExpression.Constant(Int64(100)),
            end = ConstraintProgrammingExpression.Constant(Int64(101)),
            presence = BooleanLiteral.False
        ).value!!
        assertEquals(true, NoOverlap.create(listOf(first, optional)).value!!.isSatisfied(emptyMap()).value)
    }

    @Test
    fun cumulativeShouldCheckPeakLoadAndInputShape() {
        val first = interval("first", 0, 3, 3)
        val second = interval("second", 1, 2, 3)
        val demands = listOf(
            ConstraintProgrammingExpression.Constant(Int64(2)),
            ConstraintProgrammingExpression.Constant(Int64(3))
        )
        val capacity = ConstraintProgrammingExpression.Constant(Int64(4))
        val cumulative = Cumulative.create(listOf(first, second), demands, capacity).value!!
        assertEquals(false, cumulative.isSatisfied(emptyMap()).value)
        assertEquals(
            true,
            Cumulative.create(
                listOf(first, second),
                demands,
                ConstraintProgrammingExpression.Constant(Int64(5))
            ).value!!.isSatisfied(emptyMap()).value
        )
        assertIs<Failed<*, *, *>>(
            Cumulative.create(listOf(first), demands, capacity)
        )
    }

    private fun interval(id: String, start: Long, size: Long, end: Long): IntervalVariable {
        return IntervalVariable.create(
            id = id,
            start = ConstraintProgrammingExpression.Constant(Int64(start)),
            size = ConstraintProgrammingExpression.Constant(Int64(size)),
            end = ConstraintProgrammingExpression.Constant(Int64(end))
        ).value!!
    }
}
