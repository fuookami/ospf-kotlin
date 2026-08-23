/**
 * 串行组合线性求解器 / Serial Combinatorial Linear Solver
 *
 * 将多个线性求解器串行运行，并保留每次尝试的结构化报告。
 * Runs multiple linear solvers serially while preserving a structured trace for every attempt.
 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.cancelledSolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 串行组合线性求解器 / Serial combinatorial linear solver.
 *
 * @property solvers 线性求解器列表（懒加载） / Linear solver list (lazy loaded)
 * @property stopErrorCode 保留兼容配置；正常终态现在由 SolveReport 表达 / Compatibility stop-code configuration
 */
class SerialCombinatorialLinearSolver(
    private val solvers: List<Lazy<AbstractLinearSolver>>,
    private val stopErrorCode: Set<ErrorCode> = setOf(ErrorCode.ORModelInfeasible, ErrorCode.ORModelUnbounded)
) : AbstractLinearSolver {
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

    /**
     * 串行求解并保留全部已执行尝试。 / Solve serially while preserving all executed attempts.
     */
    suspend fun solveCombinatorialReport(
        model: LinearTriadModelView,
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
                    logger.error { "Solver ${solver.name} fatal: ${result.errors.joinToString { it.message }}" }
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
        model: LinearTriadModelView,
        progressContext: SolverProgressContext?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, progressContext, null)
    }

    override suspend fun solveReport(
        model: LinearTriadModelView,
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
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, null, null)
    }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<SolveReport<Flt64>> {
        return solveReport(model, null, cancellationToken)
    }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
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
                    val report = result.value.first.withSolutionPool(result.value.second)
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
                    if (stopErrorCode.contains(result.error.code)) {
                        break
                    }
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

private fun <V> SolveReport<V>.withSolutionPool(pool: List<List<V>>): SolveReport<V> {
    return copy(solution = solution?.copy(pool = pool.map { it.toList() }))
}
