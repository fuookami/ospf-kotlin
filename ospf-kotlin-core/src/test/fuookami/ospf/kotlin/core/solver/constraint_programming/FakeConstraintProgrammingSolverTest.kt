package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.core.variable.IntVariable
import fuookami.ospf.kotlin.core.variable.IntVar
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Failed

class FakeConstraintProgrammingSolverTest {
    @Test
    fun shouldEnumerateModelAndReturnOptimalSolution() = runBlocking {
        val model = ConstraintProgrammingModel("fake-optimal", ObjectCategory.Minimum)
        val variable = IntVar("x")
        model.registerVariable(variable, IntegerDomain.interval(0, 3).value!!)
        val expression = ConstraintProgrammingExpression.Variable(variable)
        model.addConstraint(ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64.one).value!!)
        model.minimize(expression)

        val solver = FakeConstraintProgrammingSolver()
        assertEquals(SolverModelType.CP, solver.descriptor.capabilities.modelTypes.single())
        val result = solver.solve(model)
        val output = assertIs<ConstraintProgrammingFeasibleOutput>(assertIs<Ok<*, *, *>>(result).value)
        assertEquals(Int64.one, output.solution.value(variable).value)
        assertEquals(fuookami.ospf.kotlin.core.solver.output.SolverStatus.Optimal, output.status)
    }

    @Test
    fun shouldRespectMaximumObjectiveDirection() = runBlocking {
        val model = ConstraintProgrammingModel("fake-maximum", ObjectCategory.Maximum)
        val variable = IntVar("maximum-value")
        try {
            model.registerVariable(variable, IntegerDomain.interval(0, 3).value!!)
            val expression = ConstraintProgrammingExpression.Variable(variable)
            model.maximize(expression)

            val output = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(FakeConstraintProgrammingSolver().solve(model)).value
            )
            assertEquals(Int64(3), output.solution.value(variable).value)
            assertEquals(Int64(3), output.exactObjective)
            assertEquals(fuookami.ospf.kotlin.core.solver.output.SolverStatus.Optimal, output.status)
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldReturnInfeasibleAndUnknownWithoutThrowing() = runBlocking {
        val infeasible = ConstraintProgrammingModel("fake-infeasible")
        val variable = IntVar("x")
        infeasible.registerVariable(variable, IntegerDomain.singleton(Int64.zero))
        val expression = ConstraintProgrammingExpression.Variable(variable)
        infeasible.addConstraint(ConstraintProgrammingConstraint.equal(expression, Int64.one).value!!)
        val output = assertIs<ConstraintProgrammingInfeasibleOutput>(
            assertIs<Ok<*, *, *>>(FakeConstraintProgrammingSolver().solve(infeasible)).value
        )
        assertEquals(1, output.conflict?.constraintIds?.size)

        val large = ConstraintProgrammingModel("fake-unknown")
        val largeVariable = IntVar("large")
        large.registerVariable(largeVariable, IntegerDomain.interval(0, 10).value!!)
        val unknown = assertIs<ConstraintProgrammingUnknownOutput>(
            assertIs<Ok<*, *, *>>(
                FakeConstraintProgrammingSolver(enumerationLimit = 1).solve(large)
            ).value
        )
        assertEquals(fuookami.ospf.kotlin.core.solver.report.TerminationReason.NodeLimit, unknown.terminationReason)
    }

    @Test
    fun shouldReturnEvaluationErrorInsteadOfInventingAnInfeasibleOrUnknownResult() = runBlocking {
        val model = ConstraintProgrammingModel("fake-evaluation-error")
        val variable = IntVar("overflow")
        model.registerVariable(variable, IntegerDomain.singleton(Int64(2)))
        val overflowing = ConstraintProgrammingExpression.Linear(
            terms = listOf(
                ConstraintProgrammingExpression.Term(variable, Int64.maximum)
            ),
            constant = Int64.zero
        )
        model.minimize(overflowing)

        val result = FakeConstraintProgrammingSolver().solve(model)
        assertIs<Failed<*, *, *>>(result)
        Unit
    }

    @Test
    fun shouldPropagateConstraintEvaluationError() = runBlocking {
        val model = ConstraintProgrammingModel("fake-constraint-evaluation-error")
        val variable = IntVar("overflow-constraint")
        model.registerVariable(variable, IntegerDomain.singleton(Int64(2)))
        val overflowing = ConstraintProgrammingExpression.Linear(
            terms = listOf(ConstraintProgrammingExpression.Term(variable, Int64.maximum)),
            constant = Int64.zero
        )
        model.addConstraint(ConstraintProgrammingConstraint.equal(overflowing, Int64.zero).value!!)

        val result = FakeConstraintProgrammingSolver().solve(model)
        assertIs<Failed<*, *, *>>(result)
        Unit
    }

    @Test
    fun shouldPropagateIntervalEvaluationError() = runBlocking {
        val model = ConstraintProgrammingModel("fake-interval-evaluation-error")
        val variable = IntVar("overflow-interval")
        model.registerVariable(variable, IntegerDomain.singleton(Int64(2)))
        val overflowing = ConstraintProgrammingExpression.Linear(
            terms = listOf(ConstraintProgrammingExpression.Term(variable, Int64.maximum)),
            constant = Int64.zero
        )
        model.registerInterval(
            IntervalVariable.create(
                id = IntervalId("overflowing"),
                start = overflowing,
                size = ConstraintProgrammingExpression.Constant(Int64.zero),
                end = ConstraintProgrammingExpression.Constant(Int64.zero)
            ).value!!
        )

        val result = FakeConstraintProgrammingSolver().solve(model)
        assertIs<Failed<*, *, *>>(result)
        Unit
    }

    @Test
    fun shouldValidateRegisteredIntervalsAndReturnOptionalIntervalValues() = runBlocking {
        val model = ConstraintProgrammingModel("fake-intervals")
        val start = IntVar("interval-start")
        val end = IntVar("interval-end")
        model.registerVariable(start, IntegerDomain.interval(0, 3).value!!)
        model.registerVariable(end, IntegerDomain.interval(0, 3).value!!)
        val interval = IntervalVariable.fixed(
            id = IntervalId("optional"),
            start = ConstraintProgrammingExpression.Variable(start),
            size = Int64(2),
            end = ConstraintProgrammingExpression.Variable(end),
            presence = BooleanLiteral.False
        ).value!!
        model.registerInterval(interval)

        val output = assertIs<ConstraintProgrammingFeasibleOutput>(
            assertIs<Ok<*, *, *>>(FakeConstraintProgrammingSolver().solve(model)).value
        )
        val value = assertIs<Ok<IntervalValue, *, *>>(output.solution.interval(IntervalId("optional"))).value
        assertEquals(false, value.present)
        assertEquals(Int64.zero, value.start)
        assertEquals(Int64.zero, value.end)

        val session = assertIs<ConstraintProgrammingSession>(
            FakeConstraintProgrammingSolver().createSession(model).value
        )
        try {
            val invalidHint = session.solve(
                hints = ConstraintProgrammingSolution(
                    intervals = mapOf(
                        IntervalId("optional") to IntervalValue(
                            start = Int64.zero,
                            size = Int64(2),
                            end = Int64.one,
                            present = true
                        )
                    )
                )
            )
            assertIs<Failed<*, *, *>>(invalidHint)
        } finally {
            session.close()
        }
    }

    @Test
    fun fixedValuesAreHardConstraintsAndValidateDomains() = runBlocking {
        val model = ConstraintProgrammingModel("fake-fixed-values")
        val variable = IntVar("fixed")
        model.registerVariable(variable, IntegerDomain.interval(0, 3).value!!)
        val id = VariableId("${variable.identifier}:${variable.index}")
        val session = assertIs<ConstraintProgrammingSession>(
            FakeConstraintProgrammingSolver().createSession(model).value
        )
        try {
            val result = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<*, *, *>>(session.solve(fixedValues = mapOf(id to Int64(2)))).value
            )
            assertEquals(Int64(2), result.solution.value(variable).value)
            assertIs<Failed<*, *, *>>(session.solve(fixedValues = mapOf(id to Int64(4))))
        } finally {
            session.close()
            model.close()
        }
    }
}
