package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

class ConstraintProgrammingModelTest {
    @Test
    fun snapshotShouldFreezeRegistrationOrderAndRejectDuplicateIds() {
        val model = ConstraintProgrammingModel("snapshot-model", ObjectCategory.Minimum)
        val variable = IntVar("amount")
        try {
            val variableId = model.registerVariable(variable).value!!
            val expression = ConstraintProgrammingExpression.Variable(variable)
            model.registerExpression("amount-expression", expression)
            val constraint = ConstraintProgrammingConstraint.equal(expression, Int64(2)).value!!
            model.addConstraint(constraint, ConstraintId("amount-eq"))
            model.minimize(expression)

            val snapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(model.snapshot()).value
            assertEquals(variableId, snapshot.variables.single().id)
            assertEquals("amount-expression", snapshot.expressions.single().name)
            assertEquals(1, snapshot.constraints.size)
            assertEquals(1, snapshot.objectives.size)

            assertIs<Failed<*, *, *>>(model.addConstraint(constraint, ConstraintId("amount-eq")))
            model.addConstraint(constraint, ConstraintId("second"))
            assertEquals(1, snapshot.constraints.size)
            assertEquals(2, model.constraintCount)
        } finally {
            model.close()
        }
    }

    @Test
    fun snapshotShouldValidateUnregisteredReferencesAndGroupRegistration() {
        val model = ConstraintProgrammingModel("validation-model")
        val variable = IntVar("unregistered")
        val expression = ConstraintProgrammingExpression.Variable(variable)
        try {
            model.registerExpression("unregistered-expression", expression)
            assertIs<Failed<*, *, *>>(model.snapshot())

            model.registerVariable(variable)
            val group = TestConstraintGroup("amount-limits")
            model.registerConstraintGroup(group)
            model.addConstraint(ConstraintProgrammingConstraint.equal(expression, Int64.zero).value!!)
            val snapshot = model.snapshot().value!!
            assertEquals(listOf("amount-limits"), snapshot.constraintGroups)
            assertEquals("amount-limits", snapshot.constraints.single().groupName)
        } finally {
            model.close()
        }
    }

    @Test
    fun snapshotShouldPreserveMaximumDirectionAndRejectConflictingObjectives() {
        val variable = IntVar("maximum-value")
        val maximum = ConstraintProgrammingModel("maximum-model", ObjectCategory.Maximum)
        val multiple = ConstraintProgrammingModel("multiple-objective-model", ObjectCategory.Minimum)
        val conflicting = ConstraintProgrammingModel("conflicting-objective-model", ObjectCategory.Minimum)
        try {
            maximum.registerVariable(variable, IntegerDomain.interval(0, 3).value!!)
            val expression = ConstraintProgrammingExpression.Variable(variable)
            assertIs<Ok<*, *, *>>(maximum.maximize(expression))
            val maximumSnapshot = assertIs<Ok<ConstraintProgrammingModelSnapshot, *, *>>(maximum.snapshot()).value
            assertEquals(ObjectCategory.Maximum, maximumSnapshot.objectCategory)
            assertEquals(ObjectCategory.Maximum, maximumSnapshot.objectives.single().category)

            val multipleVariable = IntVar("multiple-value")
            multiple.registerVariable(multipleVariable, IntegerDomain.interval(0, 1).value!!)
            val multipleExpression = ConstraintProgrammingExpression.Variable(multipleVariable)
            assertIs<Ok<*, *, *>>(multiple.minimize(multipleExpression))
            assertIs<Ok<*, *, *>>(multiple.minimize(multipleExpression, id = ObjectiveId("second")))
            assertIs<Failed<*, *, *>>(multiple.snapshot())

            val conflictingVariable = IntVar("conflicting-value")
            conflicting.registerVariable(conflictingVariable, IntegerDomain.interval(0, 1).value!!)
            val conflictingExpression = ConstraintProgrammingExpression.Variable(conflictingVariable)
            assertIs<Ok<*, *, *>>(conflicting.maximize(conflictingExpression))
            assertIs<Failed<*, *, *>>(conflicting.snapshot())
        } finally {
            maximum.close()
            multiple.close()
            conflicting.close()
        }
    }

    private class TestConstraintGroup(override val name: String) : MetaConstraintGroup
}
