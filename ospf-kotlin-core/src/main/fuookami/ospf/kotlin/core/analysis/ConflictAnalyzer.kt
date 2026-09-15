/** Target conflict and deletion-based MUS analysis. / 目标冲突与基于删除的 MUS 分析。 */
package fuookami.ospf.kotlin.core.analysis

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingConflict
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.report.VariableDomainRef
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** Minimality conclusion for a conflict explanation. / 冲突解释的最小性结论。 */
enum class ConflictMinimality {
    /** Every remaining source passed a proven deletion check. / 每个保留来源均通过已证明的删除复验。 */
    Irreducible,

    /** At least one deletion check was not proven. / 至少一个删除复验未形成证明。 */
    Partial,

    /** No deletion checks were requested or performed. / 未请求或未执行删除复验。 */
    NotChecked
}

/** Validity of the full target conflict. / 完整目标冲突的有效性。 */
enum class ConflictValidity {
    /** Target infeasibility was proven. / 目标不可行已被证明。 */
    Verified,

    /** Target feasibility was unknown or unsupported. / 目标可行性未知或不受支持。 */
    Unknown
}

/** One deletion verification result. / 一次删除复验结果。 */
data class ConflictVerification(
    /** Candidate source considered for deletion. / 尝试删除的候选来源。 */
    val source: DiagnosticSource,
    /** Status after removing the candidate from the active background. / 从活动背景移除候选后的状态。 */
    val status: AnalysisStatus,
    /** Whether the candidate was actually removed. / 候选是否实际被移除。 */
    val removed: Boolean,
    /** Whether the candidate remains in the current conflict. / 候选是否仍在当前冲突中。 */
    val retained: Boolean,
    /** Target remains fixed in this verification. / 此复验中 target 仍保持固定。 */
    val targetFixed: Boolean = true,
    /** Verification detail. / 复验详情。 */
    val message: String? = null
)

/** Group-level conflict counts. / 冲突按组统计。 */
data class ConflictGroupSummary(
    /** Group name; null means ungrouped. / 组名称，null 表示未分组。 */
    val group: String?,
    /** Number of remaining conflict sources in this group. / 组内剩余冲突来源数。 */
    val count: Int
)

/** Options controlling deletion-based MUS verification. / 控制基于删除的 MUS 复验选项。 */
data class ConflictAnalysisOptions(
    /** CP solve options forwarded to every target check. / 传递给每次 target 检查的 CP 求解选项。 */
    val solveOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions(),
    /** Maximum deletion checks; zero keeps the full proven conflict unshrunk. / 删除复验次数上限；零表示保留完整已证明冲突。 */
    val maxDeletionChecks: Int = Int.MAX_VALUE,
    /** Wall-clock budget for target plus deletion checks. / target 与删除复验的墙钟预算。 */
    val timeBudget: Duration? = null
) {
    /** Structured option validation; callers map it to Ret at the analysis boundary. / 结构化选项校验，由分析边界映射为 Ret。 */
    val validation: Try
        get() = when {
            maxDeletionChecks < 0 -> Failed(
                ErrorCode.IllegalArgument,
                "删除复验次数不得为负 / Maximum deletion checks must not be negative"
            )

            timeBudget != null && timeBudget.isNegative() -> Failed(
                ErrorCode.IllegalArgument,
                "冲突分析时间预算不得为负 / Conflict analysis time budget must not be negative"
            )

            else -> ok
        }
}

/**
 * Conflict explanation for one fixed objective target.
 * 一个固定 ObjectiveTarget 的冲突解释。
 *
 * [availableEvidence] is the complete original activation universe. [members]
 * is the current conflict/MUS candidate and never includes [target].
 * [availableEvidence] 是完整的原始 activation 集合；[members] 是当前冲突/MUS 候选，绝不包含 [target]。
 */
