package fuookami.ospf.kotlin.core.backend.solver.iis

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*

fun computeIIS(
    model: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<LinearTriadModel> {
    val elasticModel = model.elastic()

    TODO("not implemented yet")
}
