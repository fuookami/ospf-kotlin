package fuookami.ospf.kotlin.core.solver.hyper_heuristic.scheduling

/** 可替换预算分配规则 / Replaceable budget allocation rule. */
fun interface BudgetAllocator {
    /** 分配一个片段的最大评估额度 / Allocate the maximum evaluations for one slice.
     * @param algorithm 稳定算法实例 ID / Stable algorithm instance ID
     * @param configured 配置的片段额度 / Configured slice allowance
     * @param remaining 尚未预留的全局额度 / Unreserved global allowance
     * @return 片段额度，必须大于零且不超过剩余额度 / Positive allowance not exceeding remaining budget
     */
    fun allocate(
        algorithm: String,
        configured: Long,
        remaining: Long
    ): Long
}

/** 固定片段额度 / Fixed slice allowance. */
object FixedBudgetAllocator : BudgetAllocator {
    override fun allocate(
        algorithm: String,
        configured: Long,
        remaining: Long
    ): Long = minOf(configured, remaining)
}
