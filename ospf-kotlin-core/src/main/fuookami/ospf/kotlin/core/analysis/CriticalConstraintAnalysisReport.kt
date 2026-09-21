/** 候选漏斗、有效性排序与统一分析报告。 / Candidate funnel, effectiveness ranking, and the unified report. */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.report.ConstraintId

/** 统一报告 schema。 / Unified report schema. */
const val CRITICAL_ANALYSIS_REPORT_SCHEMA_VERSION: String = "1.0"

/**
 * 排序键投影。
 *
 * [Flt64] 不实现 [Comparable]，因此排序时必须显式投影为可比较标量。该投影只用于**排序**，
 * 不参与任何模型语义判定；语义比较一律使用 [Flt64] 自身的运算符。
 *
 * Sort-key projection. [Flt64] does not implement [Comparable], so ordering must project to a
 * comparable scalar explicitly. This projection is used for **ordering only** and never
 * participates in a model-semantic decision; semantic comparisons always use [Flt64]'s own
 * operators.
 */
private fun Flt64?.sortKey(): Double {
    return this?.toSolverDouble("analysis.sortKey") ?: Double.NEGATIVE_INFINITY
}

/**
 * 候选分层。
 *
 * 分层只表达"做昂贵分析的优先级"，不表达约束是否重要。Tier C 依然保留在结果中。
 *
 * Candidate tier. The tier expresses the priority for expensive analysis only, never whether a
 * constraint matters. Tier C is still retained in the result.
 */
enum class CandidateTier {
    /** active 且 |dual| 超过阈值。 / Active with a dual above the threshold. */
    TierA,

    /** active 但 dual 约为 0。 / Active with an approximately zero dual. */
    TierB,

    /** 非 active。 / Not active. */
    TierC,

    /** 证据不足，无法分层。 / Insufficient evidence to assign a tier. */
    Unclassified;

    /** 计划中的层级名称。 / Tier name used by the plan. */
    val tierName: String
        get() = when (this) {
            TierA -> "A"
            TierB -> "B"
            TierC -> "C"
            Unclassified -> "UNCLASSIFIED"
        }

    /** 该层是否默认进入昂贵分析短名单。 / Whether this tier enters the expensive shortlist. */
    val isShortlistDefault: Boolean
        get() = this == TierA || this == TierB
}

/** 漏斗配置。 / Funnel configuration. */
data class CandidateFunnelConfig(
    /** |dual| 超过该值才算强候选。 / A dual above this value marks a strong candidate. */
    val dualThreshold: Double = DEFAULT_DUAL_THRESHOLD,
    /** 昂贵分析的默认候选上限。 / Default candidate limit for expensive analysis. */
    val defaultCandidateLimit: Int = DEFAULT_CANDIDATE_LIMIT
) {
    /** 校验配置。 / Validate the configuration. */
    val validation: Try
        get() = when {
            !dualThreshold.isFinite() || dualThreshold < 0.0 -> Failed(
                ErrorCode.IllegalArgument,
                "候选漏斗对偶阈值必须有限且非负 / Candidate funnel dual threshold must be finite and non-negative"
            )

            defaultCandidateLimit <= 0 -> Failed(
                ErrorCode.IllegalArgument,
                "候选漏斗候选上限必须为正 / Candidate funnel candidate limit must be positive"
            )

            else -> ok
        }

    companion object {
        /** 默认对偶阈值。 / Default dual threshold. */
        const val DEFAULT_DUAL_THRESHOLD: Double = 1e-9

        /** 默认候选上限。 / Default candidate limit. */
        const val DEFAULT_CANDIDATE_LIMIT: Int = 32
    }
}

/** 一个候选约束。 / One candidate constraint. */
data class ConstraintCandidate(
    /** 原始约束稳定身份。 / Stable original-constraint identity. */
    val constraintId: ConstraintId,
    /** 约束组。 / Constraint group. */
    val group: String?,
    /** 分层。 / Tier. */
    val tier: CandidateTier,
    /** 活动性状态。 / Activity status. */
    val activity: ActivityStatus,
    /** signed slack。 / Signed slack. */
    val slack: Double?,
    /** 固定整数 LP 对偶值。 / Fixed-integer LP dual value. */
    val dualValue: Flt64?,
    /** 局部有效性。 / Local effectiveness. */
    val localEffective: Boolean?,
    /** 排序位置，0 表示优先级最高。 / Rank position; 0 is the highest priority. */
    val priority: Int
) {
    /** 原始证据来源。 / Original evidence source. */
    val source: DiagnosticSource
        get() = DiagnosticSource.Constraint(constraintId)
}

