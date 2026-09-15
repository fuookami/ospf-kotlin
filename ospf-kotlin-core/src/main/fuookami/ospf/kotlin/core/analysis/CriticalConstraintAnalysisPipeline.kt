/** 临界约束分析流水线编排。 / Critical-constraint analysis pipeline orchestration. */
package fuookami.ospf.kotlin.core.analysis

import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 流水线选项。
 *
 * 每个阶段都可以独立关闭；关闭的阶段在报告中显式为 `null` 并登记原因，绝不会被推断为"无效"。
 *
 * Pipeline options. Every stage can be disabled independently; a disabled stage is explicitly
 * `null` in the report with a recorded reason and is never inferred to mean "ineffective".
 */
data class CriticalConstraintAnalysisOptions(
    /** 候选漏斗配置。 / Candidate funnel configuration. */
    val funnel: CandidateFunnelConfig = CandidateFunnelConfig(),
    /** 扰动策略。 / Perturbation policy. */
    val perturbation: ConstraintPerturbationPolicy = ConstraintPerturbationPolicy(
        deltas = emptyList(),
        removalTest = true
    ),
    /** 冲突分析选项。 / Conflict analysis options. */
    val conflict: ConflictAnalysisOptions = ConflictAnalysisOptions(),
    /** 固定整数 LP 选项。 / Fixed-integer LP options. */
    val lp: FixedIntegerLpSensitivityOptions = FixedIntegerLpSensitivityOptions(),
    /** CP 求解选项。 / CP solve options. */
    val solveOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions(),
    /** 是否运行扰动阶段。 / Whether to run the perturbation stage. */
    val runPerturbation: Boolean = true,
    /** 是否运行目标可行性与冲突阶段。 / Whether to run the target and conflict stages. */
    val runTargetAnalysis: Boolean = true,
    /** 昂贵分析的候选上限；0 表示只做筛选不做扰动。 / Candidate limit for expensive analysis; 0 means screening only. */
    val candidateLimit: Int = CandidateFunnelConfig.DEFAULT_CANDIDATE_LIMIT
)

/**
 * 临界约束分析流水线。
 *
 * 按计划第 9 节的默认策略编排：
 *
 * ```
 * baseline → activity → fixed-integer LP → candidate funnel
 *          → perturbation / removal（短名单）
 *          → target feasibility → conflict / MUS
 *          → ConstraintGroup 聚合 → 统一报告
 * ```
 *
 * 设计约束：
 * - 编排层不实现任何算法，只负责调用各分析器并聚合结果；
 * - 后端缺失时对应阶段返回 `Unsupported` 而不是被跳过或降级；
 * - 总体结论由 [combineStatus] 取最弱环节，未证明阶段不会被更强的下游结论掩盖。
 *
 * The pipeline orchestrates the plan's default strategy. The orchestration layer implements no
 * algorithm: it calls the analyzers and aggregates their results. A missing backend yields
 * `Unsupported` for that stage rather than being skipped or downgraded, and the overall conclusion
 * takes the weakest link via [combineStatus], so an unproven stage is never masked.
 */
