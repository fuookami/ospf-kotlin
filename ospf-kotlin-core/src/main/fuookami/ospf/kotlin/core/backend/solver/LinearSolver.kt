package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface LinearSolver {
    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput>

    suspend operator fun invoke(model: LinearModel): Ret<SolverOutput> {
        val intermediateModel = LinearTriadModel(model)
        return this(intermediateModel)
    }

    suspend operator fun invoke(model: LinearMetaModel): Ret<SolverOutput> {
        val mechanismModel = when (val result = LinearModel(model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel)
    }
}
