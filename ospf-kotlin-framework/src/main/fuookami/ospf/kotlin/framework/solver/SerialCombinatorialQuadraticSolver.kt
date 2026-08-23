/**
 * 串行组合二次求解器 / Serial Combinatorial Quadratic Solver
 *
 * 将多个二次求解器串行运行，第一个成功即返回。 / Runs multiple quadratic solvers serially, returning on first success.
 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.core.solver.cancelledSolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.solveReport
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.report.*

/**
 * 串行组合二次求解器 / Serial combinatorial quadratic solver
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
        /**
         * Construct from quadratic solver instances. / 从二次求解器实例构造。
         *
         * @param solvers 要组合的求解器 / Solvers to combine
         * @param stopErrorCode 遇到即停止的错误码 / Error codes that stop execution
         * @return 串行组合求解器 / Serial combinatorial solver
         */
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: List<AbstractQuadraticSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialQuadraticSolver {
            return SerialCombinatorialQuadraticSolver(solvers.map { lazy { it } }, stopErrorCode)
        }

        /**
         * Construct from quadratic solver providers. / 从二次求解器提供函数构造。
         *
         * @param solvers 求解器提供函数 / Solver providers
         * @param stopErrorCode 遇到即停止的错误码 / Error codes that stop execution
         * @return 串行组合求解器 / Serial combinatorial solver
         */
        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: List<() -> AbstractQuadraticSolver>,
            stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
        ): SerialCombinatorialQuadraticSolver {
            return SerialCombinatorialQuadraticSolver(solvers.map { lazy { it() } }, stopErrorCode)
        }
    }

    override val name: String by lazy { "SerialCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    /**
     * 串行求解并保留全部已执行尝试。 / Solve serially while preserving all executed attempts.
     *
     * @param model 二次模型 / Quadratic model
     * @param progressContext 进度上下文 / Progress context
     * @return 组合求解报告 / Combinatorial solve report
     */
    suspend fun solveCombinatorialReport(
        model: QuadraticTetradModelView,
        progressContext: SolverProgressContext? = null,
        cancellationToken: CancellationToken? = null
    ): Ret<CombinatorialSolveReport<Flt64>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val attempts = mutableListOf<SolveAttemptTrace<Flt64>>()
        val parentAttemptId = SolveAttemptId("serial-combinatorial")
        for ((index, lazySolver) in solvers.withIndex()) {
            if (cancellationToken?.isCancellationRequested == true) {
                return Ok(
                    CombinatorialSolveReport(
                        finalReport = cancelledSolveReport(cancellationToken.record?.reason),
                        attempts = attempts,
                        selectedAttemptId = null,
                        selectionReason = SolveSelectionReason.NoSuccessfulAttempt
                    )
                )
            }
            val started = TimeSource.Monotonic.markNow()
            val solver = lazySolver.value
            val attemptId = SolveAttemptId("serial-$index")
            val cancellationRecord = cancellationToken?.record
            val result = solver.solveReport(model, progressContext, cancellationToken)
            val completedAt = Instant.now()
            val cancellationRecordAtCompletion = cancellationToken?.record
            when (result) {
                is Ok -> {
                    val report = result.value
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        report = report
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    if (report.terminationReason == TerminationReason.Cancelled) {
                        return Ok(
                            CombinatorialSolveReport(
                                finalReport = report,
                                attempts = attempts,
                                selectedAttemptId = null,
                                selectionReason = SolveSelectionReason.NoSuccessfulAttempt
                            )
                        )
                    }
                    val stopCode = report.combinatorialStopErrorCode()
                    if (report.hasCombinatorialIncumbent() ||
                        stopCode != null && stopErrorCode.contains(stopCode)
                    ) {
                        return Ok(
                            CombinatorialSolveReport(
                                finalReport = report,
                                attempts = attempts,
                                selectedAttemptId = attemptId.takeIf { report.hasCombinatorialIncumbent() },
                                selectionReason = if (report.hasCombinatorialIncumbent()) {
                                    SolveSelectionReason.FirstFeasible
                                } else {
                                    SolveSelectionReason.NoSuccessfulAttempt
                                }
                            )
                        )
                    }
                }

                is Failed -> {
                    logger.warn { "Solver ${solver.name} failed with error ${result.error.code}: ${result.error.message}" }
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        errors = listOf(result.error.toSolveIssue())
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    if (stopErrorCode.contains(result.error.code)) {
                        break
                    }
                }

                is Fatal -> {
                    logger.error { "Solver ${solver.name} fatal: ${result.errors.joinToString { it.message ?: "" }}" }
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        errors = result.errors.map { it.toSolveIssue() }
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    break
                }
            }
        }
        return Ok(
            CombinatorialSolveReport(
                finalReport = null,
                attempts = attempts,
                selectedAttemptId = null,
                selectionReason = SolveSelectionReason.NoSuccessfulAttempt
            )
        )
    }

    override suspend fun solveReport(
        model: QuadraticTetradModelView,
        progressContext: SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, progressContext, null)
    }

    override suspend fun solveReport(
        model: QuadraticTetradModelView,
        progressContext: SolverProgressContext?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return when (val result = solveCombinatorialReport(model, progressContext, cancellationToken)) {
            is Ok -> Ok(result.value.toTerminalReport())
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, null, null)
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, null, cancellationToken)
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val attempts = mutableListOf<SolveAttemptTrace<Flt64>>()
        val parentAttemptId = SolveAttemptId("serial-combinatorial")
        for ((index, lazySolver) in solvers.withIndex()) {
            if (cancellationToken?.isCancellationRequested == true) {
                val report = CombinatorialSolveReport(
                    finalReport = cancelledSolveReport(cancellationToken.record?.reason),
                    attempts = attempts,
                    selectedAttemptId = null,
                    selectionReason = SolveSelectionReason.NoSuccessfulAttempt
                ).toTerminalReport()
                return Ok(report to emptyList())
            }
            val started = TimeSource.Monotonic.markNow()
            val solver = lazySolver.value
            val attemptId = SolveAttemptId("serial-$index")
            val cancellationRecord = cancellationToken?.record
            val result = solver.invoke(model, solutionAmount, solvingStatusCallBack, cancellationToken)
            val completedAt = Instant.now()
            val cancellationRecordAtCompletion = cancellationToken?.record
            when (result) {
                is Ok -> {
                    val report = result.value.first.copy(
                        solution = result.value.first.solution?.copy(
                            pool = result.value.second.map { it.toList() }
                        )
                    )
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        report = report
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    if (report.terminationReason == TerminationReason.Cancelled) {
                        val terminal = CombinatorialSolveReport(
                            finalReport = report,
                            attempts = attempts,
                            selectedAttemptId = null,
                            selectionReason = SolveSelectionReason.NoSuccessfulAttempt
                        ).toTerminalReport()
                        return Ok(terminal to emptyList())
                    }
                    val stopCode = report.combinatorialStopErrorCode()
                    val hasIncumbent = report.hasCombinatorialIncumbent()
                    if (hasIncumbent ||
                        stopCode != null && stopErrorCode.contains(stopCode)
                    ) {
                        val terminal = CombinatorialSolveReport(
                            finalReport = report,
                            attempts = attempts,
                            selectedAttemptId = attemptId.takeIf { hasIncumbent },
                            selectionReason = if (hasIncumbent) {
                                SolveSelectionReason.FirstFeasible
                            } else {
                                SolveSelectionReason.NoSuccessfulAttempt
                            }
                        ).toTerminalReport()
                        return Ok(terminal to terminal.solution?.pool.orEmpty().map { it.toList() })
                    }
                }
                is Failed -> {
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        errors = listOf(result.error.toSolveIssue())
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    if (stopErrorCode.contains(result.error.code)) break
                }
                is Fatal -> {
                    attempts += SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        errors = result.errors.map { it.toSolveIssue() }
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    break
                }
            }
        }
        val report = CombinatorialSolveReport<Flt64>(
            finalReport = null,
            attempts = attempts,
            selectedAttemptId = null,
            selectionReason = SolveSelectionReason.NoSuccessfulAttempt
        ).toTerminalReport()
        return Ok(report to emptyList())
    }
}
