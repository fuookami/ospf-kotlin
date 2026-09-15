package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** Policy limiting correction recommendations. */
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

/** Original evidence that may be relaxed and its business cost. */
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

/** Business cost and suggested relaxation amount for one evidence source. */
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

/** A validated weighted correction set. */
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

    /** Numeric relaxation is a recommendation and always needs caller-side revalidation. */
    val requiresRevalidation: Boolean
        get() = true

    /** Method form for callers that model this flag as an explicit capability check. */
    fun requiresRevalidation(): Boolean = requiresRevalidation
}

/** One alternative plan for reaching an objective target. */
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

    /** Applying a numeric candidate never proves that this target is now reachable. */
    val requiresRevalidation: Boolean
        get() = correctionSet.requiresRevalidation

    /** Method form kept alongside the property for API callers using predicate syntax. */
    fun requiresRevalidation(): Boolean = requiresRevalidation
}

/** Build a deterministic weighted correction set, retaining only policy-approved candidates. */
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

/** Result-form weighted correction builder for callers that cannot accept exceptions. / 面向不接受异常调用方的 Result 形式加权构造器。 */
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

/** Construct an explicitly validated minimal correction set supplied by the caller's shrinker. */
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
    return weighted.copy(minimal = candidates.size <= policy.maxCandidates)
}

/** Result-form minimal correction builder for callers that cannot accept exceptions. / 面向不接受异常调用方的 Result 形式最小 correction 构造器。 */
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

/** A conflict that is safe to project into criticality or correction recommendations. / 可安全投影到 criticality 或 correction recommendation 的冲突。 */
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
 * An irreducible blocking set yields one singleton correction set per member: deleting any one
 * member makes that fixed target reachable. Partial and unverified conflicts yield no MCS claim.
 */
fun correctionSetsFromConflict(conflict: ConflictExplanation): List<CorrectionSet> {
    if (!conflict.isVerifiedMinimalConflict()) return emptyList()
    return conflict.members.map { source ->
        CorrectionSet(listOf(CorrectionCandidate(source, 1.0, 1.0)), totalCost = 1.0, minimal = true)
    }
}

/** Build weighted singleton correction alternatives from a verified conflict. */
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

/** Build ranked weighted alternative-improvement plans from a verified conflict. */
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

/** Alias emphasizing that the returned plans are relaxation recommendations. */
fun relaxationRecommendationsFromConflict(
    conflict: ConflictExplanation,
    costs: Map<String, RelaxationCost> = emptyMap(),
    policy: RelaxabilityPolicy = RelaxabilityPolicy()
): List<AlternativeImprovementPlan> =
    alternativeImprovementPlansFromConflict(conflict, costs, policy)
