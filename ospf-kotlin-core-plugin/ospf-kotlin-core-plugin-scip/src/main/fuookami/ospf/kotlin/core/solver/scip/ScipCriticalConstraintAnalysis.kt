/** SCIP 临界约束分析后端。 / SCIP critical-constraint analysis backends. */
package fuookami.ospf.kotlin.core.solver.scip

import kotlin.math.abs
import jscip.SCIP_ParamSetting
import fuookami.ospf.kotlin.core.analysis.AnalysisStatus
import fuookami.ospf.kotlin.core.analysis.CapabilityMatrix
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationBackend
import fuookami.ospf.kotlin.core.analysis.ConstraintProgrammingPerturbationBackend
import fuookami.ospf.kotlin.core.analysis.CriticalConstraintAnalysisPipeline
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpBackend
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpRequest
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSolveResult
import fuookami.ospf.kotlin.core.analysis.mapExplicitProvenanceDuals
import fuookami.ospf.kotlin.core.analysis.propagateBackendFailure
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 使用真实 SCIP 求解 CP 固定整数 LP，产出原始约束级对偶值。
 *
 * 对偶取法迁移自仓库内的 `ScipColumnGenerationSolver`：**自动生成对偶模型并求解对偶模型**，
 * 配合单线程配置、关闭 presolve 与 heuristics，并用强对偶关系 `Σ rhs_i · y_i == z*` 校验。
 *
 * 本环境实测（`max 3x+2y, x+y<=4 (c1), x+3y<=6 (c2)`，最优 `(4,0)`、`z*=12`，真实对偶
 * `c1=3`、`c2=0`）：
 * - **未固定整数**的降阶 LP：显式对偶模型给出 `[3.0, 0.0]`（`Σ rhs·y = 12.0`，正确）；
 *   原生 `getDual` 给出 `[2.0, 0.0]`（`Σ rhs·y = 8.0`，**错误但数值看似合理**）。
 *   因此对偶模型是权威来源，原生取法只作回退。
 * - **整数全部钉死**的降阶 LP：`fixedValues` 被降阶为变量上下界（`x=4`、`y=0`），原始行
 *   对偶非唯一。此时 SCIP 连对偶模型也解不出可用解——返回 `objective = 0.0`、
 *   取值量级 `1e20` 的哨兵解。正确行为是整体降级为 `Unsupported`，绝不把哨兵当对偶。
 *
 * The dual extraction is migrated from `ScipColumnGenerationSolver` in this repository:
 * **generate the dual model and solve it**, with a single-threaded configuration, presolve and
 * heuristics disabled, validated by the strong-duality identity `Σ rhs_i · y_i == z*`.
 *
 * Measured in this environment for `max 3x+2y, x+y<=4 (c1), x+3y<=6 (c2)` with optimum `(4,0)`,
 * `z* = 12` and true duals `c1 = 3`, `c2 = 0`:
 * - **Unfixed** lowered LP: the explicit dual model yields `[3.0, 0.0]` (`Σ rhs·y = 12.0`,
 *   correct) while native `getDual` yields `[2.0, 0.0]` (`Σ rhs·y = 8.0`, **wrong yet
 *   plausible-looking**). The dual model is therefore authoritative and native is only a fallback.
 * - **Fully pinned** lowered LP: `fixedValues` lower to variable bounds (`x = 4`, `y = 0`) and the
 *   original row duals are non-unique. SCIP then cannot solve even the dual model to a usable
 *   answer — it returns a sentinel solution with `objective = 0.0` and values of magnitude `1e20`.
 *   The correct behaviour is a whole-result degradation to `Unsupported`; a sentinel is never
 *   passed off as a dual.
 */
