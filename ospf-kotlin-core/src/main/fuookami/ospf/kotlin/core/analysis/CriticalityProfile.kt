package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions

/** Classification of a constraint across multiple objective targets. / 跨多个目标条件对约束进行分类。 */
enum class CriticalityKind {
    LocalBottleneck,
    PersistentBottleneck,
    StructuralBottleneck
}

/** One target result used to build a multi-target profile. / 用于构建多目标 profile 的单个 target 结果。 */
data class CriticalityObservation(
    val target: ObjectiveTarget,
    val status: AnalysisStatus,
    val blockingConstraintIds: Set<ConstraintId> = emptySet()
) {
    init {
        require(blockingConstraintIds.none { it.value.isBlank() }) {
            "blocking constraint identity must not be blank"
        }
    }
}

/** Stable multi-target criticality profile. / 稳定的多目标 criticality profile。 */
data class CriticalityProfile(
    val observations: List<CriticalityObservation>,
    val classifications: Map<ConstraintId, Set<CriticalityKind>>,
    val schemaVersion: String = "1.0"
) {
    /** Constraints classified for a given kind. / 按指定类型分类的约束。 */
    fun constraints(kind: CriticalityKind): Set<ConstraintId> =
        classifications.filterValues { kind in it }.keys

    /** Only proven unreachable targets participate in bottleneck classification. / 只有已证明不可达的目标参与瓶颈分类。 */
    val provenTargetCount: Int
        get() = observations.count { it.status == AnalysisStatus.Unreachable }

    /** Validate target identity, duplicate targets, and evidence-derived classifications. / 校验目标身份、重复 target 及由证据推导的分类。 */
    fun validate(): Try {
        if (schemaVersion.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "criticality profile schema 版本不得为空 / criticality profile schema must not be blank"
            )
        }
        val targetIds = HashSet<String>()
        observations.forEach { observation ->
            when (val validation = observation.target.validate()) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            if (!targetIds.add(observation.target.stableId)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "criticality profile 不得包含重复 target / criticality profile contains duplicate targets"
                )
            }
            if (observation.blockingConstraintIds.any { it.value.isBlank() }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "criticality profile 约束身份不能为空 / criticality profile constraint ID must not be blank"
                )
            }
        }
        if (classifications.keys.any { it.value.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "criticality profile 约束身份不能为空 / criticality profile ID must not be blank"
            )
        }
        val expected = criticalityClassifications(observations)
        return if (classifications == expected) {
            ok
        } else {
            Failed(
                ErrorCode.IllegalArgument,
                "criticality profile 分类与原始证据不一致 / criticality profile classifications do not match original evidence"
            )
        }
    }

    init {
        require(schemaVersion.isNotBlank()) { "profile schema version must not be blank" }
        require(classifications.keys.all { it.value.isNotBlank() }) {
            "profile constraint identity must not be blank"
        }
    }
}

/**
 * Build a deterministic profile from target observations.
 *
 * A constraint in one proven unreachable target is local, in more than one is persistent, and in
 * every proven unreachable target is structural. Unknown/Unsupported/Reachable observations are
 * retained for auditability but never become bottleneck evidence.
 *
 * 根据目标观测构建确定性的 profile。
 * 一个已证明不可达目标中的约束属于局部瓶颈，出现在多个目标中属于持续瓶颈，出现在每个已证明不可达目标中属于结构瓶颈。
 * Unknown、Unsupported 和 Reachable 观测仅为审计保留，不会形成瓶颈证据。
 */
fun buildCriticalityProfile(
    observations: List<CriticalityObservation>
): CriticalityProfile {
    return CriticalityProfile(observations, criticalityClassifications(observations))
}

