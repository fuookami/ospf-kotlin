/** Fixed-integer LP sensitivity protocol. / 固定整数 LP 局部有效性协议。 */
package fuookami.ospf.kotlin.core.analysis

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/** Scope of a sensitivity value. / 敏感性值的作用域。 */
enum class LocalSensitivityScope {
    /** Integer variables are fixed to one incumbent pattern. / 整数变量固定为一个 incumbent 结构。 */
    FixedIntegerIncumbent
}

/** Compatibility alias matching the explicit scope wording in the plan. / 与计划明确作用域术语兼容的别名。 */
typealias FixedIntegerIncumbentScope = LocalSensitivityScope

/** A finite RHS range reported by an LP backend, when available. / LP 后端可用时返回的有限 RHS 敏感性范围。 */
data class SensitivityRange(
    /** Lower end of the valid range, if known. / 已知时的范围下界。 */
    val lower: Flt64? = null,
    /** Upper end of the valid range, if known. / 已知时的范围上界。 */
    val upper: Flt64? = null
) {
    init {
        require(lower == null || lower.isFinite()) {
            "Sensitivity range lower bound must be finite"
        }
        require(upper == null || upper.isFinite()) {
            "Sensitivity range upper bound must be finite"
        }
        require(lower == null || upper == null || lower <= upper) {
            "Sensitivity range lower bound must not exceed upper bound"
        }
    }
}

/** Local sensitivity of one original CP constraint. / 单个原始 CP 约束的局部敏感性。 */
data class LocalConstraintSensitivity(
    /** Original stable constraint ID. / 原始稳定约束 ID。 */
    val constraintId: ConstraintId,
    /** LP dual or shadow price under this fixed integer pattern. / 固定整数结构下的 LP 对偶值。 */
    val dualValue: Flt64? = null,
    /** Activity evidence for the same baseline constraint. / 同一基线约束的活动性证据。 */
    val activity: ConstraintActivity? = null,
    /** Whether the local dual exceeds the configured threshold. / 局部对偶值是否超过配置阈值。 */
    val localEffective: Boolean? = null,
    /** Optional valid RHS sensitivity range. / 可选的有效 RHS 敏感性范围。 */
    val sensitivityRange: SensitivityRange? = null,
    /** Scope label; this is never a global MILP shadow price. / 作用域标签；绝不能解释为 MILP 全局影子价格。 */
    val scope: LocalSensitivityScope = LocalSensitivityScope.FixedIntegerIncumbent
) {
    /** Short alias used by adapters. / 适配器使用的简短别名。 */
    val dual: Flt64?
        get() = dualValue

    /** Short alias for callers that use effectiveness terminology. / 使用有效性术语的调用方别名。 */
    val effective: Boolean?
        get() = localEffective
}

/** Result returned by a fixed-integer LP backend. / 固定整数 LP 后端返回的结果。 */
data class FixedIntegerLpSolveResult(
    /** Solver conclusion for the derived LP. / 派生 LP 的求解结论。 */
    val status: AnalysisStatus,
    /** Derived LP objective value, when a solution exists. / 存在解时的派生 LP 目标值。 */
    val objectiveValue: Flt64? = null,
    /** Duals keyed only by original stable constraint ID. / 仅按原始稳定约束 ID 编索引的对偶值。 */
    val duals: Map<ConstraintId, Flt64?> = emptyMap(),
    /** Optional ranges keyed only by original stable constraint ID. / 仅按原始稳定约束 ID 编索引的可选范围。 */
    val sensitivityRanges: Map<ConstraintId, SensitivityRange?> = emptyMap(),
    /** Backend solve duration. / 后端求解耗时。 */
    val solveTime: Duration? = null,
    /** Backend detail retained for diagnostics. / 保留用于诊断的后端详情。 */
    val message: String? = null
) {
    init {
        require(objectiveValue == null || objectiveValue.isFinite()) {
            "Fixed-integer LP objective must be finite"
        }
        require(duals.all { (_, value) -> value == null || value.isFinite() }) {
            "Fixed-integer LP dual values must be finite"
        }
    }

    /** Compatibility alias for adapters using dualValues. / 使用 dualValues 的适配器兼容别名。 */
    val dualValues: Map<ConstraintId, Flt64?>
        get() = duals
}

