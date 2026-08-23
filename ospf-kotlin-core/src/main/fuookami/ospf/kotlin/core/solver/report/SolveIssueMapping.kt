/** Structured mapping from legacy error codes to solve-report issue categories. / 旧错误码到报告问题分类的结构化映射。 */
package fuookami.ospf.kotlin.core.solver.report

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode

/**
 * Map an existing error code to the stable report taxonomy. / 将既有错误码映射为稳定报告分类。
 *
 * Normal terminal model statuses should be represented by [SolveReport]. If an old backend still
 * returns one as `Failed`, it remains classified as a backend issue rather than being mistaken for
 * invalid input. / 正常终态应使用 [SolveReport] 表达；旧 backend 若仍以 `Failed` 返回，仍归为 backend
 * 问题，避免被误判为输入非法。
 */
fun ErrorCode.toSolveIssueCategory(): SolveIssueCategory {
    return when (this) {
        ErrorCode.IllegalArgument,
        ErrorCode.SymbolRepetitive,
        ErrorCode.DataEmpty,
        ErrorCode.DataNotFound -> SolveIssueCategory.InvalidInput

        ErrorCode.SerializationFailed,
        ErrorCode.DeserializationFailed -> SolveIssueCategory.Parsing

        ErrorCode.AuthenticationError -> SolveIssueCategory.License

        ErrorCode.SolverNotFound,
        ErrorCode.OREngineEnvironmentLost,
        ErrorCode.OREngineConnectionOvertime -> SolveIssueCategory.Environment

        ErrorCode.ORSolutionInvalid -> SolveIssueCategory.Numerical

        ErrorCode.ORModelInfeasible,
        ErrorCode.ORModelUnbounded,
        ErrorCode.ORModelInfeasibleOrUnbounded,
        ErrorCode.OREngineModelingException,
        ErrorCode.OREngineSolvingException,
        ErrorCode.OREngineTerminated -> SolveIssueCategory.Backend

        else -> SolveIssueCategory.Backend
    }
}

/** Convert a legacy error into a structured report issue. / 将旧错误转换为结构化报告问题。 */
fun Error<ErrorCode>.toSolveIssue(): SolveIssue {
    return toSolveIssue(code.toSolveIssueCategory())
}

/** Convert an error to a structured issue with an adapter-supplied category. / 将错误按适配器指定类别转换为结构化问题。 */
fun Error<ErrorCode>.toSolveIssue(category: SolveIssueCategory): SolveIssue {
    return SolveIssue(
        code = code.toString(),
        category = category,
        message = message
    )
}

/** Mark a callback failure explicitly instead of treating it as a generic backend failure. /
 * 显式标记回调失败，避免将其误归为一般 backend 故障。 */
fun Error<ErrorCode>.toCallbackSolveIssue(): SolveIssue {
    return toSolveIssue(SolveIssueCategory.Callback)
}
