package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed

/** 限制修正建议的策略。 / Policy limiting correction recommendations.
 *
 * @property maxCandidates 最多保留的候选数 / Maximum number of candidates to retain
 * @property maxPlans 最多保留的方案数 / Maximum number of plans to retain
 * @property requirePositiveWeight 是否要求权重为正 / Whether weights must be positive
 */
data class RelaxabilityPolicy(
    val maxCandidates: Int = 32,
    val maxPlans: Int = 8,
    val requirePositiveWeight: Boolean = true
) {
    init {
        require(maxCandidates > 0 && maxPlans > 0) { "relaxability limits must be positive" }
    }

    /** Validate policy values at a Result boundary. / 在 Result 边界校验策略值。 */
    fun validate(): Try {
        return if (maxCandidates > 0 && maxPlans > 0) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "松弛策略上限必须为正 / Relaxability limits must be positive"
            )
        }
    }
}

/** 可放宽的原始证据及其业务成本。 / Original evidence that may be relaxed and its business cost.
 *
 * @property source 原始证据来源 / Original evidence source
 * @property weight 业务权重 / Business weight
 * @property relaxation 建议的放宽量 / Suggested relaxation amount
 */
data class CorrectionCandidate(
    val source: DiagnosticSource,
    val weight: Double,
    val relaxation: Double
) {
    init {
        require(weight.isFinite() && relaxation.isFinite() && relaxation > 0.0)
        require((weight * relaxation).isFinite()) {
            "correction cost must be finite / correction cost must be finite"
        }
    }

    /** Validate this candidate under a recommendation policy. / 按推荐策略校验候选项。 */
    fun validate(policy: RelaxabilityPolicy = RelaxabilityPolicy()): Try {
        when (val validation = policy.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when {
            !weight.isFinite() || !relaxation.isFinite() || relaxation <= 0.0 ||
                !(weight * relaxation).isFinite() -> Failed(
                ErrorCode.IllegalArgument,
                "correction weight/relaxation 必须有限且松弛量为正 / correction weight/relaxation must be finite with positive relaxation"
            )
            policy.requirePositiveWeight && weight <= 0.0 -> Failed(
                ErrorCode.IllegalArgument,
                "correction weight 必须为正 / correction weight must be positive"
            )
            else -> ok
        }
    }
}

/** 一个证据来源的业务成本与建议放宽量。 / Business cost and suggested relaxation amount for one evidence source.
 *
 * @property weight 业务权重 / Business weight
 * @property relaxation 建议的放宽量 / Suggested relaxation amount
 */
data class RelaxationCost(
    val weight: Double = 1.0,
    val relaxation: Double = 1.0
) {
    init {
        require(weight.isFinite() && relaxation.isFinite() && relaxation > 0.0)
        require((weight * relaxation).isFinite()) {
            "correction cost must be finite / correction cost must be finite"
        }
    }

    /** Validate this cost under a recommendation policy. / 按推荐策略校验成本。 */
    fun validate(policy: RelaxabilityPolicy = RelaxabilityPolicy()): Try {
        when (val validation = policy.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when {
            !weight.isFinite() || !relaxation.isFinite() || relaxation <= 0.0 ||
                !(weight * relaxation).isFinite() -> Failed(
                ErrorCode.IllegalArgument,
                "correction weight/relaxation 必须有限且松弛量为正 / correction weight/relaxation must be finite with positive relaxation"
            )
            policy.requirePositiveWeight && weight <= 0.0 -> Failed(
                ErrorCode.IllegalArgument,
                "correction weight 必须为正 / correction weight must be positive"
            )
            else -> ok
        }
    }
}

/** 已校验的加权修正集合。 / A validated weighted correction set.
 *
 * @property members 修正候选成员 / Correction candidates
 * @property totalCost 成员总成本 / Total member cost
 * @property minimal 是否已验证删除最小性 / Whether deletion minimality was verified
 */
data class CorrectionSet(
    val members: List<CorrectionCandidate>,
    val totalCost: Double,
    val minimal: Boolean = false
) {
    init {
        require(members.isNotEmpty())
        require(members.map { it.source.stableId }.toSet().size == members.size)
        require(totalCost.isFinite())
        require(kotlin.math.abs(totalCost - members.sumOf { it.weight * it.relaxation }) < 1e-9)
    }

    /** Whether deletion minimality was verified for this candidate set. / 是否验证了删除最小性。 */
    fun deletionMinimalityVerified(): Boolean = minimal

    /** This protocol never proves a complete model-wide MCS. / 本协议不会证明完整模型范围 MCS。 */
    fun isCompleteMcs(): Boolean = false

    /** Validate this set under a recommendation policy. / 按推荐策略校验 correction set。 */
    fun validate(policy: RelaxabilityPolicy = RelaxabilityPolicy()): Try {
        when (val validation = policy.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (members.isEmpty() || members.size > policy.maxCandidates) {
            return Failed(
                ErrorCode.IllegalArgument,
                "correction set 大小超出策略范围 / correction set size is outside policy"
            )
        }
        val seen = HashSet<String>()
        var expected = 0.0
        for (member in members) {
            when (val validation = member.validate(policy)) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            if (!seen.add(member.source.stableId)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "correction set 不得包含重复证据 / correction set contains duplicate evidence"
                )
            }
            expected += member.weight * member.relaxation
        }
        return if (expected.isFinite() && kotlin.math.abs(totalCost - expected) <= 1e-9) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "correction set 总成本不一致 / correction set total cost is inconsistent"
            )
        }
    }

    /** 数值放宽只是建议，始终需要调用方重新校验。 / Numeric relaxation is a recommendation and always needs caller-side revalidation. */
    val requiresRevalidation: Boolean
        get() = true

    /** 为使用谓词调用形式的调用方保留的方法形式。 / Method form for callers that model this flag as an explicit capability check.
     *
     * @return 是否需要重新校验 / Whether revalidation is required
     */
    fun requiresRevalidation(): Boolean = requiresRevalidation
}