/** Fixed-integer LP backend request. / 固定整数 LP 后端请求。 */
data class FixedIntegerLpRequest(
    /** Immutable source snapshot. / 不可变源 snapshot。 */
    val snapshot: ConstraintProgrammingModelSnapshot,
    /** Values fixed for every original integer variable. / 为每个原始整数变量固定的值。 */
    val fixedValues: Map<VariableId, Int64>,
    /** Baseline solution used as a warm-start/evidence source. / 用作热启动和证据来源的基线解。 */
    val baselineSolution: ConstraintProgrammingSolution,
    /** Analyzer options. / 分析器选项。 */
    val options: FixedIntegerLpSensitivityOptions
)

/** Backend-neutral fixed-integer LP solver hook. / 与后端无关的固定整数 LP 求解钩子。 */
fun interface FixedIntegerLpBackend {
    /** Solve one fixed-integer derived LP. / 求解一个固定整数的派生 LP。 */
    suspend fun solve(request: FixedIntegerLpRequest): Ret<FixedIntegerLpSolveResult>
}

/** Options for fixed-integer LP analysis. / 固定整数 LP 分析选项。 */
data class FixedIntegerLpSensitivityOptions(
    /** Absolute threshold for classifying a non-zero dual. / 判定非零对偶值的绝对阈值。 */
    val dualTolerance: Double = DEFAULT_DUAL_TOLERANCE
) {
    init {
        require(dualTolerance.isFinite() && dualTolerance >= 0.0) {
            "Dual tolerance must be finite and non-negative"
        }
    }

    companion object {
        /** Default local-effectiveness threshold. / 默认局部有效性阈值。 */
        const val DEFAULT_DUAL_TOLERANCE: Double = 1e-9
    }
}

/** Fixed-integer LP sensitivity report. / 固定整数 LP 敏感性报告。 */
data class FixedIntegerLpSensitivityReport(
    /** Analysis conclusion; Unsupported and Unknown remain distinct. / 分析结论，Unsupported 与 Unknown 保持区分。 */
    val status: AnalysisStatus,
    /** Explicit scope of all dual values in this report. / 报告中所有对偶值的明确作用域。 */
    val scope: LocalSensitivityScope = LocalSensitivityScope.FixedIntegerIncumbent,
    /** Baseline objective supplied by the caller. / 调用方提供的基线目标值。 */
    val baselineObjective: Flt64? = null,
    /** Fixed incumbent values. / 固定的 incumbent 值。 */
    val fixedValues: Map<VariableId, Int64> = emptyMap(),
    /** Original constraint sensitivities in snapshot order. / 按 snapshot 顺序排列的原始约束敏感性。 */
    val sensitivities: List<LocalConstraintSensitivity> = emptyList(),
    /** Derived LP objective, if available. / 可用时的派生 LP 目标值。 */
    val objectiveValue: Flt64? = null,
    /** Derived LP solve duration. / 派生 LP 求解耗时。 */
    val solveTime: Duration? = null,
    /** Diagnostics from dispatch or backend. / 调度或后端诊断详情。 */
    val message: String? = null
) {
    /** Compatibility alias for local sensitivity terminology. / 局部敏感性术语兼容别名。 */
    val localSensitivities: List<LocalConstraintSensitivity>
        get() = sensitivities

    /** Find one original constraint's sensitivity. / 查找一个原始约束的敏感性。 */
    fun constraint(id: ConstraintId): LocalConstraintSensitivity? {
        return sensitivities.firstOrNull { it.constraintId == id }
    }
}

/** Compatibility alias for callers using the shorter local-report name. / 使用较短局部报告名称的调用方兼容别名。 */
typealias LocalConstraintSensitivityReport = FixedIntegerLpSensitivityReport

/**
 * Builds an exact fixed-integer LP through the existing CP lowerer and delegates
 * backend-specific dumping, LP relaxation, and dual extraction.
 * 使用现有 CP lowerer 构造精确固定整数 LP，并将转储、LP 松弛和对偶提取委托给后端。
 */
