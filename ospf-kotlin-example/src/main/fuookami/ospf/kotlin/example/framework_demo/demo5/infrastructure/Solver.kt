package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * demo5 列生成求解器构建器。 / demo5 column generation solver builder.
 *
 * 支持 Gurobi 和 SCIP LP/MILP 求解器后端。
 * Supports Gurobi and SCIP LP/MILP solver backends.
 */
object LinearSolverBuilder {
    /**
     * 创建列生成求解器。 / Create a column generation solver.
     *
     * @param solver 求解器名称 / Solver name
     * @param config 求解器配置 / Solver configuration
     * @return 列生成求解器 / Column generation solver
     */
    operator fun invoke(
        solver: String = "gurobi",
        config: SolverConfig = SolverConfig()
    ): ColumnGenerationSolver {
        return when (solver.lowercase()) {
            "scip" -> ScipColumnGenerationSolver(config = config)
            else -> GurobiColumnGenerationSolver(config = config)
        }
    }
}