/** 一个达到目标条件的备选方案。 / One alternative plan for reaching an objective target.
 *
 * @property target 目标条件 / Objective target
 * @property correctionSet 修正集合 / Correction set
 * @property rank 方案排序名次 / Plan rank
 */
data class AlternativeImprovementPlan(
    val target: ObjectiveTarget,
    val correctionSet: CorrectionSet,
    val rank: Int
) {
    init { require(rank >= 0) }

    /** Whether this plan is a complete model-wide MCS. / 是否为完整模型范围 MCS。 */
    fun isCompleteMcs(): Boolean = correctionSet.isCompleteMcs()

    /** Validate this plan under a recommendation policy. / 按推荐策略校验改进方案。 */
    fun validate(policy: RelaxabilityPolicy = RelaxabilityPolicy()): Try {
        when (val validation = policy.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        when (val targetValidation = target.validate()) {
            is Ok -> {}
            is Failed -> return Failed(targetValidation.error)
            is Fatal -> return Fatal(targetValidation.errors)
        }
        when (val setValidation = correctionSet.validate(policy)) {
            is Ok -> {}
            is Failed -> return Failed(setValidation.error)
            is Fatal -> return Fatal(setValidation.errors)
        }
        return if (rank < policy.maxPlans) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "改进方案名次超出策略范围 / improvement plan rank exceeds policy"
            )
        }
    }

    /** 应用数值候选项不能证明目标已经可达。 / Applying a numeric candidate never proves that this target is now reachable. */
    val requiresRevalidation: Boolean
        get() = correctionSet.requiresRevalidation

    /** 与属性并存，供使用谓词语法的 API 调用方使用。 / Method form kept alongside the property for API callers using predicate syntax.
     *
     * @return 是否需要重新校验 / Whether revalidation is required
     */
    fun requiresRevalidation(): Boolean = requiresRevalidation
}

