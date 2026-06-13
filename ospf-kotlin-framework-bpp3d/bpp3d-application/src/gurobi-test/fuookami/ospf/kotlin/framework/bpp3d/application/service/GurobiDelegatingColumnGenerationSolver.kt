package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearSolverCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

class GurobiDelegatingColumnGenerationSolver(
    private val delegate: ColumnGenerationSolver = GurobiColumnGenerationSolver()
) : ColumnGenerationSolver by delegate {
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
