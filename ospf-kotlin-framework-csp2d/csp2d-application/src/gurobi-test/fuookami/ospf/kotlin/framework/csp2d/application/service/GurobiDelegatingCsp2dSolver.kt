/**
 * Gurobi 委托列生成求解器。 / Gurobi delegating column generation solver.
 *
 * 把 `GurobiColumnGenerationSolver` 适配到框架的 `ColumnGenerationSolver` 接口，避免应用层直接依赖 gurobi
 * 插件类型。 / Adapts `GurobiColumnGenerationSolver` to the framework `ColumnGenerationSolver` interface so the
 * application layer does not depend on gurobi plugin types directly.
*/
package fuookami.ospf.kotlin.framework.csp2d.application.service

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearSolverCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * Gurobi 委托列生成求解器。 / Gurobi delegating column generation solver.
 *
 * @property delegate 被委托的列生成求解器 / delegated column generation solver
*/
class GurobiDelegatingCsp2dSolver(
    private val delegate: ColumnGenerationSolver = GurobiColumnGenerationSolver()
) : ColumnGenerationSolver by delegate {

    /**
     * 由 Gurobi 求解器配置构造。 / Build from a Gurobi solver configuration.
     *
     * @param config 求解器配置 / solver configuration
     * @param callBack 线性求解器回调 / linear solver callback
    */
    constructor(
        config: SolverConfig,
        callBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack()
    ) : this(
        GurobiColumnGenerationSolver(
            config = config,
            callBack = callBack
        )
    )
}
