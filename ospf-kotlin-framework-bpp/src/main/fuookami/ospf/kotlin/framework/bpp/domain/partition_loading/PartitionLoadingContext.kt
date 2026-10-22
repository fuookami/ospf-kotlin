package fuookami.ospf.kotlin.framework.bpp.domain.partition_loading

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.AssignmentKey

/** 显式分区，不从空间名称、几何或列表顺序推断。 / Explicit partition, never inferred from names, geometry or order.
 * @property positionIds 唯一归属的逻辑舱位 / Uniquely owned logical positions
 */
data class LoadingPartition(val id: String, val positionIds: Set<String>)

/** 下游局部决策的稳定绑定。 / Stable binding of a downstream local decision.
 * @property partitionId 所属分区 / Owning partition
 * @property variable 完整模型中的变量身份 / Variable identity in the full model
 */
data class PartitionDecision(val id: String, val partitionId: String, val variable: AbstractVariableItem<*, *>)

/** 一套局部完整装载配置；不是单舱位列。 / A complete local loading configuration, not a single-position column.
 * @property decisions 包含实际、占位和下游局部变量的具体值 / Concrete assignment, occupancy and downstream values
 * @property assignments 货物到舱位 ID / Cargo-to-position IDs
 * @property occupiedPositions 整体占位 / Whole-position occupancy
 * @property cargoUsage 货物使用向量 / Cargo-use vector
 * @property counts 各舱位件数 / Counts per position
 * @property weights 各舱位实际货重（附加资源另见 decisions） / Actual cargo weights; additional resources remain in decisions
 * @property compatibleLayouts 每个选择组的兼容布局 / Compatible layouts per selection group
 */
data class PartitionLoadingColumn<V : FloatingNumber<V>>(
    val id: String,
    val partitionId: String,
    val decisions: Map<String, Flt64>,
    val assignments: Map<String, String>,
    val occupiedPositions: Set<String>,
    val cargoUsage: Map<String, UInt64>,
    val counts: Map<String, UInt64>,
    val weights: Map<String, Quantity<V>>,
    val compatibleLayouts: Map<String, Set<String>>
)

/** 定价请求；线性业务行和目标自动参与 reduced-cost 系数。 / Pricing request including all linear business rows and objectives.
 * @property coefficients 稳定局部决策 ID 到检验数系数 / Stable decision IDs to reduced-cost coefficients
 * @property convexityPrice 分区恰选一列行的影子价格 / Exactly-one-column shadow price
 * @property rowPrices 稳定完整行 ID 到影子价格 / Stable full-row IDs to shadow prices
 */
typealias PartitionPricingRequest = ColumnPricingRequest<Flt64>

/** 领域分区身份。 / Domain partition identity. */
val PartitionPricingRequest.partitionId: String get() = blockId

/** 定价返回值；定价证明不等同于整数最优证明。 / Pricing result; a pricing proof is not an integer-optimality proof.
 * @property values 一个或多个局部配置 / One or more local configurations
 * @property optimal 是否证明本次线性定价最优 / Whether this pricing problem was proven optimal
 */
data class PartitionPricingResult(val values: List<Map<String, Flt64>>, val optimal: Boolean)

/** 可替换局部 MILP、搜索或组合定价器。 / Replaceable local MILP, search or combinatorial pricer. */
fun interface PartitionPricingOracle<V : FloatingNumber<V>> {
    /** 执行一次分区定价。 / Price one partition.
     * @param context 完整行和局部模型边界 / Full-row and local-model boundary
     * @param request 当前影子价格 / Current shadow prices
     * @return 可行配置及定价证明 / Feasible configurations and pricing certificate
     */
    suspend fun price(context: PartitionLoadingContext<V>, request: PartitionPricingRequest): Ret<PartitionPricingResult>
}

/** 完整线性行的 solver-boundary 快照。 / Solver-boundary snapshot of a complete linear row.
 * @property coefficients 变量身份到系数 / Variable identities to coefficients
 */
