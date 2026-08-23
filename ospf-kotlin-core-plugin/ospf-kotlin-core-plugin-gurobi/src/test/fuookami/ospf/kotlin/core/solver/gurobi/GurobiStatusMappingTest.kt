package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.GRB
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.TerminationReason

/** Gurobi late-cancellation status mapping tests. / Gurobi 迟到取消状态映射测试。 */
class GurobiStatusMappingTest {
    @Test
    fun completedNativeStatusesWinOverLateCancellation() {
        assertEquals(
            TerminationReason.Completed,
            gurobiTerminationReason(GRB.OPTIMAL, cancellationRequested = true)
        )
        assertEquals(
            TerminationReason.Completed,
            gurobiTerminationReason(GRB.INFEASIBLE, cancellationRequested = true)
        )
        assertEquals(
            TerminationReason.Completed,
            gurobiTerminationReason(GRB.UNBOUNDED, cancellationRequested = true)
        )
        assertEquals(
            TerminationReason.Completed,
            gurobiTerminationReason(GRB.INF_OR_UNBD, cancellationRequested = true)
        )
    }

    @Test
    fun nonTerminalNativeStatusStillHonorsCancellation() {
        assertEquals(
            TerminationReason.Cancelled,
            gurobiTerminationReason(GRB.INTERRUPTED, cancellationRequested = true)
        )
        assertEquals(
            TerminationReason.TimeLimit,
            gurobiTerminationReason(GRB.TIME_LIMIT, cancellationRequested = false)
        )
    }
}