/** 候选漏斗排序结果。 / Candidate funnel ranking. */
data class CandidateFunnelRanking(
    /** 使用的对偶阈值。 / Dual threshold used. */
    val dualThreshold: Double,
    /** 全部候选，按优先级排序。 / Every candidate, ordered by priority. */
    val candidates: List<ConstraintCandidate>,
    /** Tier A 数量。 / Tier A count. */
    val tierA: Int,
    /** Tier B 数量。 / Tier B count. */
    val tierB: Int,
    /** Tier C 数量。 / Tier C count. */
    val tierC: Int,
    /** 无法分层的数量。 / Unclassified count. */
    val unclassified: Int,
    /** 报告 schema。 / Report schema. */
    val schemaVersion: String = CRITICAL_ANALYSIS_REPORT_SCHEMA_VERSION
) {
    /**
     * 昂贵分析短名单。
     *
     * 短名单优先包含强候选与退化候选；Tier C 及未分层候选只有在短名单仍有名额时才被纳入，
     * 因此它们被降权而不会被永久丢弃。
     *
     * Shortlist for expensive analysis. The shortlist prefers strong and degenerate candidates;
     * Tier C and unclassified candidates are appended only while slots remain, so they are
     * deprioritised rather than dropped.
     */
    fun shortlist(limit: Int = CandidateFunnelConfig.DEFAULT_CANDIDATE_LIMIT): List<ConstraintCandidate> {
        if (limit <= 0) {
            return emptyList()
        }
        val preferred = candidates.filter { it.tier.isShortlistDefault }
        if (preferred.size >= limit) {
            return preferred.take(limit)
        }
        val fallback = candidates.filter { !it.tier.isShortlistDefault }
        return preferred + fallback.take(limit - preferred.size)
    }

    /** 分层查找。 / Look up one candidate by tier. */
    fun withTier(tier: CandidateTier): List<ConstraintCandidate> {
        return candidates.filter { it.tier == tier }
    }

    /** 校验报告不变量。 / Validate report invariants. */
    fun validate(): Try {
        if (schemaVersion.isBlank() || !dualThreshold.isFinite() || dualThreshold < 0.0) {
            return Failed(ErrorCode.IllegalArgument, "候选漏斗报告无效 / Candidate funnel ranking is invalid")
        }
        val seen = HashSet<ConstraintId>()
        candidates.forEachIndexed { position, candidate ->
            if (candidate.constraintId.value.isBlank() || candidate.priority != position) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "候选优先级必须连续且与稳定身份一致 / Candidate funnel priorities must be dense and match stable identities"
                )
            }
            if (!seen.add(candidate.constraintId)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "候选漏斗包含重复约束 / Candidate funnel contains duplicate constraints"
                )
            }
            when (candidate.tier) {
                CandidateTier.TierA -> {
                    val dual = candidate.dualValue ?: return Failed(
                        ErrorCode.IllegalArgument,
                        "Tier A 候选必须有对偶值 / Tier A candidate requires a dual value"
                    )
                    if (dual.abs() <= Flt64(dualThreshold)) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "Tier A 候选的对偶值必须超过阈值 / Tier A candidate requires a dual above the threshold"
                        )
                    }
                }

                CandidateTier.TierB -> {
                    val dual = candidate.dualValue ?: return Failed(
                        ErrorCode.IllegalArgument,
                        "Tier B 候选必须有对偶值 / Tier B candidate requires a dual value"
                    )
                    if (dual.abs() > Flt64(dualThreshold)) {
                        return Failed(
                            ErrorCode.IllegalArgument,
                            "Tier B 候选的对偶值不得超过阈值 / Tier B candidate requires a dual at or below the threshold"
                        )
                    }
                }

                CandidateTier.TierC -> if (candidate.activity == ActivityStatus.Active ||
                    candidate.activity == ActivityStatus.NearlyActive
                ) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "Tier C 候选不得为 active / Tier C candidate must not be active"
                    )
                }

                CandidateTier.Unclassified -> {}
            }
        }
        val counted = mapOf(
            CandidateTier.TierA to tierA,
            CandidateTier.TierB to tierB,
            CandidateTier.TierC to tierC,
            CandidateTier.Unclassified to unclassified
        )
        for ((tier, expected) in counted) {
            if (withTier(tier).size != expected) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "候选漏斗层级计数与候选列表不一致 / Candidate funnel tier counts do not match the candidate list"
                )
            }
        }
        return ok
    }
}

