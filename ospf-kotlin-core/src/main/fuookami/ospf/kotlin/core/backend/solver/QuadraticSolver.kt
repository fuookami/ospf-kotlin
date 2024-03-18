package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface QuadraticSolver {
    suspend operator fun invoke(model: QuadraticTetradModelView): Ret<SolverOutput>

    suspend operator fun invoke(model: QuadraticModel): Ret<SolverOutput> {
        val intermediateModel = QuadraticTetradModel(model)
        return this(intermediateModel)
    }

    suspend operator fun invoke(model: QuadraticMetaModel): Ret<SolverOutput> {
        val mechanismModel = when (val result = QuadraticModel(model)) {
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
