package fuookami.ospf.kotlin.framework.bpp.domain.partition_loading

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
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
 * @property rowContributions 完整模型各行的列系数，包括下游业务行 / Column coefficients for all full-model rows, including business rows
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
    val compatibleLayouts: Map<String, Set<String>>,
    val rowContributions: Map<String, Flt64>
)

/** 定价请求；线性业务行和目标自动参与 reduced-cost 系数。 / Pricing request including all linear business rows and objectives.
 * @property coefficients 稳定局部决策 ID 到检验数系数 / Stable decision IDs to reduced-cost coefficients
 * @property convexityPrice 分区恰选一列行的影子价格 / Exactly-one-column shadow price
 * @property rowPrices 稳定完整行 ID 到影子价格 / Stable full-row IDs to shadow prices
 */
data class PartitionPricingRequest(
    val partitionId: String,
    val coefficients: Map<String, Flt64>,
    val convexityPrice: Flt64,
    val rowPrices: Map<String, Flt64>
) {
    /** 计算完整检验数。 / Evaluate the complete reduced cost.
     * @param values 局部决策向量 / Local decision vector
     * @return 检验数或不完整/非有限向量错误 / Reduced cost or incomplete/nonfinite vector error
     */
    fun reducedCost(values: Map<String, Flt64>): Ret<Flt64> {
        if (values.keys != coefficients.keys || values.values.any { !it.isFinite() } || coefficients.values.any { !it.isFinite() } || !convexityPrice.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "定价向量必须完整且有限 / Pricing vector must be complete and finite")
        }
        val cost = coefficients.entries.fold(-convexityPrice) { total, (id, coefficient) -> total + coefficient * values.getValue(id) }
        return if (cost.isFinite()) Ok(cost) else Failed(ErrorCode.IllegalArgument, "检验数溢出 / Reduced cost overflow")
    }
}

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
data class PartitionMasterRow(
    val id: String,
    val sourceName: String,
    val coefficients: Map<AbstractVariableItem<*, *>, Flt64>,
    val rhs: Flt64,
    val comparison: Comparison
)

/** 每轮新建的受限主问题，避免目标展平和删列留下旧引用。 / Fresh restricted master, avoiding stale flattened objectives and removed-column references.
 * @property columnVariables 列 ID 到当前模型变量 / Column IDs to model-local variables
 */
data class PartitionMasterRegistration(
    val model: LinearMetaModel<Flt64>,
    val columnVariables: Map<String, URealVar>,
    val refinedResources: Set<AbstractVariableItem<*, *>> = emptySet()
)

/** 分区列生命周期及完整模型系数桥接。 / Partition-column lifecycle and full-model coefficient bridge.
 * @property decisions 显式、唯一的局部变量归属 / Explicit unique local ownership
 * @property rows 所有原始硬约束，跨分区行不会丢失 / All original hard rows, including cross-partition rows
 */
