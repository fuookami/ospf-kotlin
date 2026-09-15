/** RHS perturbation and removal analysis. / RHS 扰动与删除分析。 */
package fuookami.ospf.kotlin.core.analysis

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/** Perturbation operation applied to one original constraint. / 施加到一个原始约束的扰动操作。 */
enum class ConstraintPerturbationKind {
    /** Change an integer comparison RHS. / 修改整数比较约束 RHS。 */
    Rhs,

    /** Remove one original constraint from a derived snapshot. / 从派生 snapshot 删除一个原始约束。 */
    Removal
}

/** Outcome of one objective comparison. / 一次目标比较的结果。 */
enum class PerturbationOutcome {
    /** The perturbed model produced a proven objective improvement. / 扰动模型产生了已证明的目标改善。 */
    Effective,

    /** The perturbed model was proven and no objective improvement was observed. / 已证明扰动模型且未观察到目标改善。 */
    NoObservedEffect,

    /** The solve did not support an objective conclusion. / 求解未能支持目标结论。 */
    Unknown
}

/** Result classification for an adaptive perturbation sequence. / 自适应扰动序列的结果分类。 */
enum class AdaptivePerturbationOutcome {
    /** Every attempted delta had no observed effect. / 所有尝试的 delta 均无观察到效果。 */
    NoObservedEffect,

    /** The first effective delta was observed. / 观察到首次有效 delta。 */
    EffectiveAt,

    /** A lower/upper effective threshold interval was established. / 建立了有效阈值上下区间。 */
    ThresholdInterval,

    /** The budget ended before a proven conclusion was possible. / 预算耗尽且无法形成已证明结论。 */
    UnknownDueToBudget
}

/** Policy for adaptive delta growth and optional threshold refinement. / 自适应 delta 增长及可选阈值细化策略。 */
data class AdaptivePerturbationPolicy(
    /** First positive RHS delta. / 第一个正 RHS delta。 */
    val initialDelta: Flt64 = Flt64.one,
    /** Multiplicative growth factor. / 乘法增长因子。 */
    val growthFactor: Double = 2.0,
    /** Largest delta to try. / 允许尝试的最大 delta。 */
    val maxDelta: Flt64 = Flt64(16.0),
    /** Maximum number of growth attempts. / 增长阶段最多尝试次数。 */
    val maxSteps: Int = 8,
    /** Whether to refine the first effective interval. / 是否细化首次有效区间。 */
    val refineThreshold: Boolean = false,
    /** Maximum binary refinement calls. / 二分细化最多调用次数。 */
    val refinementIterations: Int = 4
) {
    init {
        require(initialDelta.isFinite() && initialDelta > Flt64.zero) {
            "Adaptive initial delta must be finite and positive"
        }
        require(growthFactor.isFinite() && growthFactor > 1.0) {
            "Adaptive growth factor must be finite and greater than one"
        }
        require(maxDelta.isFinite() && maxDelta >= initialDelta) {
            "Adaptive max delta must be finite and no smaller than initial delta"
        }
        require(maxSteps > 0)
        require(refinementIterations >= 0)
    }
}

/** Policy for one constraint's RHS and removal analysis. / 一个约束的 RHS 与删除分析策略。 */
data class ConstraintPerturbationPolicy(
    /** Explicit positive RHS deltas. / 显式正 RHS delta 列表。 */
    val deltas: List<Flt64> = listOf(Flt64.one),
    /** Optional adaptive sequence. / 可选自适应序列。 */
    val adaptive: AdaptivePerturbationPolicy? = null,
    /** Whether to run the single-constraint removal test. / 是否执行单约束删除测试。 */
    val removalTest: Boolean = false,
    /** Objective improvement threshold. / 目标改善判定阈值。 */
    val objectiveTolerance: Double = 1e-9,
    /** Maximum backend solve calls, including refinement and removal. / 后端求解调用总次数上限，包含细化和删除。 */
    val maxSolves: Int = 32,
    /** Wall-clock budget for this candidate. / 此候选的墙钟时间预算。 */
    val timeBudget: Duration? = null,
    /** Whether the previous feasible solution may be passed as a hint. / 是否可将上次可行解作为提示传入。 */
    val warmStart: Boolean = true
) {
    init {
        require(deltas.all { it.isFinite() && it > Flt64.zero }) {
            "Perturbation deltas must be finite and positive"
        }
        require(deltas.isNotEmpty() || adaptive != null || removalTest) {
            "At least one explicit, adaptive, or removal operation is required"
        }
        require(objectiveTolerance.isFinite() && objectiveTolerance >= 0.0) {
            "Objective tolerance must be finite and non-negative"
        }
        require(maxSolves > 0)
        require(timeBudget == null || timeBudget.isPositive()) {
            "Perturbation time budget must be positive"
        }
    }
}

