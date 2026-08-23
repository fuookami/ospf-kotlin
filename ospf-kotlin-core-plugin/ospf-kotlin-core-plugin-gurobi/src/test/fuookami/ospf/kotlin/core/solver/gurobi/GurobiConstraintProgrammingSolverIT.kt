package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.constraint_programming.MipBackedConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok

/** 验证 CP 精确降阶可以通过 Gurobi 求解 / Verify exact CP lowering through Gurobi. */
class GurobiConstraintProgrammingSolverIT {
    @Test
    fun shouldSolveLoweredCpWithHintAndAssumptionRebuild() = runBlocking {
        val model = ConstraintProgrammingModel("gurobi-backed-cp", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("value")
            model.registerVariable(value, IntegerDomain.interval(0, 5).value!!)
            val expression = ConstraintProgrammingExpression.Variable(value)
            model.addConstraint(
                ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64(3)).value!!
            )
            model.minimize(expression)

            val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
            val session = assertIs<ConstraintProgrammingSession>(solver.createSession(model).value)
            try {
                val solved = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve()).value
                )
                assertEquals(Int64(3), solved.solution.value(value).value)

                val hinted = assertIs<ConstraintProgrammingFeasibleOutput>(
                    assertIs<Ok<*, *, *>>(
                        session.solve(
                            hints = ConstraintProgrammingSolution(
                                values = mapOf(
                                    VariableId("${value.identifier}:${value.index}") to Int64(4)
                                )
                            )
                        )
                    ).value
                )
                assertEquals(Int64(3), hinted.solution.value(value).value)

                val infeasible = assertIs<ConstraintProgrammingInfeasibleOutput>(
                    assertIs<Ok<*, *, *>>(session.solve(assumptions = listOf(BooleanLiteral.False))).value
                )
                assertEquals(fuookami.ospf.kotlin.core.solver.report.ProofStatus.Verified, infeasible.proofStatus)
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }
}
