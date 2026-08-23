package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok

class ConstraintProgrammingConstraintTest {
    @Test
    fun integerAndBooleanConstraintsShouldEvaluateAgainstExactAssignments() {
        val amount = IntVar("amount")
        val left = ConstraintProgrammingExpression.Variable(amount)
        val comparison = ConstraintProgrammingConstraint.greaterOrEqual(left, Int64(3)).value!!
        val amountId = left.variableId
        assertEquals(true, comparison.isSatisfied(mapOf(amountId to Int64(4))).value)
        assertEquals(false, comparison.isSatisfied(mapOf(amountId to Int64(2))).value)

        val first = BinVar("first")
        val second = BinVar("second")
        val firstLiteral = BooleanLiteral(first)
        val secondLiteral = BooleanLiteral(second)
        val xor = ConstraintProgrammingConstraint.boolXor(listOf(firstLiteral, secondLiteral)).value!!
        val assignment = mapOf(
            firstLiteral.variableId!! to Int64.one,
            secondLiteral.variableId!! to Int64.zero
        )
        assertEquals(true, xor.isSatisfied(assignment).value)
        assertEquals(false, xor.isSatisfied(assignment + (secondLiteral.variableId!! to Int64.one)).value)
    }

    @Test
    fun implicationReificationAndAllDifferentShouldPreserveLogicalSemantics() {
        val enforcementVariable = BinVar("enforcement")
        val valueVariable = IntVar("value")
        val enforcement = BooleanLiteral(enforcementVariable)
        val value = ConstraintProgrammingExpression.Variable(valueVariable)
        val inner = ConstraintProgrammingConstraint.equal(value, Int64(7)).value!!
        val implication = ConstraintProgrammingConstraint.implies(enforcement, inner).value!!
        val enforcementId = enforcement.variableId!!
        val valueId = value.variableId

        assertEquals(true, implication.isSatisfied(mapOf(enforcementId to Int64.zero)).value)
        assertEquals(false, implication.isSatisfied(mapOf(enforcementId to Int64.one, valueId to Int64(6))).value)
        assertEquals(true, implication.isSatisfied(mapOf(enforcementId to Int64.one, valueId to Int64(7))).value)

        val reified = ConstraintProgrammingConstraint.reified(
            literal = enforcement,
            constraint = inner,
            direction = ReificationDirection.Equivalent
        ).value!!
        assertEquals(true, reified.isSatisfied(mapOf(enforcementId to Int64.one, valueId to Int64(7))).value)
        assertEquals(false, reified.isSatisfied(mapOf(enforcementId to Int64.zero, valueId to Int64(7))).value)

        val other = IntVar("other")
        val allDifferent = ConstraintProgrammingConstraint.allDifferent(
            listOf(value, ConstraintProgrammingExpression.Variable(other))
        ).value!!
        assertEquals(
            false,
            allDifferent.isSatisfied(mapOf(valueId to Int64(1), VariableIdOf(other) to Int64(1))).value
        )
    }

    @Test
    fun elementAndTableConstraintsShouldValidateShapeAndAssignments() {
        val indexVariable = IntVar("index")
        val targetVariable = IntVar("target")
        val index = ConstraintProgrammingExpression.Variable(indexVariable)
        val target = ConstraintProgrammingExpression.Variable(targetVariable)
        val element = ConstraintProgrammingConstraint.element(
            index = index,
            values = listOf(Int64(10), Int64(20), Int64(30)),
            target = target
        ).value!!
        val assignment = mapOf(VariableIdOf(indexVariable) to Int64(1), VariableIdOf(targetVariable) to Int64(20))
        assertEquals(true, element.isSatisfied(assignment).value)
        assertEquals(false, element.isSatisfied(assignment + (VariableIdOf(targetVariable) to Int64(10))).value)

        val x = IntVar("x")
        val y = IntVar("y")
        val expressions = listOf(
            ConstraintProgrammingExpression.Variable(x),
            ConstraintProgrammingExpression.Variable(y)
        )
        val allowed = ConstraintProgrammingConstraint.allowedAssignments(
            expressions,
            listOf(listOf(0, 1), listOf(1, 0))
        )
        assertIs<Ok<ConstraintProgrammingConstraint.AllowedAssignments, *, *>>(allowed)
        val table = allowed.value!!
        val allowedAssignment = mapOf(VariableIdOf(x) to Int64.zero, VariableIdOf(y) to Int64.one)
        assertTrue(table.isSatisfied(allowedAssignment).value!!)
    }
}

private fun VariableIdOf(variable: IntVar) =
    fuookami.ospf.kotlin.core.solver.report.VariableId("${variable.identifier}:${variable.index}")
