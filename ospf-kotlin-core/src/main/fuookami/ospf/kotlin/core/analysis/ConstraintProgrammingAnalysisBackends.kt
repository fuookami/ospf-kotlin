/** 基于现有 CP 求解器 SPI 的分析后端实现。 / Analysis backends implemented on the existing CP solver SPI. */
package fuookami.ospf.kotlin.core.analysis

import kotlin.time.Duration
import kotlin.time.TimeSource
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingUnknownOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingInfeasibleOutput
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions

/**
 * 通用 CP 扰动后端。
 *
 * 该实现直接复用现有 [ConstraintProgrammingSolver] SPI，因此对任何后端都成立：它把分析器已经
 * 精确派生好的 snapshot（RHS 扰动或约束移除）忠实重建为 CP 模型并求解，再把结果映射回
 * 与后端无关的 [ConstraintPerturbationSolveResult]。
 *
 * 两条语义红线：
 * 1. **warm start 只影响性能，不影响结论**：提示值只是提示，最终结论必须来自本次求解的证明；
 * 2. **证明门控**：只有 `ProofStatus.Verified` 或明确的 `Optimal` 才升格为 `Reachable`，
 *    否则一律 `Unknown`，绝不把"受限求解拿到 incumbent"当成已证明最优。
 *
 * Generic CP perturbation backend. It reuses the existing [ConstraintProgrammingSolver] SPI and
 * therefore works for every backend: the analyzer has already derived an exact snapshot (perturbed
 * RHS or removed constraint), which this rebuilds faithfully, solves, and maps back to the
 * backend-neutral [ConstraintPerturbationSolveResult].
 *
 * Two semantic invariants:
 * 1. **Warm start affects performance only, never the conclusion**: a hint is only a hint, and the
 *    final conclusion must come from this solve's proof;
 * 2. **Proof gating**: only `ProofStatus.Verified` or an explicit `Optimal` upgrades to
 *    `Reachable`; anything else stays `Unknown`, so an incumbent from a budget-limited solve is
 *    never reported as a proven optimum.
 */
class ConstraintProgrammingPerturbationBackend(
    /** Underlying CP solver. / 底层 CP 求解器。 */
    private val solver: ConstraintProgrammingSolver,
    /** Per-solve options forwarded to the backend. / 转发给后端的单次求解选项。 */
    private val solveOptions: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
) : ConstraintPerturbationBackend {
    override suspend fun solve(request: ConstraintPerturbationRequest): Ret<ConstraintPerturbationSolveResult> {
        val derived = request.derivedSnapshot
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "CP 扰动后端需要调用方派生 snapshot / The CP perturbation backend requires a caller-derived snapshot"
            )
        val modelResult = ConstraintProgrammingSnapshotBuilder.buildModel(derived)
        if (modelResult !is Ok) {
            return propagateBackendFailure(modelResult)
        }
        val model = modelResult.value!!
        val startedAt = TimeSource.Monotonic.markNow()
        try {
            val sessionResult = solver.createSession(model, solveOptions)
            if (sessionResult !is Ok) {
                return propagateBackendFailure(sessionResult)
            }
            val session = sessionResult.value!!
            try {
                // Warm start is a hint only; a rejected or ignored hint must not change the conclusion.
                // / 热启动只是提示；提示被拒绝或忽略都不得改变结论。
                val hints = if (request.warmStart != null) request.warmStart else null
                val outputResult = session.solve(hints = hints)
                val elapsed = startedAt.elapsedNow()
                if (outputResult !is Ok) {
                    return propagateBackendFailure(outputResult)
                }
                return ok(mapOutput(outputResult.value!!, elapsed))
            } finally {
                session.close()
            }
        } finally {
            model.close()
        }
    }

    private fun mapOutput(
        output: ConstraintProgrammingSolverOutput,
        elapsed: Duration
    ): ConstraintPerturbationSolveResult {
        return when (output) {
            is ConstraintProgrammingFeasibleOutput -> {
                val proven = output.isProvenOptimal()
                ConstraintPerturbationSolveResult(
                    status = AnalysisStatus.from(ProblemStatus.Feasible, proven),
                    objectiveValue = output.objective,
                    solution = output.solution,
                    solveTime = elapsed,
                    message = if (proven) {
                        null
                    } else {
                        "CP 扰动求解未形成最优性证明 / CP perturbation solve did not prove optimality"
                    }
                )
            }

            is ConstraintProgrammingInfeasibleOutput -> {
                val proven = output.isProvenInfeasible()
                ConstraintPerturbationSolveResult(
                    status = AnalysisStatus.from(ProblemStatus.Infeasible, proven),
                    solveTime = elapsed,
                    message = if (proven) {
                        "扰动模型已证明不可行 / The perturbed model was proven infeasible"
                    } else {
                        "扰动模型的不可行性未形成证明 / Infeasibility of the perturbed model was not proven"
                    }
                )
            }

            is ConstraintProgrammingUnknownOutput -> ConstraintPerturbationSolveResult(
                status = AnalysisStatus.Unknown,
                solveTime = elapsed,
                message = "CP 扰动求解未形成结论：${output.terminationReason} / " +
                    "CP perturbation solve formed no conclusion: ${output.terminationReason}"
            )
        }
    }
}

