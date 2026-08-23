package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.MipBackedConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.scip.ScipConstraintProgrammingSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok

/** Compare SCIP native CP and Gurobi exact MIP lowering on one common model. / 对同一模型比较 SCIP 原生 CP 与 Gurobi 精确 MIP 降维。 */
class ConstraintProgrammingDifferentialIT {
    @Test
    fun supportedModelShouldAgreeAcrossScipAndGurobi() = runBlocking {
        val scip = solveWith(ScipConstraintProgrammingSolver())
        val gurobi = solveWith(MipBackedConstraintProgrammingSolver(GurobiLinearSolver()))

        assertEquals(scip.status, gurobi.status)
        assertEquals(scip.value, gurobi.value)
        assertEquals(scip.objective, gurobi.objective)
    }

    private data class DifferentialResult(
        val status: SolverStatus,
        val value: Int64,
        val objective: Flt64?
    )

    private suspend fun solveWith(
        solver: ConstraintProgrammingSolver
    ): DifferentialResult {
        val model = ConstraintProgrammingModel("cp-differential", ObjectCategory.Minimum)
        try {
            val value = fuookami.ospf.kotlin.core.variable.IntVar("value")
            model.registerVariable(value, IntegerDomain.interval(0, 5).value!!)
            val expression = ConstraintProgrammingExpression.Variable(value)
            model.addConstraint(
                ConstraintProgrammingConstraint.greaterOrEqual(expression, Int64(3)).value!!
            )
            model.minimize(expression)

            val result = solver.solve(model)
            val failureMessage = (result as? fuookami.ospf.kotlin.utils.functional.Failed<*, *, *>)
                ?.error
                ?.let { "${it.code}: ${it.message}" }
            val successful = assertIs<Ok<*, *, *>>(
                result,
                "CP differential solver failed: ${failureMessage ?: result}"
            )
            val output = assertIs<ConstraintProgrammingFeasibleOutput>(successful.value)
            return DifferentialResult(
                status = output.status,
                value = output.solution.value(value).value!!,
                objective = output.objective
            )
        } finally {
            model.close()
        }
    }
}