class ScipFixedIntegerLpBackend(
    /** SCIP solver configuration. / SCIP 求解器配置。 */
    private val config: SolverConfig = SolverConfig(),
    /** CP-to-linear lowerer. / CP 到线性模型的降阶器。 */
    private val lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer()
) : FixedIntegerLpBackend {
    override suspend fun solve(request: FixedIntegerLpRequest): Ret<FixedIntegerLpSolveResult> {
        val loweredResult = lowerer.lower(request.snapshot, fixedValues = request.fixedValues)
        if (loweredResult !is Ok) {
            return propagateBackendFailure(loweredResult)
        }
        val lowered = loweredResult.value!!
        try {
            val mechanismResult = ScipLinearSolver(config).dump(lowered.model, null, null)
            if (mechanismResult !is Ok) {
                return propagateBackendFailure(mechanismResult)
            }
            val mechanism = mechanismResult.value!!
            try {
                val triad = ScipLinearSolver(config).dump(mechanism)
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
            // 固定整数子问题按连续 LP 求解，从而存在最优基与对偶值。
            // The fixed-integer subproblem is solved as a continuous LP so that an optimal basis
            // and duals exist.
            triad.linearRelax()

            // 对偶提取必须使用**单线程**配置，这是从 `ScipColumnGenerationSolver` 迁移过来的关键一点：
            // SCIP 在并行模式下不保留可读的 LP 对偶。该适配器同时关闭 presolve 与 heuristics，
            // 并对结果做强对偶校验、不通过则改用显式对偶模型。
            //
            // Dual extraction must use a **single-threaded** configuration. This is the key point
            // migrated from `ScipColumnGenerationSolver`: SCIP does not keep readable LP duals in
            // parallel mode. That adapter also disables presolve and heuristics, validates the result
            // against strong duality, and re-solves an explicit dual model when the check fails.
            val dualConfig = config.copy(threadNum = UInt64.one)
            var nativeDuals: List<Double>? = null
            val capturing = ScipLinearSolver(
                dualConfig,
                ScipSolverCallBack()
                    .configuration { _, model, _, _ ->
                        model.setPresolving(SCIP_ParamSetting.SCIP_PARAMSETTING_OFF, true)
                        model.setHeuristics(SCIP_ParamSetting.SCIP_PARAMSETTING_OFF, true)
                        ok
                    }
                    .analyzingSolution { _, scipModel, _, constraints ->
                        nativeDuals = constraints.map { scipModel.getDual(it) }
                        ok
                    }
            )
            val report = when (val solved = capturing.solveReport(triad)) {
                is Ok -> solved.value!!
                is Failed -> return Failed(solved.error)
                is Fatal -> return Fatal(solved.errors)
            }
            val proven = report.problemStatus == ProblemStatus.Feasible &&
                report.solutionPresence == SolutionPresence.Optimal
            if (!proven) {
                return ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.from(report.problemStatus, proven),
                        objectiveValue = report.solution?.objective,
                        message = "固定整数 LP 未形成可行最优解 / Fixed-integer LP did not produce a feasible optimum"
                    )
                )
            }

            val objective = report.solution?.objective

            // 主路径：**自动生成对偶模型并求解**——这是从 `ScipColumnGenerationSolver` 迁移过来的
            // 对偶取法，也是本环境实测唯一可靠的一条。
            //
            // 为什么把原生 `getDual` 降为回退路径：在本仓库的 CP→线性降阶模型上实测，原生行对偶会
            // 给出**看似合理但错误**的值。以 `max 3x+2y, x+y<=4 (c1), x+3y<=6 (c2)` 的未固定降阶 LP
            // 为例（真实对偶 c1=3、c2=0）：
            //   - 原生 `getDual` = `[2.0, 0.0]`，`Σ rhs_i·y_i = 8.0 != 12.0` —— 数值有限、量级正常，
            //     只有强对偶校验才能识破；
            //   - 显式对偶模型 = `[3.0, 0.0]`，`Σ rhs_i·y_i = 12.0` —— 正确。
            // 因此对偶模型是权威来源；原生值只在对偶模型不可用时、且同样通过强对偶校验时才采用。
            //
            // Primary route: **generate the dual model and solve it** — the pattern migrated from
            // `ScipColumnGenerationSolver`, and the only reliable one measured in this environment.
            //
            // Why native `getDual` is demoted to a fallback: on this repository's CP-to-linear lowered
            // models it returns a **plausible but wrong** value. For the unfixed lowered LP of
            // `max 3x+2y, x+y<=4 (c1), x+3y<=6 (c2)` (true duals c1=3, c2=0):
            //   - native `getDual` = `[2.0, 0.0]`, `Σ rhs_i·y_i = 8.0 != 12.0` — finite and
            //     plausible-looking, detectable only by the strong-duality check;
            //   - explicit dual model = `[3.0, 0.0]`, `Σ rhs_i·y_i = 12.0` — correct.
            // The dual model is therefore authoritative; native values are used only when the dual
            // model is unusable and they pass the same strong-duality check.
            val generated = generatedDualModelDuals(triad, dualConfig)
            val generatedAccepted = generated != null &&
                generated.rowDuals.all { it.isFinite() && !isScipInfinity(it) } &&
                satisfiesStrongDuality(triad, generated.rowDuals, objective) &&
                agreesWithObjective(generated.objective, objective)
            val captured = nativeDuals
            val nativeAccepted = !generatedAccepted &&
                captured != null &&
                captured.size == triad.constraints.rhs.size &&
                captured.all { it.isFinite() && !isScipInfinity(it) } &&
                satisfiesStrongDuality(triad, captured, objective)

            val acceptedRowDuals = when {
                generatedAccepted -> generated!!.rowDuals
                nativeAccepted -> captured!!
                else -> null
            }
            val originalIds = request.snapshot.constraints.mapTo(linkedSetOf()) { it.id }
            val duals = if (acceptedRowDuals == null) {
                emptyMap()
            } else {
                mapOriginalDuals(
                    triad = triad,
                    lowered = lowered,
                    originalIds = originalIds,
                    dualsByOrigin = triad.constraints.indices.mapIndexedNotNull { index, _ ->
                        triad.constraints.origins.getOrNull(index)?.let { it to Flt64(acceptedRowDuals[index]) }
                    }.toMap()
                )
            }
            // 最终防线：任何非有限值或 SCIP 无穷哨兵都不得进入公开报告。上面的强对偶校验已经
            // 拦下了本环境实测出的两类坏值（原生 `2.0`、对偶模型 `1e20`），这里再逐项兜底一次
            // ——校验只针对整行向量，而回映可能改变可见子集。
            //
            // Final guard: no non-finite value and no SCIP infinity sentinel may reach a public
            // report. The strong-duality check above already rejects both bad values measured here
            // (native `2.0`, dual-model `1e20`); this re-checks every entry because verification
            // covers the whole row vector while remapping can narrow the visible subset.
            val rejected = duals.filterValues { value ->
                val raw = value.toDouble()
                !raw.isFinite() || isScipInfinity(raw)
            }
            val validatedDuals = duals.filterKeys { it !in rejected.keys }
            // 只要**有任何一条**原始约束的对偶被丢弃，整份对偶证据就不可信：部分对偶集合会让
            // 调用方把"缺少证据"误读为"该约束无效"，正是 dual=0 ≠ globally ineffective 的反面陷阱。
            // 因此整体降级为 Unsupported，而不是给出部分结果。
            //
            // If **any** original constraint's dual had to be dropped, the whole dual evidence set is
            // untrustworthy: a partial set invites reading "missing evidence" as "this constraint is
            // ineffective", the mirror image of the `dual = 0 ≠ globally ineffective` trap. The result
            // therefore degrades to `Unsupported` instead of returning a partial answer.
            val dualsUsable = rejected.isEmpty() && acceptedRowDuals != null
            return ok(
                FixedIntegerLpSolveResult(
                    // 没有通过校验的对偶时返回 Unsupported，而**不是**把 SCIP 的无穷哨兵值
                    // （`1e20`）当成真实对偶返回。这与计划 12.3-S2 对"无精确线性表示返回
                    // Unsupported"的要求同源：缺少可用证据时必须显式降级。
                    //
                    // Without a validated dual the result is `Unsupported` rather than SCIP's
                    // infinity sentinel (`1e20`) passed off as a real dual. This mirrors plan
                    // 12.3-S2's requirement to report `Unsupported` when no exact representation is
                    // available: missing evidence must degrade explicitly.
                    status = if (dualsUsable) {
                        AnalysisStatus.Reachable
                    } else {
                        AnalysisStatus.Unsupported
                    },
                    objectiveValue = objective,
                    duals = if (dualsUsable) validatedDuals else emptyMap(),
                    solveTime = report.statistics.solveTime,
                    message = when {
                        !dualsUsable ->
                            "SCIP 对偶不可用：自动生成的对偶模型与原生 getDual 均未通过强对偶校验，" +
                                "已整体降级为 Unsupported（固定整数 LP 在整数全部钉死后退化，" +
                                "对偶最优面可能无界，SCIP 会返回量级为 1e20 的哨兵解）/ " +
                                "SCIP duals are unusable: neither the auto-generated dual model nor " +
                                "native getDual passed the strong-duality check, so the whole result " +
                                "degrades to Unsupported (once every integer is pinned the fixed-integer " +
                                "LP is degenerate with a possibly unbounded dual optimal face, and SCIP " +
                                "can return a sentinel solution of magnitude 1e20)"

                        nativeAccepted ->
                            "SCIP 对偶来自原生 getDual（自动生成的对偶模型未通过强对偶校验）/ " +
                                "SCIP duals came from native getDual (the auto-generated dual model failed " +
                                "the strong-duality check)"

                        else -> null
                    }
                )
            )
        } finally {
            triad.close()
        }
    }

    /**
     * 自动生成对偶模型并求解，返回按 triad 行顺序排列的对偶值。
     *
     * 这里直接调用 `triad.dual()` 而不是复用 `solveDual(...)`：`solveDual` 通过
     * `tidyDualSolution` 只返回"有来源约束"的行，而未降阶产生辅助行时那些行的对偶值同样参与
     * 强对偶关系 `Σ rhs_i·y_i == z*`。取完整的行向量才能对整行做校验，而不是只校验一个子集。
     *
     * Generates and solves the dual model, returning duals in triad row order. This calls
     * `triad.dual()` directly rather than reusing `solveDual(...)`: `solveDual` goes through
     * `tidyDualSolution` and returns only rows that have a source constraint, yet the duals of
     * lowering-generated auxiliary rows also participate in the strong-duality identity
     * `Σ rhs_i·y_i == z*`. Only the full row vector can be verified as a whole.
     *
     * @param triad 已线性松弛的原始三元模型 / Linearly relaxed primal triad model
     * @param config 对偶模型的求解配置 / Solver configuration for the dual model
     * @return 行对偶值与对偶模型目标值；不可用时为 null / Row duals and the dual objective, or null
     */
    private suspend fun generatedDualModelDuals(
        triad: LinearTriadModel,
        config: SolverConfig
    ): GeneratedDualDuals? {
        val dualModel = triad.dual()
        try {
            val report = when (val solved = ScipLinearSolver(config).solveReport(dualModel)) {
                is Ok -> solved.value!!
                is Failed, is Fatal -> return null
            }
            val values = report.solution?.values ?: return null
            val rows = triad.constraints.size
            if (values.size < rows) {
                return null
            }
            // 对偶模型的变量按原始行索引排列（见 `LinearTriadModel.dual()`），因此前 `rows` 个
            // 取值就是逐行对偶值。
            // The dual model's variables are indexed by the primal row index (see
            // `LinearTriadModel.dual()`), so the first `rows` values are the row duals.
            return GeneratedDualDuals(
                rowDuals = values.take(rows).map { it.toDouble() },
                objective = report.solution?.objective
            )
        } finally {
            dualModel.close()
        }
    }

    /** 自动生成对偶模型的求解结果。 / Result of solving the auto-generated dual model. */
    private data class GeneratedDualDuals(
        /** 按原始行顺序排列的对偶值 / Dual values in primal row order. */
        val rowDuals: List<Double>,
        /** 对偶模型目标值 / Dual model objective value. */
        val objective: Flt64?
    )

    /**
     * 把按来源对象编索引的对偶映射回映为原始约束身份。
     *
     * 用泛型 `T` 而非 `Any`：把参数写死成 `Map<Any, Flt64>` 会让
     * [mapExplicitProvenanceDuals] 的类型参数被推成 `Any`，调用方就无法传入按具体来源类型
     * 编索引的映射（原生路径与回退路径的来源类型并不相同）。
     *
     * Remaps a dual map keyed by origin object into original constraint identities. The helper is
     * generic in `T` rather than `Any`: pinning the parameter to `Map<Any, Flt64>` would force the
     * type parameter of [mapExplicitProvenanceDuals] to `Any` and prevent callers from passing a
     * map keyed by the concrete origin type — the native and fallback paths do not share one.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> mapOriginalDuals(
        triad: LinearTriadModel,
        lowered: ConstraintProgrammingLoweredLinearModel,
        originalIds: Set<ConstraintId>,
        dualsByOrigin: Map<T, Flt64>
    ): Map<ConstraintId, Flt64> {
        return mapExplicitProvenanceDuals(
            rowProvenance = triad.constraints.indices.map {
                triad.constraints.origins.getOrNull(it)?.origin?.let(lowered.constraintProvenance::get)
            },
            // 两个调用点的 `dual*ByOrigin` 都由同一个 `origins` 列表构造，因此这里的类型断言
            // 与实际元素类型一致。 / Both call sites build their dual map from the same `origins`
            // list, so this type assertion matches the actual element type.
            rowOrigins = triad.constraints.indices.map {
                triad.constraints.origins.getOrNull(it) as? T
            },
            dualsByOrigin = dualsByOrigin,
            originalIds = originalIds
        )
    }

    private companion object {
        /** SCIP 的无穷大表示。 / SCIP's infinity representation. */
        const val SCIP_INFINITY: Double = 1e20

        /** 强对偶校验的相对容差。 / Relative tolerance for the strong-duality check. */
        const val DUAL_TOLERANCE: Double = 1e-6

        fun isScipInfinity(value: Double): Boolean = abs(value) >= SCIP_INFINITY

        /**
         * 强对偶校验：`Σ rhs_i · y_i` 必须等于最优目标值。
         *
         * 该校验只依赖对偶值与 RHS，不依赖任何后端私有状态，因此可以用来判定"这组对偶值是否
         * 可信"。校验不通过时调用方必须回退，而不是采用可能错误的数值。
         *
         * Strong-duality check: `Σ rhs_i · y_i` must equal the optimal objective. The check depends
         * only on the dual values and the RHS and not on any backend-private state, so it is a valid
         * trust test. When it fails the caller must fall back rather than accept possibly wrong
         * numbers.
         */
        fun satisfiesStrongDuality(
            triad: LinearTriadModel,
            duals: List<Double>,
            objective: Flt64?
        ): Boolean {
            if (objective == null) {
                return false
            }
            val rhs = triad.constraints.rhs
            if (rhs.size != duals.size) {
                return false
            }
            var total = 0.0
            for (index in rhs.indices) {
                total += rhs[index].toDouble() * duals[index]
            }
            val target = objective.toDouble()
            val scale = kotlin.math.max(1.0, abs(target))
            return abs(total - target) <= DUAL_TOLERANCE * scale
        }

        /**
         * 对偶模型自身的目标值必须与原始 LP 最优值一致（强对偶定理）。
         *
         * 该校验独立于行对偶值，用来识别"对偶模型根本没被正确求解"的情形：本环境实测过固定整数
         * LP 的对偶模型返回 `objective = 0.0`、取值量级 `1e20` 的哨兵解，此时行对偶同样不可信。
         *
         * The dual model's own objective must equal the primal LP optimum (strong duality). This
         * check is independent of the row dual values and detects "the dual model was not actually
         * solved correctly": for a fixed-integer LP this environment returned a sentinel solution
         * with `objective = 0.0` and values of magnitude `1e20`, in which case the row duals are
         * equally untrustworthy.
         */
        fun agreesWithObjective(dualObjective: Flt64?, primalObjective: Flt64?): Boolean {
            if (primalObjective == null) {
                return false
            }
            if (dualObjective == null) {
                // 对偶模型未报告目标值时不做额外否决：行对偶的强对偶校验已经独立成立。
                // A missing dual objective does not veto on its own: the row-dual strong-duality
                // check already holds independently.
                return true
            }
            val scale = kotlin.math.max(1.0, abs(primalObjective.toDouble()))
            return abs(dualObjective.toDouble() - primalObjective.toDouble()) <= DUAL_TOLERANCE * scale
        }
    }
}