/**
 * 分层一条候选。
 *
 * 只有同时具备活动性与对偶证据时才能给出分层；否则返回 [CandidateTier.Unclassified]，
 * 以免把"未知"当成"不重要"。
 *
 * Classify one candidate. A tier requires both activity and dual evidence; otherwise the
 * candidate is [CandidateTier.Unclassified], so "unknown" can never be read as "unimportant".
 */
fun classifyCandidate(
    activity: ActivityStatus,
    dualValue: Flt64?,
    dualThreshold: Double = CandidateFunnelConfig.DEFAULT_DUAL_THRESHOLD
): CandidateTier {
    if (activity == ActivityStatus.Active || activity == ActivityStatus.NearlyActive) {
        return when {
            dualValue == null -> CandidateTier.Unclassified
            dualValue.abs() > Flt64(dualThreshold) -> CandidateTier.TierA
            else -> CandidateTier.TierB
        }
    }
    if (activity == ActivityStatus.Inactive ||
        activity == ActivityStatus.SatisfiedWithoutSlackMetric
    ) {
        return CandidateTier.TierC
    }
    return CandidateTier.Unclassified
}

/**
 * 构建候选漏斗。
 *
 * `sensitivity` 为 `null` 时，所有候选都只能停在 [CandidateTier.Unclassified]：没有对偶证据
 * 就不允许声称分层。Tier C 依然按活动性给出，并保留在结果中。
 *
 * Build the candidate funnel. When `sensitivity` is `null` every candidate stays
 * [CandidateTier.Unclassified]: without dual evidence no tier may be claimed. Tier C is still
 * derived from activity and retained in the result.
 */