class PartitionLoadingContext<V : FloatingNumber<V>> private constructor(
    val loading: DiscreteLoadingContext<V>,
    val source: LinearMetaModel<Flt64>,
    val partitions: List<LoadingPartition>,
    val decisions: List<PartitionDecision>,
    val variables: List<AbstractVariableItem<*, *>>,
    val rows: List<PartitionMasterRow>,
    val objective: Map<AbstractVariableItem<*, *>, Flt64>,
    val objectiveConstant: Flt64
) {
    private val pool = linkedMapOf<String, PartitionLoadingColumn<V>>()
    val columns: List<PartitionLoadingColumn<V>> get() = pool.values.toList()
    private val byVariable = decisions.associateBy { it.variable }
    /** 可在已生成分配/占位轮廓内精确补全的下游资源变量。 / Downstream resources that can be completed inside generated assignment/occupancy profiles. */
    val resourceDecisions: List<PartitionDecision> get() {
        val core = loading.aggregation.assignments.values.toSet() + loading.aggregation.occupied.values
        return decisions.filter { it.variable !in core }
    }
    private var rowPrices = emptyMap<String, Flt64>()
    private var convexityPrices = emptyMap<String, Flt64>()

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
            val expanded = when (val result = LinearMechanismModel(source)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return expanded.use { full ->
                val variables = full.tokens.tokens.map { it.variable }
                if (bindings.any { it.variable !in variables }) return@use Failed(ErrorCode.IllegalArgument, "局部决策不属于完整模型 / Local decision is not in the full model")
                for (variable in variables.filter { source.tokens.find(it) == null }) {
                    val added = source.add(variable)
                    if (added !is Ok) return@use when (added) { is Failed -> Failed(added.error); is Fatal -> Fatal(added.errors); else -> Failed(ErrorCode.ApplicationError) }
                }
                val rows = full.constraints.mapIndexed { index, row ->
                    val coefficients = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
                    for (cell in row.lhs) {
                        cell as LinearCell<Flt64>
                        coefficients[cell.token.variable] = (coefficients[cell.token.variable] ?: Flt64.zero) + cell.coefficient
                    }
                    PartitionMasterRow("full_row_$index", row.name, coefficients.filterValues { it != Flt64.zero }, row.rhs, when (row.sign) {
                        ConstraintRelation.LessEqual -> Comparison.LE
                        ConstraintRelation.GreaterEqual -> Comparison.GE
                        else -> Comparison.EQ
                    })
                }
                val objective = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
                var constant = Flt64.zero
                for (part in full.objectFunction.subObjects) {
                    val sign = if (part.category == ObjectCategory.Minimum) Flt64.one else -Flt64.one
                    constant += sign * part.constant
                    for ((coefficient, variable) in part.linearTerms()) objective[variable] = (objective[variable] ?: Flt64.zero) + sign * coefficient
                }
                Ok(PartitionLoadingContext(loading, source, partitions, bindings, variables, rows, objective, constant))
            }
        }
    }

    /** 注册种子列。 / Register seed columns.
     * @param values 分区和局部决策向量 / Partition IDs and local vectors
     * @return 注册结果 / Registration result
     */
    fun register(values: List<Pair<String, Map<String, Flt64>>>): Try = addColumns(values)

    /** 增列，独立验证局部边界和所有局部硬约束。 / Add columns after independently checking local bounds and every local hard row.
     * @param values 分区及具体配置 / Partitions and concrete configurations
     * @return 操作结果 / Operation result
     */
    fun addColumns(values: List<Pair<String, Map<String, Flt64>>>): Try {
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
            pool[id] = PartitionLoadingColumn(
                id = id, partitionId = partitionId, decisions = vector.toMap(), assignments = assignments,
                occupiedPositions = occupied, cargoUsage = assignments.keys.associateWith { UInt64.one },
                counts = positions.associateWith { position -> UInt64(assignments.values.count { it == position }.toULong()) },
                weights = weights,
                compatibleLayouts = loading.problem.layoutGroups.associate { group -> group.id to group.layouts.filter { it.positionIds.containsAll(occupied.intersect(group.positionIds)) }.map { it.id }.toSet() },
                rowContributions = rows.associate { row -> row.id to row.coefficients.entries.fold(Flt64.zero) { total, (variable, coefficient) -> total + coefficient * (byVar[variable] ?: Flt64.zero) } }
            )
        }
        return ok
    }

    /** 删除列，下一轮注册不保留任何旧 token。 / Remove columns without retaining old tokens in the next registration.
     * @param ids 列 ID / Column IDs
     * @return 操作结果 / Operation result
     */
    fun removeColumns(ids: Set<String>): Try { ids.forEach { pool.remove(it) }; return ok }

    /** 刷新稳定行 ID 的影子价格。 / Refresh shadow prices by stable row ID.
     * @param dual LP 原始行对偶价格 / LP original-row duals
     * @return 操作结果 / Operation result
     */
    fun refreshShadowPrice(dual: ColumnGenerationSolver.LPResult): Try {
        if (dual.dualSolution.values.any { !it.isFinite() }) return Failed(ErrorCode.IllegalArgument, "影子价格必须有限 / Shadow prices must be finite")
        rowPrices = dual.dualSolution.entries.associate { it.key.name to it.value }
        convexityPrices = partitions.associate { it.id to (rowPrices["partition_choose_${it.id}"] ?: Flt64.zero) }
        return ok
    }

    /** 完整的定价系数，包含全局资源、冲突、布局和下游行。 / Complete pricing coefficients including resources, conflicts, layouts and business rows.
     * @param partitionId 分区 ID / Partition ID
     * @param phaseOne 是否最小化人工变量 / Whether minimizing artificial variables
     * @return 定价请求 / Pricing request
     */
    fun pricingRequest(partitionId: String, phaseOne: Boolean): PartitionPricingRequest {
        val coefficients = decisions.filter { it.partitionId == partitionId }.associate { decision ->
            var value = if (phaseOne) Flt64.zero else objective[decision.variable] ?: Flt64.zero
            for (row in rows) value -= (rowPrices[row.id] ?: Flt64.zero) * (row.coefficients[decision.variable] ?: Flt64.zero)
            decision.id to value
        }
        return PartitionPricingRequest(partitionId, coefficients, convexityPrices[partitionId] ?: Flt64.zero, rowPrices)
    }

    /** 构造包含所有局部硬行的定价模型，可保留连续预估重量。 / Build a pricing model retaining local hard rows and continuous estimated weights.
     * @param request 定价请求 / Pricing request
     * @return 局部 MILP / Local MILP
     */
    fun pricingModel(request: PartitionPricingRequest): Ret<LinearMetaModel<Flt64>> {
        val partition = partitions.find { it.id == request.partitionId }
            ?: return Failed(ErrorCode.IllegalArgument, "未知定价分区 / Unknown pricing partition: ${request.partitionId}")
        val local = decisions.filter { it.partitionId == request.partitionId }
        if (request.coefficients.keys != local.map { it.id }.toSet() || request.coefficients.values.any { !it.isFinite() } || !request.convexityPrice.isFinite()) {
            return Failed(ErrorCode.IllegalArgument, "定价系数必须完整且有限 / Pricing coefficients must be complete and finite")
        }
        val model = LinearMetaModel<Flt64>(name = "partition_price_${request.partitionId}", converter = IntoValue.Identity)
        for (decision in local) {
            val result = model.add(decision.variable)
            if (result is Failed) return Failed(result.error)
            if (result is Fatal) return Fatal(result.errors)
        }
        val variables = local.map { it.variable }.toSet()
        for (row in rows.filter { it.coefficients.keys.all { variable -> variable in variables } }) {
            val result = model.addConstraint(LinearInequality(
                LinearPolynomial(row.coefficients.map { LinearMonomial(it.value, it.key) }, Flt64.zero), LinearPolynomial(row.rhs), row.comparison
            ), name = row.id)
            if (result is Failed) return Failed(result.error)
            if (result is Fatal) return Fatal(result.errors)
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
        val result = model.minimize(LinearPolynomial(local.map { LinearMonomial(request.coefficients.getValue(it.id), it.variable) }, -request.convexityPrice))
        if (result is Failed) return Failed(result.error)
        if (result is Fatal) return Fatal(result.errors)
        return Ok(model)
    }

    /** 重建受限 LP 或最终整数主问题；全局业务变量仍按原模型注册。 / Rebuild the restricted LP or final integer master, retaining original global variables.
     * @param phaseOne 是否使用无大 M 的人工变量可行性阶段 / Whether to run a big-M-free feasibility phase
     * @param integer 是否最终整数列选择 / Whether selecting integer columns
     * @param refineResources 是否仅在已有轮廓内补全附加资源 / Whether completing additional resources only inside existing profiles
     * @return 当前注册结果 / Current registration
     */
    fun master(phaseOne: Boolean, integer: Boolean, refineResources: Boolean = false): Ret<PartitionMasterRegistration> {
        val model = LinearMetaModel<Flt64>(name = "partition_master_${phaseOne}_$integer", converter = IntoValue.Identity)
        val flexible = if (refineResources) resourceDecisions.map { it.variable }.toSet() else emptySet()
        val lambdas = linkedMapOf<String, URealVar>()
        for ((index, column) in columns.withIndex()) {
            val variable = URealVar("partition_column_$index")
            lambdas[column.id] = variable
            val added = model.add(variable)
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
            if (integer) {
                val selected = BinVar("partition_selected_$index")
                for (added in listOf(model.add(selected), model.addConstraint(LinearPolynomial(variable) eq LinearPolynomial(selected), name = "partition_integral_$index"))) {
                    if (added is Failed) return Failed(added.error)
                    if (added is Fatal) return Fatal(added.errors)
                }
            }
        }
        for (variable in variables.filter { it !in byVariable || it in flexible }) {
            val added = model.add(variable)
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        for (partition in partitions) {
            val added = model.addConstraint(sum(columns.filter { it.partitionId == partition.id }.map { lambdas.getValue(it.id) }) eq Flt64.one, name = "partition_choose_${partition.id}")
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        var objective = LinearPolynomial(if (phaseOne) Flt64.zero else objectiveConstant)
        if (!phaseOne) {
            for ((variable, coefficient) in this.objective.filterKeys { it !in byVariable || it in flexible }) objective += LinearPolynomial(coefficient * variable)
            for (column in columns) {
                val coefficient = decisions.filter { it.partitionId == column.partitionId && it.variable !in flexible }.fold(Flt64.zero) { total, decision -> total + (this.objective[decision.variable] ?: Flt64.zero) * column.decisions.getValue(decision.id) }
                objective += LinearPolynomial(coefficient * lambdas.getValue(column.id))
            }
        }
        for (row in rows) {
            var lhs = LinearPolynomial(row.coefficients.filterKeys { it !in byVariable || it in flexible }.map { LinearMonomial(it.value, it.key) }, Flt64.zero)
            for (column in columns) {
                val flexibleContribution = resourceDecisions.filter { it.partitionId == column.partitionId && it.variable in flexible }.fold(Flt64.zero) { total, decision ->
                    total + (row.coefficients[decision.variable] ?: Flt64.zero) * column.decisions.getValue(decision.id)
                }
                lhs += LinearPolynomial((column.rowContributions.getValue(row.id) - flexibleContribution) * lambdas.getValue(column.id))
            }
            if (phaseOne) {
                if (row.comparison != Comparison.GE) {
                    val slack = URealVar("excess_${row.id}")
                    val added = model.add(slack)
                    if (added is Failed) return Failed(added.error)
                    if (added is Fatal) return Fatal(added.errors)
                    lhs -= LinearPolynomial(slack); objective += LinearPolynomial(slack)
                }
                if (row.comparison != Comparison.LE) {
                    val slack = URealVar("shortage_${row.id}")
                    val added = model.add(slack)
                    if (added is Failed) return Failed(added.error)
                    if (added is Fatal) return Fatal(added.errors)
                    lhs += LinearPolynomial(slack); objective += LinearPolynomial(slack)
                }
            }
            val added = model.addConstraint(LinearInequality(lhs, LinearPolynomial(row.rhs), row.comparison), name = row.id)
            if (added is Failed) return Failed(added.error)
            if (added is Fatal) return Fatal(added.errors)
        }
        val minimized = model.minimize(objective)
        if (minimized is Failed) return Failed(minimized.error)
        if (minimized is Fatal) return Fatal(minimized.errors)
        return Ok(PartitionMasterRegistration(model, lambdas, flexible))
    }

    /** 按列和原变量身份合并完整模型解。 / Merge columns and original global variables by identity.
     * @param registration 当前整数主问题 / Current integer master
     * @param values 主问题结果 / Master values
     * @return 完整模型自身 token 顺序的结果 / Values in full-model token order
     */
    fun extractSolution(registration: PartitionMasterRegistration, values: List<Flt64>): Ret<List<Flt64>> {
        val full = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
        fun value(variable: AbstractVariableItem<*, *>) = registration.model.tokens.indexOf(variable)?.let { values.getOrNull(it) }
        for (variable in variables.filter { it !in byVariable || it in registration.refinedResources }) full[variable] = value(variable) ?: return Failed(ErrorCode.DataNotFound, "缺少全局变量值 / Missing global variable value")
        for (partition in partitions) {
            val selected = columns.filter { it.partitionId == partition.id && (value(registration.columnVariables.getValue(it.id)) ?: Flt64.zero) > Flt64(0.5) }
            if (selected.size != 1) return Failed(ErrorCode.ApplicationError, "分区必须恰选一个完整配置 / A partition must select exactly one complete configuration")
            for (decision in decisions.filter { it.partitionId == partition.id && it.variable !in registration.refinedResources }) full[decision.variable] = selected.single().decisions.getValue(decision.id)
        }
        return Ok(source.tokens.tokensInSolver.map { full[it.variable] ?: return Failed(ErrorCode.DataNotFound, "缺少完整模型变量 / Missing full-model variable") })
    }
}
