package fuookami.ospf.kotlin.core.backend.solver.iis

import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModel
import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.backend.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.utils.functional.Ret

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
suspend fun computeIIS(
    model: QuadraticTetradModelView,
    solver: AbstractQuadraticSolver,
    config: IISConfig
): Ret<QuadraticTetradModel> {
    val elasticModel = model.elastic()

    TODO("not implemented yet")
}
