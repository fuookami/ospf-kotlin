/** SCIP CP terminal-state mapping. / SCIP CP 终态映射。 */
package fuookami.ospf.kotlin.core.solver.scip

import jscip.SCIP_Status
import fuookami.ospf.kotlin.core.solver.report.TerminationReason

/** SCIP 终态到 OSPF 终止原因的纯函数映射。 / Pure mapping from SCIP status to OSPF termination reason. */
object ScipConstraintProgrammingStatusMapper {
    /** 映射求解终止原因。 / Map the termination reason.
     *
     * @param status SCIP terminal status. / SCIP 终态。
     * @return OSPF termination reason. / OSPF 终止原因。
     */
    fun terminationReason(status: SCIP_Status): TerminationReason {
        return when (status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL,
            SCIP_Status.SCIP_STATUS_INFEASIBLE,
            SCIP_Status.SCIP_STATUS_UNBOUNDED,
            SCIP_Status.SCIP_STATUS_INFORUNBD -> TerminationReason.Completed
            SCIP_Status.SCIP_STATUS_TIMELIMIT,
            SCIP_Status.SCIP_STATUS_MEMLIMIT,
            SCIP_Status.SCIP_STATUS_GAPLIMIT -> TerminationReason.TimeLimit
            SCIP_Status.SCIP_STATUS_NODELIMIT,
            SCIP_Status.SCIP_STATUS_TOTALNODELIMIT,
            SCIP_Status.SCIP_STATUS_STALLNODELIMIT -> TerminationReason.NodeLimit
            SCIP_Status.SCIP_STATUS_SOLLIMIT,
            SCIP_Status.SCIP_STATUS_BESTSOLLIMIT -> TerminationReason.SolutionLimit
            SCIP_Status.SCIP_STATUS_USERINTERRUPT -> TerminationReason.Cancelled
            SCIP_Status.SCIP_STATUS_TERMINATE -> TerminationReason.Interrupted
            else -> TerminationReason.BackendFailure
        }
    }

    /** Map a status while preserving a completed native conclusion over a late cancellation. /
     * 在迟到取消请求下仍优先保留已完成的原生结论。
     *
     * @param status SCIP terminal status. / SCIP 终态
     * @param cancellationRequested whether cancellation was requested after native solve. / 原生求解后是否收到取消请求
     * @return OSPF termination reason. / OSPF 终止原因
     */
    fun terminationReason(
        status: SCIP_Status,
        cancellationRequested: Boolean
    ): TerminationReason {
        val mapped = terminationReason(status)
        return if (mapped == TerminationReason.Completed || !cancellationRequested) {
            mapped
        } else {
            TerminationReason.Cancelled
        }
    }
}
