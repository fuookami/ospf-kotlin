package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

