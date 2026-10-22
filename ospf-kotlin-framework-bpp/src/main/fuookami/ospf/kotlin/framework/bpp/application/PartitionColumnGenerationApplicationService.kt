package fuookami.ospf.kotlin.framework.bpp.application

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.bpp.domain.partition_loading.*

/** 分区列生成预算。 / Partition-column generation budget.
 * @property iterationLimit 包含可行性阶段的根节点迭代上限 / Root iteration limit including feasibility phase
 * @property columnLimit 根节点列数上限；资源补全最多另加每分区一列 / Root column budget; resource completion may add one column per partition
 * @property tolerance 定价及人工变量容差 / Pricing and artificial-variable tolerance
 */
data class PartitionColumnGenerationConfig(val iterationLimit: Int = 64, val columnLimit: Int = 10000, val tolerance: Double = 1e-6)

/** 分区列生成轨迹。 / Partition-column generation trace.
 * @property phaseOne 是否人工变量可行性阶段 / Whether in the artificial-variable feasibility phase
 * @property objective 当前受限 LP 目标 / Current restricted LP objective
 * @property addedColumns 新增列数 / Number of added columns
 * @property pricingProven 所有分区本轮定价是否有证明 / Whether every partition pricing problem was proven
 */
data class PartitionColumnGenerationIteration(val phaseOne: Boolean, val objective: Flt64, val addedColumns: Int, val pricingProven: Boolean)

/** 根节点列生成加最终受限整数主问题结果，不冒充 branch-and-price。 / Root column generation plus restricted integer master, not branch-and-price.
 * @property loading 已独立复核的完整装载结果 / Independently verified full loading result
 * @property trace 定价轨迹 / Pricing trace
 * @property columnCount 最终列数 / Final column count
 * @property lpConverged 是否证明根节点 LP 定价收敛 / Whether root LP pricing convergence was proven
 */
data class PartitionColumnGenerationResult<V : FloatingNumber<V>>(
    val loading: DiscreteLoadingSolveResult<V>,
    val trace: List<PartitionColumnGenerationIteration>,
    val columnCount: Int,
    val lpConverged: Boolean
)

/** 默认局部 MILP 定价器；完整全局行的对偶贡献来自共享上下文。 / Default local MILP pricer with full global-row dual contributions.
 * @property solver 可注入、无商业绑定的执行器 / Injectable backend without commercial binding
 */