/** 构造确定性的加权修正集合，只保留策略允许的候选项。 / Build a deterministic weighted correction set, retaining only policy-approved candidates.
 *
 * @param candidates 候选项 / Candidate corrections
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 加权修正集合 / Weighted correction set
 */
fun weightedCorrectionSet(
    candidates: List<CorrectionCandidate>,
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): CorrectionSet {
    require(candidates.all { !policy.requirePositiveWeight || it.weight > 0.0 }) {
        "correction weight must be positive"
    }
    val selected = candidates
        .sortedWith(compareBy<CorrectionCandidate> { it.weight * it.relaxation }.thenBy { it.source.stableId })
        .take(policy.maxCandidates)
    return CorrectionSet(selected, selected.sumOf { it.weight * it.relaxation })
}

/** 面向不接受异常调用方的 Result 形式加权构造器。 / Result-form weighted correction builder for callers that cannot accept exceptions.
 *
 * @param candidates 候选项 / Candidate corrections
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 加权修正集合结果 / Result containing the weighted correction set
 */
fun tryWeightedCorrectionSet(
    candidates: List<CorrectionCandidate>,
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): Ret<CorrectionSet> {
    return try {
        ok(weightedCorrectionSet(candidates, policy))
    } catch (error: IllegalArgumentException) {
        Failed(
            ErrorCode.IllegalArgument,
            "构造加权 correction set 失败：${error.message} / Failed to build weighted correction set: ${error.message}"
        )
    }
}

/** 构造调用方收缩器提供的、已明确校验的最小修正集合。 / Construct an explicitly validated minimal correction set supplied by the caller's shrinker.
 *
 * @param candidates 候选项 / Candidate corrections
 * @param verified 是否已完成删除验证 / Whether deletion verification completed
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 最小修正集合 / Minimal correction set
 */
fun minimalCorrectionSet(
    candidates: List<CorrectionCandidate>,
    verified: Boolean,
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): CorrectionSet {
    require(verified) { "minimal correction set requires deletion verification" }
    val weighted = weightedCorrectionSet(candidates, policy)
    // Once the policy truncates the input, deletion verification no longer covers the returned
    // set's complete candidate universe. Keep the weighted recommendation, but do not claim it is
    // a verified minimal set.
    // 输入一旦被策略截断，删除验证就不再覆盖返回集合的完整候选全集。
    // 保留加权建议，但不得声称它是已验证的最小集合。
    return weighted.copy(minimal = candidates.size <= policy.maxCandidates)
}

/** 面向不接受异常调用方的 Result 形式最小修正构造器。 / Result-form minimal correction builder for callers that cannot accept exceptions.
 *
 * @param candidates 候选项 / Candidate corrections
 * @param verified 是否已完成删除验证 / Whether deletion verification completed
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 最小修正集合结果 / Result containing the minimal correction set
 */
fun tryMinimalCorrectionSet(
    candidates: List<CorrectionCandidate>,
    verified: Boolean,
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): Ret<CorrectionSet> {
    return try {
        ok(minimalCorrectionSet(candidates, verified, policy))
    } catch (error: IllegalArgumentException) {
        Failed(
            ErrorCode.IllegalArgument,
            "构造最小 correction set 失败：${error.message} / Failed to build minimal correction set: ${error.message}"
        )
    }
}

/** 可安全投影到 criticality 或 correction recommendation 的冲突。 / A conflict that is safe to project into criticality or correction recommendations.
 *
 * @return 冲突是否满足安全投影条件 / Whether the conflict is safe to project
 */
fun ConflictExplanation.isVerifiedMinimalConflict(): Boolean =
    status == AnalysisStatus.Unreachable &&
        validity == ConflictValidity.Verified &&
        minimalityVerified &&
        !backgroundBlocking &&
        targetFeasibility.target == target &&
        targetFeasibility.status == AnalysisStatus.Unreachable &&
        members.isNotEmpty() &&
        members.map { it.stableId }.toSet().size == members.size &&
        members.all { source ->
            source.asInfeasibilityMember() != null &&
                source in availableEvidence &&
                source !is DiagnosticSource.ObjectiveTarget
        }