/** Short policy alias retained for plan terminology. / 保留计划术语的简短策略别名。 */
typealias PerturbationPolicy = ConstraintPerturbationPolicy

/** Compatibility alias for callers naming the operation by its RHS. / 以 RHS 命名操作的调用方兼容别名。 */
typealias RhsPerturbationPolicy = ConstraintPerturbationPolicy

/** One derived-model solve request. / 一次派生模型求解请求。 */
data class ConstraintPerturbationRequest(
    /** Immutable baseline snapshot. / 不可变基线 snapshot。 */
    val baselineSnapshot: ConstraintProgrammingModelSnapshot,
    /** Immutable derived snapshot, or null when a continuous backend must derive it itself. / 不可变派生 snapshot；连续后端自行派生时可为空。 */
    val derivedSnapshot: ConstraintProgrammingModelSnapshot?,
    /** Candidate original constraint. / 候选原始约束。 */
    val constraintId: ConstraintId,
    /** Operation applied to the candidate. / 施加到候选上的操作。 */
    val kind: ConstraintPerturbationKind,
    /** RHS delta for an RHS request. / RHS 请求的 delta。 */
    val delta: Flt64? = null,
    /** Resulting RHS when representable. / 可表示时的结果 RHS。 */
    val perturbedRhs: Flt64? = null,
    /** Baseline incumbent, if available. / 可用时的基线 incumbent。 */
    val baselineSolution: ConstraintProgrammingSolution? = null,
    /** Previous feasible solution used as a warm start, if enabled. / 启用时用于热启动的上一次可行解。 */
    val warmStart: ConstraintProgrammingSolution? = null
) {
    /** Compatibility alias for consumers expecting a derived snapshot named snapshot. / 期望 snapshot 名称的调用方兼容别名。 */
    val snapshot: ConstraintProgrammingModelSnapshot?
        get() = derivedSnapshot

    /** Whether this request removes the candidate. / 此请求是否删除候选约束。 */
    val isRemoval: Boolean
        get() = kind == ConstraintPerturbationKind.Removal
}

/** Backend result for one perturbed model. / 一次扰动模型的后端结果。 */
data class ConstraintPerturbationSolveResult(
    /** Solve conclusion, preserving Unknown and Unsupported. / 保留 Unknown 与 Unsupported 的求解结论。 */
    val status: AnalysisStatus,
    /** New optimal objective value, when available. / 可用时的新最优目标值。 */
    val objectiveValue: Flt64? = null,
    /** New incumbent, when available. / 可用时的新 incumbent。 */
    val solution: ConstraintProgrammingSolution? = null,
    /** Backend solve duration. / 后端求解耗时。 */
    val solveTime: Duration? = null,
    /** Backend detail retained for diagnostics. / 保留用于诊断的后端详情。 */
    val message: String? = null
) {
    init {
        require(objectiveValue == null || objectiveValue.isFinite()) {
            "Perturbation objective must be finite"
        }
    }
}

/** Backend-neutral reoptimization hook. / 与后端无关的重新优化钩子。 */
fun interface ConstraintPerturbationBackend {
    /** Solve the request's immutable derived model. / 求解请求中的不可变派生模型。 */
    suspend fun solve(request: ConstraintPerturbationRequest): Ret<ConstraintPerturbationSolveResult>
}

