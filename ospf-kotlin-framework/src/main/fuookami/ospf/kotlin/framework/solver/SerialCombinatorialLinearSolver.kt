package fuookami.ospf.kotlin.framework.solver

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

class SerialCombinatorialLinearSolver(
    private val solvers: List<Lazy<AbstractLinearSolver>>,
    private val stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
): AbstractLinearSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: List<AbstractLinearSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialLinearSolver {
            return SerialCombinatorialLinearSolver(solvers.map { lazy { it } }, stopErrorCode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: List<() -> AbstractLinearSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialLinearSolver {
            return SerialCombinatorialLinearSolver(solvers.map { lazy { it() } }, stopErrorCode)
        }
    }

    override val name: String by lazy { "SerialCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput> {
        for (solver in solvers) {
            when (val result = solver.value.invoke(model, solvingStatusCallBack)) {
                is Ok -> {
                    return Ok(result.value)
                }

                is Failed -> {
                    if (stopErrorCode.contains(result.error.code)) {
                        return Failed(result.error.code, result.error.message)
                    } else {
                        logger.warn { "Solver ${solver.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                    }
                }
            }
        }
        return Failed(ErrorCode.SolverNotFound, "No solver valid.")
    }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        for (solver in solvers) {
            when (val result = solver.value.invoke(model, solutionAmount, solvingStatusCallBack)) {
                is Ok -> {
                    return Ok(result.value)
                }

                is Failed -> {
                    if (stopErrorCode.contains(result.error.code)) {
                        return Failed(result.error.code, result.error.message)
                    } else {
                        logger.warn { "Solver ${solver.value.name} failed with error ${result.error.code}: ${result.error.message}" }
                    }
                }
            }
        }
        return Failed(ErrorCode.SolverNotFound, "No solver valid.")
    }
}