fun buildCandidateFunnel(
    activity: ConstraintActivityReport,
    sensitivity: FixedIntegerLpSensitivityReport?,
    config: CandidateFunnelConfig = CandidateFunnelConfig()
): Ret<CandidateFunnelRanking> {
    when (val validation = config.validation) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    val duals: Map<ConstraintId, Flt64?> = sensitivity
        ?.sensitivities
        ?.associate { it.constraintId to it.dualValue }
        ?: emptyMap()

    val candidates = activity.constraintActivities.map { record ->
        val dual = duals[record.constraintId]
        ConstraintCandidate(
            constraintId = record.constraintId!!,
            group = record.group,
            tier = classifyCandidate(record.status, dual, config.dualThreshold),
            activity = record.status,
            slack = record.slack,
            dualValue = dual,
            localEffective = dual?.let { it.abs() > Flt64(config.dualThreshold) },
            priority = 0
        )
    }.sortedWith(
        // 优先级：先按层级，再按 |dual| 降序，最后按稳定身份保证确定性。
        // Priority: tier order, then descending |dual|, then stable identity for determinism.
        compareBy<ConstraintCandidate> { it.tier.ordinal }
            .thenByDescending { it.dualValue?.abs().sortKey() }
            .thenBy { it.constraintId.value }
    ).mapIndexed { position, candidate -> candidate.copy(priority = position) }

    val ranking = CandidateFunnelRanking(
        dualThreshold = config.dualThreshold,
        candidates = candidates,
        tierA = candidates.count { it.tier == CandidateTier.TierA },
        tierB = candidates.count { it.tier == CandidateTier.TierB },
        tierC = candidates.count { it.tier == CandidateTier.TierC },
        unclassified = candidates.count { it.tier == CandidateTier.Unclassified }
    )
    return when (val validation = ranking.validate()) {
        is Ok -> ok(ranking)
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 一条约束的有效性证据。
 *
 * 各层结论并列保存，绝不互相覆盖，也不合成单一的"是否临界"判决。
 *
 * Effectiveness evidence for one constraint. Each layer's conclusion is stored side by side;
 * they never overwrite one another and are never collapsed into a single "is critical" verdict.
 */
data class EffectivenessEntry(
    /** 原始约束稳定身份。 / Stable original-constraint identity. */
    val constraintId: ConstraintId,
    /** 约束组。 / Constraint group. */
    val group: String?,
    /** 漏斗分层。 / Funnel tier. */
    val tier: CandidateTier,
    /** 活动性状态。 / Activity status. */
    val activity: ActivityStatus,
    /** 固定整数结构下的局部对偶值。 / Local dual under the fixed integer pattern. */
    val dualValue: Flt64?,
    /** 局部有效性。 / Local effectiveness. */
    val localEffective: Boolean?,
    /** 单删是否改善目标。 / Whether single removal improved the objective. */
    val removalEffective: Boolean?,
    /** 首次观察到改善的 delta。 / First delta that produced an improvement. */
    val firstEffectiveDelta: Flt64?,
    /** 已证明的最大目标改善。 / Largest proven objective improvement. */
    val maxImprovement: Flt64?,
    /** 每单位 delta 的边际效应。 / Marginal effect per unit of delta. */
    val marginalEffect: Flt64?,
    /** 该约束的分析状态。 / Analysis status for this constraint. */
    val status: AnalysisStatus,
    /** 未形成结论的原因。 / Why no conclusion was formed. */
    val unavailableReason: String? = null
) {
    /** 是否观察到全局改善。 / Whether a global improvement was observed. */
    val isGloballyEffective: Boolean
        get() = maxImprovement != null && maxImprovement > Flt64.zero

    /** 原始证据来源。 / Original evidence source. */
    val source: DiagnosticSource
        get() = DiagnosticSource.Constraint(constraintId)
}

/** 有效性排序。 / Effectiveness ranking. */
data class EffectivenessRanking(
    /** baseline 目标值。 / Baseline objective value. */
    val baselineObjective: Flt64,
    /** 按已证明的最大改善降序排列。 / Entries ordered by proven maximum improvement. */
    val entries: List<EffectivenessEntry>,
    /** 报告 schema。 / Report schema. */
    val schemaVersion: String = CRITICAL_ANALYSIS_REPORT_SCHEMA_VERSION
) {
    /**
     * 观察到全局改善的约束。
     *
     * 这只是"已证明有收益"的子集；不在其中的约束**不**表示无关，因为单独移除无收益也可能是
     * 组合瓶颈的一部分（计划 8.7）。
     *
     * Constraints with an observed global improvement. This is only the subset with proven
     * benefit; absence from it does **not** mean irrelevance, because a constraint whose single
     * removal shows no benefit can still be part of a combined bottleneck (plan 8.7).
     */
    val globallyEffective: List<EffectivenessEntry>
        get() = entries.filter { it.isGloballyEffective }

    /** 局部有效的约束。 / Constraints that are locally effective. */
    val locallyEffective: List<EffectivenessEntry>
        get() = entries.filter { it.localEffective == true }

    /** 查找一条约束。 / Look up one constraint. */
    fun entry(id: ConstraintId): EffectivenessEntry? {
        return entries.firstOrNull { it.constraintId == id }
    }

    /** 校验报告不变量。 / Validate report invariants. */
    fun validate(): Try {
        if (schemaVersion.isBlank() || !baselineObjective.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "有效性排序报告无效 / Effectiveness ranking is invalid")
        }
        val seen = HashSet<ConstraintId>()
        for (entry in entries) {
            if (entry.constraintId.value.isBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "有效性条目必须带稳定约束身份 / Effectiveness entry requires a stable constraint identity"
                )
            }
            if (!seen.add(entry.constraintId)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "有效性排序包含重复约束 / Effectiveness ranking contains duplicate constraints"
                )
            }
            val finite = listOf(entry.dualValue, entry.maxImprovement, entry.marginalEffect)
                .all { it == null || it.isFinite() }
            val deltaOk = entry.firstEffectiveDelta == null ||
                (entry.firstEffectiveDelta.isFinite() && entry.firstEffectiveDelta > Flt64.zero)
            if (!finite || !deltaOk) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "有效性条目包含非有限或非正数值 / Effectiveness entry contains a non-finite or non-positive value"
                )
            }
            // 局部判决只在对偶值确实可用时才允许存在。
            // Local decisions are allowed only when a dual value is actually available.
            if (entry.localEffective != null && entry.dualValue == null) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "局部有效性需要可用的对偶值 / Local effectiveness requires an available dual value"
                )
            }
        }
        return ok
    }
}

