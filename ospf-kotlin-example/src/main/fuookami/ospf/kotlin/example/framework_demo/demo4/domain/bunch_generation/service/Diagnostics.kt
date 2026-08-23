package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service

/**
 * Route graph construction counters for one aircraft.
 * 单架飞机路线图构建统计。
 *
 * @property candidateTaskCount 被检查的候选任务数 / Number of candidate tasks checked.
 * @property feasibleExpansionCount 可行扩展数 / Number of feasible expansions.
 * @property infeasibleExpansionCount 不可行扩展数 / Number of infeasible expansions.
 */
data class RouteGraphDiagnostics(
    var candidateTaskCount: Int = 0,
    var feasibleExpansionCount: Int = 0,
    var infeasibleExpansionCount: Int = 0
)

/**
 * Label-setting pricing counters for one aircraft and one pricing round.
 * 单架飞机单轮标号定价统计。
 *
 * @property candidateTaskCount 被检查的候选扩展数 / Number of candidate extensions checked.
 * @property feasibleExpansionCount 可行标签扩展数 / Number of feasible label extensions.
 * @property infeasibleExpansionCount 不可行标签扩展数 / Number of infeasible label extensions.
 * @property expandedLabelCount 实际从队列取出并扩展的标签数 / Number of labels actually dequeued and expanded.
 * @property dominatedLabelCount 因支配剪枝丢弃的标签数 / Number of labels discarded by dominance pruning.
 * @property labelLimitReachedCount 触发标签上限的次数 / Number of insertions that reached the label limit.
 * @property negativeReducedCostLabelCount 负 reduced cost 终点标签数 / Number of negative reduced-cost end labels.
 * @property generatedColumnCount 实际生成列数 / Number of generated columns.
 * @property invalidColumnCount 负 reduced cost 但无法生成列的标签数 / Number of negative labels rejected by total-cost construction.
 * @property searchTruncatedByLabelLimit 标签上限是否截断了搜索 / Whether the label limit truncated the search.
 * @property noFeasiblePath 是否没有可行完整路径 / Whether no feasible complete path exists.
 * @property noNegativeReducedCost 是否没有负 reduced cost 标签 / Whether no negative reduced-cost label exists.
 */
data class PricingDiagnostics(
    var candidateTaskCount: Int = 0,
    var feasibleExpansionCount: Int = 0,
    var infeasibleExpansionCount: Int = 0,
    var expandedLabelCount: Int = 0,
    var dominatedLabelCount: Int = 0,
    var labelLimitReachedCount: Int = 0,
    var negativeReducedCostLabelCount: Int = 0,
    var generatedColumnCount: Int = 0,
    var invalidColumnCount: Int = 0,
    var searchTruncatedByLabelLimit: Boolean = false,
    var noFeasiblePath: Boolean = false,
    var noNegativeReducedCost: Boolean = false
)
