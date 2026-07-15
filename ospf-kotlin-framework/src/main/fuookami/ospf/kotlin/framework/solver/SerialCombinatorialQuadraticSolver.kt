/**
 * 串行组合二次求解器
 * Serial Combinatorial Quadratic Solver
 *
 * 将多个二次求解器串行运行，第一个成功即返回。
 * Runs multiple quadratic solvers serially, returning on first success.
*/
package fuookami.ospf.kotlin.framework.solver

import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.error.SolverNotFoundError
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 串行组合二次求解器
 * Serial combinatorial quadratic solver
 *
 * @property solvers 二次求解器列表（懒加载） / Quadratic solver list (lazy loaded)
 * @property stopErrorCode 遇到即停止的错误码 / Error codes that stop execution
*/
class SerialCombinatorialQuadraticSolver(
    private val solvers: List<Lazy<AbstractQuadraticSolver>>,
    private val stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
) : AbstractQuadraticSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: List<AbstractQuadraticSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialQuadraticSolver {
            return SerialCombinatorialQuadraticSolver(solvers.map { lazy { it } }, stopErrorCode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: List<() -> AbstractQuadraticSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialQuadraticSolver {
            return SerialCombinatorialQuadraticSolver(solvers.map { lazy { it() } }, stopErrorCode)
        }
    }

    override val name: String by lazy { "SerialCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
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

                is Fatal<*, *, *> -> {
                    return Fatal(ErrorCode.OREngineSolvingException, result.errors.joinToString("; ") { it.message ?: "" })
                }
            }
        }
        return Failed(SolverNotFoundError())
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
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

                is Fatal<*, *, *> -> {
                    return Fatal(ErrorCode.OREngineSolvingException, result.errors.joinToString("; ") { it.message ?: "" })
                }
            }
        }
        return Failed(SolverNotFoundError())
    }
}