/**
 * 从扰动报告构建有效性排序。
 *
 * 只有已证明的观测才能进入排序：`Unknown` 求解绝不转化为改善，未证明的观测不计入。
 *
 * Build an effectiveness ranking from perturbation reports. Only proven observations may feed
 * the ranking: an `Unknown` solve never becomes an improvement, and a non-proven observation is
 * never counted.
 */
fun buildEffectivenessRanking(
    baselineObjective: Flt64,
    funnel: CandidateFunnelRanking?,
    perturbationReports: List<ConstraintPerturbationReport>
): Ret<EffectivenessRanking> {
    if (!baselineObjective.isFinite()) {
        return Failed(ErrorCode.IllegalArgument, "基线目标值必须有限 / Baseline objective must be finite")
    }
    val funnelById = funnel?.candidates?.associateBy { it.constraintId } ?: emptyMap()

    val entries = perturbationReports.map { report ->
        val candidate = funnelById[report.constraintId]
        val proven = (report.observations + listOfNotNull(report.removal))
            .filter { it.outcome != PerturbationOutcome.Unknown }
        val improvements = proven.mapNotNull { it.objectiveImprovement }
            .filter { it.isFinite() }
        val maxImprovement = improvements.maxByOrNull { it.sortKey() }
        val firstEffectiveDelta = proven
            .filter { it.outcome == PerturbationOutcome.Effective }
            .mapNotNull { it.delta }
            .minByOrNull { it.sortKey() }
        val marginalEffect = if (maxImprovement != null && firstEffectiveDelta != null &&
            firstEffectiveDelta > Flt64.zero
        ) {
            maxImprovement / firstEffectiveDelta
        } else {
            null
        }
        EffectivenessEntry(
            constraintId = report.constraintId,
            group = candidate?.group,
            tier = candidate?.tier ?: CandidateTier.Unclassified,
            activity = candidate?.activity ?: ActivityStatus.Unknown,
            dualValue = candidate?.dualValue,
            localEffective = candidate?.localEffective,
            removalEffective = report.removal?.removalEffective,
            firstEffectiveDelta = firstEffectiveDelta,
            maxImprovement = maxImprovement,
            marginalEffect = marginalEffect,
            status = report.status,
            unavailableReason = report.message
        )
    }.sortedWith(
        // 排序：已证明改善降序，其次 |dual| 降序，最后稳定身份。
        compareByDescending<EffectivenessEntry> { it.maxImprovement.sortKey() }
            .thenByDescending { it.dualValue?.abs().sortKey() }
            .thenBy { it.constraintId.value }
    )

    val ranking = EffectivenessRanking(baselineObjective = baselineObjective, entries = entries)
    return when (val validation = ranking.validate()) {
        is Ok -> ok(ranking)
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 业务级约束组汇总。
 *
 * 对应计划事项 M：默认展示组级统计，并支持下钻到实例。
 *
 * Business-level constraint-group summary. Corresponds to plan item M: group-level statistics by
 * default, with drill-down to instances.
 */
data class AnalysisGroupSummary(
    /** 组名称；`null` 表示未分组。 / Group name; `null` means ungrouped. */
    val group: String?,
    /** 该组涉及的约束实例数。 / Number of involved constraint instances. */
    val involvedInstances: Int,
    /** 其中处于 active 的实例数。 / Instances that are active. */
    val activeInstances: Int,
    /** 其中进入当前 MUS 的实例数。 / Instances in the current MUS. */
    val blockingInstances: Int,
    /** 组内观察到的已证明最大改善。 / Largest proven improvement observed in this group. */
    val maxImprovement: Flt64?
) {
    /** 该组是否参与当前阻塞。 / Whether this group participates in the current blocking set. */
    val isBlocking: Boolean
        get() = blockingInstances > 0
}

/** 跨阶段汇总约束组。 / Aggregate constraint groups across stages. */
fun summarizeGroups(
    activity: ConstraintActivityReport?,
    effectiveness: EffectivenessRanking?,
    conflict: ConflictExplanation?
): List<AnalysisGroupSummary> {
    val involved = LinkedHashMap<String?, Int>()
    val active = LinkedHashMap<String?, Int>()
    val blocking = LinkedHashMap<String?, Int>()
    val improvement = LinkedHashMap<String?, Flt64>()
    val groupOf = LinkedHashMap<ConstraintId, String?>()

    activity?.constraintActivities?.forEach { record ->
        val group = record.group
        val id = record.constraintId ?: return@forEach
        groupOf[id] = group
        involved[group] = (involved[group] ?: 0) + 1
        if (record.status == ActivityStatus.Active) {
            active[group] = (active[group] ?: 0) + 1
        }
    }
    effectiveness?.entries?.forEach { record ->
        val group = groupOf[record.constraintId] ?: record.group
        val value = record.maxImprovement ?: return@forEach
        val current = improvement[group]
        improvement[group] = if (current == null || value > current) value else current
    }
    // 阻塞约束的组归属取自活动性报告，因为冲突报告刻意只保存稳定身份。
    // Group membership of a blocking constraint comes from the activity report, because the
    // conflict report deliberately stores only stable identities.
    conflict?.constraintIds?.forEach { id ->
        val group = groupOf[id]
        blocking[group] = (blocking[group] ?: 0) + 1
    }

    val keys = LinkedHashSet<String?>()
    keys += involved.keys
    keys += improvement.keys
    keys += blocking.keys
    return keys.map { group ->
        AnalysisGroupSummary(
            group = group,
            involvedInstances = involved[group] ?: 0,
            activeInstances = active[group] ?: 0,
            blockingInstances = blocking[group] ?: 0,
            maxImprovement = improvement[group]
        )
    }.sortedWith(
        compareByDescending<AnalysisGroupSummary> { it.blockingInstances }
            .thenByDescending { it.activeInstances }
            .thenBy { it.group ?: "" }
    )
}

/** 基线摘要。 / Baseline summary. */
data class AnalysisBaselineSummary(
    /** 目标身份。 / Objective identity. */
    val objectiveId: String,
    /** 优化方向。 / Optimization direction. */
    val sense: String,
    /** 基线目标值。 / Baseline objective value. */
    val objectiveValue: Flt64?,
    /** 求解结论。 / Solve conclusion. */
    val status: AnalysisStatus,
    /** 求解是否已证明最优。 / Whether the solve proved optimality. */
    val provenOptimal: Boolean
)

/** 统一报告构建器。 / Unified report builder. */
class CriticalConstraintAnalysisReportBuilder {
    private var baseline: AnalysisBaselineSummary? = null
    private var activity: ConstraintActivityReport? = null
    private var localSensitivity: FixedIntegerLpSensitivityReport? = null
    private var perturbation: List<ConstraintPerturbationReport> = emptyList()
    private var target: TargetFeasibilityReport? = null
    private var conflict: ConflictExplanation? = null
    private val unavailableReasons = ArrayList<String>()
    private var hasUnsupportedStage = false

    /** 设置基线。 / Set the baseline. */
    fun baseline(value: AnalysisBaselineSummary): CriticalConstraintAnalysisReportBuilder = apply {
        baseline = value
    }

    /** 设置活动性报告。 / Set the activity report. */
    fun activity(value: ConstraintActivityReport): CriticalConstraintAnalysisReportBuilder = apply {
        activity = value
    }

    /** 设置局部有效性报告。 / Set the local sensitivity report. */
    fun localSensitivity(value: FixedIntegerLpSensitivityReport): CriticalConstraintAnalysisReportBuilder = apply {
        localSensitivity = value
    }

    /** 追加扰动报告。 / Append perturbation reports. */
    fun perturbation(value: List<ConstraintPerturbationReport>): CriticalConstraintAnalysisReportBuilder = apply {
        perturbation = value
    }

    /** 设置目标可行性报告。 / Set the target-feasibility report. */
    fun target(value: TargetFeasibilityReport): CriticalConstraintAnalysisReportBuilder = apply {
        target = value
    }

    /** 设置冲突解释。 / Set the conflict explanation. */
    fun conflict(value: ConflictExplanation): CriticalConstraintAnalysisReportBuilder = apply {
        conflict = value
    }

    /** 记录一个阶段未完成的原因。 / Record why a stage did not complete. */
    fun noteUnavailable(reason: String): CriticalConstraintAnalysisReportBuilder = apply {
        unavailableReasons += reason
        hasUnsupportedStage = true
    }

    /** 构建报告。 / Build the report. */
    fun build(
        config: CandidateFunnelConfig = CandidateFunnelConfig()
    ): Ret<CriticalConstraintAnalysisReport> {
        val baseline = this.baseline
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "临界约束分析需要基线摘要 / Critical constraint analysis requires a baseline summary"
            )
        val activity = this.activity
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "临界约束分析需要活动性报告 / Critical constraint analysis requires an activity report"
            )

        val funnel = when (val result = buildCandidateFunnel(activity, localSensitivity, config)) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val effectiveness = if (perturbation.isEmpty()) {
            null
        } else {
            val baselineObjective = baseline.objectiveValue
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "有效性排序需要已证明的基线目标值 / Effectiveness ranking requires a proven baseline objective value"
                )
            when (val result = buildEffectivenessRanking(baselineObjective, funnel, perturbation)) {
                is Ok -> result.value!!
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val conflict = this.conflict
        val groupSummaries = summarizeGroups(activity, effectiveness, conflict)

        // 总体结论取最弱环节：任一阶段为 Unknown 或 Unsupported 即主导。
        // The overall status is the weakest link: any Unknown or Unsupported stage dominates.
        var status = if (baseline.provenOptimal) AnalysisStatus.Reachable else AnalysisStatus.Unknown
        if (hasUnsupportedStage) {
            status = combineStatus(status, AnalysisStatus.Unsupported)
        }
        status = combineStatus(status, target?.status)
        status = combineStatus(status, conflict?.status)
        perturbation.forEach { status = combineStatus(status, it.status) }
        localSensitivity?.let { status = combineStatus(status, it.status) }

        val reasons = ArrayList(unavailableReasons)
        if (funnel.unclassified > 0) {
            reasons += "有 ${funnel.unclassified} 条候选因缺少对偶证据而无法分层 / " +
                "${funnel.unclassified} candidate(s) could not be tiered because dual evidence was unavailable"
        }

        val report = CriticalConstraintAnalysisReport(
            baseline = baseline,
            activity = activity,
            localSensitivity = localSensitivity,
            funnel = funnel,
            effectiveness = effectiveness,
            target = target,
            conflict = conflict,
            groupSummaries = groupSummaries,
            status = status,
            unavailableReasons = reasons
        )
        return when (val validation = report.validate()) {
            is Ok -> ok(report)
            is Failed -> Failed(validation.error)
            is Fatal -> Fatal(validation.errors)
        }
    }
}

/**
 * Phase 6 统一报告。
 *
 * 该报告把 baseline → activity → fixed-integer LP → perturbation → target → conflict → MUS
 * 各阶段结果聚合到一个公共结构中。所有字段只承载原始模型证据；任何阶段缺失都显式表示为
 * `null` 或 `Unsupported`，绝不推断为"无效"。
 *
 * Phase 6 unified report. It aggregates baseline → activity → fixed-integer LP → perturbation →
 * target → conflict → MUS into one public structure. Every field carries original-model evidence
 * only; a missing stage is expressed as `null` or `Unsupported` and is never inferred to mean
 * "ineffective".
 */
data class CriticalConstraintAnalysisReport(
    /** 基线。 / Baseline. */
    val baseline: AnalysisBaselineSummary,
    /** 活动性。 / Activity. */
    val activity: ConstraintActivityReport,
    /** 固定整数 LP 局部有效性。 / Fixed-integer LP local sensitivity. */
    val localSensitivity: FixedIntegerLpSensitivityReport?,
    /** 候选漏斗。 / Candidate funnel. */
    val funnel: CandidateFunnelRanking,
    /** 有效性排序。 / Effectiveness ranking. */
    val effectiveness: EffectivenessRanking?,
    /** 目标可行性。 / Target feasibility. */
    val target: TargetFeasibilityReport?,
    /** 冲突解释。 / Conflict explanation. */
    val conflict: ConflictExplanation?,
    /** 按约束组聚合的业务级汇总。 / Business-level group aggregation. */
    val groupSummaries: List<AnalysisGroupSummary>,
    /** 整条链路的总体结论。 / Overall conclusion for the whole chain. */
    val status: AnalysisStatus,
    /** 未完成阶段的原因。 / Reasons stages did not complete. */
    val unavailableReasons: List<String>,
    /** 报告 schema。 / Report schema. */
    val schemaVersion: String = CRITICAL_ANALYSIS_REPORT_SCHEMA_VERSION
) {
    /** 是否存在已证明的最小阻塞集合。 / Whether a proven minimal blocking set exists. */
    val hasMinimalBlockingSet: Boolean
        get() = conflict?.minimalityVerified == true

    /**
     * 业务可读的阻塞摘要。
     *
     * 当阻塞来自固定背景时明确说明，避免把空约束集合读成"没有阻塞约束"。
     *
     * Business-readable blocking summary. When blocking comes from the fixed background this is
     * stated explicitly, so an empty constraint set is never read as "no blocking constraints".
     */
    fun blockingSummary(): String {
        val explanation = conflict
            ?: return "阻塞分析未执行 / Blocking analysis was not run"
        if (explanation.backgroundBlocking) {
            return "目标在没有原始约束后仍不可达，阻塞来自固定背景 / " +
                "The target remains unreachable without an original constraint; blocking comes " +
                "from fixed background"
        }
        val detail = explanation.groupSummaries.joinToString(", ") {
            "${it.group ?: "<ungrouped>"}: ${it.count}"
        }
        val minimality = if (explanation.minimalityVerified) "已验证最小" else "未证明最小"
        return "最小阻塞集合（$minimality）：${explanation.members.size} 个原始证据 [$detail] / " +
            "Minimal blocking set ($minimality): ${explanation.members.size} original evidence items [$detail]"
    }

    /**
     * 校验报告不变量。
     *
     * 这里强制公开证据边界：报告不得出现 solver 生成的辅助元素痕迹。
     *
     * Validate report invariants. The public evidence boundary is enforced here: the report must
     * not carry any trace of a solver-generated auxiliary element.
     */
    fun validate(): Try {
        if (schemaVersion.isBlank()) {
            return Failed(ErrorCode.IllegalArgument, "报告 schema 版本不得为空 / Report schema version must not be blank")
        }
        if (baseline.objectiveId.isBlank() || baseline.sense.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "基线需要目标身份与优化方向 / Baseline requires an objective identity and sense"
            )
        }
        if (baseline.provenOptimal && baseline.objectiveValue == null) {
            return Failed(
                ErrorCode.IllegalArgument,
                "已证明的基线必须带目标值 / A proven baseline requires an objective value"
            )
        }
        when (val validation = funnel.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        effectiveness?.let {
            when (val validation = it.validate()) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
        }
        conflict?.let { explanation ->
            if (explanation.backgroundBlocking &&
                explanation.members.any { it is DiagnosticSource.Constraint }
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "背景阻塞报告不得包含原始约束成员 / A background-blocking report must not contain original constraint members"
                )
            }
            if (explanation.backgroundBlocking &&
                explanation.minimality == ConflictMinimality.Irreducible
            ) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "背景阻塞不得宣称约束级最小性 / Background blocking must not claim constraint-level minimality"
                )
            }
        }
        return ok
    }
}

/**
 * 合并两个分析状态，取更弱的一方。
 *
 * 顺序由强到弱：`Reachable`、`Unreachable`、`Unknown`、`Unsupported`。这与计划 8.14 一致：
 * 任何未证明的阶段都不得被更强的结论掩盖。
 *
 * Combine two analysis statuses, keeping the weaker one. Order from strongest to weakest:
 * `Reachable`, `Unreachable`, `Unknown`, `Unsupported`. This matches plan 8.14: no unproven stage
 * may be masked by a stronger conclusion.
 */
fun combineStatus(current: AnalysisStatus, next: AnalysisStatus?): AnalysisStatus {
    if (next == null) {
        return current
    }
    fun rank(status: AnalysisStatus): Int = when (status) {
        AnalysisStatus.Reachable -> 3
        AnalysisStatus.Unreachable -> 2
        AnalysisStatus.Unknown -> 1
        AnalysisStatus.Unsupported -> 0
    }
    return if (rank(next) < rank(current)) next else current
}