/**
 * 是否已证明最优。
 *
 * 判定依据是 `SolutionPresence.Optimal`——它本身就是"具备可靠最优性证明"的语义标记，
 * 与 Rust 侧 `SolveReport::is_optimal()` 一致。**刻意不要求 `ProofStatus.Verified`**：
 * 真实后端适配器对最优解上报的是 `ProofStatus.Claimed`（见 `SolverStatus.toSolveReport`），
 * 要求 `Verified` 会让所有真实后端的有效性阶段静默退化为"无结论"——这与
 * "Timeout→UNSAT" 是同一类错误，只是方向相反（把可用能力误判为不可用）。
 *
 * Whether optimality was proven. The signal is `SolutionPresence.Optimal`, which by definition
 * means "a reliable optimality proof exists", matching Rust's `SolveReport::is_optimal()`.
 * `ProofStatus.Verified` is deliberately **not** required: real backend adapters report
 * `ProofStatus.Claimed` for optimal solutions (see `SolverStatus.toSolveReport`), so requiring
 * `Verified` would silently degrade the effectiveness stage to "no conclusion" on every real
 * backend — the same class of error as "Timeout → UNSAT", only in the opposite direction
 * (treating an available capability as unavailable).
 */
internal fun ConstraintProgrammingFeasibleOutput.isProvenOptimal(): Boolean {
    val unified = report
    return if (unified != null) {
        unified.problemStatus == ProblemStatus.Feasible &&
            unified.solutionPresence == SolutionPresence.Optimal
    } else {
        // 没有统一报告时只能依赖输出自身的证明声明；`None` 表示没有证明。
        // Without a unified report the output's own proof claim is the only signal; `None` means no proof.
        // / 没有统一报告时只能依赖输出自身的证明声明；`None` 表示没有证明。
        proofStatus == ProofStatus.Verified || proofStatus == ProofStatus.Claimed
    }
}

/**
 * 是否已证明不可行。
 *
 * 不可行性必须有证书，因此这里**要求** `ProofStatus.Verified`：适配器对不可行上报的正是
 * `Verified`，而 `Claimed` 不足以支撑"目标不可达"这类强结论。
 * Whether infeasibility was proven. Infeasibility requires a certificate, so
 * `ProofStatus.Verified` is **required** here: adapters report `Verified` for infeasible, and a
 * mere `Claimed` is not sufficient to support a strong conclusion such as "target unreachable".
 */
internal fun ConstraintProgrammingInfeasibleOutput.isProvenInfeasible(): Boolean {
    val unified = report
    return if (unified != null) {
        unified.problemStatus == ProblemStatus.Infeasible &&
            unified.proof.status == ProofStatus.Verified
    } else {
        proofStatus == ProofStatus.Verified
    }
}

/**
 * 把分析器的原始证据激活状态表达为后端 assumption。
 *
 * 只有**布尔文字**类证据可以表达为 assumption；数值约束必须通过移除约束的派生模型表达。
 * 该实现刻意只处理 [BooleanLiteral]，从而不会为不可表达的约束伪造 assumption。
 *
 * Express the analyzer's original-evidence activation state as backend assumptions. Only
 * Boolean-literal evidence can be expressed as an assumption; numeric constraints must be
 * expressed by deriving a model without them. This deliberately handles only [BooleanLiteral], so
 * it never fabricates an assumption for a constraint that cannot be expressed as one.
 */
object ConstraintProgrammingAssumptionMapper {
    /** 从激活集合推导 assumption 列表。 / Derive assumptions from an activation set.
     *
     * @param activations 原始证据激活集合 / Original-evidence activation set
     * @param literals 按约束 ID 编索引的布尔文字 / Boolean literals keyed by constraint ID
     * @return 后端 assumption 列表 / Backend assumption list
     */
    fun assumptions(
        activations: DiagnosticActivationSet,
        literals: Map<ConstraintId, BooleanLiteral>
    ): List<BooleanLiteral> {
        return activations.activeSources()
            .mapNotNull { source ->
                val id = (source as? DiagnosticSource.Constraint)?.id ?: return@mapNotNull null
                literals[id]
            }
            .distinct()
    }
}

/**
 * 固定整数值提取：从基线 CP 解中取出所有整数变量的赋值。
 *
 * 该函数只做投影，不做数值近似：`Int64` 原样传递给后端。
 * Extract fixed integer values: project every integer-variable assignment out of a baseline CP
 * solution. This only projects and never approximates: `Int64` values reach the backend unchanged.
 */
object FixedIntegerProjection {
    /** 从基线解提取固定值。 / Extract fixed values from a baseline solution.
     *
     * @param solution 基线 CP 解，可为 null / Baseline CP solution, or null
     * @return 按变量 ID 编索引的固定值 / Fixed values keyed by variable ID
     */
    fun fixedValues(solution: ConstraintProgrammingSolution?): Map<VariableId, Int64> {
        return solution?.values.orEmpty()
    }

    /** 派生 LP 目标与基线目标的一致性校验。 / Validate that a derived LP objective matches the baseline.
     *
     * @param baseline 基线目标值 / Baseline objective value
     * @param derived 派生目标值 / Derived objective value
     * @param tolerance 相对容差 / Relative tolerance
     * @return 两个目标值是否一致 / Whether the objective values agree
     */
    fun objectivesAgree(baseline: Flt64?, derived: Flt64?, tolerance: Double): Boolean {
        if (baseline == null || derived == null) {
            return false
        }
        val left = baseline.toSolverDouble("fixedIntegerLp.baselineObjective")
        val right = derived.toSolverDouble("fixedIntegerLp.derivedObjective")
        val difference = kotlin.math.abs(left - right)
        val scale = kotlin.math.max(1.0, kotlin.math.abs(left))
        return difference <= tolerance * scale
    }
}

/** 将后端失败映射为调用方结果类型。 / Map a backend failure to the caller's result type.
 *
 * @param T 结果值类型 / Result value type
 * @param result 后端结果 / Backend result
 * @return 映射后的结果 / Mapped result
 */
fun <T> propagateBackendFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        is Ok -> Failed(
            ErrorCode.ApplicationError,
            "CP 后端返回了无效的结果状态 / The CP backend returned an invalid result state"
        )
    }
}
