package fuookami.ospf.kotlin.core.backend.solver.iis

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*

suspend fun computeIIS(
    model: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    config: IISConfig
): Ret<QuadraticTetradModel> {
    val elasticModel = model.elastic()

    TODO("not implemented yet")
}
