/** Gurobi 临界约束分析后端。 / Gurobi critical-constraint analysis backends. */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.math.abs
import gurobi.GRB
import gurobi.GRBConstr
import gurobi.GRBModel
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.constraint_programming.MipBackedConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.analysis.CapabilityMatrix
import fuookami.ospf.kotlin.core.analysis.SensitivityRange
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpBackend
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationBackend
import fuookami.ospf.kotlin.core.analysis.LinearSolverFixedIntegerLpBackend
import fuookami.ospf.kotlin.core.analysis.CriticalConstraintAnalysisPipeline
import fuookami.ospf.kotlin.core.analysis.ConstraintProgrammingPerturbationBackend

/**
 * 使用真实 Gurobi 求解 CP 固定整数 LP，产出原始约束级对偶值与 RHS 敏感性范围。
 *
 * 实现复用与后端无关的 [LinearSolverFixedIntegerLpBackend]；Gurobi 只额外提供原生 ranging 钩子，
 * 因此两端（Gurobi/SCIP）共用同一条经测试的实现路径，不存在重复逻辑。
 *
 * 语义边界（计划 3.2 / 7.6 / 8.3）：
 * - 对偶与范围都只按**原始约束稳定 ID** 暴露，降阶辅助行一律不出现在报告中；
 * - 仅当某个原始约束恰好对应**唯一**降阶行时才给出值；
 * - 对偶表示 `FixedIntegerIncumbent` 作用域下的局部边际价值，**绝不是** MILP 全局影子价格；
 * - 只有真正形成最优解时才升格为 `Reachable`，受限求解的 incumbent 保持 `Unknown`。
 *
 * Solves a CP fixed-integer LP with real Gurobi, producing original-constraint-level duals and RHS
 * sensitivity ranges. The implementation reuses the backend-neutral
 * [LinearSolverFixedIntegerLpBackend]; Gurobi only adds a native ranging hook, so both backends share
 * one tested path with no duplicated logic.
 *
 * @param config 求解器配置 / Solver configuration
 * @param lowerer CP 到线性模型的降阶器 / CP-to-linear-model lowerer
 * @return 固定整数 LP 后端 / Fixed-integer LP backend
 */
fun gurobiFixedIntegerLpBackend(
    config: SolverConfig = SolverConfig(),
    lowerer: ConstraintProgrammingToLinearModelLowerer = ConstraintProgrammingToLinearModelLowerer()
): FixedIntegerLpBackend {
    // 在**主求解**过程中捕获范围，而不是事后重解一次。
    //
    // 实测发现：对同一个 triad 二次求解时 presolve 配置不再生效，读回的 `SARHSLow` 会退化成
    // presolve 打开时的哨兵值（c1 得到 4 而不是 0）。在主求解上捕获既避免了这个问题，也省掉
    // 一次额外 LP，符合 8.16 的求解次数预算。
    //
    // Ranges are captured during the **primary** solve rather than by re-solving afterwards.
    // Measurement showed that a second solve of the same triad no longer honours the presolve
    // configuration and returns presolve-on sentinel values (c1 gets 4 instead of 0). Capturing on the
    // primary solve avoids that and also saves an extra LP, matching the 8.16 solve-count budget.
    // Keep ranging state inside one backend invocation. A backend instance can be reused for
    // different snapshots, so factory-level state would leak the first solve's ranges into later
    // reports. / 将 ranging 状态限制在一次 backend 调用内，避免复用 backend 时串入首次求解结果。
    return FixedIntegerLpBackend { request ->
        // 在**主求解**过程中捕获范围，而不是事后重解一次。
        // Ranges are captured during the **primary** solve rather than by re-solving afterwards.
        val captured = LinkedHashMap<Int, SensitivityRange>()
        var primarySolved = false
        val solver = GurobiLinearSolver(
            config,
            GurobiLinearSolverCallBack()
                .analyzingSolution { status, gurobi, _, constraints ->
                    // 只捕获主求解：随后求对偶时会再次触发本回调，其结果不得覆盖主求解的范围。
                    // Capture the primary solve only: the later dual solve re-triggers this callback,
                    // and its result must not overwrite the primary ranges.
                    if (!primarySolved) {
                        primarySolved = true
                        if (status != null && status.succeeded) {
                            captureSensitivityRanges(
                                gurobi = gurobi,
                                constraints = constraints,
                                target = captured
                            )
                        }
                    }
                    ok
                }
        )
        LinearSolverFixedIntegerLpBackend(
            linearSolver = solver,
            lowerer = lowerer,
            sensitivityRanges = { captured },
            // 对偶求解使用独立求解器，使两者互不干扰。
            // The dual solve uses a separate solver so the two cannot interfere.
            dualSolver = GurobiLinearSolver(config)
        ).solve(request)
    }
}

