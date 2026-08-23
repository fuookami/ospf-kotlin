/** 求解器状态支持 / Solver status support */
package fuookami.ospf.kotlin.core.solver

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolveStatistics
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.core.solver.report.SolveIssue
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.TerminationReason

/**
 * `core.solver` 的插件支持 API：状态归一与失败兜底。 / Plugin support APIs in `core.solver` for status normalization and failure fallback.
 *
 * 目标：统一 callback 失败中止语义与状态到错误码映射语义，减少插件重复实现。 / Goal: unify callback-abort semantics and status-to-error-code mapping to reduce duplicated plugin logic.
 *
 * 非目标：不替代 solver 原生状态机，也不判断业务可恢复性。 / Non-goal: does not replace native solver state machines or decide business-level recoverability.
*/

/**
 * 统一 callback 失败结果处理，命中失败分支时执行 abort 并返回 `true`。 / Unified callback failure handling; executes abort and returns `true` on failure branches.
 *
 * @param callbackResult 回调结果（可选）/ Callback result (optional)
 * @param abort 中止操作 / Abort operation
 * @return 是否命中失败分支 / Whether a failure branch was hit
*/
inline fun shouldAbortOnCallbackFailure(
    callbackResult: Try?,
    abort: () -> Unit
): Boolean {
    return when (callbackResult) {
        is Failed, is Fatal -> {
            abort()
            true
        }

        else -> {
            false
        }
    }
}

/**
 * 将 `SolverStatus` 映射到错误码，并在缺失时返回兜底值。 / Map `SolverStatus` to error code and return fallback when status error code is absent.
 *
 * @param fallback 兜底错误码 / Fallback error code
 * @return 错误码 / Error code
*/
fun SolverStatus.resolveErrCode(
    fallback: ErrorCode = ErrorCode.OREngineSolvingException
): ErrorCode {
    return errCode ?: fallback
}

/**
 * 基于 `SolverStatus` 构造统一失败结果。
 * Build a unified failed result from `SolverStatus`.
 *
 * @param status 求解器状态 / Solver status
 * @param fallback 兜底错误码 / Fallback error code
 * @return 失败结果 / Failure result
*/
fun failByStatus(
    status: SolverStatus,
    fallback: ErrorCode = ErrorCode.OREngineSolvingException
): Try {
    return Failed(Err(status.resolveErrCode(fallback)))
}

/**
 * 构造尚未启动或没有 incumbent 的取消报告。 / Build a cancellation report with no incumbent.
 *
 * @param reason 取消原因 / Cancellation reason
 * @return 结构化取消报告 / Structured cancellation report
 */
fun cancelledSolveReport(reason: String? = null): SolveReport<Flt64> {
    return SolveReport(
        problemStatus = ProblemStatus.Unknown,
        terminationReason = TerminationReason.Cancelled,
        solutionPresence = SolutionPresence.None,
        diagnostics = SolveDiagnostics(
            warnings = listOfNotNull(
                reason?.let {
                    SolveIssue(
                        code = "solve-cancelled",
                        category = SolveIssueCategory.Backend,
                        message = "求解已取消：$it / Solve was cancelled: $it"
                    )
                }
            )
        )
    )
}

/**
 * 将正交求解报告映射为旧的状态视图，仅用于仍暴露 [SolverStatus] 的领域结果类型。 /
 * Map an orthogonal solve report to the legacy status view for domain result types that still expose [SolverStatus].
 *
 * @return 状态视图 / Status view
 */
fun SolveReport<*>.toSolverStatus(): SolverStatus {
    return when (problemStatus) {
        ProblemStatus.Infeasible -> SolverStatus.Infeasible
        ProblemStatus.Unbounded -> SolverStatus.Unbounded
        ProblemStatus.InfeasibleOrUnbounded -> SolverStatus.InfeasibleOrUnbounded
        ProblemStatus.Unknown -> SolverStatus.SolvingException
        ProblemStatus.Feasible -> if (solutionPresence == SolutionPresence.Optimal) {
            SolverStatus.Optimal
        } else {
            SolverStatus.Feasible
        }
    }
}

/**
 * 将后端状态和已提取的数值组装为统一报告。 /
 * Assemble a unified report from a backend status and extracted numerical values.
 *
 * @param objective incumbent objective / incumbent 目标值
 * @param values incumbent variable values / incumbent 变量值
 * @param solveTime backend solve time / 后端求解耗时
 * @param bestBound incumbent best bound / 当前最佳界
 * @param gap incumbent optimality gap / 当前最优间隙
 * @param terminationReason explicit termination reason / 显式终止原因
 * @return unified solve report / 统一求解报告
 */
fun SolverStatus.toSolveReport(
    objective: Flt64? = null,
    values: List<Flt64>? = null,
    solveTime: Duration,
    bestBound: Flt64? = null,
    gap: Flt64? = null,
    iterations: ULong? = null,
    nodes: ULong? = null,
    terminationReason: TerminationReason? = null,
    diagnostics: SolveDiagnostics<Flt64> = SolveDiagnostics()
): SolveReport<Flt64> {
    val problemStatus = when (this) {
        SolverStatus.Infeasible -> ProblemStatus.Infeasible
        SolverStatus.Unbounded -> ProblemStatus.Unbounded
        SolverStatus.InfeasibleOrUnbounded -> ProblemStatus.InfeasibleOrUnbounded
        SolverStatus.SolvingException -> ProblemStatus.Unknown
        SolverStatus.Optimal, SolverStatus.Feasible -> ProblemStatus.Feasible
    }
    val effectiveTermination = terminationReason ?: if (this == SolverStatus.SolvingException) {
        TerminationReason.BackendFailure
    } else {
        TerminationReason.Completed
    }
    val hasIncumbent = values != null
    return SolveReport(
        problemStatus = problemStatus,
        terminationReason = effectiveTermination,
        solutionPresence = if (!hasIncumbent) {
            SolutionPresence.None
        } else if (this == SolverStatus.Optimal) {
            SolutionPresence.Optimal
        } else {
            SolutionPresence.Incumbent
        },
        solution = values?.let { SolveSolution(values = it, objective = objective) },
        proof = SolveProof(
            status = when (this) {
                SolverStatus.Optimal -> ProofStatus.Claimed
                SolverStatus.Infeasible -> ProofStatus.Verified
                else -> ProofStatus.None
            },
            kind = "backend-status"
        ),
        statistics = SolveStatistics(
            solveTime = solveTime,
            iterations = iterations,
            nodes = nodes,
            bestBound = bestBound,
            gap = gap
        ),
        diagnostics = diagnostics
    )
}