/** Build a Result-valued profile for callers that need structured validation errors. / 为需要结构化校验错误的调用方构造 Result 形式 profile。 */
fun tryBuildCriticalityProfile(
    observations: List<CriticalityObservation>
): Ret<CriticalityProfile> {
    val profile = buildCriticalityProfile(observations)
    return when (val validation = profile.validate()) {
        is Ok -> ok(profile)
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

private fun criticalityClassifications(
    observations: List<CriticalityObservation>
): Map<ConstraintId, Set<CriticalityKind>> {
    val proven = observations.filter { it.status == AnalysisStatus.Unreachable }
    val counts = proven
        .flatMap { it.blockingConstraintIds }
        .groupingBy { it }
        .eachCount()
    val total = proven.size
    return counts.keys.sortedBy { it.value }.associateWith { id ->
        val count = counts.getValue(id)
        buildSet {
            if (count == 1) add(CriticalityKind.LocalBottleneck)
            if (count > 1) add(CriticalityKind.PersistentBottleneck)
            // A single target is a local observation; structural stability requires a
            // genuinely multi-target profile.
            // 单个 target 只是局部观测；结构稳定性要求真正的多目标 profile。
            if (total > 1 && count == total) add(CriticalityKind.StructuralBottleneck)
        }
    }
}

/**
 * Complete result of a multi-target pipeline run.
 *
 * The individual reports are retained so callers can inspect target, perturbation, and conflict
 * evidence without reconstructing it from the aggregate profile. Recommendations are derived
 * only from deletion-verified conflicts; an unknown or unsupported target never produces a
 * correction claim.
 *
 * 多目标 pipeline 运行的完整结果。
 * 保留各个报告，使调用方无需从汇总 profile 重建 target、扰动和 conflict 证据即可检查它们。
 * 修正建议只来源于已通过删除验证的冲突；Unknown 或 Unsupported target 绝不会产生修正结论。
 */
data class CriticalityProfileAnalysisResult(
    /** One complete pipeline report per requested target, in input order. */
    val reports: List<CriticalConstraintAnalysisReport>,
    /** Aggregate local/persistent/structural classification. */
    val profile: CriticalityProfile,
    /** Alternative correction plans backed by verified minimal conflicts. */
    val improvementPlans: List<AlternativeImprovementPlan> = emptyList(),
    /** Weakest target conclusion in the batch. */
    val status: AnalysisStatus,
    /** Public result schema. */
    val schemaVersion: String = "1.0"
) {
    /** Compatibility name for callers that refer to reports as target reports. */
    val targetReports: List<CriticalConstraintAnalysisReport>
        get() = reports

    /** Compatibility name for callers that refer to plans as recommendations. */
    val recommendations: List<AlternativeImprovementPlan>
        get() = improvementPlans

    init {
        require(schemaVersion.isNotBlank()) { "multi-target profile schema version must not be blank" }
        require(reports.size == profile.observations.size) {
            "one profile observation is required for every target report"
        }
        reports.zip(profile.observations).forEach { (report, observation) ->
            require(report.target?.target == observation.target) {
                "profile observation target does not match its pipeline report"
            }
            require(report.target?.status == observation.status) {
                "profile observation status does not match its pipeline report"
            }
        }
        require(improvementPlans.map { it.rank } == improvementPlans.indices.toList()) {
            "improvement-plan ranks must be dense and ordered"
        }
    }
}

/**
 * Execute the complete critical-constraint pipeline for every target and aggregate its evidence.
 *
 * The same immutable snapshot and pipeline are reused, while each pipeline invocation creates
 * its own analysis session. This keeps backend state isolated between targets and still ensures
 * that every target uses the same Gurobi/SCIP perturbation, target, and conflict adapters. Target
 * statuses are taken from the target stage, so an unavailable optional stage does not turn a
 * proven target into a fabricated `Unknown` or `Unsupported` target result.
 */
suspend fun analyzeCriticalityTargets(
    pipeline: CriticalConstraintAnalysisPipeline,
    snapshot: ConstraintProgrammingModelSnapshot,
    baselineSolution: ConstraintProgrammingSolution?,
    baselineObjective: Flt64?,
    baselineProvenOptimal: Boolean,
    objectiveId: String,
    targets: List<ObjectiveTarget>,
    options: CriticalConstraintAnalysisOptions = CriticalConstraintAnalysisOptions(),
    recommendationPolicy: RelaxabilityPolicy = RelaxabilityPolicy(),
    relaxationCosts: Map<String, RelaxationCost> = emptyMap()
): Ret<CriticalityProfileAnalysisResult> {
    when (val validation = validateTargetBatch(targets)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }

    val reports = ArrayList<CriticalConstraintAnalysisReport>(targets.size)
    for (target in targets) {
        when (
            val result = pipeline.analyze(
                snapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjective = baselineObjective,
                baselineProvenOptimal = baselineProvenOptimal,
                objectiveId = objectiveId,
                target = target,
                options = options
            )
        ) {
            is Ok -> reports += result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }

    val observations = reports.mapIndexed { index, report ->
        val targetReport = report.target
        val conflict = report.conflict
        CriticalityObservation(
            target = targets[index],
            status = targetReport?.status ?: AnalysisStatus.Unsupported,
            // A target report can be unreachable while the conflict is only partial. Keep that
            // report for auditability, but only a verified irreducible conflict is criticality
            // evidence.
            blockingConstraintIds = if (
                targetReport?.status == AnalysisStatus.Unreachable &&
                conflict != null &&
                conflict.target == targets[index] &&
                conflict.isVerifiedMinimalConflict()
            ) {
                conflict.constraintIds
            } else {
                emptySet()
            }
        )
    }
    val profile = buildCriticalityProfile(observations)
    val plans = reports
        .asSequence()
        .mapNotNull { report ->
            val conflict = report.conflict ?: return@mapNotNull null
            if (report.target?.status != AnalysisStatus.Unreachable ||
                report.target.target != conflict.target ||
                !conflict.isVerifiedMinimalConflict()
            ) {
                return@mapNotNull null
            }
            conflict
        }
        .flatMap {
            alternativeImprovementPlansFromConflict(
                conflict = it,
                costs = relaxationCosts,
                policy = recommendationPolicy
            ).asSequence()
        }
        .sortedWith(
            compareBy<AlternativeImprovementPlan> { it.correctionSet.totalCost }
                .thenBy { it.target.stableId }
                .thenBy { plan ->
                    plan.correctionSet.members.joinToString(",") { it.source.stableId }
                }
        )
        .take(recommendationPolicy.maxPlans)
        .mapIndexed { index, plan -> plan.copy(rank = index) }
        .toList()
    val targetStatus = observations
        .map { it.status }
        .reduce(::combineStatus)
    return ok(
        CriticalityProfileAnalysisResult(
            reports = reports,
            profile = profile,
            improvementPlans = plans,
            status = targetStatus
        )
    )
}

/** Execute conflict/target analysis for every target using one solver and aggregate the results. */
suspend fun analyzeCriticalityTargets(
    solver: ConstraintProgrammingSolver,
    snapshot: ConstraintProgrammingModelSnapshot,
    targets: List<ObjectiveTarget>,
    options: ConflictAnalysisOptions = ConflictAnalysisOptions(
        solveOptions = ConstraintProgrammingSolveOptions()
    )
): Ret<CriticalityProfile> {
    when (val validation = validateTargetBatch(targets)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }
    val observations = ArrayList<CriticalityObservation>(targets.size)
    val analyzer = ConflictAnalyzer(solver, options.solveOptions)
    for (target in targets) {
        when (val result = analyzer.analyze(snapshot, target, options)) {
            is Ok -> {
                val explanation = result.value!!
                observations += CriticalityObservation(
                    target = target,
                    status = explanation.status,
                    blockingConstraintIds = if (explanation.isVerifiedMinimalConflict()) {
                        explanation.constraintIds
                    } else {
                        emptySet()
                    }
                )
            }
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok(buildCriticalityProfile(observations))
}

/** Shared validation for every multi-target entry point. */
private fun validateTargetBatch(targets: List<ObjectiveTarget>): Ret<Unit> {
    if (targets.isEmpty()) {
        return Failed(
            ErrorCode.IllegalArgument,
            "至少需要一个 ObjectiveTarget / At least one ObjectiveTarget is required"
        )
    }
    val duplicate = targets
        .groupingBy { it.stableId }
        .eachCount()
        .entries
        .firstOrNull { it.value > 1 }
    if (duplicate != null) {
        return Failed(
            ErrorCode.IllegalArgument,
            "ObjectiveTarget 不得重复：${duplicate.key} / Duplicate ObjectiveTarget: ${duplicate.key}"
        )
    }
    return ok(Unit)
}
