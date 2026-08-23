package fuookami.ospf.kotlin.framework.network_scheduling.application.model

/**
 * Branch-and-Price 终止状态。 / Branch-and-Price termination status.
 *
 * 正常算法终态返回 Ok(VrptwSolveResult)；
 * 输入、单位转换、solver 调用或对偶证书失败返回 Failed。 / Normal algorithm terminal states return Ok(VrptwSolveResult);
 * input, unit conversion, solver call, or dual certificate failures return Failed.
 */
enum class BranchAndPriceStatus {
    /** 活动节点为空且有 incumbent / No active nodes and incumbent exists */
    Optimal,

    /**
     * 队列耗尽且存在 incumbent，但由有效节点下界计算出的 gap 尚未闭合。 / An incumbent exists after the queue is exhausted, but the gap computed from valid node
     * lower bounds is not closed.
     */
    Feasible,

    /** 活动节点为空且无 incumbent / No active nodes and no incumbent */
    Infeasible,

    /**
     * 达到时间上限终止；可能有也可能无 incumbent（无 incumbent 时 solution 为 null、upperBound 为 +∞、gap 为 +∞）。
     * Terminated at the time limit; may or may not carry an incumbent (solution null, upperBound +∞, gap +∞ when absent).
     */
    TimeLimit,

    /**
     * 达到节点上限终止；可能有也可能无 incumbent（无 incumbent 时 solution 为 null、upperBound 为 +∞、gap 为 +∞）。
     * Terminated at the node limit; may or may not carry an incumbent (solution null, upperBound +∞, gap +∞ when absent).
     */
    NodeLimit,

    /**
     * 外部 solver 携带有效 bound/incumbent 的正常停止（非最优、非资源上限）。纯 B&P 不将
     * 缺少精确 LP 证书的状态映射为此值；该状态保留给外部停止协议。 / External solver normal stop carrying a valid bound/incumbent (neither optimal nor a resource
     * limit). Pure B&P does not map an LP without an exact certificate to this value; it is reserved
     * for external-stop protocols.
     */
    SolverStopped
}