/**
 * 读取 Gurobi 原生的 RHS 敏感性范围，按降阶行索引返回。
 *
 * **实测确定的语义**：`SARHSLow` / `SARHSUp` 是 RHS 的**绝对边界值**，不是增量；因此直接采用
 * 属性值，**不做** `rhs +` 运算。（早期一次探针曾按"增量"解释，在未固定整数的降阶模型上得到
 * 错误的 `[8, ∞)`，而绝对值解释给出与人工推导一致的 `[0, 6]`。）
 *
 * **重要的量纲提醒**：本阶段是**固定整数 LP**，所有整数变量已被固定，LP 往往退化为单点。
 * 此时范围刻画的是"在保持该整数结构不变的前提下 RHS 可移动多远"，与原始 MILP 上的范围是
 * 不同模型上的不同量。例如 `max 3x+2y, x+y≤4, x+3y≤6` 在固定 `x=4, y=0` 后，两条约束的
 * 有效范围都是 `[4, ∞)`（低于 4 即不可行），而不是原始 MILP 上的 `c1 ∈ [0, 6]`。
 *
 * Gurobi 用 `GRB.INFINITY`（`1e100`）表示无界；该值 `isFinite()` 为真，必须显式识别并映射为
 * "该侧无界"，绝不能当成有限边界返回。
 *
 * Reads Gurobi's native RHS sensitivity ranges keyed by lowered row index. **Measured semantics**:
 * `SARHSLow`/`SARHSUp` are **absolute** RHS bounds rather than deltas, so the attribute values are
 * used directly with no `rhs +`. (An early probe read them as deltas and produced a wrong `[8, ∞)` on
 * an unfixed lowered model, whereas the absolute reading gives `[0, 6]`, matching the hand-derived
 * range.)
 *
 * **Scaling caveat**: this stage is the **fixed-integer LP**, where every integer variable is pinned
 * and the LP often collapses to a single point. The range therefore describes how far the RHS may move
 * while that integer structure is held fixed, which is a different quantity on a different model from
 * the range of the original MILP. For `max 3x+2y, x+y≤4, x+3y≤6` fixed at `x=4, y=0` both constraints
 * have the range `[4, ∞)` (anything below 4 is infeasible) rather than the original MILP's
 * `c1 ∈ [0, 6]`.
 *
 * `GRB.INFINITY` is `1e100`, which passes `isFinite()`, so it must be recognised explicitly and mapped
 * to "unbounded on that side" rather than returned as a bound.
 */
private fun captureSensitivityRanges(
    gurobi: GRBModel,
    constraints: List<GRBConstr>,
    target: MutableMap<Int, SensitivityRange>
) {
    if (constraints.isEmpty()) {
        return
    }
    runCatching {
        val array = constraints.toTypedArray()
        val lows = gurobi.get(GRB.DoubleAttr.SARHSLow, array)
        val ups = gurobi.get(GRB.DoubleAttr.SARHSUp, array)
        for (index in constraints.indices) {
            val low = lows.getOrNull(index) ?: continue
            val up = ups.getOrNull(index) ?: continue
            if (low.isNaN() || up.isNaN()) {
                continue
            }
            if (low > up) {
                continue
            }
            val lower = if (abs(low) >= GRB.INFINITY) null else Flt64(low)
            val upper = if (abs(up) >= GRB.INFINITY) null else Flt64(up)
            if (lower == null && upper == null) {
                continue
            }
            target[index] = SensitivityRange(lower = lower, upper = upper)
        }
    }.onFailure {
        // ranging 是可选证据：属性不可用不得导致求解失败。 / Ranging is optional evidence: an
        // unavailable attribute must not fail the solve.
        target.clear()
    }
}

/**
 * 使用 Gurobi 的 CP 路径重新优化扰动与删除模型。
 *
 * 复用与后端无关的 [ConstraintProgrammingPerturbationBackend]：warm start 只是提示，
 * 且只有在求解形成最优性证明时才报告改善。
 *
 * Reoptimize perturbed and removal models through Gurobi's CP path. This reuses the
 * backend-neutral [ConstraintProgrammingPerturbationBackend]: a warm start only affects
 * performance, and an improvement is reported only when the solve proves optimality.
 *
 * @param config 求解器配置 / Solver configuration
 * @return 约束扰动后端 / Constraint perturbation backend
 */
fun gurobiPerturbationBackend(
    config: SolverConfig = SolverConfig()
): ConstraintPerturbationBackend {
    return ConstraintProgrammingPerturbationBackend(
        MipBackedConstraintProgrammingSolver(GurobiLinearSolver(config))
    )
}

/**
 * 构建由单一真实 Gurobi 配置驱动的完整临界约束分析管线。 / Build the complete
 * critical-constraint pipeline backed by one real Gurobi configuration.
 *
 * CP 求解器描述符提供精确降阶和重复求解冲突能力，线性描述符提供 Gurobi 对偶能力；将两者置于同一能力矩阵，
 * 可让管线执行目标/冲突分析与固定整数 LP 分析，而不虚构线性求解器原生支持全部 CP 特性。 /
 * The CP solver descriptor supplies exact lowering and repeated-solving conflict capabilities;
 * the linear descriptor contributes Gurobi's dual capability. Keeping both parts in one matrix
 * lets the pipeline execute target/conflict analysis and fixed-integer LP analysis without
 * pretending that the linear solver natively supports every CP feature.
 *
 * @param config 求解器配置 / Solver configuration
 * @return 临界约束分析管线 / Critical-constraint analysis pipeline
 */
fun gurobiCriticalConstraintAnalysisPipeline(
    config: SolverConfig = SolverConfig()
): CriticalConstraintAnalysisPipeline {
    val linearSolver = GurobiLinearSolver(config)
    val cpSolver = MipBackedConstraintProgrammingSolver(linearSolver)
    val capabilityMatrix = CapabilityMatrix.from(cpSolver.descriptor).copy(
        modelTypes = linearSolver.descriptor.capabilities.modelTypes,
        dual = linearSolver.descriptor.capabilities.dual
    )
    return CriticalConstraintAnalysisPipeline(
        solver = cpSolver,
        lpBackend = gurobiFixedIntegerLpBackend(config),
        perturbationBackend = gurobiPerturbationBackend(config),
        capabilityMatrix = capabilityMatrix
    )
}
