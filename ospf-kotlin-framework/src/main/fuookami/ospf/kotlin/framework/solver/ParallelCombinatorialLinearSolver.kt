/**
 * 并行组合线性求解器 / Parallel Combinatorial Linear Solver
 *
 * 并行执行多个 backend，并保留全部尝试和最终选择依据。
 * Runs multiple backends in parallel while preserving every attempt and the final selection reason.
 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.cancelledSolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 并行组合线性求解器 / Parallel combinatorial linear solver.
 *
 * @property solvers 线性求解器列表（懒加载） / Linear solver list (lazy loaded)
 * @property mode 并行组合模式 / Parallel combination mode
 */
class ParallelCombinatorialLinearSolver(
    private val solvers: List<Lazy<AbstractLinearSolver>>,
    private val mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
) : AbstractLinearSolver {
    private val logger = logger()

    companion object {
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: Iterable<AbstractLinearSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialLinearSolver {
            return ParallelCombinatorialLinearSolver(solvers.map { lazy { it } }, mode)
        }

        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: Iterable<() -> AbstractLinearSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialLinearSolver {
            return ParallelCombinatorialLinearSolver(solvers.map { lazy { it() } }, mode)
        }
    }

    override val name: String by lazy { "ParallelCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    /** 并行求解并保留全部 backend 尝试。 / Solve in parallel and retain all backend attempts. */
    suspend fun solveCombinatorialReport(
        model: LinearTriadModelView,
        progressContext: SolverProgressContext? = null,
        cancellationToken: CancellationToken? = null
    ): Ret<CombinatorialSolveReport<Flt64>> = coroutineScope {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return@coroutineScope Failed(validation.error)
            is Fatal -> return@coroutineScope Fatal(validation.errors)
        }
        if (cancellationToken?.isCancellationRequested == true) {
            return@coroutineScope Ok(
                CombinatorialSolveReport(
                    finalReport = cancelledSolveReport(cancellationToken.record?.reason),
                    attempts = emptyList(),
                    selectedAttemptId = null,
                    selectionReason = SolveSelectionReason.NoSuccessfulAttempt
                )
            )
        }
        val parentAttemptId = SolveAttemptId("parallel-combinatorial")
        val attempts = solvers.mapIndexed { index, lazySolver ->
            async(Dispatchers.Default) {
                val started = TimeSource.Monotonic.markNow()
                val solver = lazySolver.value
                val attemptId = SolveAttemptId("parallel-$index")
                val cancellationRecord = cancellationToken?.record
                val result = solver.solveReport(model, progressContext, cancellationToken)
                val completedAt = Instant.now()
                val cancellationRecordAtCompletion = cancellationToken?.record
                when (result) {
                    is Ok -> SolveAttemptTrace<Flt64>(
                        attemptId = attemptId,
                        backendId = solver.name,
                        report = result.value
                    ).withAttemptMetadata(
                        started = started,
                        cancellationRecord = cancellationRecord,
                        parentAttemptId = parentAttemptId,
                        cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                        completedAt = completedAt
                    )
                    is Failed -> {
                        logger.warn { "Solver ${solver.name} failed with error ${result.error.code}: ${result.error.message}" }
                        SolveAttemptTrace<Flt64>(
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
                    }
                    is Fatal -> {
                        logger.error { "Solver ${solver.name} fatal: ${result.errors.joinToString { it.message }}" }
                        SolveAttemptTrace<Flt64>(
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
                    }
                }
            }
        }.awaitAll()
        val successful = attempts.filter { it.report?.hasCombinatorialIncumbent() == true }
        val selected = selectAttempt(model, successful)
        val terminal = selected ?: attempts.knownTerminalAttempt()
        Ok(
            CombinatorialSolveReport(
                finalReport = terminal?.report,
                attempts = attempts,
                selectedAttemptId = selected?.attemptId,
                selectionReason = selectionReason(selected)
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
        return solveWithSolutionPool(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return solveWithSolutionPool(model, solutionAmount, solvingStatusCallBack, cancellationToken)
    }

    private suspend fun solveWithSolutionPool(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> = coroutineScope {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return@coroutineScope Failed(validation.error)
            is Fatal -> return@coroutineScope Fatal(validation.errors)
        }
        if (cancellationToken?.isCancellationRequested == true) {
            val report = cancelledSolveReport(cancellationToken.record?.reason)
            return@coroutineScope Ok(report to emptyList())
        }
        val parentAttemptId = SolveAttemptId("parallel-combinatorial")
        val attempts = solvers.mapIndexed { index, lazySolver ->
            async(Dispatchers.Default) {
                val started = TimeSource.Monotonic.markNow()
                val solver = lazySolver.value
                val attemptId = SolveAttemptId("parallel-$index")
                val cancellationRecord = cancellationToken?.record
                val result = solver.invoke(model, solutionAmount, solvingStatusCallBack, cancellationToken)
                val completedAt = Instant.now()
                val cancellationRecordAtCompletion = cancellationToken?.record
                when (result) {
                    is Ok -> {
                        SolveAttemptTrace<Flt64>(
                            attemptId = attemptId,
                            backendId = solver.name,
                            report = result.value.first.withSolutionPool(result.value.second)
                        ).withAttemptMetadata(
                            started = started,
                            cancellationRecord = cancellationRecord,
                            parentAttemptId = parentAttemptId,
                            cancellationRecordAtCompletion = cancellationRecordAtCompletion,
                            completedAt = completedAt
                        )
                    }
                    is Failed -> SolveAttemptTrace<Flt64>(
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
                    is Fatal -> SolveAttemptTrace<Flt64>(
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
                }
            }
        }.awaitAll()
        val successful = attempts.filter { it.report?.hasCombinatorialIncumbent() == true }
        val selected = selectAttempt(model, successful)
        val terminal = selected ?: attempts.knownTerminalAttempt()
        val report = CombinatorialSolveReport(
            finalReport = terminal?.report,
            attempts = attempts,
            selectedAttemptId = selected?.attemptId,
            selectionReason = selectionReason(selected)
        ).toTerminalReport()
        Ok(report to selected?.report?.solution?.pool.orEmpty().map { it.toList() })
    }

    private fun selectAttempt(
        model: LinearTriadModelView,
        attempts: List<SolveAttemptTrace<Flt64>>
    ): SolveAttemptTrace<Flt64>? {
        return when {
            attempts.isEmpty() -> null
            mode == ParallelCombinatorialMode.First -> attempts.first()
            model.objective.category == ObjectCategory.Minimum -> attempts.minBy { it.report?.solution?.objective ?: Flt64.infinity }
            else -> attempts.maxBy { it.report?.solution?.objective ?: Flt64.negativeInfinity }
        }
    }

    private fun selectionReason(selected: SolveAttemptTrace<Flt64>?): SolveSelectionReason {
        return when {
            selected == null -> SolveSelectionReason.NoSuccessfulAttempt
            mode == ParallelCombinatorialMode.First -> SolveSelectionReason.FirstFeasible
            else -> SolveSelectionReason.BestObjective
        }
    }
}

private fun <V> SolveReport<V>.withSolutionPool(pool: List<List<V>>): SolveReport<V> {
    return copy(solution = solution?.copy(pool = pool.map { it.toList() }))
}