class LoweredFixedIntegerLpBackend(
    /** CP-to-linear lowerer. / CP 到线性模型的降阶器。 */
    private val lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer(),
    /** Backend operation on one lowered model. / 针对一个降阶模型的后端操作。 */
    private val delegate: LoweredFixedIntegerLpSolver
) : FixedIntegerLpBackend {
    override suspend fun solve(request: FixedIntegerLpRequest): Ret<FixedIntegerLpSolveResult> {
        return when (val lowered = lowerer.lower(request.snapshot, fixedValues = request.fixedValues)) {
            is Ok -> {
                try {
                    delegate.solve(lowered.value!!, request)
                } finally {
                    lowered.value!!.close()
                }
            }
            is Failed -> Failed(lowered.error)
            is Fatal -> Fatal(lowered.errors)
        }
    }
}

/** Operation on a lowered fixed-integer model. / 针对降阶固定整数模型的操作。 */
fun interface LoweredFixedIntegerLpSolver {
    /** Solve and extract original-ID keyed duals. / 求解并提取按原始 ID 编索引的对偶值。 */
    suspend fun solve(
        loweredModel: ConstraintProgrammingLoweredLinearModel,
        request: FixedIntegerLpRequest
    ): Ret<FixedIntegerLpSolveResult>
}

/**
 * Adapter using an existing [LinearSolver] for CP fixed-integer LPs.
 * 使用现有 [LinearSolver] 求解 CP 固定整数 LP 的适配器。
 *
 * Root integer-comparison rows are mapped by their stable CP ID. Lowering
 * helper rows and rows without a unique original source are intentionally
 * omitted from the returned dual map.
 * 根整数比较行按稳定 CP ID 映射；降阶辅助行及无法唯一归属原始来源的行不会进入返回的对偶映射。
 */
