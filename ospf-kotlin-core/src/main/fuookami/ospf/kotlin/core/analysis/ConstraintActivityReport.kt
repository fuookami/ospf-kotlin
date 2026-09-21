/** Activity report models. / 活动性报告模型。 */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.solver.report.ConstraintId

/**
 * 一个原始约束或变量值域成员的活动性结果。 / Activity result for one original constraint or variable-domain member.
 *
 * 变量边界和稀疏值域的 `constraintId` 为 null；所有记录都应使用 [source] 作为稳定身份。
 * / `constraintId` is null for variable bounds and sparse domains; use [source]` as the stable
 * identity for every record.
 *
 * @property constraintId 原始约束 ID，变量或值域成员为 null / Original constraint ID, or null for variable/domain members
 * @property group 约束所属组名称 / Constraint group name
 * @property status 活动性结论 / Activity conclusion
 * @property slack 有符号标量松弛，无自然度量时为 null / Signed scalar slack, or null without a natural metric
 * @property normalizedSlack 按相关量级归一化的松弛 / Slack normalized by the relevant magnitude
 * @property tolerance 此度量使用的绝对容差 / Absolute tolerance used for the metric
 * @property evidence 原始模型证据 / Original-model evidence
 * @property source 稳定来源身份 / Stable source identity
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

/** 一个约束组的活动性状态计数。 / Counts of activity states for one constraint group.
 *
 * @property group 组名称，null 表示未分组约束 / Group name, or null for ungrouped constraints
 * @property total 组内约束实例数 / Number of constraint instances in the group
 * @property active Active 状态数量 / Number of Active records
 * @property nearlyActive NearlyActive 状态数量 / Number of NearlyActive records
 * @property inactive Inactive 状态数量 / Number of Inactive records
 * @property satisfiedWithoutSlackMetric 无标量松弛度量但满足的数量 / Number satisfied without a scalar slack metric
 * @property violated Violated 状态数量 / Number of Violated records
 * @property unknown Unknown 状态数量 / Number of Unknown records
 */
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
 * 一个基线赋值的不可变活动性报告。 / Immutable activity report for one baseline assignment.
 *
 * 组汇总只统计约束实例；变量边界和稀疏值域仍保留在 [activities] 中，并通过 [variableActivities]
 * 提供给需要完整证据集合的调用方。 / Group summaries deliberately count only constraint instances;
 * variable bounds and sparse domains remain in [activities] and are exposed through
 * [variableActivities] for callers that need the full evidence set.
 *
 * @property activities 按 snapshot 顺序排列的约束与变量活动性记录 / Constraint and variable activity records in snapshot order
 * @property groupSummaries 原始约束的组级计数 / Group-level counts for original constraints
 * @property tolerance 分析器使用的绝对容差 / Absolute tolerance used by the analyzer
 * @property nearlyActiveTolerance NearlyActive 阈值 / Threshold for NearlyActive
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

    /** 按稳定 ID 查找约束活动性。 / Find a constraint activity by stable ID.
     *
     * @param id 要查找的约束 ID / Constraint ID to find
     * @return 匹配的活动性记录，找不到时为 null / Matching activity record, or null when absent
     */
    fun constraint(id: ConstraintId): ConstraintActivity? {
        return constraintActivities.firstOrNull { it.constraintId == id }
    }
}
