/**
 * 并行组合二次求解器 / Parallel Combinatorial Quadratic Solver
 *
 * 将多个二次求解器并行运行，取第一个或最优结果。 / Runs multiple quadratic solvers in parallel, taking the first or best result.
 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.AbstractQuadraticSolver
import fuookami.ospf.kotlin.core.solver.cancelledSolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.solveReport
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.report.*

/**
 * 并行组合二次求解器 / Parallel combinatorial quadratic solver
 *
 * @property solvers 二次求解器列表（懒加载） / Quadratic solver list (lazy loaded)
 * @property mode 并行组合模式，默认 Best / Parallel combinatorial mode, default Best
 */
class ParallelCombinatorialQuadraticSolver(
    private val solvers: List<Lazy<AbstractQuadraticSolver>>,
    private val mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
) : AbstractQuadraticSolver {
    private val logger = logger()

    companion object {
        /**
         * Construct from quadratic solver instances. / 从二次求解器实例构造。
         *
         * @param solvers 要组合的求解器 / Solvers to combine
         * @param mode 组合模式 / Combination mode
         * @return 并行组合求解器 / Parallel combinatorial solver
         */
        @JvmName("constructBySolvers")
        operator fun invoke(
            solvers: Iterable<AbstractQuadraticSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialQuadraticSolver {
            return ParallelCombinatorialQuadraticSolver(solvers.map { lazy { it } }, mode)
        }

        /**
         * Construct from quadratic solver providers. / 从二次求解器提供函数构造。
         *
         * @param solvers 求解器提供函数 / Solver providers
         * @param mode 组合模式 / Combination mode
         * @return 并行组合求解器 / Parallel combinatorial solver
         */
        @JvmName("constructBySolverExtractors")
        operator fun invoke(
            solvers: Iterable<() -> AbstractQuadraticSolver>,
            mode: ParallelCombinatorialMode = ParallelCombinatorialMode.Best
        ): ParallelCombinatorialQuadraticSolver {
            return ParallelCombinatorialQuadraticSolver(solvers.map { lazy { it() } }, mode)
        }
    }

    override val name: String by lazy { "ParallelCombinatorial(${solvers.joinToString(",") { it.value.name }})" }

    /**
     * 并行求解并保留全部 backend 尝试。 / Solve in parallel while preserving all backend attempts.
     *
     * @param model 二次模型 / Quadratic model
     * @param progressContext 进度上下文 / Progress context
     * @param cancellationToken 取消令牌 / Cancellation token
     * @return 组合求解报告 / Combinatorial solve report
     */
    suspend fun solveCombinatorialReport(
        model: QuadraticTetradModelView,
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
                        logger.error { "Solver ${solver.name} fatal: ${result.errors.joinToString { it.message ?: "" }}" }
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
        val selected = when {
            successful.isEmpty() -> null
            mode == ParallelCombinatorialMode.First -> successful.first()
            model.objective.category == ObjectCategory.Minimum -> successful.minBy { attempt ->
                attempt.report?.solution?.objective ?: Flt64.infinity
            }
            else -> successful.maxBy { attempt ->
                attempt.report?.solution?.objective ?: Flt64.negativeInfinity
            }
        }
        val terminal = selected ?: attempts.knownTerminalAttempt()
        Ok(
            CombinatorialSolveReport(
                finalReport = terminal?.report,
                attempts = attempts,
                selectedAttemptId = selected?.attemptId,
                selectionReason = when {
                    selected == null -> SolveSelectionReason.NoSuccessfulAttempt
                    mode == ParallelCombinatorialMode.First -> SolveSelectionReason.FirstFeasible
                    else -> SolveSelectionReason.BestObjective
                }
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
        return solveWithSolutionPool(model, solutionAmount, solvingStatusCallBack, null)
    }

    override suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?,
        cancellationToken: CancellationToken?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return solveWithSolutionPool(model, solutionAmount, solvingStatusCallBack, cancellationToken)
    }

    private suspend fun solveWithSolutionPool(
        model: QuadraticTetradModelView,
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
                    is Ok -> SolveAttemptTrace<Flt64>(
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
            selectionReason = if (selected == null) {
                SolveSelectionReason.NoSuccessfulAttempt
            } else if (mode == ParallelCombinatorialMode.First) {
                SolveSelectionReason.FirstFeasible
            } else {
                SolveSelectionReason.BestObjective
            }
        ).toTerminalReport()
        val pool = selected?.report?.solution?.pool.orEmpty().map { it.toList() }
        Ok(report to pool)
    }

    private fun selectAttempt(
        model: QuadraticTetradModelView,
        attempts: List<SolveAttemptTrace<Flt64>>
    ): SolveAttemptTrace<Flt64>? {
        return when {
            attempts.isEmpty() -> null
            mode == ParallelCombinatorialMode.First -> attempts.first()
            model.objective.category == ObjectCategory.Minimum -> attempts.minBy { it.report?.solution?.objective ?: Flt64.infinity }
            else -> attempts.maxBy { it.report?.solution?.objective ?: Flt64.negativeInfinity }
        }
    }
}

private fun <V> SolveReport<V>.withSolutionPool(pool: List<List<V>>): SolveReport<V> {
    return copy(solution = solution?.copy(pool = pool.map { it.toList() }))
}
