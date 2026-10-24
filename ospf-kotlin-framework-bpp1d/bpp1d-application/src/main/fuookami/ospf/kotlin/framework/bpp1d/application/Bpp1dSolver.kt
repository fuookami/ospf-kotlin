package fuookami.ospf.kotlin.framework.bpp1d.application

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.bpp.infrastructure.PackingValueAdapter
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.packing.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_generation.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.pattern_assignment.*

/** 一维求解配置。 / One-dimensional solve configuration.
 * @property generation 候选预算 / Candidate budget
 * @property iterationLimit 列生成迭代限制 / Column-generation iteration limit
 * @property columnsPerIteration 每轮新增列上限 / Maximum columns added per iteration
 * @property extensions 建模扩展 / Modeling extensions
 */
data class Bpp1dSolveConfig<V : FloatingNumber<V>>(
    val generation: PatternGenerationConfig = PatternGenerationConfig(),
    val iterationLimit: Int = 32,
    val columnsPerIteration: Int = 32,
    val extensions: List<PatternModelingExtension<V>> = emptyList()
)

/** 一维求解结果，区分整数最优证书与候选完备性。 / Solve result separating integer proof and candidate completeness.
 * @property packing 物理装箱结果 / Physical packing
 * @property status Optimal 或 Feasible / Optimal or Feasible
 * @property objective 目标值 / Objective value
 * @property candidateComplete 候选是否完整 / Candidate completeness
 * @property columnCount 最终列数 / Final column count
 * @property pricingIterations 定价迭代数 / Pricing iterations
 */
data class Bpp1dSolution<V : FloatingNumber<V>>(
    val packing: PackingResult<V>,
    val status: String,
    val objective: V,
    val candidateComplete: Boolean,
    val columnCount: Int,
    val pricingIterations: Int,
    val enhancements: Map<String, String> = emptyMap()
)

/** 一维普通 MILP 入口。 / Ordinary BPP1D MILP entry.
 * @property solver 可注入求解器 / Injectable solver
 * @property adapter 数值边界 / Numeric boundary
 * @property generator 候选生成策略 / Candidate generation strategy
 */