/** One observed perturbation point. / 一个扰动观测点。 */
data class ConstraintPerturbationObservation(
    /** Candidate original constraint. / 候选原始约束。 */
    val constraintId: ConstraintId,
    /** RHS or removal operation. / RHS 或删除操作。 */
    val kind: ConstraintPerturbationKind,
    /** RHS delta, null for removal. / RHS delta，删除时为 null。 */
    val delta: Flt64? = null,
    /** Resulting RHS, null for removal or unsupported derivation. / 结果 RHS，删除或无法派生时为 null。 */
    val perturbedRhs: Flt64? = null,
    /** Backend solve status. / 后端求解状态。 */
    val status: AnalysisStatus,
    /** Baseline objective. / 基线目标值。 */
    val baselineObjective: Flt64? = null,
    /** Perturbed objective. / 扰动后的目标值。 */
    val newObjective: Flt64? = null,
    /** Improvement in the model's optimization direction. / 按模型优化方向计算的改善量。 */
    val objectiveImprovement: Flt64? = null,
    /** Whether the incumbent integer pattern changed. / incumbent 整数结构是否变化。 */
    val integerPatternChanged: Boolean? = null,
    /** Backend duration. / 后端耗时。 */
    val solveTime: Duration? = null,
    /** Distinguishes a proven no-effect from an unknown solve. / 区分已证明无效与未知求解。 */
    val outcome: PerturbationOutcome,
    /** Diagnostic detail. / 诊断详情。 */
    val message: String? = null
) {
    /** Compatibility alias for objective difference. / 目标差异兼容别名。 */
    val objectiveDifference: Flt64?
        get() = objectiveImprovement

    /** Compatibility alias for removal effectiveness. / 删除有效性兼容别名。 */
    val removalEffective: Boolean?
        get() = if (kind == ConstraintPerturbationKind.Removal) outcome == PerturbationOutcome.Effective else null
}

/** Summary of adaptive probing. / 自适应探测汇总。 */
data class AdaptivePerturbationResult(
    /** Adaptive conclusion. / 自适应结论。 */
    val outcome: AdaptivePerturbationOutcome,
    /** First effective delta, if observed. / 观察到时的首次有效 delta。 */
    val firstEffectiveDelta: Flt64? = null,
    /** Proven no-effect lower endpoint of a threshold interval. / 阈值区间已证明无效的下端点。 */
    val lowerBound: Flt64? = null,
    /** Proven effective upper endpoint of a threshold interval. / 阈值区间已证明有效的上端点。 */
    val upperBound: Flt64? = null,
    /** Number of adaptive observations. / 自适应观测数量。 */
    val attempts: Int = 0
) {
    /** Compatibility alias for threshold lower endpoint. / 阈值下界兼容别名。 */
    val thresholdLower: Flt64?
        get() = lowerBound

    /** Compatibility alias for threshold upper endpoint. / 阈值上界兼容别名。 */
    val thresholdUpper: Flt64?
        get() = upperBound
}

/** Report for one candidate constraint. / 一个候选约束的扰动报告。 */
data class ConstraintPerturbationReport(
    /** Candidate original constraint. / 候选原始约束。 */
    val constraintId: ConstraintId,
    /** Aggregate analysis status. / 聚合分析状态。 */
    val status: AnalysisStatus,
    /** Baseline objective. / 基线目标值。 */
    val baselineObjective: Flt64? = null,
    /** Explicit and adaptive RHS observations. / 显式及自适应 RHS 观测。 */
    val observations: List<ConstraintPerturbationObservation> = emptyList(),
    /** Adaptive summary, when configured. / 配置自适应策略时的汇总。 */
    val adaptive: AdaptivePerturbationResult? = null,
    /** Optional single-removal observation. / 可选单约束删除观测。 */
    val removal: ConstraintPerturbationObservation? = null,
    /** Diagnostic detail. / 诊断详情。 */
    val message: String? = null
) {
    /** Compatibility alias for RHS observations. / RHS 观测兼容别名。 */
    val perturbations: List<ConstraintPerturbationObservation>
        get() = observations

    /** Compatibility alias for point terminology. / 使用观测点术语的调用方兼容别名。 */
    val points: List<ConstraintPerturbationObservation>
        get() = observations

    /** Whether at least one point proved a global improvement. / 是否至少有一个点证明全局改善。 */
    val globallyEffective: Boolean
        get() = observations.any { it.outcome == PerturbationOutcome.Effective } ||
            removal?.outcome == PerturbationOutcome.Effective

    /** Whether the removal test completed and proved an improvement. / 删除测试是否完成且证明改善。 */
    val removalEffective: Boolean?
        get() = removal?.removalEffective
}

/**
 * Executes RHS perturbation, adaptive probing, and optional removal tests.
 * 执行 RHS 扰动、自适应探测和可选删除测试。
 */