class LinearSolverFixedIntegerLpBackend(
    /** Existing linear backend. / 现有线性后端。 */
    private val linearSolver: LinearSolver,
    /** CP-to-linear lowerer. / CP 到线性模型的降阶器。 */
    private val lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer(),
    /**
     * 可选的 RHS 敏感性范围提供者，按**降阶行索引**返回范围。
     *
     * 只有具备原生 ranging 能力的后端才提供该钩子（目前是 Gurobi，需关闭 presolve 后读取
     * `SARHSLow`/`SARHSUp`）。未提供时报告中的 `sensitivityRange` 保持为空——按计划红线，
     * 宁缺勿造。
     *
     * Optional RHS sensitivity-range provider keyed by **lowered row index**. Only backends with a
     * native ranging capability supply it (currently Gurobi, which must disable presolve before
     * reading `SARHSLow`/`SARHSUp`). When absent, `sensitivityRange` stays empty in the report: per
     * the plan's rule, omitting is preferred over fabricating.
     */
    private val sensitivityRanges: (suspend (LinearTriadModel) -> Map<Int, SensitivityRange>)? = null,
    /**
     * 求对偶时使用的求解器；默认为 [linearSolver]。
     *
     * 需要与主求解分开，是因为原生 ranging 要求关闭 presolve，而关闭 presolve 会改变退化模型上
     * 求得的对偶解（实测同一模型的对偶从正确的 `3.0` 变成 `0.0`）。对偶正确性优先于范围证据，
     * 因此主求解可以带 ranging 配置，对偶求解必须使用未加该配置的求解器。
     *
     * Solver used for the dual solve; defaults to [linearSolver]. It must be separable because native
     * ranging requires presolve to be disabled, and disabling presolve changes the dual solution found
     * on a degenerate model (measured: the same model's dual moves from a correct `3.0` to `0.0`). Dual
     * correctness takes priority over range evidence, so the primary solve may carry ranging
     * configuration while the dual solve uses a solver without it.
     */
    private val dualSolver: LinearSolver = linearSolver
) : FixedIntegerLpBackend {
    override suspend fun solve(request: FixedIntegerLpRequest): Ret<FixedIntegerLpSolveResult> {
        val loweredResult = lowerer.lower(request.snapshot, fixedValues = request.fixedValues)
        if (loweredResult !is Ok) {
            return when (loweredResult) {
                is Failed -> Failed(loweredResult.error)
                is Fatal -> Fatal(loweredResult.errors)
                is Ok -> Failed(
                    ErrorCode.ApplicationError,
                    "CP lowerer returned an invalid result state / CP lowerer returned an invalid result state"
                )
            }
        }
        val lowered = loweredResult.value!!
        try {
            val mechanismResult = linearSolver.dump(lowered.model, null, null)
            if (mechanismResult !is Ok) {
                return when (mechanismResult) {
                    is Failed -> Failed(mechanismResult.error)
                    is Fatal -> Fatal(mechanismResult.errors)
                    is Ok -> Failed(
                        ErrorCode.ApplicationError,
                        "Linear dump returned an invalid result state / Linear dump returned an invalid result state"
                    )
                }
            }
            val mechanism = mechanismResult.value!!
            try {
                val triad = linearSolver.dump(mechanism)
                return solveTriad(triad, request, lowered)
            } finally {
                mechanism.close()
            }
        } finally {
            lowered.close()
        }
    }

    private suspend fun solveTriad(
        triad: LinearTriadModel,
        request: FixedIntegerLpRequest,
        lowered: ConstraintProgrammingLoweredLinearModel
    ): Ret<FixedIntegerLpSolveResult> {
        try {
            triad.linearRelax()
            val report = when (val solved = linearSolver.solveReport(triad)) {
                is Ok -> solved.value!!
                is Failed -> return Failed(solved.error)
                is Fatal -> return Fatal(solved.errors)
            }
            // 证明门控：只有真正形成最优解时才升格为 Reachable；受限求解返回的 incumbent
            // 必须保持 Unknown，不得被读成"已证明最优"（计划 8.14 / 3.5）。
            // Proof gating: only a genuinely optimal solve upgrades to `Reachable`; an incumbent
            // from a budget-limited solve stays `Unknown` and is never read as a proven optimum.
            val proven = report.problemStatus == ProblemStatus.Feasible &&
                report.solutionPresence == SolutionPresence.Optimal
            val status = AnalysisStatus.from(report.problemStatus, proven)
            if (status != AnalysisStatus.Reachable) {
                return ok(
                    FixedIntegerLpSolveResult(
                        status = status,
                        objectiveValue = report.solution?.objective,
                        message = "固定整数 LP 未形成可行最优解 / Fixed-integer LP did not produce a feasible optimum"
                    )
                )
            }
            val duals = when (val dualResult = fuookami.ospf.kotlin.core.model.intermediate.solveDual(triad, dualSolver)) {
                is Ok -> mapOriginalDuals(request.snapshot, triad, lowered, dualResult.value!!)
                is Failed -> return ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.Unsupported,
                        objectiveValue = report.solution?.objective,
                        message = "LP 对偶求解失败，无法形成局部有效性证据：${dualResult.error.message} / " +
                            "LP dual solve failed; local-sensitivity evidence is unsupported: ${dualResult.error.message}"
                    )
                )
                is Fatal -> return ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.Unsupported,
                        objectiveValue = report.solution?.objective,
                        message = "LP 对偶求解失败，无法形成局部有效性证据：" +
                            dualResult.errors.joinToString(separator = "; ") { it.message } +
                            " / LP dual solve failed; local-sensitivity evidence is unsupported"
                    )
                )
            }
            if (request.snapshot.constraints.isNotEmpty() && duals.isEmpty()) {
                return ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.Unsupported,
                        objectiveValue = report.solution?.objective,
                        message = "LP 后端未返回可映射的原始约束对偶值 / " +
                            "LP backend returned no mappable original-constraint duals"
                    )
                )
            }
            // 后端提供原生 ranging 时映射回原始约束身份；只有"唯一降阶行"才回映，
            // 多行 lowering 不冒充单约束范围。 / When the backend supplies native ranging, map it
            // back onto original constraint identities; only a unique lowered row is mapped, so a
            // multi-row lowering never impersonates a single constraint's range.
            val ranges = sensitivityRanges?.let { provider ->
                val byRow = provider(triad)
                if (byRow.isEmpty()) {
                    emptyMap()
                } else {
                    val origins = triad.constraints.indices.map {
                        triad.constraints.origins.getOrNull(it)
                    }
                    val byOrigin = byRow.mapNotNull { (row, range) ->
                        origins.getOrNull(row)?.let { it to range }
                    }.toMap()
                    mapExplicitProvenanceRanges(
                        rowProvenance = triad.constraints.indices.map {
                            triad.constraints.origins.getOrNull(it)?.origin
                                ?.let(lowered.constraintProvenance::get)
                        },
                        rowOrigins = origins,
                        rangesByOrigin = byOrigin,
                        originalIds = request.snapshot.constraints.mapTo(linkedSetOf()) { it.id }
                    )
                }
            } ?: emptyMap()
            return ok(
                FixedIntegerLpSolveResult(
                    status = status,
                    objectiveValue = report.solution?.objective,
                    duals = duals,
                    sensitivityRanges = ranges,
                    message = null
                )
            )
        } finally {
            triad.close()
        }
    }

    private fun mapOriginalDuals(
        snapshot: ConstraintProgrammingModelSnapshot,
        triad: LinearTriadModel,
        lowered: ConstraintProgrammingLoweredLinearModel,
        duals: Map<Constraint<Flt64, Linear>, Flt64>
    ): Map<ConstraintId, Flt64> {
        return mapExplicitProvenanceDuals(
            rowProvenance = triad.constraints.indices.map {
                triad.constraints.origins.getOrNull(it)?.origin?.let(lowered.constraintProvenance::get)
            },
            rowOrigins = triad.constraints.indices.map {
                triad.constraints.origins.getOrNull(it)
            },
            dualsByOrigin = duals,
            originalIds = snapshot.constraints.mapTo(linkedSetOf()) { it.id }
        )
    }
}

