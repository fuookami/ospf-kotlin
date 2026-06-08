package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolveConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution

/**
 * CSP1D 恢复求解入口（最小实现）/ CSP1D recovery entry point (minimal implementation)
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dRecovery<V : RealNumber<V>>(
    solver: ColumnGenerationSolver,
    private val milp: Csp1dMilp<V> = Csp1dMilp(solver)
) {
    /**
     * 在异常恢复场景下重新求解 / Re-solve for recovery scenarios
     *
     * @param problem 问题定义 / Problem definition
     * @return 求解结果 / Solution
     */
    suspend fun solve(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dSolution<V> {
        return milp.solve(
            problem = problem,
            solveConfig = solveConfig
        )
    }
}