class ConstraintPerturbationAnalyzer(
    /** Optional reoptimization backend. / 可选重新优化后端。 */
    private val backend: ConstraintPerturbationBackend? = null
) {
    /** Analyze one candidate using the session baseline and cache the report. / 使用 session 基线分析一个候选并缓存报告。 */
    suspend fun analyze(
        session: CriticalConstraintAnalysisSession,
        constraintId: ConstraintId,
        policy: ConstraintPerturbationPolicy = ConstraintPerturbationPolicy()
    ): Ret<ConstraintPerturbationReport> {
        invalidSessionResult<ConstraintPerturbationReport>(session)?.let { return it }
        if (session.isClosed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "扰动分析会话已关闭 / Perturbation analysis session is closed"
            )
        }
        val key = CacheKey(
            analyzer = this,
            constraintId = constraintId,
            solution = session.baselineSolution,
            baselineObjective = session.baselineObjectiveValue,
            policy = policy
        )
        session.cached<ConstraintPerturbationReport>(AnalysisCacheKind.Perturbation, key)?.let {
            return ok(it)
        }
        return analyze(
            snapshot = session.baselineSnapshot,
            constraintId = constraintId,
            baselineSolution = session.baselineSolution,
            baselineObjective = session.baselineObjectiveValue,
            capabilityMatrix = session.capabilityMatrix,
            policy = policy
        ).map { report ->
            session.cache(AnalysisCacheKind.Perturbation, key, report)
            report
        }
    }

    /** Analyze one candidate snapshot with an explicit baseline. / 使用显式基线分析一个候选 snapshot。 */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        constraintId: ConstraintId,
        baselineSolution: ConstraintProgrammingSolution? = null,
        baselineObjective: Flt64? = null,
        capabilityMatrix: CapabilityMatrix = CapabilityMatrix(),
        policy: ConstraintPerturbationPolicy = ConstraintPerturbationPolicy()
    ): Ret<ConstraintPerturbationReport> {
        if (!snapshot.validateIdentity() || !snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份或目标语义无效 / CP snapshot identity or objective semantics is invalid"
            )
        }
        if (snapshot.constraint(constraintId) == null) {
            return Failed(
                ErrorCode.DataNotFound,
                "扰动候选约束不存在：$constraintId / Perturbation candidate does not exist: $constraintId"
            )
        }
        if (baselineObjective != null && !baselineObjective.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "基线目标值必须有限 / Baseline objective must be finite")
        }
        val needsRhsPerturbation = policy.deltas.isNotEmpty() || policy.adaptive != null
        val rhsSupported = !needsRhsPerturbation || capabilityAllowed(
            capabilityMatrix,
            AnalysisCapability.RhsPerturbation
        )
        val removalSupported = !policy.removalTest || capabilityAllowed(
            capabilityMatrix,
            AnalysisCapability.RemovalTest
        )
        if (backend == null || !rhsSupported && !removalSupported) {
            return ok(
                ConstraintPerturbationReport(
                    constraintId = constraintId,
                    status = AnalysisStatus.Unsupported,
                    baselineObjective = baselineObjective,
                    message = "当前求解器未声明 RHS 扰动能力 / No RHS perturbation backend is declared"
                )
            )
        }

        val startedAt = System.nanoTime()
        var solveCount = 0
        var warmStart: ConstraintProgrammingSolution? = baselineSolution
        val observations = ArrayList<ConstraintPerturbationObservation>()
        val requestedDeltas = requestedDeltas(policy)
        var previousNoEffect: ConstraintPerturbationObservation? = null
        var firstEffective: ConstraintPerturbationObservation? = null
        var budgetStopped = false
        val activeBackend = backend

        suspend fun runDelta(delta: Flt64): ConstraintPerturbationObservation {
            if (!rhsSupported) {
                return unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = delta,
                    message = "当前求解器未声明 RHS 扰动能力 / Solver does not declare RHS perturbation support"
                ).copy(status = AnalysisStatus.Unsupported)
            }
            if (solveCount >= policy.maxSolves || budgetExceeded(startedAt, policy.timeBudget)) {
                budgetStopped = true
                return unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = delta,
                    message = "扰动分析预算已耗尽 / Perturbation analysis budget was exhausted"
                )
            }
            val derived = deriveRhsSnapshot(snapshot, constraintId, delta)
            if (derived is DerivedSnapshotUnsupported) {
                return unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = delta,
                    message = derived.message
                ).copy(status = AnalysisStatus.Unsupported)
            }
            val derivedSnapshot = (derived as DerivedSnapshotReady).snapshot
            val perturbedRhs = derived.newRhs
            val request = ConstraintPerturbationRequest(
                baselineSnapshot = snapshot,
                derivedSnapshot = derivedSnapshot,
                constraintId = constraintId,
                kind = ConstraintPerturbationKind.Rhs,
                delta = delta,
                perturbedRhs = perturbedRhs,
                baselineSolution = baselineSolution,
                warmStart = if (policy.warmStart) warmStart else null
            )
            solveCount += 1
            return when (val result = activeBackend!!.solve(request)) {
                is Ok -> {
                    val solved = result.value!!
                    val improvement = objectiveImprovement(snapshot.objectCategory, baselineObjective, solved.objectiveValue)
                    val outcome = classifyOutcome(solved.status, improvement, policy.objectiveTolerance)
                    if (solved.solution != null && solved.status == AnalysisStatus.Reachable) {
                        warmStart = solved.solution
                    }
                    ConstraintPerturbationObservation(
                        constraintId = constraintId,
                        kind = ConstraintPerturbationKind.Rhs,
                        delta = delta,
                        perturbedRhs = perturbedRhs,
                        status = solved.status,
                        baselineObjective = baselineObjective,
                        newObjective = solved.objectiveValue,
                        objectiveImprovement = improvement,
                        integerPatternChanged = integerPatternChanged(baselineSolution, solved.solution),
                        solveTime = solved.solveTime,
                        outcome = outcome,
                        message = solved.message
                    )
                }
                is Failed -> unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = delta,
                    message = result.error.message
                )
                is Fatal -> unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = delta,
                    message = result.errors.joinToString(separator = "; ") { it.message }
                )
            }
        }

        for (delta in requestedDeltas) {
            val observation = runDelta(delta)
            observations += observation
            if (observation.outcome == PerturbationOutcome.Effective) {
                firstEffective = observation
                break
            }
            if (observation.outcome == PerturbationOutcome.NoObservedEffect) {
                previousNoEffect = observation
            }
            if (budgetStopped) {
                break
            }
        }

        val adaptiveResult = policy.adaptive?.let { adaptivePolicy ->
            val adaptiveObservations = observations.toList()
            if (firstEffective == null && !budgetStopped) {
                var delta = adaptivePolicy.initialDelta
                repeat(adaptivePolicy.maxSteps) {
                    if (firstEffective != null || budgetStopped || delta > adaptivePolicy.maxDelta) {
                        return@repeat
                    }
                    val candidate = delta
                    if (observations.none { it.delta == candidate }) {
                        val observation = runDelta(candidate)
                        observations += observation
                        if (observation.outcome == PerturbationOutcome.Effective) {
                            firstEffective = observation
                        } else if (observation.outcome == PerturbationOutcome.NoObservedEffect) {
                            previousNoEffect = observation
                        }
                    }
                    val next = delta * Flt64(adaptivePolicy.growthFactor)
                    delta = if (next <= delta || !next.isFinite()) {
                        adaptivePolicy.maxDelta + Flt64.one
                    } else {
                        next
                    }
                }
            }
            if (firstEffective == null && observations.any { it.outcome == PerturbationOutcome.Unknown }) {
                AdaptivePerturbationResult(
                    outcome = AdaptivePerturbationOutcome.UnknownDueToBudget,
                    attempts = observations.size - adaptiveObservations.size
                )
            } else if (firstEffective == null) {
                AdaptivePerturbationResult(
                    outcome = if (budgetStopped) {
                        AdaptivePerturbationOutcome.UnknownDueToBudget
                    } else {
                        AdaptivePerturbationOutcome.NoObservedEffect
                    },
                    attempts = observations.size - adaptiveObservations.size
                )
            } else {
                val lower = previousNoEffect?.delta
                val upper = firstEffective!!.delta
                if (adaptivePolicy.refineThreshold && lower != null && upper != null) {
                    refineThreshold(
                        snapshot = snapshot,
                        constraintId = constraintId,
                        baselineSolution = baselineSolution,
                        baselineObjective = baselineObjective,
                        policy = policy,
                        adaptivePolicy = adaptivePolicy,
                        startedAt = startedAt,
                        observations = observations,
                        lower = lower,
                        upper = upper,
                        solveCounter = { solveCount++ },
                        getSolveCount = { solveCount },
                        getWarmStart = { warmStart },
                        setWarmStart = { warmStart = it }
                    )
                }
                AdaptivePerturbationResult(
                    outcome = if (lower != null) {
                        AdaptivePerturbationOutcome.ThresholdInterval
                    } else {
                        AdaptivePerturbationOutcome.EffectiveAt
                    },
                    firstEffectiveDelta = firstEffective!!.delta,
                    lowerBound = lower,
                    upperBound = firstEffective!!.delta,
                    attempts = observations.size - adaptiveObservations.size
                )
            }
        }

        val removal = if (policy.removalTest && !budgetStopped && removalSupported) {
            runRemoval(
                snapshot = snapshot,
                constraintId = constraintId,
                baselineSolution = baselineSolution,
                baselineObjective = baselineObjective,
                policy = policy,
                startedAt = startedAt,
                solveCount = { solveCount },
                incrementSolveCount = { solveCount++ },
                warmStart = warmStart
            )
        } else {
            if (policy.removalTest && !removalSupported) {
                unknownObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Removal,
                    message = "当前求解器未声明 removal test 能力 / Solver does not declare removal-test support"
                ).copy(status = AnalysisStatus.Unsupported)
            } else {
                null
            }
        }
        val allObservations = observations + listOfNotNull(removal)
        val aggregateStatus = aggregateStatus(allObservations, budgetStopped)
        return ok(
            ConstraintPerturbationReport(
                constraintId = constraintId,
                status = aggregateStatus,
                baselineObjective = baselineObjective,
                observations = observations,
                adaptive = adaptiveResult,
                removal = removal,
                message = if (budgetStopped) {
                    "扰动分析达到预算上限，未知结论保持 Unknown / Perturbation analysis reached its budget; unknown conclusions remain Unknown"
                } else {
                    null
                }
            )
        )
    }

    private suspend fun runRemoval(
        snapshot: ConstraintProgrammingModelSnapshot,
        constraintId: ConstraintId,
        baselineSolution: ConstraintProgrammingSolution?,
        baselineObjective: Flt64?,
        policy: ConstraintPerturbationPolicy,
        startedAt: Long,
        solveCount: () -> Int,
        incrementSolveCount: () -> Unit,
        warmStart: ConstraintProgrammingSolution?
    ): ConstraintPerturbationObservation {
        if (solveCount() >= policy.maxSolves || budgetExceeded(startedAt, policy.timeBudget)) {
            return unknownObservation(
                constraintId = constraintId,
                kind = ConstraintPerturbationKind.Removal,
                message = "删除测试预算已耗尽 / Removal test budget was exhausted"
            )
        }
            val derived = snapshot.copy(
            constraints = snapshot.constraints.filter { it.id != constraintId }
        )
        val request = ConstraintPerturbationRequest(
            baselineSnapshot = snapshot,
            derivedSnapshot = derived,
            constraintId = constraintId,
            kind = ConstraintPerturbationKind.Removal,
            baselineSolution = baselineSolution,
            warmStart = if (policy.warmStart) warmStart else null
        )
        incrementSolveCount()
        return when (val result = backend!!.solve(request)) {
            is Ok -> {
                val solved = result.value!!
                val improvement = objectiveImprovement(snapshot.objectCategory, baselineObjective, solved.objectiveValue)
                ConstraintPerturbationObservation(
                    constraintId = constraintId,
                    kind = ConstraintPerturbationKind.Removal,
                    status = solved.status,
                    baselineObjective = baselineObjective,
                    newObjective = solved.objectiveValue,
                    objectiveImprovement = improvement,
                    integerPatternChanged = integerPatternChanged(baselineSolution, solved.solution),
                    solveTime = solved.solveTime,
                    outcome = classifyOutcome(solved.status, improvement, policy.objectiveTolerance),
                    message = solved.message
                )
            }
            is Failed -> unknownObservation(
                constraintId = constraintId,
                kind = ConstraintPerturbationKind.Removal,
                message = result.error.message
            )
            is Fatal -> unknownObservation(
                constraintId = constraintId,
                kind = ConstraintPerturbationKind.Removal,
                message = result.errors.joinToString(separator = "; ") { it.message }
            )
        }
    }

    private suspend fun refineThreshold(
        snapshot: ConstraintProgrammingModelSnapshot,
        constraintId: ConstraintId,
        baselineSolution: ConstraintProgrammingSolution?,
        baselineObjective: Flt64?,
        policy: ConstraintPerturbationPolicy,
        adaptivePolicy: AdaptivePerturbationPolicy,
        startedAt: Long,
        observations: MutableList<ConstraintPerturbationObservation>,
        lower: Flt64,
        upper: Flt64,
        solveCounter: () -> Unit,
        getSolveCount: () -> Int,
        getWarmStart: () -> ConstraintProgrammingSolution?,
        setWarmStart: (ConstraintProgrammingSolution?) -> Unit
    ) {
        var lo = lower
        var hi = upper
        repeat(adaptivePolicy.refinementIterations) {
            if (getSolveCount() >= policy.maxSolves || budgetExceeded(startedAt, policy.timeBudget)) {
                return@repeat
            }
            val midpoint = ((lo + hi) / Flt64(2.0)).floor()
            if (midpoint <= lo || midpoint >= hi) {
                return@repeat
            }
            val delta = midpoint
            val derived = deriveRhsSnapshot(snapshot, constraintId, delta)
            if (derived !is DerivedSnapshotReady) {
                return@repeat
            }
            val request = ConstraintPerturbationRequest(
                baselineSnapshot = snapshot,
                derivedSnapshot = derived.snapshot,
                constraintId = constraintId,
                kind = ConstraintPerturbationKind.Rhs,
                delta = delta,
                perturbedRhs = derived.newRhs,
                baselineSolution = baselineSolution,
                warmStart = if (policy.warmStart) getWarmStart() else null
            )
            solveCounter()
            when (val result = backend!!.solve(request)) {
                is Ok -> {
                    val solved = result.value!!
                    val improvement = objectiveImprovement(snapshot.objectCategory, baselineObjective, solved.objectiveValue)
                    val observation = ConstraintPerturbationObservation(
                        constraintId = constraintId,
                        kind = ConstraintPerturbationKind.Rhs,
                        delta = delta,
                        perturbedRhs = derived.newRhs,
                        status = solved.status,
                        baselineObjective = baselineObjective,
                        newObjective = solved.objectiveValue,
                        objectiveImprovement = improvement,
                        integerPatternChanged = integerPatternChanged(baselineSolution, solved.solution),
                        solveTime = solved.solveTime,
                        outcome = classifyOutcome(solved.status, improvement, policy.objectiveTolerance),
                        message = solved.message
                    )
                    observations += observation
                    if (observation.outcome == PerturbationOutcome.Effective) {
                        hi = midpoint
                    } else if (observation.outcome == PerturbationOutcome.NoObservedEffect) {
                        lo = midpoint
                    } else {
                        return@repeat
                    }
                    if (solved.solution != null && solved.status == AnalysisStatus.Reachable) {
                        setWarmStart(solved.solution)
                    }
                }
                is Failed,
                is Fatal -> return@repeat
            }
        }
    }

    private fun requestedDeltas(policy: ConstraintPerturbationPolicy): List<Flt64> {
        val result = LinkedHashMap<Flt64, Flt64>()
        policy.deltas.forEach { result[it] = it }
        return result.values.toList()
    }

    private fun deriveRhsSnapshot(
        snapshot: ConstraintProgrammingModelSnapshot,
        constraintId: ConstraintId,
        delta: Flt64
    ): DerivedSnapshotResult {
        val definition = snapshot.constraint(constraintId)
            ?: return DerivedSnapshotUnsupported("扰动候选约束不存在 / Perturbation candidate does not exist")
        val comparison = definition.constraint as? ConstraintProgrammingConstraint.IntegerComparison
            ?: return DerivedSnapshotUnsupported(
                "仅支持整数比较约束的精确 RHS 派生 / Exact RHS derivation currently supports integer comparisons only"
            )
        val deltaInteger = try {
            BigDecimal.valueOf(delta.toSolverDouble("perturbation.delta")).toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }
        if (deltaInteger == null) {
            return DerivedSnapshotUnsupported(
                "CP RHS 扰动必须为整数；连续后端需自行派生模型 / CP RHS perturbations must be integral; continuous backends must derive their own model"
            )
        }
        val newRhs = try {
            BigInteger.valueOf(comparison.rhs.toLong()).add(deltaInteger).longValueExact()
        } catch (_: ArithmeticException) {
            return DerivedSnapshotUnsupported("RHS 扰动超出 Int64 范围 / Perturbed RHS exceeds Int64 range")
        }
        val updated = definition.copy(
            constraint = comparison.copy(rhs = Int64(newRhs))
        )
        return DerivedSnapshotReady(
            snapshot = snapshot.copy(
                constraints = snapshot.constraints.map { if (it.id == constraintId) updated else it }
            ),
            newRhs = Int64(newRhs).toFlt64()
        )
    }

    private fun objectiveImprovement(
        category: ObjectCategory,
        baseline: Flt64?,
        current: Flt64?
    ): Flt64? {
        if (baseline == null || current == null) {
            return null
        }
        val value = when (category) {
            ObjectCategory.Maximum -> current - baseline
            ObjectCategory.Minimum -> baseline - current
        }
        return if (value.isFinite()) value else null
    }

    private fun classifyOutcome(
        status: AnalysisStatus,
        improvement: Flt64?,
        tolerance: Double
    ): PerturbationOutcome {
        if (status != AnalysisStatus.Reachable || improvement == null) {
            return PerturbationOutcome.Unknown
        }
        return if (improvement > Flt64(tolerance)) {
            PerturbationOutcome.Effective
        } else {
            PerturbationOutcome.NoObservedEffect
        }
    }

    private fun integerPatternChanged(
        baseline: ConstraintProgrammingSolution?,
        current: ConstraintProgrammingSolution?
    ): Boolean? {
        if (baseline == null || current == null) {
            return null
        }
        return baseline.values != current.values
    }

    private fun aggregateStatus(
        observations: List<ConstraintPerturbationObservation>,
        budgetStopped: Boolean
    ): AnalysisStatus {
        if (observations.isEmpty()) {
            return if (budgetStopped) AnalysisStatus.Unknown else AnalysisStatus.Unsupported
        }
        if (observations.any { it.status == AnalysisStatus.Unsupported }) {
            return if (observations.any { it.status == AnalysisStatus.Reachable }) {
                AnalysisStatus.Unknown
            } else {
                AnalysisStatus.Unsupported
            }
        }
        if (observations.any { it.status == AnalysisStatus.Unknown }) {
            return AnalysisStatus.Unknown
        }
        if (observations.any { it.status == AnalysisStatus.Reachable }) {
            return AnalysisStatus.Reachable
        }
        return AnalysisStatus.Unreachable
    }

    private fun capabilityAllowed(
        matrix: CapabilityMatrix,
        capability: AnalysisCapability
    ): Boolean {
        val explicit = matrix.analysisCapabilities[capability]
        if (explicit != null) {
            return explicit != CapabilitySupport.Unsupported
        }
        return matrix.modelTypes.isEmpty() || matrix.support(capability) != CapabilitySupport.Unsupported
    }

    private fun unknownObservation(
        constraintId: ConstraintId,
        kind: ConstraintPerturbationKind,
        delta: Flt64? = null,
        message: String
    ): ConstraintPerturbationObservation {
        return ConstraintPerturbationObservation(
            constraintId = constraintId,
            kind = kind,
            delta = delta,
            status = AnalysisStatus.Unknown,
            outcome = PerturbationOutcome.Unknown,
            message = message
        )
    }

    private fun budgetExceeded(startedAt: Long, budget: Duration?): Boolean {
        return budget != null && System.nanoTime() - startedAt >= budget.inWholeNanoseconds
    }

    private sealed interface DerivedSnapshotResult

    private data class DerivedSnapshotReady(
        val snapshot: ConstraintProgrammingModelSnapshot,
        val newRhs: Flt64
    ) : DerivedSnapshotResult

    private data class DerivedSnapshotUnsupported(val message: String) : DerivedSnapshotResult

    private data class CacheKey(
        val analyzer: ConstraintPerturbationAnalyzer,
        val constraintId: ConstraintId,
        val solution: ConstraintProgrammingSolution?,
        val baselineObjective: Flt64?,
        val policy: ConstraintPerturbationPolicy
    )
}
