package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

interface LinearSolver {
    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolverOutput>
    suspend operator fun invoke(model: LinearTriadModelView, solutionAmount: UInt64): Ret<Pair<SolverOutput, List<Solution>>>

    suspend operator fun invoke(model: LinearMechanismModel): Ret<SolverOutput> {
        val intermediateModel = LinearTriadModel(model)
        return this(intermediateModel)
    }

    suspend operator fun invoke(model: LinearMechanismModel, solutionAmount: UInt64): Ret<Pair<SolverOutput, List<Solution>>> {
        val intermediateModel = LinearTriadModel(model)
        return this(intermediateModel, solutionAmount)
    }

    suspend operator fun invoke(model: LinearMetaModel): Ret<SolverOutput> {
        val mechanismModel = when (val result = LinearMechanismModel(model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel)
    }

    suspend operator fun invoke(model: LinearMetaModel, solutionAmount: UInt64): Ret<Pair<SolverOutput, List<Solution>>> {
        val mechanismModel = when (val result = LinearMechanismModel(model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
        return this(mechanismModel, solutionAmount)
    }
}