typealias PartitionMasterRow = LinearModelRow<Flt64>

/** 共享分解层的当前主问题注册。 / Active master registration from the shared decomposition layer. */
typealias PartitionMasterRegistration = ColumnMasterRegistration<Flt64>

/** 分区列生命周期及完整模型系数桥接。 / Partition-column lifecycle and full-model coefficient bridge.
 * @property decisions 显式、唯一的局部变量归属 / Explicit unique local ownership
 * @property rows 所有原始硬约束，跨分区行不会丢失 / All original hard rows, including cross-partition rows
 */
class PartitionLoadingContext<V : FloatingNumber<V>> private constructor(
    val loading: DiscreteLoadingContext<V>,
    val source: LinearMetaModel<Flt64>,
    val partitions: List<LoadingPartition>,
    val decisions: List<PartitionDecision>,
    val projection: ColumnProjection<Flt64>,
    private var structure: LinearModelSnapshot<Flt64>
) {
    val variables get() = structure.variables.map { it.variable }
    val rows get() = structure.rows
    val objective get() = structure.objective
    val objectiveConstant get() = structure.objectiveConstant
    private val pool = linkedMapOf<String, PartitionLoadingColumn<V>>()
    val columns: List<PartitionLoadingColumn<V>> get() = pool.values.toList()
    /** 可在已生成分配/占位轮廓内精确补全的下游资源变量。 / Downstream resources that can be completed inside generated assignment/occupancy profiles. */
    val resourceDecisions: List<PartitionDecision> get() {
        val core = loading.aggregation.assignments.values.toSet() + loading.aggregation.occupied.values
        return decisions.filter { it.variable !in core }
    }

    companion object {
        /** 从同一完整模型获取所有全局行及业务目标；无航空规则。 / Capture every global row and business objective from the same full model.
         * @param loading 几何适配后的离散上下文 / Geometry-adapted discrete context
         * @param source 已注册下游扩展的完整模型 / Full model including downstream extensions
         * @param partitions 显式分区，必须完整且互不重复地覆盖舱位 / Explicit disjoint exhaustive position ownership
         * @param extraDecisions 预估、推荐重量及其他局部变量的显式归属 / Explicit ownership of estimated/recommended weights and other local variables
         * @return 生命周期上下文或错误 / Lifecycle context or error
         */
        suspend fun <V : FloatingNumber<V>> create(
            loading: DiscreteLoadingContext<V>,
            source: LinearMetaModel<Flt64>,
            partitions: List<LoadingPartition>,
            extraDecisions: List<PartitionDecision> = emptyList()
        ): Ret<PartitionLoadingContext<V>> {
            val ids = loading.problem.positions.map { it.id }.toSet()
            val memberships = partitions.flatMap { p -> p.positionIds.map { it to p.id } }
            if (partitions.isEmpty() || partitions.any { it.id.isBlank() || it.positionIds.isEmpty() } ||
                partitions.map { it.id }.distinct().size != partitions.size || memberships.map { it.first }.toSet() != ids || memberships.size != ids.size) {
                return Failed(ErrorCode.IllegalArgument, "分区必须唯一且完整地覆盖舱位 / Partitions must uniquely and exhaustively own positions")
            }
            val owners = memberships.toMap()
            fun decisionId(prefix: String, a: String, b: String = "") = "$prefix:${a.length}:$a:${b.length}:$b"
            val bindings = loading.aggregation.assignments.map { (key, variable) ->
                PartitionDecision(decisionId("assignment", key.itemId, key.positionId), owners.getValue(key.positionId), variable)
            } + loading.aggregation.occupied.map { (id, variable) -> PartitionDecision(decisionId("occupied", id), owners.getValue(id), variable) } + extraDecisions
            if (bindings.any { it.id.isBlank() || it.partitionId !in partitions.map { p -> p.id } } ||
                bindings.map { it.id }.distinct().size != bindings.size || bindings.map { it.variable }.distinct().size != bindings.size) {
                return Failed(ErrorCode.IllegalArgument, "局部决策身份或归属重复 / Duplicate local decision identity or ownership")
            }
            val projection = ColumnProjection(
                source = source,
                decisions = bindings.map { ColumnDecision(id = it.id, blockId = it.partitionId, variable = it.variable) },
                blockIds = partitions.map { it.id }.toSet()
            )
            val snapshot = when (val result = projection.snapshot()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return Ok(PartitionLoadingContext(
                loading = loading,
                source = source,
                partitions = partitions,
                decisions = bindings,
                projection = projection,
                structure = snapshot
            ))
        }
    }

    /** 刷新模型基础层快照，不维护第二套行定义。 / Refresh model-owned state without another row definition.
     * @return 当前结构验证 / Current structure validation
     */
    suspend fun refresh(): Try {
        return when (val result = projection.snapshot()) {
            is Ok -> { structure = result.value; ok }
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /** 注册种子列。 / Register seed columns.
     * @param values 分区和局部决策向量 / Partition IDs and local vectors
     * @return 注册结果 / Registration result
     */
    suspend fun register(values: List<Pair<String, Map<String, Flt64>>>): Try = addColumns(values)

    /** 增列，独立验证局部边界和所有局部硬约束。 / Add columns after independently checking local bounds and every local hard row.
     * @param values 分区及具体配置 / Partitions and concrete configurations
     * @return 操作结果 / Operation result
     */
    suspend fun addColumns(values: List<Pair<String, Map<String, Flt64>>>): Try {
        val refreshed = refresh()
        if (refreshed !is Ok) return refreshed
        for ((partitionId, supplied) in values) {
            val local = decisions.filter { it.partitionId == partitionId }
            if (local.isEmpty() || supplied.keys != local.map { it.id }.toSet()) return Failed(ErrorCode.IllegalArgument, "列必须包含分区全部局部决策 / A column must contain every local decision")
            for (decision in local) {
                val variable = decision.variable
                val value = supplied.getValue(decision.id)
                if (!value.isFinite() || variable.lowerBound?.value?.unwrapOrNull()?.let { value < it - Flt64(1e-6) } == true ||
                    variable.upperBound?.value?.unwrapOrNull()?.let { value > it + Flt64(1e-6) } == true ||
                    (variable.type.isIntegerType && (value - value.round()).abs() > Flt64(1e-6))) return Failed(ErrorCode.IllegalArgument, "局部列违反变量范围 / Local column violates variable bounds")
            }
            // 只归一化已验证的整数决策；连续资源不得用阈值抹掉正重量。
            // Normalize only validated integer decisions; never threshold positive continuous resources.
            val vector = local.associate { decision ->
                val value = supplied.getValue(decision.id)
                val normalized = if (decision.variable.type.isIntegerType) value.round() else value
                decision.id to if (normalized.toDouble() == 0.0) Flt64.zero else normalized
            }
            val byVar = local.associate { it.variable to vector.getValue(it.id) }
            for (row in rows.filter { it.coefficients.keys.all { variable -> variable in byVar } }) {
                val value = row.coefficients.entries.fold(Flt64.zero) { total, (variable, coefficient) -> total + coefficient * byVar.getValue(variable) }
                val valid = when (row.comparison) { Comparison.LE -> value <= row.rhs + Flt64(1e-6); Comparison.GE -> value >= row.rhs - Flt64(1e-6); else -> (value - row.rhs).abs() <= Flt64(1e-6) }
                if (!valid) return Failed(ErrorCode.IllegalArgument, "局部列违反完整模型的局部行 / Local column violates a local full-model row: ${row.id}")
            }
            val id = "${partitionId.length}:$partitionId|" + vector.toSortedMap().entries.joinToString { "${it.key.length}:${it.key}=${it.value}" }
            if (id in pool) continue
            val assignmentKeys = loading.aggregation.assignments.filter { (_, variable) -> (byVar[variable] ?: Flt64.zero) > Flt64(0.5) }.keys
            if (assignmentKeys.map { it.itemId }.distinct().size != assignmentKeys.size) return Failed(ErrorCode.IllegalArgument, "局部列重复使用货物 / A local column uses cargo more than once")
            val assignments = assignmentKeys.associate { it.itemId to it.positionId }
            val occupied = loading.aggregation.occupied.filter { (_, variable) -> (byVar[variable] ?: Flt64.zero) > Flt64(0.5) }.keys
            if (loading.problem.conflicts.any { it.first in occupied && it.second in occupied }) return Failed(ErrorCode.IllegalArgument, "局部列存在内部占位冲突 / A local column has an internal occupancy conflict")
            if (loading.problem.layoutGroups.any { group -> group.layouts.none { it.positionIds.containsAll(occupied.intersect(group.positionIds)) } }) {
                return Failed(ErrorCode.IllegalArgument, "局部列没有兼容布局 / Local column has no compatible layout")
            }
            val positions = partitions.first { it.id == partitionId }.positionIds
            val weights = positions.associateWith { position ->
                val weight = assignments.filterValues { it == position }.keys.fold(Flt64.zero) { total, item ->
                    total + loading.aggregation.weights.getValue(position).monomials.filter { term -> term.symbol == loading.aggregation.assignments[AssignmentKey(item, position)] }.fold(Flt64.zero) { sum, term -> sum + term.coefficient }
                }
                Quantity(loading.adapter.fromSolver(weight), loading.problem.weightUnit)
            }
            projection.columnsChanged()
            pool[id] = PartitionLoadingColumn(
                id = id, partitionId = partitionId, decisions = vector.toMap(), assignments = assignments,
                occupiedPositions = occupied, cargoUsage = assignments.keys.associateWith { UInt64.one },
                counts = positions.associateWith { position -> UInt64(assignments.values.count { it == position }.toULong()) },
                weights = weights,
                compatibleLayouts = loading.problem.layoutGroups.associate { group -> group.id to group.layouts.filter { it.positionIds.containsAll(occupied.intersect(group.positionIds)) }.map { it.id }.toSet() }
            )
        }
        return ok
    }

    /** 删除列，下一轮注册不保留任何旧 token。 / Remove columns without retaining old tokens in the next registration.
     * @param ids 列 ID / Column IDs
     * @return 操作结果 / Operation result
     */
    fun removeColumns(ids: Set<String>): Try {
        if (ids.any { it in pool }) projection.columnsChanged()
        ids.forEach { pool.remove(it) }
        return ok
    }

    /** 刷新共享分解层影子价格。 / Refresh shared-decomposition shadow prices.
     * @param registration 产生结果的当前注册 / Current registration that produced the result
     * @param dual 当前最优 LP 结果 / Current optimal LP result
     * @return 验证结果 / Validation result
     */
    suspend fun refreshShadowPrice(registration: PartitionMasterRegistration, dual: ColumnGenerationSolver.LPResult): Try {
        if (dual.status != SolverStatus.Optimal) return Failed(ErrorCode.IllegalArgument, "定价需要最优 LP 对偶 / Pricing requires optimal LP duals")
        return projection.refreshShadowPrice(registration, dual.dualSolution)
    }

    /** 从共享层获取同源、带版本的定价系数。 / Obtain same-source versioned pricing coefficients.
     * @param partitionId 分区身份 / Partition identity
     * @param phaseOne 是否人工变量阶段 / Artificial-variable phase
     * @param seed 是否显式零对偶种子阶段 / Explicit zero-dual seeding
     * @return 定价请求或过期状态错误 / Pricing request or stale-state error
     */
    suspend fun pricingRequest(partitionId: String, phaseOne: Boolean, seed: Boolean = false): Ret<PartitionPricingRequest> {
        return projection.pricingRequest(
            blockId = partitionId,
            phaseOne = phaseOne,
            seed = seed
        )
    }

    /** 构造包含所有局部硬行的定价模型，可保留连续预估重量。 / Build a pricing model retaining local hard rows and continuous estimated weights.
     * @param request 定价请求 / Pricing request
     * @return 局部 MILP / Local MILP
     */
    suspend fun pricingModel(request: PartitionPricingRequest): Ret<LinearMetaModel<Flt64>> {
        val partition = partitions.find { it.id == request.partitionId }
            ?: return Failed(ErrorCode.IllegalArgument, "未知定价分区 / Unknown pricing partition: ${request.partitionId}")
        val model = when (val result = projection.pricingModel(request)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val positions = partition.positionIds
        for (item in loading.problem.items) {
            val assignments = loading.aggregation.assignments.filterKeys { it.itemId == item.id && it.positionId in positions }.values
            val added = model.addConstraint(sum(assignments.toList()) leq Flt64.one, name = "local_unique_${item.id}")
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        for ((index, clique) in loading.conflictIndex.cliques.withIndex()) {
            val localPositions = clique.intersect(positions)
            if (localPositions.size < 2) continue
            val added = model.addConstraint(sum(localPositions.map { loading.aggregation.occupied.getValue(it) }) leq Flt64.one, name = "local_conflict_$index")
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        for ((index, group) in loading.problem.layoutGroups.withIndex()) {
            if (group.positionIds.intersect(positions).isEmpty()) continue
            val layouts = group.layouts.mapIndexed { layoutIndex, layout -> layout to BinVar("local_layout_${index}_$layoutIndex") }
            for ((_, variable) in layouts) {
                val added = model.add(variable)
                if (added is Failed) return Failed(added.error)
                if (added is Fatal) return Fatal(added.errors)
            }
            val choice = model.addConstraint(sum(layouts.map { it.second }) eq Flt64.one, name = "local_layout_choose_$index")
            if (choice is Failed) return Failed(choice.error)
            if (choice is Fatal) return Fatal(choice.errors)
            for (position in group.positionIds.intersect(positions)) {
                val added = model.addConstraint(
                    LinearPolynomial(loading.aggregation.occupied.getValue(position)) leq sum(layouts.filter { position in it.first.positionIds }.map { it.second }),
                    name = "local_layout_${index}_$position"
                )
                if (added is Failed) return Failed(added.error)
                if (added is Fatal) return Fatal(added.errors)
            }
        }
        return Ok(model)
    }

    /** 重建受限 LP 或最终整数主问题；全局业务变量仍按原模型注册。 / Rebuild the restricted LP or final integer master, retaining original global variables.
     * @param phaseOne 是否使用无大 M 的人工变量可行性阶段 / Whether to run a big-M-free feasibility phase
     * @param integer 是否最终整数列选择 / Whether selecting integer columns
     * @param refineResources 是否仅在已有轮廓内补全附加资源 / Whether completing additional resources only inside existing profiles
     * @return 当前注册结果 / Current registration
     */
    suspend fun master(phaseOne: Boolean, integer: Boolean, refineResources: Boolean = false): Ret<PartitionMasterRegistration> {
        val refreshed = refresh()
        if (refreshed is Failed) return Failed(refreshed.error)
        if (refreshed is Fatal) return Fatal(refreshed.errors)
        return projection.master(
            columns = columns.map { ProjectedColumn(id = it.id, blockId = it.partitionId, values = it.decisions) },
            phaseOne = phaseOne,
            integer = integer,
            refinedResources = if (refineResources) resourceDecisions.map { it.variable }.toSet() else emptySet(),
            tolerance = Flt64(1e-6)
        )
    }

    /** 按列和原变量身份合并完整模型解。 / Merge columns and original global variables by identity.
     * @param registration 当前整数主问题 / Current integer master
     * @param values 主问题结果 / Master values
     * @return 完整模型自身 token 顺序的结果 / Values in full-model token order
     */
    suspend fun extractSolution(registration: PartitionMasterRegistration, values: List<Flt64>): Ret<List<Flt64>> {
        return projection.extractSolution(
            registration = registration,
            values = values,
            tolerance = Flt64(1e-6)
        )
    }
}