data class ConflictExplanation(
    /** Fixed objective target. / 固定的目标条件。 */
    val target: ObjectiveTarget,
    /** Initial target-feasibility result with all evidence active. / 所有证据激活时的初始 target 可行性结果。 */
    val targetFeasibility: TargetFeasibilityReport,
    /** Extraction path actually selected for the backend. / 后端实际选择的冲突提取路径。 */
    val extractionTier: ConflictExtractionTier = ConflictExtractionTier.RepeatedSolving,
    /** Aggregate target status. / target 聚合状态。 */
    val status: AnalysisStatus,
    /** Complete original evidence universe. / 完整原始证据全集。 */
    val availableEvidence: List<DiagnosticSource>,
    /** Current conflict or MUS members. / 当前冲突或 MUS 成员。 */
    val members: List<DiagnosticSource>,
    /** Stable source for the fixed target. / 固定 target 的稳定来源。 */
    val targetSource: DiagnosticSource.ObjectiveTarget = DiagnosticSource.ObjectiveTarget(target),
    /** Whether the initial target conflict is proven. / 初始 target 冲突是否已证明。 */
    val validity: ConflictValidity,
    /** Deletion-based minimality status. / 基于删除的最小性状态。 */
    val minimality: ConflictMinimality,
    /** Every attempted deletion verification. / 每次删除复验记录。 */
    val verifications: List<ConflictVerification> = emptyList(),
    /** Remaining members grouped by original constraint group. / 按原始约束组统计剩余成员。 */
    val groupSummaries: List<ConflictGroupSummary> = emptyList(),
    /** Whether the target is blocked by fixed background rather than an original constraint. /
     * 目标是否由固定背景而非原始约束阻塞。 */
    val backgroundBlocking: Boolean = false,
    /** Human-readable detail. / 面向诊断的详情。 */
    val message: String? = null
) {
    /** Constraint-only IDs; variable bounds/domains remain in [members]. / 仅约束 ID；变量边界和值域仍保留在 [members]。 */
    val constraintIds: Set<ConstraintId>
        get() = members.mapNotNull { (it as? DiagnosticSource.Constraint)?.id }.toSet()

    /** Structured original members for existing IIS consumers. / 供既有 IIS 调用方使用的结构化原始成员。 */
    val infeasibilityMembers: Set<InfeasibilityMember>
        get() = members.mapNotNull { it.asInfeasibilityMember() }.toSet()

    /** Compatibility alias for deletion checks. / 删除复验兼容别名。 */
    val deletionChecks: List<ConflictVerification>
        get() = verifications

    /** Whether all remaining members passed proven deletion checks. / 剩余成员是否全部通过已证明的删除复验。 */
    val minimalityVerified: Boolean
        get() = minimality == ConflictMinimality.Irreducible

    /** Alias used by callers naming the set explicitly. / 使用集合术语的调用方别名。 */
    val minimalBlockingSet: List<DiagnosticSource>
        get() = members

    /** Compatibility alias for callers using the shorter name. / 使用较短名称的调用方兼容别名。 */
    val tier: ConflictExtractionTier
        get() = extractionTier

    /** Validate proof and original-evidence invariants. / 校验证明与原始证据不变量。 */
    fun validate(): Try {
        when (val validation = target.validate()) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (targetFeasibility.target != target || targetFeasibility.source.target != target) {
            return Failed(
                ErrorCode.IllegalArgument,
                "冲突与 target 可行性报告的目标不一致 / conflict and target-feasibility targets do not match"
            )
        }
        if (availableEvidence.any { it is DiagnosticSource.ObjectiveTarget }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "原始 conflict evidence 不得包含 ObjectiveTarget / original conflict evidence must not contain ObjectiveTarget"
            )
        }
        if (availableEvidence.map { it.stableId }.toSet().size != availableEvidence.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "原始 conflict evidence 不得重复 / original conflict evidence must not contain duplicates"
            )
        }
        if (members.map { it.stableId }.toSet().size != members.size ||
            members.any { it is DiagnosticSource.ObjectiveTarget || it !in availableEvidence }
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "冲突成员必须是唯一的原始 evidence / conflict members must be unique original evidence"
            )
        }
        when (status) {
            AnalysisStatus.Reachable -> {
                if (targetFeasibility.status != AnalysisStatus.Reachable || members.isNotEmpty()) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "可达 target 不得包含 blocking members / reachable target must not contain blocking members"
                    )
                }
            }
            AnalysisStatus.Unreachable -> {
                if (targetFeasibility.status != AnalysisStatus.Unreachable ||
                    validity != ConflictValidity.Verified
                ) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "已证明不可达冲突必须保留一致的 Verified target evidence / proven unreachable conflicts require consistent Verified target evidence"
                    )
                }
                if (backgroundBlocking && minimality == ConflictMinimality.Irreducible) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "背景阻塞不得宣称约束级最小性 / background blocking must not claim constraint-level minimality"
                    )
                }
            }
            AnalysisStatus.Unknown,
            AnalysisStatus.Unsupported -> {
                if (targetFeasibility.status != status || validity != ConflictValidity.Unknown) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "未知或不支持的冲突必须保留相同状态 / unknown or unsupported conflicts must preserve their status"
                    )
                }
            }
        }
        if (minimality == ConflictMinimality.Irreducible) {
            if (members.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "不可约冲突必须至少包含一个 evidence 成员 / irreducible conflicts require at least one evidence member"
                )
            }
            val provenMembers = verifications
                .asSequence()
                .filter {
                    it.targetFixed &&
                        it.status == AnalysisStatus.Reachable &&
                        it.retained &&
                        it.source in members
                }
                .map { it.source }
                .toSet()
            if (provenMembers.size != members.size || members.any { it !in provenMembers }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "不可约冲突必须有每个成员的 target-fixed 删除证明 / irreducible conflicts require target-fixed deletion proofs"
                )
            }
        }
        return ok
    }
}