/**
 * Map dual values through explicit row provenance rather than display names.
 * 通过显式行 provenance 映射对偶值，不依赖显示名称。
 *
 * A source ID is returned only when exactly one lowered row carries that ID.
 * A global CP constraint may lower to several rows, in which case no single
 * row dual can be advertised as the original constraint dual.
 * 只有唯一降阶行携带来源 ID 时才返回该来源；一个 CP 全局约束若降为多行，
 * 则不能把任意单行对偶冒充为原始约束对偶。
 *
 * 该函数与 [mapExplicitProvenanceRanges] 是后端适配器的公共映射边界：后端插件负责取得
 * 原生对偶与范围，但**必须**经由这里回映为原始约束身份，不得直接暴露行索引。
 * This function and [mapExplicitProvenanceRanges] form the public mapping boundary for backend
 * adapters: a backend plugin obtains native duals and ranges but **must** remap them through here
 * into original constraint identities, never exposing row indices.
 */
fun <T : Any> mapExplicitProvenanceDuals(
    rowProvenance: List<ConstraintId?>,
    rowOrigins: List<T?>,
    dualsByOrigin: Map<T, Flt64>,
    originalIds: Set<ConstraintId>
): Map<ConstraintId, Flt64> {
    val rowsBySource = rowProvenance.indices.mapNotNull { index ->
        val source = rowProvenance[index]
        val origin = rowOrigins.getOrNull(index)
        if (source == null || origin == null || source !in originalIds) {
            null
        } else {
            source to origin
        }
    }.groupBy({ it.first }, { it.second })
    return rowsBySource.mapNotNull { (source, origins) ->
        if (origins.size != 1) {
            null
        } else {
            dualsByOrigin[origins.single()]?.let { source to it }
        }
    }.toMap()
}

/**
 * Map RHS sensitivity ranges through explicit row provenance.
 * 通过显式行 provenance 映射 RHS 敏感性范围。
 *
 * 与对偶映射遵守同一条规则：只有**唯一**降阶行携带来源 ID 时才返回该来源的范围，
 * 否则宁可缺省也不把多行中的任意一行范围冒充原始约束的范围。
 *
 * This follows the same rule as dual mapping: a source range is returned only when exactly one
 * lowered row carries that source ID. Otherwise the range is omitted rather than passing off one
 * row's range as the original constraint's range.
 */