class CriticalConstraintAnalysisPipeline(
    /** CP solver used for target feasibility and conflict analysis. / 用于目标可行性与冲突分析的 CP 求解器。 */
    private val solver: ConstraintProgrammingSolver,
    /** Optional LP dual backend. / 可选 LP 对偶后端。 */
    private val lpBackend: FixedIntegerLpBackend? = null,
    /** Optional reoptimization backend. / 可选重新优化后端。 */
    private val perturbationBackend: ConstraintPerturbationBackend? = null,
    /** Activity analyzer reused across stages. / 各阶段复用的活动性分析器。 */
    private val activityAnalyzer: ConstraintActivityAnalyzer = ConstraintActivityAnalyzer(),
    /** Capability matrix; defaults to the solver descriptor. / 能力矩阵，默认取自求解器描述符。 */
    private val capabilityMatrix: CapabilityMatrix = CapabilityMatrix.from(solver.descriptor)
) {
    /** Injected analysis backends may add capability for the stage they implement. */
    private val effectiveCapabilityMatrix: CapabilityMatrix = if (
        lpBackend != null &&
        capabilityMatrix.analysisCapabilities[AnalysisCapability.FixedIntegerLpSensitivity] !=
            CapabilitySupport.Unsupported
    ) {
        capabilityMatrix.copy(
            analysisCapabilities = capabilityMatrix.analysisCapabilities + mapOf(
                AnalysisCapability.FixedIntegerLpSensitivity to CapabilitySupport.Supported
            )
        )
    } else {
        capabilityMatrix
    }

    /**
     * 运行完整分析流水线。
     *
     * @param snapshot 不可变基线 snapshot / Immutable baseline snapshot
     * @param baselineSolution 基线 CP 解 / Baseline CP solution
     * @param baselineObjective 基线目标值 / Baseline objective value
     * @param baselineProvenOptimal 基线是否已证明最优 / Whether the baseline is proven optimal
     * @param objectiveId 目标身份 / Objective identity
     * @param target 可选目标突破条件 / Optional objective target
     * @param options 流水线选项 / Pipeline options
     */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        baselineSolution: ConstraintProgrammingSolution?,
        baselineObjective: Flt64?,
        baselineProvenOptimal: Boolean,
        objectiveId: String,
        target: ObjectiveTarget? = null,
        options: CriticalConstraintAnalysisOptions = CriticalConstraintAnalysisOptions()
    ): Ret<CriticalConstraintAnalysisReport> {
        if (!snapshot.validateIdentity() || !snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份或目标语义无效 / CP snapshot identity or objective semantics is invalid"
            )
        }
        val session = when (
            val result = CriticalConstraintAnalysisSession.tryFromSnapshot(
                snapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjectiveValue = baselineObjective,
                solverDescriptor = solver.descriptor,
                capabilityMatrix = effectiveCapabilityMatrix
            )
        ) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return try {
            val builder = CriticalConstraintAnalysisReportBuilder()
                .baseline(
                    AnalysisBaselineSummary(
                        objectiveId = objectiveId,
                        sense = snapshot.objectCategory.name,
                        objectiveValue = baselineObjective,
                        status = if (baselineProvenOptimal) {
                            AnalysisStatus.Reachable
                        } else {
                            AnalysisStatus.Unknown
                        },
                        provenOptimal = baselineProvenOptimal
                    )
                )

        // 阶段 1：活动性。 / Stage 1: activity.
        val activity = when (val result = activityAnalyzer.analyze(session)) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        builder.activity(activity)

        // 阶段 2：固定整数 LP 局部有效性。缺失后端时显式 Unsupported。
        // Stage 2: fixed-integer LP local sensitivity; an absent backend is explicitly Unsupported.
        var localSensitivity: FixedIntegerLpSensitivityReport? = null
        if (lpBackend == null) {
            builder.noteUnavailable(
                "固定整数 LP 后端未提供；局部有效性阶段返回 Unsupported / " +
                    "No fixed-integer LP backend was supplied; the local-sensitivity stage reports Unsupported"
            )
        } else {
            val analyzer = FixedIntegerLpSensitivityAnalyzer(lpBackend, activityAnalyzer)
            when (
                val result = analyzer.analyze(
                    session = session,
                    options = options.lp
                )
            ) {
                is Ok -> {
                    localSensitivity = result.value!!
                    builder.localSensitivity(result.value!!)
                }

                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        // 阶段 3：候选漏斗。 / Stage 3: candidate funnel.
        val funnel = when (
            val result = buildCandidateFunnel(activity, localSensitivity, options.funnel)
        ) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 阶段 4：RHS 扰动与 removal（仅短名单）。 / Stage 4: RHS perturbation and removal (shortlist only).
        val perturbationReports = ArrayList<ConstraintPerturbationReport>()
        if (!options.runPerturbation) {
            builder.noteUnavailable(
                "扰动阶段被调用方关闭 / The perturbation stage was disabled by the caller"
            )
        } else if (perturbationBackend == null) {
            builder.noteUnavailable(
                "扰动后端未提供；有效性阶段返回 Unsupported / " +
                    "No perturbation backend was supplied; the effectiveness stage reports Unsupported"
            )
        } else {
            val analyzer = ConstraintPerturbationAnalyzer(perturbationBackend)
            for (candidate in funnel.shortlist(options.candidateLimit)) {
                when (
                    val result = analyzer.analyze(
                        session = session,
                        constraintId = candidate.constraintId,
                        policy = options.perturbation
                    )
                ) {
                    is Ok -> perturbationReports += result.value!!
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            if (perturbationReports.isEmpty() && funnel.candidates.isNotEmpty()) {
                builder.noteUnavailable(
                    "短名单为空，未执行任何扰动分析 / The shortlist was empty, so no perturbation was analyzed"
                )
            }
        }
        if (perturbationReports.isNotEmpty()) {
            builder.perturbation(perturbationReports)
        }

        // 阶段 5：目标可行性与冲突。 / Stage 5: target feasibility and conflict.
        var targetReport: TargetFeasibilityReport? = null
        if (!options.runTargetAnalysis) {
            builder.noteUnavailable(
                "目标可行性阶段被调用方关闭 / The target-feasibility stage was disabled by the caller"
            )
        } else if (target == null) {
            builder.noteUnavailable(
                "未提供目标条件，阻塞性分析未执行 / No objective target was supplied, so blocking analysis did not run"
            )
        } else {
            val targetAnalyzer = TargetFeasibilityAnalyzer(solver, options.solveOptions)
            when (
                val result = targetAnalyzer.analyze(
                    session = session,
                    target = target,
                    options = options.solveOptions
                )
            ) {
                is Ok -> {
                    targetReport = result.value!!
                    builder.target(result.value!!)
                }

                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (targetReport?.status == AnalysisStatus.Unreachable) {
                val conflictAnalyzer = ConflictAnalyzer(solver, options.solveOptions)
                when (
                    val result = conflictAnalyzer.analyze(
                        session = session,
                        target = target,
                        options = options.conflict
                    )
                ) {
                    is Ok -> builder.conflict(result.value!!)
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

            builder.build(options.funnel)
        } finally {
            session.close()
        }
    }

    /**
     * 运行流水线并一次性给出最小阻塞集合的业务解释。
     *
     * 便捷入口，等价于 [analyze] 后再读取报告。
     * Convenience entry point equivalent to running [analyze] and reading the report.
     */
    suspend fun analyzeAndExplain(
        snapshot: ConstraintProgrammingModelSnapshot,
        baselineSolution: ConstraintProgrammingSolution?,
        baselineObjective: Flt64?,
        baselineProvenOptimal: Boolean,
        objectiveId: String,
        target: ObjectiveTarget? = null,
        options: CriticalConstraintAnalysisOptions = CriticalConstraintAnalysisOptions()
    ): Ret<Pair<CriticalConstraintAnalysisReport, String>> {
        return when (
            val result = analyze(
                snapshot = snapshot,
                baselineSolution = baselineSolution,
                baselineObjective = baselineObjective,
                baselineProvenOptimal = baselineProvenOptimal,
                objectiveId = objectiveId,
                target = target,
                options = options
            )
        ) {
            is Ok -> ok(result.value!! to result.value!!.blockingSummary())
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}

/**
 * 从扰动报告中抽取"最需要关注"的候选。
 *
 * 排序依据是已证明的目标改善；没有已证明改善时回退到 |对偶|。该函数只做排序，不产出任何
 * "无效"判决。
 *
 * Extract the candidates most worth attention from perturbation reports, ordered by proven
 * objective improvement and falling back to |dual|. This only sorts and never emits an
 * "ineffective" verdict.
 */
fun rankConstraintCandidates(
    ranking: EffectivenessRanking?,
    funnel: CandidateFunnelRanking?,
    limit: Int = CandidateFunnelConfig.DEFAULT_CANDIDATE_LIMIT
): List<ConstraintId> {
    if (limit <= 0) {
        return emptyList()
    }
    if (ranking != null) {
        return ranking.entries.take(limit).map { it.constraintId }
    }
    return funnel?.shortlist(limit)?.map { it.constraintId } ?: emptyList()
}
