/** Activity report models. / 活动性报告模型。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.solver.report.ConstraintId

/**
 * Activity result for one original constraint or variable-domain member.
 *
 * `constraintId` is null for variable bounds and sparse domains; use [source]
 * as the stable identity for every record.
 */
data class ConstraintActivity(
    /** Original constraint ID, or null for a variable/domain member. / 原始约束 ID，变量或值域成员为 null。 */
    val constraintId: ConstraintId?,
    /** Constraint group name, when the record represents a constraint. / 约束记录所属组名称。 */
    val group: String?,
    /** Activity conclusion. / 活动性结论。 */
    val status: ActivityStatus,
    /** Signed scalar slack; null when no natural metric exists. / 有符号标量松弛；无自然度量时为 null。 */
    val slack: Double?,
    /** Slack normalized by the larger relevant magnitude. / 按相关量级归一化的松弛。 */
    val normalizedSlack: Double?,
    /** Absolute tolerance used for this metric. / 此度量使用的绝对容差。 */
    val tolerance: Double?,
    /** Original-model evidence. / 原始模型证据。 */
    val evidence: ActivityEvidence,
    /** Stable source identity; defaults to the evidence source. / 稳定来源身份，默认为证据来源。 */
    val source: DiagnosticSource = evidence.source
)

/** Counts of activity states for one constraint group. / 一个约束组的活动性状态计数。 */
data class ActivityGroupSummary(
    /** Group name; null means ungrouped constraints. / 组名称，null 表示未分组约束。 */
    val group: String?,
    /** Number of constraint instances in the group. / 组内约束实例数。 */
    val total: Int,
    /** Active count. / Active 数量。 */
    val active: Int,
    /** Nearly-active count. / NearlyActive 数量。 */
    val nearlyActive: Int,
    /** Inactive count. / Inactive 数量。 */
    val inactive: Int,
    /** Semantic-only satisfied count. / 仅语义求值且满足的数量。 */
    val satisfiedWithoutSlackMetric: Int,
    /** Violated count. / Violated 数量。 */
    val violated: Int,
    /** Unknown count. / Unknown 数量。 */
    val unknown: Int
) {
    /** Compatibility name for consumers that use constraintCount. / 使用 constraintCount 的兼容名称。 */
    val constraintCount: Int
        get() = total
}

/**
 * Immutable activity report for one baseline assignment.
 *
 * Group summaries deliberately count only constraint instances. Variable
 * bounds and sparse domains remain available in [activities] and are exposed
 * through [variableActivities] for callers that need the full evidence set.
 */
data class ConstraintActivityReport(
    /** Constraint and variable activity records in snapshot order. / 按 snapshot 顺序排列的约束与变量活动性记录。 */
    val activities: List<ConstraintActivity>,
    /** Group-level counts for original constraints. / 原始约束的组级计数。 */
    val groupSummaries: List<ActivityGroupSummary>,
    /** Absolute tolerance used by the analyzer. / 分析器使用的绝对容差。 */
    val tolerance: Double,
    /** Threshold for NearlyActive. / NearlyActive 阈值。 */
    val nearlyActiveTolerance: Double
) {
    /** Constraint-only records. / 仅约束记录。 */
    val constraintActivities: List<ConstraintActivity>
        get() = activities.filter { it.constraintId != null }

    /** Variable bounds and sparse-domain records. / 变量边界和值域记录。 */
    val variableActivities: List<ConstraintActivity>
        get() = activities.filter { it.constraintId == null }

    /** Number of original constraints. / 原始约束数量。 */
    val totalConstraints: Int
        get() = constraintActivities.size

    /** Number of all activity records. / 全部活动性记录数量。 */
    val totalActivities: Int
        get() = activities.size

    /** Group summaries alias. / 组汇总别名。 */
    val groups: List<ActivityGroupSummary>
        get() = groupSummaries

    /** Find a constraint activity by stable ID. / 按稳定 ID 查找约束活动性。 */
    fun constraint(id: ConstraintId): ConstraintActivity? {
        return constraintActivities.firstOrNull { it.constraintId == id }
    }
}