/**
 * 不可约阻塞集合为每个成员生成一个单例修正集合：删除任一成员都会使固定目标可达；部分或未校验的冲突不声明 MCS。
 * / An irreducible blocking set yields one singleton correction set per member: deleting any one member makes
 * that fixed target reachable. Partial and unverified conflicts yield no MCS claim.
 *
 * @param conflict 待投影的冲突解释 / Conflict explanation to project
 * @return 单例修正集合 / Singleton correction sets
 */
fun correctionSetsFromConflict(conflict: ConflictExplanation): List<CorrectionSet> {
    if (!conflict.isVerifiedMinimalConflict()) return emptyList()
    return conflict.members.map { source ->
        CorrectionSet(listOf(CorrectionCandidate(source, 1.0, 1.0)), totalCost = 1.0, minimal = true)
    }
}

/** 从已校验冲突构造加权单例修正备选项。 / Build weighted singleton correction alternatives from a verified conflict.
 *
 * @param conflict 已校验的冲突解释 / Verified conflict explanation
 * @param costs 按稳定来源 ID 给出的成本 / Costs keyed by stable source ID
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 加权修正集合 / Weighted correction sets
 */
fun weightedCorrectionSetsFromConflict(
    conflict: ConflictExplanation,
    costs: Map<String, RelaxationCost> = emptyMap(),
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): List<CorrectionSet> {
    if (!conflict.isVerifiedMinimalConflict()) return emptyList()

    return conflict.members
        .map { source ->
            val cost = costs[source.stableId] ?: RelaxationCost()
            require(!policy.requirePositiveWeight || cost.weight > 0.0) {
                "correction weight must be positive"
            }
            val candidate = CorrectionCandidate(
                source = source,
                weight = cost.weight,
                relaxation = cost.relaxation
            )
            CorrectionSet(
                members = listOf(candidate),
                totalCost = candidate.weight * candidate.relaxation,
                minimal = true
            )
        }
        .sortedWith(
            compareBy<CorrectionSet> { it.totalCost }
                .thenBy { it.members.first().source.stableId }
        )
        .take(policy.maxPlans)
}

/** 从已校验冲突构造排序后的加权改进方案。 / Build ranked weighted alternative-improvement plans from a verified conflict.
 *
 * @param conflict 已校验的冲突解释 / Verified conflict explanation
 * @param costs 按稳定来源 ID 给出的成本 / Costs keyed by stable source ID
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 排序后的改进方案 / Ranked improvement plans
 */
fun alternativeImprovementPlansFromConflict(
    conflict: ConflictExplanation,
    costs: Map<String, RelaxationCost> = emptyMap(),
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): List<AlternativeImprovementPlan> {
    return weightedCorrectionSetsFromConflict(conflict, costs, policy)
        .mapIndexed { rank, correctionSet ->
            AlternativeImprovementPlan(
                target = conflict.target,
                correctionSet = correctionSet,
                rank = rank
            )
        }
}

/** 强调返回方案属于放宽建议的别名。 / Alias emphasizing that the returned plans are relaxation recommendations.
 *
 * @param conflict 已校验的冲突解释 / Verified conflict explanation
 * @param costs 按稳定来源 ID 给出的成本 / Costs keyed by stable source ID
 * @param policy 修正建议策略 / Correction recommendation policy
 * @return 放宽建议方案 / Relaxation recommendation plans
 */
fun relaxationRecommendationsFromConflict(
    conflict: ConflictExplanation,
    costs: Map<String, RelaxationCost> = emptyMap(),
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): List<AlternativeImprovementPlan> =
    alternativeImprovementPlansFromConflict(conflict, costs, policy)