/**
 * 使用真实 SCIP 求解 CP 固定整数 LP（便捷入口）。 / Convenience entry point for the SCIP
 * fixed-integer LP backend.
 */
fun scipFixedIntegerLpBackend(
    config: SolverConfig = SolverConfig(),
    lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer()
): FixedIntegerLpBackend {
    return ScipFixedIntegerLpBackend(config, lowerer)
}

/**
 * 使用 SCIP 的 CP 路径重新优化扰动与删除模型。
 *
 * 复用与后端无关的 [ConstraintProgrammingPerturbationBackend]：warm start 只是提示，
 * 且只有在求解形成最优性证明时才报告改善。
 *
 * Reoptimize perturbed and removal models through SCIP's CP path. This reuses the
 * backend-neutral [ConstraintProgrammingPerturbationBackend]: a warm start only affects
 * performance, and an improvement is reported only when the solve proves optimality.
 */
fun scipPerturbationBackend(
    sparseDomainLimit: Int = 128,
    decompositionLimit: Int = 256
): ConstraintPerturbationBackend {
    return ConstraintProgrammingPerturbationBackend(
        ScipConstraintProgrammingSolver(
            sparseDomainLimit = sparseDomainLimit,
            decompositionLimit = decompositionLimit
        )
    )
}

/**
 * 构建由真实 SCIP 适配器支持的完整临界约束分析流水线。
 *
 * Build the complete critical-constraint pipeline backed by real SCIP adapters.
 *
 * SCIP 原生 CP 求解器负责目标与冲突分析路径，线性 SCIP 适配器负责固定整数 LP 证据。
 * 合并后的能力矩阵记录两类能力，并显式保留不支持的 CP 特性，避免将其静默视为线性支持。
 *
 * SCIP's native CP solver supplies the target/conflict path, while the linear SCIP adapter is
 * used for fixed-integer LP evidence. The merged capability matrix records both facts and keeps
 * unsupported CP features explicit instead of silently treating them as linear support.
 */
fun scipCriticalConstraintAnalysisPipeline(
    config: SolverConfig = SolverConfig(),
    sparseDomainLimit: Int = 128,
    decompositionLimit: Int = 256
): CriticalConstraintAnalysisPipeline {
    val linearSolver = ScipLinearSolver(config)
    val cpSolver = ScipConstraintProgrammingSolver(
        sparseDomainLimit = sparseDomainLimit,
        decompositionLimit = decompositionLimit
    )
    val capabilityMatrix = CapabilityMatrix.from(cpSolver.descriptor).copy(
        modelTypes = linearSolver.descriptor.capabilities.modelTypes,
        dual = linearSolver.descriptor.capabilities.dual
    )
    return CriticalConstraintAnalysisPipeline(
        solver = cpSolver,
        lpBackend = scipFixedIntegerLpBackend(config),
        perturbationBackend = scipPerturbationBackend(sparseDomainLimit, decompositionLimit),
        capabilityMatrix = capabilityMatrix
    )
}
