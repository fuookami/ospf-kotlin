package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import java.util.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.*
import fuookami.ospf.kotlin.core.solver.gurobi.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.framework.solver.*

/**
 * Builder for constructing linear solver instances with configurable solver selection and parameters.
 * 线性求解器构建器，支持可配置的求解器选择和参数。
*/
data object LinearSolverBuilder {
    operator fun invoke(
        solver: String? = null,
        config: SolverConfig = SolverConfig(),
        gurobiConfig: GurobiSolverConfig? = null,
        callBack: Any? = null
    ): AbstractLinearSolver {
        return (if (callBack != null) {
            when (callBack) {
                is GurobiLinearSolverCallBack -> {
                    GurobiLinearSolver(
                        config = config,
                        callBack = callBack
                    )
                }

                is ScipSolverCallBack -> {
                    ScipLinearSolver(
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
                    GurobiLinearSolver(config = config)
                }

                "scip" -> {
                    ScipLinearSolver(config = config)
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }) ?: defaultSolver(
            config = config,
            gurobiConfig = gurobiConfig
        )
    }

    private fun defaultSolver(
        config: SolverConfig,
        gurobiConfig: GurobiSolverConfig?
    ): AbstractLinearSolver {
        val solvers = ArrayList<AbstractLinearSolver>()
        if (backendClassAvailable("gurobi.GRBException")) {
            val gurobiSolverConfig = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
                config.copy(backendConfiguration = gurobiConfig)
            } else {
                config
            }
            solvers.add(GurobiLinearSolver(config = gurobiSolverConfig))
        }
        if (backendClassAvailable("jscip.Scip")) {
            solvers.add(ScipLinearSolver(config = config))
        }
        return SerialCombinatorialLinearSolver(solvers = solvers)
    }

    private fun backendClassAvailable(className: String): Boolean {
        return try {
            Class.forName(className, false, LinearSolverBuilder::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }
}
