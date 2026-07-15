package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

class SolverStatusSupportTest {
    @Test
    fun shouldResolveSolverStatusErrorCodeWithFallback() {
        assertEquals(ErrorCode.ORModelInfeasible, SolverStatus.Infeasible.resolveErrCode())
        assertEquals(ErrorCode.OREngineSolvingException, SolverStatus.Feasible.resolveErrCode())
        assertEquals(
            ErrorCode.ORModelUnbounded,
            SolverStatus.Feasible.resolveErrCode(ErrorCode.ORModelUnbounded)
        )
    }

    @Test
    fun shouldAbortOnFailedOrFatalCallbackResult() {
        var aborted = false
        val failed: Try = Failed(Err(ErrorCode.OREngineSolvingException))
        val fatal: Try = Fatal(Err(ErrorCode.OREngineSolvingException))
        val success: Try = ok

        assertTrue(shouldAbortOnCallbackFailure(failed) { aborted = true })
        assertTrue(aborted)

        aborted = false
        assertTrue(shouldAbortOnCallbackFailure(fatal) { aborted = true })
        assertTrue(aborted)

        aborted = false
        assertFalse(shouldAbortOnCallbackFailure(success) { aborted = true })
        assertFalse(aborted)
    }
}