class MilpPartitionPricingOracle<V : FloatingNumber<V>>(private val solver: ColumnGenerationSolver) : PartitionPricingOracle<V> {
    override suspend fun price(context: PartitionLoadingContext<V>, request: PartitionPricingRequest): Ret<PartitionPricingResult> {
        val model = when (val result = context.pricingModel(request)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val solved = when (val result = solver.solveMILP(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (solved.solution == null) return Failed(ErrorCode.OREngineSolvingException, "局部定价没有可行配置 / Local pricing has no feasible configuration: ${solved.toSolverStatus()}")
        val values = context.decisions.filter { it.partitionId == request.partitionId }.associate { decision ->
            val index = model.tokens.indexOf(decision.variable) ?: return Failed(ErrorCode.DataNotFound, "缺少局部 token / Missing local token")
            decision.id to (solved.values.getOrNull(index) ?: return Failed(ErrorCode.DataNotFound, "缺少局部变量值 / Missing local value"))
        }
        val bound = solved.bestBound
        val cost = when (val result = request.reducedCost(values)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val optimal = solved.toSolverStatus() == SolverStatus.Optimal && (bound == null || (cost - bound).abs() <= Flt64(1e-6))
        return Ok(PartitionPricingResult(listOf(values), optimal))
    }
}

/** 两个几何框架复用的独立分区列生成路径；不联合 Benders。 / Independent partition CG path shared by both geometry frameworks, not combined with Benders.
 * @property solver LP、局部 MILP 与最终整数主问题执行器 / LP, local MILP and final integer master backend
 * @property oracle 可替换局部定价策略 / Replaceable local pricing strategy
 */
class PartitionColumnGenerationApplicationService<V : FloatingNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val oracle: PartitionPricingOracle<V> = MilpPartitionPricingOracle(solver)
) {
    /** 从同一完整模型获取全局行和目标，不在最终模型才补业务规则。 / Obtain business rows and objectives before pricing, not just in the final model.
     * @param registration 同一领域注册路径的完整模型 / Full model from the common domain registration path
     * @param partitions 显式分区 / Explicit partitions
     * @param extraDecisions 下游局部变量归属 / Downstream local variable ownership
     * @param config 根节点预算 / Root budget
     * @return 受限整数池可行解或结构化失败 / Restricted-pool feasible solution or structured failure
     */
    suspend fun solve(
        registration: RegisteredDiscreteLoading<V>,
        partitions: List<LoadingPartition>,
        extraDecisions: List<PartitionDecision> = emptyList(),
        config: PartitionColumnGenerationConfig = PartitionColumnGenerationConfig()
    ): Ret<PartitionColumnGenerationResult<V>> {
        if (config.iterationLimit < 1 || config.columnLimit < partitions.size || !config.tolerance.isFinite() || config.tolerance <= 0) {
            return Failed(ErrorCode.IllegalArgument, "分区列生成预算无效 / Invalid partition CG budget")
        }
        val context = when (val result = PartitionLoadingContext.create(
            loading = registration.context, source = registration.model, partitions = partitions, extraDecisions = extraDecisions
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for ((index, partition) in partitions.withIndex()) {
            val request = when (val result = context.pricingRequest(
                partitionId = partition.id,
                phaseOne = true,
                seed = true
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val seed = when (val result = oracle.price(context, request)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val current = context.projection.requireCurrent(request.revision)
            if (current is Failed) return Failed(current.error)
            if (current is Fatal) return Fatal(current.errors)
            val available = config.columnLimit - context.columns.size - (partitions.size - index - 1)
            val added = context.register(seed.values.take(available).map { partition.id to it })
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        var phaseOne = true
        var converged = false
        val trace = arrayListOf<PartitionColumnGenerationIteration>()
        for (iteration in 1..config.iterationLimit) {
            val master = when (val result = context.master(phaseOne, false)) {
                is Ok -> result.value
                is Failed -> return if (result.error.code == ErrorCode.ORModelInfeasible) {
                    Failed(ErrorCode.OREngineSolvingException, "受限主问题不可行，未证明原问题不可行 / Restricted master infeasible; original infeasibility unproven")
                } else Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val lp = when (val result = solver.solveLP(master.model)) {
                is Ok -> result.value
                is Failed -> return if (result.error.code == ErrorCode.ORModelInfeasible) {
                    Failed(ErrorCode.OREngineSolvingException, "受限主问题不可行，未证明原问题不可行 / Restricted master infeasible; original infeasibility unproven")
                } else Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (lp.result.solution == null || lp.status != SolverStatus.Optimal) break
            if (phaseOne && lp.obj <= Flt64(config.tolerance)) {
                trace.add(PartitionColumnGenerationIteration(true, lp.obj, 0, false))
                phaseOne = false
                continue
            }
            val refreshed = context.refreshShadowPrice(master, lp)
            if (refreshed is Failed) return Failed(refreshed.error)
            if (refreshed is Fatal) return Fatal(refreshed.errors)
            val before = context.columns.size
            var proven = true
            var improvingFound = false
            val pending = arrayListOf<Pair<String, Map<String, Flt64>>>()
            for (partition in partitions) {
                val request = when (val result = context.pricingRequest(partition.id, phaseOne)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val priced = when (val result = oracle.price(context, request)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val current = context.projection.requireCurrent(request.revision)
                if (current is Failed) return Failed(current.error)
                if (current is Fatal) return Fatal(current.errors)
                proven = proven && priced.optimal
                val improving = arrayListOf<Map<String, Flt64>>()
                for (values in priced.values) {
                    val cost = when (val result = request.reducedCost(values)) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                    if (cost < Flt64(-config.tolerance)) improvingFound = true
                    if (cost < Flt64(-config.tolerance) && improving.size + pending.size < config.columnLimit - context.columns.size) improving.add(values)
                }
                pending.addAll(improving.map { partition.id to it })
            }
            // 同轮所有分区消费同一组对偶，统一加列后再失效。 / All blocks consume one dual snapshot before batch insertion.
            val inserted = context.addColumns(pending)
            if (inserted is Failed) return Failed(inserted.error)
            if (inserted is Fatal) return Fatal(inserted.errors)
            val added = context.columns.size - before
            trace.add(PartitionColumnGenerationIteration(phaseOne, lp.obj, added, proven))
            if (added == 0) {
                converged = !phaseOne && proven && !improvingFound
                break
            }
            if (context.columns.size >= config.columnLimit) break
        }
        // 仅在已有分配/占位轮廓内补全连续或整数资源，随后物化为完整新列。
        // Complete resources only inside generated assignment/occupancy profiles, then materialize complete columns.
        if (context.resourceDecisions.isNotEmpty()) {
            val refinement = when (val result = context.master(phaseOne = false, integer = true, refineResources = true)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val completed = when (val result = solver.solveMILP(refinement.model)) {
                is Ok -> result.value
                is Failed -> return Failed(ErrorCode.OREngineSolvingException, "受限分区轮廓无法补全资源 / Restricted partition profiles cannot complete resources: ${result.error.message}")
                is Fatal -> return Fatal(result.errors)
            }
            if (completed.solution == null) return Failed(ErrorCode.OREngineSolvingException, "受限分区轮廓没有资源补全解，未证明原问题不可行 / No resource completion for restricted profiles; original infeasibility unproven")
            val values = when (val result = context.extractSolution(refinement, completed.values)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val materialized = context.addColumns(partitions.map { partition ->
                partition.id to context.decisions.filter { it.partitionId == partition.id }.associate { decision ->
                    val index = registration.model.tokens.indexOf(decision.variable)
                        ?: return Failed(ErrorCode.DataNotFound, "缺少资源补全 token / Missing resource-completion token")
                    decision.id to values[index]
                }
            })
            if (materialized is Failed) return Failed(materialized.error)
            if (materialized is Fatal) return Fatal(materialized.errors)
        }
        // 整数主问题不带人工变量；有限列池无解不能证明原问题不可行。
        // No artificial variables in the integer master; pool infeasibility is not original infeasibility.
        val master = when (val result = context.master(false, true)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val solved = when (val result = solver.solveMILP(master.model)) {
            is Ok -> result.value
            is Failed -> return Failed(ErrorCode.OREngineSolvingException, "受限分区列池未获得整数解 / Restricted partition pool has no integer incumbent: ${result.error.message}")
            is Fatal -> return Fatal(result.errors)
        }
        val objective = solved.solution?.objective ?: return Failed(ErrorCode.OREngineSolvingException, "受限分区列池未获得整数解，未证明原问题不可行 / No restricted-pool integer incumbent; original infeasibility is unproven")
        val values = when (val result = context.extractSolution(master, solved.values)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val report = SolverStatus.Feasible.toSolveReport(
            objective = if (registration.model.objectCategory == ObjectCategory.Maximum) -objective else objective,
            values = values, solveTime = solved.statistics.solveTime ?: Duration.ZERO
        )
        val verified = when (val result = DiscreteLoadingApplicationService<V>(milpSolver = { Ok(report) }).solve(factory = { Ok(registration) })) {
            is Ok -> result.value.copy(path = "partition_column_generation", termination = if (converged) "root LP converged; restricted integer pool" else "pricing budget or proof incomplete; restricted integer pool")
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(PartitionColumnGenerationResult(verified, trace, context.columns.size, converged))
    }
}