class Bpp1dMilp<V : FloatingNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: PackingValueAdapter<V>,
    private val generator: PatternGenerationContext<V> = PatternGenerationContext()
) {
    /** 求解普通装箱模型。 / Solve an ordinary packing model.
     * @param problem 输入 / Input
     * @param config 配置 / Configuration
     * @return 解或失败 / Solution or failure
     */
    suspend fun solve(problem: PackingProblem<V>, config: Bpp1dSolveConfig<V> = Bpp1dSolveConfig()): Ret<Bpp1dSolution<V>> {
        val generated = when (val result = generator.generate(problem, config.generation)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return solveFinal(
            problem = problem,
            patterns = generated.patterns,
            complete = generated.complete,
            iterations = 0,
            stage = PackingModelStage.MILP,
            config = config,
            solver = solver,
            adapter = adapter
        )
    }
}

/** 有完整生命周期的列生成入口。 / Column-generation entry with a complete lifecycle.
 * @property solver 可注入求解器 / Injectable solver
 * @property adapter 数值边界 / Numeric boundary
 * @property generator 候选/定价策略 / Generation/pricing strategy
 */
class Bpp1dColumnGeneration<V : FloatingNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: PackingValueAdapter<V>,
    private val generator: PatternGenerationContext<V> = PatternGenerationContext()
) {
    /** 求解列生成模型；最终使用完整已生成池，避免把受限池整数最优误报为全局最优。 / Solve CG, finalizing over the generated pool.
     * @param problem 输入 / Input
     * @param config 配置 / Configuration
     * @return 解或失败 / Solution or failure
     */
    suspend fun solve(problem: PackingProblem<V>, config: Bpp1dSolveConfig<V> = Bpp1dSolveConfig()): Ret<Bpp1dSolution<V>> {
        if (config.iterationLimit < 1 || config.columnsPerIteration < 1) return Failed(ErrorCode.IllegalArgument, "列生成预算无效 / Invalid column-generation budget")
        val generated = when (val result = generator.generate(problem, config.generation)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val seeds = arrayListOf<PackingPattern<V>>()
        for (bestFit in listOf(false, true)) {
            when (val result = generator.initialPatterns(problem, bestFit)) {
                is Ok -> seeds.addAll(result.value)
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val model = LinearMetaModel<Flt64>(name = "bpp1d_rmp", converter = IntoValue.Identity)
        val context = PatternAssignmentContext(
            problem = problem,
            adapter = adapter,
            model = model,
            stage = PackingModelStage.LP,
            extensions = config.extensions
        )
        when (val result = context.register(seeds.distinctBy { it.key })) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        var iterations = 0
        for (iteration in 1..config.iterationLimit) {
            val lp = when (val result = solver.solveLP(model)) {
                is Ok -> result.value
                // 初始列受库存耦合而不可行时，使用已生成池求最终模型，不宣称 LP 定价已收敛。
                // Stock coupling may make seeds infeasible; finalization uses the generated pool without an LP certificate.
                is Failed -> if (result.error.code == ErrorCode.ORModelInfeasible) break else return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (lp.status != SolverStatus.Optimal) break
            iterations = iteration
            val dual = lp.dualSolution.toMeta()
            when (val result = context.refreshShadowPrice(dual)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val existing = context.patterns.map { it.key }.toSet()
            val unregistered = generated.patterns.filter { it.key !in existing }
            // 既有扩展未承诺只读，保留它们逐候选读取当前价格的行为。 / Existing extensions do not promise read-only pricing; retain their per-candidate current-price reads.
            val priced = if (context.extensions.isEmpty()) {
                unregistered.zip(context.reducedCosts(unregistered))
            } else {
                unregistered.map { it to context.reducedCost(it) }
            }
            val candidates = priced
                .filter { it.second < Flt64(-1e-6) }
                .sortedBy { it.second }
                .take(config.columnsPerIteration)
                .map { it.first }
            if (candidates.isEmpty()) break
            when (val result = context.addColumns(candidates)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return solveFinal(
            problem = problem,
            patterns = (generated.patterns + seeds).distinctBy { it.key },
            complete = generated.complete,
            iterations = iterations,
            stage = PackingModelStage.FINAL_MILP,
            config = config,
            solver = solver,
            adapter = adapter
        )
    }
}

private suspend fun <V : FloatingNumber<V>> solveFinal(
    problem: PackingProblem<V>,
    patterns: List<PackingPattern<V>>,
    complete: Boolean,
    iterations: Int,
    stage: PackingModelStage,
    config: Bpp1dSolveConfig<V>,
    solver: ColumnGenerationSolver,
    adapter: PackingValueAdapter<V>
): Ret<Bpp1dSolution<V>> {
    val model = LinearMetaModel<Flt64>(name = "bpp1d_final", converter = IntoValue.Identity)
    val context = PatternAssignmentContext(
        problem = problem,
        adapter = adapter,
        model = model,
        stage = stage,
        extensions = config.extensions
    )
    when (val result = context.register(patterns)) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val output = when (val result = solver.solveMILP(model)) {
        is Ok -> result.value
        is Failed -> return if (!complete && result.error.code == ErrorCode.ORModelInfeasible) {
            Failed(ErrorCode.OREngineSolvingException, "受限候选池无解，尚未证明原问题不可行 / Restricted candidate pool is infeasible; original infeasibility is not proven")
        } else Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val objective = output.solution?.objective ?: return if (complete && output.toSolverStatus() == SolverStatus.Infeasible) {
        Failed(ErrorCode.ORModelInfeasible, "完整装箱模型已证明不可行 / Complete packing model is proven infeasible")
    } else {
        Failed(ErrorCode.OREngineSolvingException, "装箱尚无可行解或候选尚不完备 / No packing incumbent or incomplete candidate pool: ${output.toSolverStatus()}")
    }
    val usages = when (val result = context.extractSolution(output.values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val packing = when (val result = PackingContext<V>().pack(problem, usages)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val enhancements = linkedMapOf<String, String>()
    for (extension in config.extensions) {
        val extracted = when (val result = extension.extractSolution(context, output.values)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (extracted.keys.any { it in enhancements }) return Failed(ErrorCode.IllegalArgument, "扩展指标键重复 / Duplicate extension indicator key")
        enhancements.putAll(extracted)
    }
    return Ok(Bpp1dSolution(
        packing = packing,
        status = if (complete && output.toSolverStatus() == SolverStatus.Optimal) "Optimal" else "Feasible",
        objective = adapter.fromSolver(objective),
        candidateComplete = complete,
        columnCount = patterns.size,
        pricingIterations = iterations,
        enhancements = enhancements
    ))
}
