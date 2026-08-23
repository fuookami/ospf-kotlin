/** Combinatorial solve report support. / 组合求解报告支持。 */
package fuookami.ospf.kotlin.framework.solver

import java.time.Instant
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.core.solver.report.CancellationRecord
import fuookami.ospf.kotlin.core.solver.report.CombinatorialSolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveAttemptId
import fuookami.ospf.kotlin.core.solver.report.SolveAttemptTrace
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.utils.error.ErrorCode

/**
 * Complete backend attempt timing and cancellation metadata. / 补齐 backend attempt 的耗时和取消元数据。
 *
 * `completedAt` is the wrapper's concurrency linearization point captured after the backend invocation returns;
 * it is not the native solver call's exact physical return instant. / `completedAt` 是 wrapper 在 backend 调用返回后捕获的并发线性化点，
 * 不是 native 求解调用物理返回的精确时刻。
 */
internal fun <V> SolveAttemptTrace<V>.withAttemptMetadata(
    started: TimeSource.Monotonic.ValueTimeMark,
    cancellationRecord: CancellationRecord?,
    parentAttemptId: SolveAttemptId,
    cancellationRecordAtCompletion: CancellationRecord?,
    completedAt: Instant
): SolveAttemptTrace<V> {
    val cancelled = report?.terminationReason == TerminationReason.Cancelled
    val effectiveCancellationRecord = if (cancelled) {
        cancellationRecordAtCompletion
            ?.takeIf { record ->
                !record.requestedAt.isAfter(completedAt)
            }
            ?: cancellationRecord
    } else {
        cancellationRecord
    }
    val cancellationReason = if (cancelled || (report == null && cancellationRecord != null)) {
        effectiveCancellationRecord?.reason ?: "cancellation requested"
    } else {
        null
    }
    return copy(
        parentAttemptId = parentAttemptId,
        elapsed = started.elapsedNow(),
        cancellationReason = cancellationReason
    )
}

/** Whether a report contains a usable incumbent for backend selection. / 报告是否包含可用于组合选择的 incumbent。 */
internal fun SolveReport<*>.hasCombinatorialIncumbent(): Boolean {
    return solution != null && solutionPresence != SolutionPresence.None
}

/** Map a normal terminal conclusion back to the old stop-code configuration. / 将正常终态映射回旧 stop-code 配置。 */
internal fun SolveReport<*>.combinatorialStopErrorCode(): ErrorCode? {
    return when (problemStatus) {
        ProblemStatus.Infeasible -> ErrorCode.ORModelInfeasible
        ProblemStatus.Unbounded -> ErrorCode.ORModelUnbounded
        ProblemStatus.InfeasibleOrUnbounded -> ErrorCode.ORModelInfeasibleOrUnbounded
        else -> null
    }
}

/** Select a known no-incumbent terminal report for a complete attempt trace. / 选择已知的无 incumbent 正常终态。 */
internal fun <V> List<SolveAttemptTrace<V>>.knownTerminalAttempt(): SolveAttemptTrace<V>? {
    return firstOrNull { attempt ->
        attempt.report?.let { report ->
            report.problemStatus != ProblemStatus.Unknown ||
                report.terminationReason == TerminationReason.Cancelled
        } == true
    }
}

/**
 * Convert a combinatorial report with no selected attempt into a terminal report. /
 * 将没有选中尝试的组合报告转换为终态报告。
 *
 * Every backend error remains in `diagnostics.errors`; no information is compressed into a
 * single solver-not-found error. / 每个 backend 错误都保留在 `diagnostics.errors` 中，不再压缩为
 * 单一 solver-not-found 错误。
 *
 * @return terminal report / 终态报告
 */
internal fun <V> CombinatorialSolveReport<V>.toTerminalReport(): SolveReport<V> {
    val attemptErrors = attempts.flatMap { it.errors }
    val fallback = finalReport ?: SolveReport(
        problemStatus = ProblemStatus.Unknown,
        terminationReason = TerminationReason.BackendFailure,
        solutionPresence = SolutionPresence.None,
        diagnostics = SolveDiagnostics()
    )
    val errors = fallback.diagnostics.errors + attemptErrors
    val terminalErrors = if (errors.isNotEmpty()) {
        errors
    } else if (finalReport == null) {
        listOf(
            SolveIssue(
                code = "combinatorial-no-successful-attempt",
                category = SolveIssueCategory.Backend,
                message = "所有组合 backend 均未形成报告 / No combinatorial backend produced a report"
            )
        )
    } else {
        emptyList()
    }
    return fallback.copy(
        diagnostics = fallback.diagnostics.copy(errors = terminalErrors),
        attempts = attempts,
        selectedAttemptId = selectedAttemptId,
        selectionReason = selectionReason
    )
}
