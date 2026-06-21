/**
 * 串行组合列生成求解器
 * Serial Combinatorial Column Generation Solver
 *
 * 将多个列生成求解器串行运行，第一个成功即返回。
 * Runs multiple column generation solvers serially, returning on first success.
 */
package fuookami.ospf.kotlin.framework.solver

import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.error.SolverNotFoundError
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.*

/**
 * 串行组合列生成求解器
 * Serial combinatorial column generation solver
 *
 * @property solvers 列生成求解器列表（懒加载） / Column generation solver list (lazy loaded)
 * @property stopErrorCode 遇到即停止的错误码 / Error codes that stop execution
 */
class SerialCombinatorialColumnGenerationSolver(
    private val solvers: List<Lazy<ColumnGenerationSolver>>,
    private val stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
) : ColumnGenerationSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: List<ColumnGenerationSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialColumnGenerationSolver {
            return SerialCombinatorialColumnGenerationSolver(solvers.map { lazy { it } }, stopErrorCode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: List<() -> ColumnGenerationSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialColumnGenerationSolver {
            return SerialCombinatorialColumnGenerationSolver(solvers.map { lazy { it() } }, stopErrorCode)
        }
    }

    override val name: String by lazy { "SerialCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        for ((i, solver) in solvers.withIndex()) {
            when (val result = solver.value.solveMILP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack?.let {
                    { status: SolvingStatus -> it(status.copy(solver = solver.value.name, solverIndex = UInt64(i))) }
                }
            )) {
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return Failed(SolverNotFoundError())
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        for ((i, solver) in solvers.withIndex()) {
            when (val result = solver.value.solveLP(
                name = name,
                metaModel = metaModel,
                toLogModel = toLogModel,
                registrationStatusCallBack = registrationStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack?.let {
                    { status: SolvingStatus -> it(status.copy(solver = solver.value.name, solverIndex = UInt64(i))) }
                }
            )) {
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return Failed(SolverNotFoundError())
    }
}