/**
 * Runs a target check and shrinks a proven conflict by deletion.
 * 执行 target 检查，并通过删除复验缩减已证明冲突。
 */
class ConflictAnalyzer(
    /** Existing CP solver. / 现有 CP 求解器。 */
    private val solver: ConstraintProgrammingSolver,
    /** Default target solve options. / 默认 target 求解选项。 */
    private val defaultSolveOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
) {
    /** Analyze a session target and cache the complete explanation. / 分析 session target 并缓存完整解释。 */
    suspend fun analyze(
        session: CriticalConstraintAnalysisSession,
        target: ObjectiveTarget,
        options: ConflictAnalysisOptions = ConflictAnalysisOptions(
            solveOptions = defaultSolveOptions
        )
    ): Ret<ConflictExplanation> {
        invalidSessionResult<ConflictExplanation>(session)?.let { return it }
        if (session.isClosed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "冲突分析会话已关闭 / Conflict analysis session is closed"
            )
        }
        when (val validation = options.validation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        val key = CacheKey(this, target, options)
        session.cached<ConflictExplanation>(AnalysisCacheKind.Conflict, key)?.let { return ok(it) }
        return analyze(
            snapshot = session.baselineSnapshot,
            target = target,
            options = options,
            capabilityMatrix = session.capabilityMatrix
        ).map { explanation ->
            session.cache(AnalysisCacheKind.Conflict, key, explanation)
            explanation
        }
    }

    /** Analyze one immutable snapshot and perform deletion-based verification. / 分析不可变 snapshot 并执行基于删除的复验。 */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        options: ConflictAnalysisOptions = ConflictAnalysisOptions(
            solveOptions = defaultSolveOptions
        ),
        capabilityMatrix: CapabilityMatrix = CapabilityMatrix.from(solver.descriptor)
    ): Ret<ConflictExplanation> {
        when (val validation = options.validation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!snapshot.validateIdentity() || !snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份或目标语义无效 / CP snapshot identity or objective semantics is invalid"
            )
        }
        val selectedTier = ConflictExtractionTier.select(capabilityMatrix)
        if (!selectedTier.isAvailable) {
            return unsupportedExplanation(snapshot, target, selectedTier)
        }
        val evidence = snapshot.diagnosticActivations().map(::sourceOf).distinct()
        val active = LinkedHashSet(evidence)
        val targetAnalyzer = TargetFeasibilityAnalyzer(solver, options.solveOptions)
        val startedAt = System.nanoTime()
        val initial = when (
            val result = targetAnalyzer.analyzeActiveSources(
                snapshot = snapshot,
                target = target,
                activeSources = active,
                options = options.solveOptions,
                capabilityMatrix = capabilityMatrix
            )
        ) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (initial.status != AnalysisStatus.Unreachable) {
            return ok(
                ConflictExplanation(
                    target = target,
                    targetFeasibility = initial,
                    extractionTier = selectedTier,
                    status = initial.status,
                    availableEvidence = evidence,
                    members = if (initial.status == AnalysisStatus.Reachable) emptyList() else evidence,
                    validity = if (initial.status == AnalysisStatus.Unknown) {
                        ConflictValidity.Unknown
                    } else if (initial.status == AnalysisStatus.Unsupported) {
                        ConflictValidity.Unknown
                    } else {
                        ConflictValidity.Verified
                    },
                    minimality = ConflictMinimality.NotChecked,
                    groupSummaries = groupSummaries(snapshot, if (initial.status == AnalysisStatus.Reachable) emptyList() else evidence),
                    message = when (initial.status) {
                        AnalysisStatus.Reachable -> "目标可达，不存在 target blocking conflict / Target is reachable; no blocking conflict exists"
                        AnalysisStatus.Unknown -> "目标检查未知，保留全部原始证据 / Target check is unknown; all original evidence is retained"
                        AnalysisStatus.Unsupported -> "目标检查不受支持，保留全部原始证据 / Target check is unsupported; all original evidence is retained"
                        AnalysisStatus.Unreachable -> null
                    }
                )
            )
        }

        var extractionTier = selectedTier
        if (!budgetExceeded(startedAt, options.timeBudget)) {
            val seed = when (selectedTier) {
                ConflictExtractionTier.NativeUnsatCore -> nativeSeed(
                    snapshot = snapshot,
                    target = target,
                    evidence = evidence,
                    options = options
                )

                ConflictExtractionTier.AssumptionExtraction -> assumptionSeed(
                    snapshot = snapshot,
                    target = target,
                    evidence = evidence,
                    options = options
                )

                ConflictExtractionTier.RepeatedSolving,
                ConflictExtractionTier.Unavailable -> null
            }
            if (seed != null) {
                val seedSources = LinkedHashSet(seed)
                val seedCheck = targetAnalyzer.analyzeActiveSources(
                    snapshot = snapshot,
                    target = target,
                    activeSources = seedSources,
                    options = options.solveOptions,
                    capabilityMatrix = capabilityMatrix
                )
                val provenSeed = when (seedCheck) {
                    is Ok -> seedCheck.value?.status == AnalysisStatus.Unreachable
                    is Failed,
                    is Fatal -> false
                }
                if (provenSeed) {
                    active.clear()
                    active.addAll(seedSources)
                } else {
                    // A backend conflict is only a seed. If its projection cannot be independently
                    // revalidated, discard it and use the proven full model for deletion shrinking.
                    // 后端 conflict 只能作为种子；若无法独立复验其投影，则丢弃并从已证明的完整模型收缩。
                    extractionTier = ConflictExtractionTier.RepeatedSolving
                }
            } else if (selectedTier != ConflictExtractionTier.RepeatedSolving) {
                extractionTier = ConflictExtractionTier.RepeatedSolving
            }
        } else if (selectedTier != ConflictExtractionTier.RepeatedSolving) {
            extractionTier = ConflictExtractionTier.RepeatedSolving
        }

        val verifications = ArrayList<ConflictVerification>()
        var partial = options.maxDeletionChecks == 0
        var checks = 0
        for (source in evidence) {
            if (source !in active) {
                continue
            }
            if (checks >= options.maxDeletionChecks || budgetExceeded(startedAt, options.timeBudget)) {
                partial = true
                break
            }
            val candidate = LinkedHashSet(active)
            candidate.remove(source)
            val verification = when (
                val result = targetAnalyzer.analyzeActiveSources(
                    snapshot = snapshot,
                    target = target,
                    activeSources = candidate,
                    options = options.solveOptions,
                    capabilityMatrix = capabilityMatrix
                )
            ) {
                is Ok -> {
                    val checked = result.value!!
                    when (checked.status) {
                        AnalysisStatus.Reachable -> {
                            ConflictVerification(
                                source = source,
                                status = checked.status,
                                removed = false,
                                retained = true,
                                message = "删除后 target 可达，保留来源 / Target is reachable after deletion; source retained"
                            )
                        }
                        AnalysisStatus.Unreachable -> {
                            active.remove(source)
                            ConflictVerification(
                                source = source,
                                status = checked.status,
                                removed = true,
                                retained = false,
                                message = "删除后 target 仍不可达，移除来源 / Target remains unreachable after deletion; source removed"
                            )
                        }
                        AnalysisStatus.Unknown,
                        AnalysisStatus.Unsupported -> {
                            partial = true
                            ConflictVerification(
                                source = source,
                                status = checked.status,
                                removed = false,
                                retained = true,
                                message = "删除复验未形成证明，保留来源 / Deletion check did not form a proof; source retained"
                            )
                        }
                    }
                }
                is Failed -> {
                    partial = true
                    ConflictVerification(
                        source = source,
                        status = AnalysisStatus.Unknown,
                        removed = false,
                        retained = true,
                        message = result.error.message
                    )
                }
                is Fatal -> {
                    partial = true
                    ConflictVerification(
                        source = source,
                        status = AnalysisStatus.Unknown,
                        removed = false,
                        retained = true,
                        message = result.errors.joinToString(separator = "; ") { it.message }
                    )
                }
            }
            verifications += verification
            ++checks
        }
        val members = evidence.filter { it in active }
        // An empty constraint projection can still be unreachable because a Boolean domain or a
        // sparse domain remains a fixed semantic background. Such a result is useful evidence,
        // but it is not a constraint-level MUS and must never be labelled Irreducible.
        // 约束投影为空时，Boolean/稀疏值域等固定语义背景仍可能使 target 不可达。该结果仍是有用
        // 的证据，但不是约束级 MUS，绝不能标记为 Irreducible。
        val backgroundBlocking = initial.status == AnalysisStatus.Unreachable &&
            members.none { it is DiagnosticSource.Constraint }
        // A one-pass deletion shrink is sufficient for a monotone feasibility oracle: once a
        // member's deletion is proven reachable, deleting additional sources cannot make that
        // candidate infeasible again. Keep an explicit final evidence gate nevertheless, so a
        // future change cannot claim Irreducible unless every final member has its own proven
        // target-fixed deletion check.
        // 对单调可行性判定而言，单轮删除收缩已经足够：某成员删除后若已证明可达，继续删除
        // 其他来源不会让该候选重新不可行。但仍保留显式的最终证据门槛，防止未来改动在没有
        // 为每个最终成员留下 target-fixed 且已证明可达的逐项删除复验时声称 Irreducible。
        val everyMemberHasProvenDeletionCheck = members.all { member ->
            verifications.any { verification ->
                verification.source == member &&
                    verification.targetFixed &&
                    verification.retained &&
                    verification.status == AnalysisStatus.Reachable
            }
        }
        if (!everyMemberHasProvenDeletionCheck) {
            partial = true
        }
        return ok(
            ConflictExplanation(
                target = target,
                targetFeasibility = initial,
                extractionTier = extractionTier,
                status = AnalysisStatus.Unreachable,
                availableEvidence = evidence,
                members = members,
                validity = ConflictValidity.Verified,
                minimality = when {
                    options.maxDeletionChecks == 0 -> ConflictMinimality.NotChecked
                    backgroundBlocking -> ConflictMinimality.Partial
                    partial -> ConflictMinimality.Partial
                    else -> ConflictMinimality.Irreducible
                },
                verifications = verifications,
                groupSummaries = groupSummaries(snapshot, members),
                backgroundBlocking = backgroundBlocking,
                message = if (backgroundBlocking) {
                    "目标在没有原始约束后仍不可达，阻塞来自固定背景，不能形成约束级 MUS / " +
                        "The target remains unreachable without an original constraint; blocking " +
                        "comes from fixed background, so no constraint-level MUS is claimed"
                } else if (partial) {
                    "至少一个删除复验未知或未执行，MUS 未完全证明 / At least one deletion check was unknown or not executed; MUS is not fully proven"
                } else {
                    null
                }
            )
        )
    }

    private fun sourceOf(activation: fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingActivationSnapshot): DiagnosticSource {
        return when (val member = activation.member) {
            is InfeasibilityMember.Constraint -> DiagnosticSource.Constraint(member.id)
            is InfeasibilityMember.VariableBound -> when (member.ref.side) {
                BoundSide.Lower -> DiagnosticSource.VariableLowerBound(member.ref.variableId)
                BoundSide.Upper -> DiagnosticSource.VariableUpperBound(member.ref.variableId)
            }
            is InfeasibilityMember.VariableDomain -> DiagnosticSource.SparseDomain(member.ref.variableId)
        }
    }

    private fun groupSummaries(
        snapshot: ConstraintProgrammingModelSnapshot,
        sources: List<DiagnosticSource>
    ): List<ConflictGroupSummary> {
        val groups = sources.mapNotNull { source ->
            val constraint = (source as? DiagnosticSource.Constraint)?.id?.let(snapshot::constraint)
            constraint?.groupName
        }.groupingBy { it }.eachCount()
        return groups.entries.map { (group, count) -> ConflictGroupSummary(group, count) }
    }

    private fun unsupportedExplanation(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        extractionTier: ConflictExtractionTier
    ): Ret<ConflictExplanation> {
        val evidence = snapshot.diagnosticActivations().map(::sourceOf).distinct()
        val targetReport = TargetFeasibilityReport(
            target = target,
            status = AnalysisStatus.Unsupported,
            targetFixed = false,
            message = "当前求解器未声明 conflict 能力 / Solver does not declare conflict support"
        )
        return ok(
            ConflictExplanation(
                target = target,
                targetFeasibility = targetReport,
                extractionTier = extractionTier,
                status = AnalysisStatus.Unsupported,
                availableEvidence = evidence,
                members = evidence,
                validity = ConflictValidity.Unknown,
                minimality = ConflictMinimality.NotChecked,
                groupSummaries = groupSummaries(snapshot, evidence),
                message = targetReport.message
            )
        )
    }

    /**
     * Request a backend conflict seed through the native conflict option.
     * 通过后端原生 conflict 选项请求冲突种子。
     *
     * The returned members are always projected through the immutable activation set. The target
     * constraint and every solver-generated artifact are therefore excluded before deletion starts.
     * 返回成员始终经过 immutable activation 集合投影，因此 target 与 solver 辅助元素会在删除前排除。
     */
    private suspend fun nativeSeed(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        evidence: List<DiagnosticSource>,
        options: ConflictAnalysisOptions
    ): List<DiagnosticSource>? {
        val built = ConstraintProgrammingSnapshotBuilder.build(snapshot, target)
        if (built !is Ok) {
            return null
        }
        val model = built.value!!.model
        return try {
            val result = try {
                solver.solve(
                    model,
                    options.solveOptions.copy(
                        collectConflict = true,
                        shrinkConflict = false,
                        // Constraint activations are guaranteed to exist in every diagnostic
                        // compiler. Bound/domain activations can be omitted by a backend when its
                        // original bound already equals the safe base domain; passing those IDs
                        // would turn a valid native request into an unknown-ID modeling error.
                        // 每个诊断编译器都保证创建约束 activation；当原始 bound 已等于安全基础值域时，
                        // 后端可以省略 bound/domain activation，故不能把这些 ID 强行传入。
                        conflictActivationIds = evidence
                            .filterIsInstance<DiagnosticSource.Constraint>()
                            .map { "constraint:${it.id.value}" }
                            .toSet()
                    )
                )
            } catch (_: Throwable) {
                return null
            }
            if (result !is Ok) {
                return null
            }
            val output = result.value!!
            if (output !is ConstraintProgrammingInfeasibleOutput || !output.isProvenInfeasible()) {
                return null
            }
            val projected = remapBackendConflict(
                snapshot = snapshot,
                conflict = output.conflict,
                requested = evidence
            )
            // Native requests currently constrain the backend activation universe to original
            // constraints. Keep the original bound/domain background in the seed; otherwise a
            // backend core that omits those members would make the independent re-check solve an
            // artificially unbounded model and could turn a valid seed into UNKNOWN.
            // 原生请求当前把后端 activation 范围限定为原始约束。种子必须保留原始边界/值域背景；
            // 否则后端 core 未返回这些成员时，独立复验会错误地求解人为无界模型并变成 UNKNOWN。
            val withBackground = LinkedHashSet<DiagnosticSource>()
            withBackground.addAll(projected)
            withBackground.addAll(evidence.filterNot { it is DiagnosticSource.Constraint })
            withBackground.takeIf { it.isNotEmpty() }?.toList()
        } finally {
            model.close()
        }
    }

    /**
     * Extract a conflict using the CP session assumption contract.
     * 使用 CP session assumption 契约提取冲突。
     *
     * The derived model contains one guard implication per original constraint. Bounds/domains are
     * kept active in this path and remain part of the independently verified seed, while the
     * existing deletion analyzer continues to handle their removable semantics.
     * 派生模型为每个原始约束建立一条 guard 蕴含；本路径保持 bounds/domain 激活，并把它们并入独立复验
     * 的种子，随后仍由现有 deletion analyzer 处理其可删除语义。
     */
    private suspend fun assumptionSeed(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        evidence: List<DiagnosticSource>,
        options: ConflictAnalysisOptions
    ): List<DiagnosticSource>? {
        val fixedSources = evidence.filterNot { it is DiagnosticSource.Constraint }.toSet()
        if (snapshot.constraints.isEmpty()) {
            return null
        }
        val built = ConstraintProgrammingSnapshotBuilder.build(snapshot, target, fixedSources)
        if (built !is Ok) {
            return null
        }
        val model = built.value!!.model
        val literals = LinkedHashMap<ConstraintId, BooleanLiteral>()
        return try {
            for ((index, entry) in snapshot.constraints.withIndex()) {
                val variable = BinVar("analysis-assumption-$index")
                val variableId = VariableId("analysis-assumption:${entry.id.value}")
                val registered = model.registerVariable(
                    id = variableId,
                    variable = variable,
                    domain = IntegerDomain.boolean
                )
                if (registered !is Ok) {
                    return null
                }
                val literal = BooleanLiteral.Variable(
                    variable = variable,
                    negated = false,
                    id = variableId
                )
                val guarded = ConstraintProgrammingConstraint.implies(literal, entry.constraint)
                if (guarded !is Ok) {
                    return null
                }
                val added = model.addConstraint(
                    constraint = guarded.value!!,
                    id = entry.id,
                    name = entry.name
                )
                if (added !is Ok) {
                    return null
                }
                literals[entry.id] = literal
            }
            val activationSet = DiagnosticActivationSet.fromSnapshot(snapshot)
            val assumptions = ConstraintProgrammingAssumptionMapper.assumptions(
                activations = activationSet,
                literals = literals
            )
            if (assumptions.isEmpty()) {
                return null
            }
            val sessionResult = solver.createSession(
                model,
                options.solveOptions.copy(
                    collectConflict = true,
                    shrinkConflict = false,
                    conflictActivationIds = null
                )
            )
            if (sessionResult !is Ok) {
                return null
            }
            val session = sessionResult.value!!
            val result = try {
                session.solve(assumptions = assumptions)
            } catch (_: Throwable) {
                return null
            } finally {
                session.close()
            }
            if (result !is Ok) {
                return null
            }
            val output = result.value!!
            if (output !is ConstraintProgrammingInfeasibleOutput || !output.isProvenInfeasible()) {
                return null
            }
            val projected = remapBackendConflict(
                snapshot = snapshot,
                conflict = output.conflict,
                requested = evidence,
                assumptions = assumptions,
                literals = literals
            )
            // Bounds/domains are kept active in the assumption-derived model, so they are part
            // of the independently verifiable seed even when the backend reports only guarded
            // constraints in its conflict.
            // assumption 派生模型保持边界/值域激活，因此即使后端只报告 guard 约束，种子也必须
            // 包含这些可独立复验的背景来源。
            val withBackground = LinkedHashSet<DiagnosticSource>()
            withBackground.addAll(projected)
            withBackground.addAll(evidence.filterNot { it is DiagnosticSource.Constraint })
            withBackground.takeIf { it.isNotEmpty() }?.toList()
        } finally {
            model.close()
        }
    }

    /** Project backend members, activation IDs, and assumptions to original sources only. */
    private fun remapBackendConflict(
        snapshot: ConstraintProgrammingModelSnapshot,
        conflict: ConstraintProgrammingConflict?,
        requested: List<DiagnosticSource>,
        assumptions: List<BooleanLiteral> = emptyList(),
        literals: Map<ConstraintId, BooleanLiteral> = emptyMap()
    ): List<DiagnosticSource> {
        conflict ?: return emptyList()
        val activationSet = DiagnosticActivationSet.fromSnapshot(snapshot)
        val wanted = LinkedHashSet<DiagnosticSource>()
        wanted.addAll(activationSet.remapBackendEvidence(conflict.members))
        val originalByConstraint = snapshot.constraints.associate { entry ->
            entry.id to DiagnosticSource.Constraint(entry.id)
        }
        conflict.constraintIds.forEach { id ->
            originalByConstraint[id]?.let(wanted::add)
        }
        val originalByActivationId = snapshot.diagnosticActivations().associate { activation ->
            activation.id to sourceOf(activation)
        }
        conflict.activationIds.forEach { id ->
            originalByActivationId[id]?.let(wanted::add)
        }

        val assumptionSources = if (conflict.assumptions.isEmpty()) {
            assumptions
        } else {
            conflict.assumptions
        }
        if (assumptionSources.isNotEmpty()) {
            val sourceByLiteral = literals.entries.associate { (id, literal) ->
                literalKey(literal) to DiagnosticSource.Constraint(id)
            }
            assumptionSources.forEach { literal ->
                sourceByLiteral[literalKey(literal)]?.let(wanted::add)
            }
        }
        return requested.filter { it in wanted }
    }

    private fun literalKey(literal: BooleanLiteral): String {
        return "${literal.variableId?.value}:${literal.negated}:${literal.constant}"
    }

    private fun budgetExceeded(startedAt: Long, budget: Duration?): Boolean {
        return budget != null && System.nanoTime() - startedAt >= budget.inWholeNanoseconds
    }

    private data class CacheKey(
        val analyzer: ConflictAnalyzer,
        val target: ObjectiveTarget,
        val options: ConflictAnalysisOptions
    )
}