fun <T : Any> mapExplicitProvenanceRanges(    rowProvenance: List<ConstraintId?>,
    rowOrigins: List<T?>,
    rangesByOrigin: Map<T, SensitivityRange>,
    originalIds: Set<ConstraintId>
): Map<ConstraintId, SensitivityRange> {
    val rowsBySource = rowProvenance.indices.mapNotNull { index ->
        val source = rowProvenance[index]
        val origin = rowOrigins.getOrNull(index)
        if (source == null || origin == null || source !in originalIds) {
            null
        } else {
            source to origin
        }
    }.groupBy({ it.first }, { it.second })
    return rowsBySource.mapNotNull { (source, origins) ->
        if (origins.size != 1) {
            null
        } else {
            rangesByOrigin[origins.single()]?.let { source to it }
        }
    }.toMap()
}

/**
 * Coordinates fixed-integer LP analysis and keeps derived results in the
 * session cache. It never exposes lowered rows or auxiliary columns.
 * 协调固定整数 LP 分析并将派生结果缓存到 session；不会暴露降阶行或辅助列。
 */
class FixedIntegerLpSensitivityAnalyzer(
    /** Optional backend adapter. / 可选后端适配器。 */
    private val backend: FixedIntegerLpBackend? = null,
    /** Activity analyzer reused for the same baseline. / 复用同一基线的活动性分析器。 */
    private val activityAnalyzer: ConstraintActivityAnalyzer = ConstraintActivityAnalyzer()
) {
    /** Analyze the baseline attached to a session. / 分析 session 附带的基线。 */
    suspend fun analyze(
        session: CriticalConstraintAnalysisSession,
        options: FixedIntegerLpSensitivityOptions = FixedIntegerLpSensitivityOptions()
    ): Ret<FixedIntegerLpSensitivityReport> {
        invalidSessionResult<FixedIntegerLpSensitivityReport>(session)?.let { return it }
        if (session.isClosed) {
            return Failed(
                ErrorCode.ApplicationStopped,
                "固定整数 LP 分析会话已关闭 / Fixed-integer LP analysis session is closed"
            )
        }
        val solution = session.baselineSolution
        val key = CacheKey(
            analyzer = this,
            solution = solution,
            baselineObjective = session.baselineObjectiveValue,
            options = options
        )
        session.cached<FixedIntegerLpSensitivityReport>(AnalysisCacheKind.FixedIntegerLp, key)?.let {
            return ok(it)
        }
        return analyze(
            snapshot = session.baselineSnapshot,
            solution = solution,
            baselineObjective = session.baselineObjectiveValue,
            capabilityMatrix = session.capabilityMatrix,
            options = options
        ).map { report ->
            session.cache(AnalysisCacheKind.FixedIntegerLp, key, report)
            report
        }
    }

    /** Analyze a snapshot using an explicit baseline solution. / 使用显式基线解分析 snapshot。 */
    suspend fun analyze(
        snapshot: ConstraintProgrammingModelSnapshot,
        solution: ConstraintProgrammingSolution?,
        baselineObjective: Flt64? = null,
        capabilityMatrix: CapabilityMatrix = CapabilityMatrix(),
        options: FixedIntegerLpSensitivityOptions = FixedIntegerLpSensitivityOptions()
    ): Ret<FixedIntegerLpSensitivityReport> {
        if (!snapshot.validateIdentity() || !snapshot.validateObjectiveSemantics()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP snapshot 身份或目标语义无效 / CP snapshot identity or objective semantics is invalid"
            )
        }
        if (baselineObjective != null && !baselineObjective.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "基线目标值必须有限 / Baseline objective must be finite")
        }
        val activity = activityAnalyzer.analyze(snapshot, solution)
        val activityReport = when (activity) {
            is Ok -> activity.value
            is Failed -> null
            is Fatal -> null
        }
        val allActivities = snapshot.constraints.map { constraint ->
            activityReport?.constraint(constraint.id)
        }
        val baseSensitivities = snapshot.constraints.mapIndexed { index, constraint ->
            LocalConstraintSensitivity(
                constraintId = constraint.id,
                activity = allActivities[index]
            )
        }
        val fixedValuesResult = fixedValues(snapshot, solution)
        if (fixedValuesResult is FixedValuesFailure) {
            return ok(
                FixedIntegerLpSensitivityReport(
                    status = AnalysisStatus.Unknown,
                    baselineObjective = baselineObjective,
                    sensitivities = baseSensitivities,
                    message = fixedValuesResult.message
                )
            )
        }
        val fixedValues = (fixedValuesResult as FixedValuesSuccess).values
        if (backend == null || !capabilityAllowed(capabilityMatrix, AnalysisCapability.FixedIntegerLpSensitivity)) {
            return ok(
                FixedIntegerLpSensitivityReport(
                    status = AnalysisStatus.Unsupported,
                    baselineObjective = baselineObjective,
                    fixedValues = fixedValues,
                    sensitivities = baseSensitivities,
                    message = "当前求解器未声明固定整数 LP 对偶能力 / No fixed-integer LP dual backend is declared"
                )
            )
        }
        val request = FixedIntegerLpRequest(
            snapshot = snapshot,
            fixedValues = fixedValues,
            baselineSolution = solution!!,
            options = options
        )
        return when (val result = backend.solve(request)) {
            is Ok -> {
                val solved = result.value!!
                val sensitivities = snapshot.constraints.mapIndexed { index, constraint ->
                    val dual = solved.duals[constraint.id]
                    LocalConstraintSensitivity(
                        constraintId = constraint.id,
                        dualValue = dual,
                        activity = allActivities[index],
                        localEffective = dual?.let {
                            kotlin.math.abs(it.toSolverDouble("fixedIntegerLp.dualValue")) > options.dualTolerance
                        },
                        sensitivityRange = solved.sensitivityRanges[constraint.id],
                        scope = LocalSensitivityScope.FixedIntegerIncumbent
                    )
                }
                ok(
                    FixedIntegerLpSensitivityReport(
                        status = solved.status,
                        baselineObjective = baselineObjective,
                        fixedValues = fixedValues,
                        sensitivities = sensitivities,
                        objectiveValue = solved.objectiveValue,
                        solveTime = solved.solveTime,
                        message = solved.message
                    )
                )
            }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    private fun fixedValues(
        snapshot: ConstraintProgrammingModelSnapshot,
        solution: ConstraintProgrammingSolution?
    ): FixedValuesResult {
        if (solution == null) {
            return FixedValuesFailure("缺少 baseline CP 解 / Baseline CP solution is missing")
        }
        val unknown = solution.values.keys.firstOrNull { snapshot.variable(it) == null }
        if (unknown != null) {
            return FixedValuesFailure("baseline CP 解包含未知变量：$unknown / Baseline CP solution contains an unknown variable: $unknown")
        }
        val missing = snapshot.variables.firstOrNull { it.id !in solution.values }
        if (missing != null) {
            return FixedValuesFailure("缺少整数变量赋值：${missing.id} / Missing integer assignment: ${missing.id}")
        }
        val outside = snapshot.variables.firstOrNull { definition ->
            !definition.domain.contains(solution.values[definition.id]!!)
        }
        if (outside != null) {
            return FixedValuesFailure("整数变量赋值超出值域：${outside.id} / Integer assignment is outside its domain: ${outside.id}")
        }
        return FixedValuesSuccess(snapshot.variables.associate { it.id to solution.values.getValue(it.id) })
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

    private sealed interface FixedValuesResult

    private data class FixedValuesSuccess(val values: Map<VariableId, Int64>) : FixedValuesResult

    private data class FixedValuesFailure(val message: String) : FixedValuesResult

    private data class CacheKey(
        val analyzer: FixedIntegerLpSensitivityAnalyzer,
        val solution: ConstraintProgrammingSolution?,
        val baselineObjective: Flt64?,
        val options: FixedIntegerLpSensitivityOptions
    )
}
