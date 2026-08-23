package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

class ConstraintProgrammingExpressionTest {
    @Test
    fun integerDomainShouldCanonicalizeSparseValuesAndRejectInvalidIntervals() {
        val sparse = IntegerDomain.values(listOf(3, 1, 3, Int64(2)))
        val sparseValue = assertIs<Ok<IntegerDomain.Values, *, *>>(sparse).value
        assertEquals(listOf(Int64.one, Int64(2), Int64(3)), sparseValue.values)
        assertTrue(Int64(2) in sparseValue)
        assertEquals(3UL, sparseValue.cardinality)

        val invalid = IntegerDomain.interval(Int64(4), Int64(2))
        assertIs<Failed<*, *, *>>(invalid)
    }

    @Test
    fun booleanLiteralShouldSupportNegationAndStableVariableId() {
        val variable = BinVar("flag")
        val positive = BooleanLiteral(variable)
        val negative = !positive

        assertFalse(positive.isConstant)
        assertTrue(positive.positive)
        assertFalse(negative.positive)
        assertEquals(
            VariableId("${variable.identifier}:${variable.index}"),
            positive.variableId
        )
        assertEquals(true, negative.evaluate(mapOf(positive.variableId!! to false)).value)
        assertEquals(false, BooleanLiteral.True.negate().constant)
    }

    @Test
    fun linearExpressionShouldMergeTermsAndRejectEvaluationOverflow() {
        val variable = IntVar("amount")
        val expression = ConstraintProgrammingExpression.linear(
            terms = listOf(
                ConstraintProgrammingExpression.Term(variable, Int64(2)),
                ConstraintProgrammingExpression.Term(variable, Int64(3))
            ),
            constant = Int64.one
        )
        val linear = assertIs<Ok<ConstraintProgrammingExpression.Linear, *, *>>(expression).value
        assertEquals(1, linear.terms.size)
        assertEquals(Int64(5), linear.terms.single().coefficient)
        assertEquals(
            Int64(16),
            linear.evaluate(
                mapOf(linear.terms.single().variableId to Int64(3))
            ).value
        )

        val overflowing = ConstraintProgrammingExpression.Linear(
            terms = listOf(ConstraintProgrammingExpression.Term(variable, Int64.maximum)),
            constant = Int64.zero
        )
        assertIs<Failed<*, *, *>>(
            overflowing.evaluate(mapOf(overflowing.terms.single().variableId to Int64(2)))
        )
    }
}
