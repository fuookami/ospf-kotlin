@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure

import java.util.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.config.*
import fuookami.ospf.kotlin.core.solver.gurobi.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.framework.solver.*

/** 基于配置和回调类型构建列生成求解器的构建器。Builder for constructing a column generation solver based on configuration and callback type. */
data object LinearSolverBuilder {

    /**
     * Creates a [ColumnGenerationSolver] using the given solver name, config, and optional callback / 使用给定的求解器名称、配置和可选回调创建列生成求解器
     *
     * @param solver The solver name (e.g., "gurobi", "scip") / 求解器名称（如"gurobi"、"scip"）
     * @param config The solver configuration / 求解器配置
    */
    operator fun invoke(
        solver: String? = null,
        config: SolverConfig = SolverConfig(),
        callBack: Any? = null
    ): ColumnGenerationSolver {
        return (if (callBack != null) {
            when (callBack) {
                is GurobiLinearSolverCallBack -> {
                    GurobiColumnGenerationSolver(
                        config = config,
                        callBack = callBack
                    )
                }

                is ScipSolverCallBack -> {
                    ScipColumnGenerationSolver(
                        config = config,
                        callBack = callBack
                    )
                }

                else -> {
                    null
                }
            }
        } else if (solver != null) {
            when (solver) {
                "gurobi" -> {
                    GurobiColumnGenerationSolver(config = config)
                }

                "scip" -> {
                    ScipColumnGenerationSolver(config = config)
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }) ?: if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
            GurobiColumnGenerationSolver(
                config = config
            )
        } else {
            GurobiColumnGenerationSolver(
                config = config
            )
        }
    }
}