/**
 * Convenience facade returning only the verified or partial blocking set.
 * 仅返回已验证或部分验证阻塞集合的便捷 facade。
 */
class MinimalConflictAnalyzer(
    /** Conflict analyzer performing real solver-backed checks. / 执行真实 solver 检查的 conflict analyzer。 */
    private val delegate: ConflictAnalyzer
) {
    /** Build a facade from an existing CP solver. / 使用现有 CP solver 构造 facade。 */
    constructor(
        solver: ConstraintProgrammingSolver,
        defaultSolveOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
    ) : this(ConflictAnalyzer(solver, defaultSolveOptions))

    /** Analyze and return a minimal-blocking-set view. / 分析并返回最小阻塞集合视图。 */
    suspend fun analyze(
        session: CriticalConstraintAnalysisSession,
        target: ObjectiveTarget,
        options: ConflictAnalysisOptions = ConflictAnalysisOptions()
    ): Ret<MinimalBlockingSet> {
        return when (val result = delegate.analyze(session, target, options)) {
            is Ok -> ok(MinimalBlockingSet.from(result.value!!))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /** Analyze an immutable snapshot and return a minimal-blocking-set view. / 分析不可变 snapshot 并返回最小阻塞集合视图。 */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        target: ObjectiveTarget,
        options: ConflictAnalysisOptions = ConflictAnalysisOptions()
    ): Ret<MinimalBlockingSet> {
        return when (val result = delegate.analyze(snapshot, target, options)) {
            is Ok -> ok(MinimalBlockingSet.from(result.value!!))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}

/** Public minimal-blocking-set projection. / 公共最小阻塞集合投影。 */
data class MinimalBlockingSet(
    /** Fixed target. / 固定 target。 */
    val target: ObjectiveTarget,
    /** Remaining original evidence members. / 剩余原始证据成员。 */
    val members: List<DiagnosticSource>,
    /** Solver conclusion for the fixed target. / 固定 target 的求解结论。 */
    val status: AnalysisStatus,
    /** Minimality conclusion. / 最小性结论。 */
    val minimality: ConflictMinimality,
    /** Validity of the original target conflict. / 原始 target 冲突有效性。 */
    val validity: ConflictValidity,
    /** Verification checks. / 复验记录。 */
    val verifications: List<ConflictVerification>
) {
    /** Whether minimality was proven. / 是否已证明最小性。 */
    val verified: Boolean
        get() = minimality == ConflictMinimality.Irreducible

    companion object {
        /** Project a full explanation. / 将完整解释投影为最小阻塞集合。 */
        fun from(explanation: ConflictExplanation): MinimalBlockingSet {
            return MinimalBlockingSet(
                target = explanation.target,
                members = explanation.members,
                status = explanation.status,
                minimality = explanation.minimality,
                validity = explanation.validity,
                verifications = explanation.verifications
            )
        }
    }
}
