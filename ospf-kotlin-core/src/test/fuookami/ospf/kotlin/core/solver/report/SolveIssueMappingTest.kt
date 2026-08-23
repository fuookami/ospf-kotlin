package fuookami.ospf.kotlin.core.solver.report

import kotlin.test.Test
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Err

/** Structured error taxonomy regression tests. / 结构化错误分类回归测试。 */
class SolveIssueMappingTest {
    @Test
    fun legacyErrorCodesMapToStableCategories() {
        assertEquals(SolveIssueCategory.InvalidInput, ErrorCode.IllegalArgument.toSolveIssueCategory())
        assertEquals(SolveIssueCategory.Environment, ErrorCode.SolverNotFound.toSolveIssueCategory())
        assertEquals(SolveIssueCategory.License, ErrorCode.AuthenticationError.toSolveIssueCategory())
        assertEquals(SolveIssueCategory.Numerical, ErrorCode.ORSolutionInvalid.toSolveIssueCategory())
        assertEquals(SolveIssueCategory.Parsing, ErrorCode.DeserializationFailed.toSolveIssueCategory())
        assertEquals(SolveIssueCategory.Backend, ErrorCode.OREngineSolvingException.toSolveIssueCategory())
    }

    @Test
    fun adapterCanMarkCallbackFailuresWithoutChangingStableErrorCode() {
        val issue = Err(ErrorCode.ApplicationError, "callback failed").toCallbackSolveIssue()

        assertEquals(ErrorCode.ApplicationError.toString(), issue.code)
        assertEquals(SolveIssueCategory.Callback, issue.category)
    }
}
