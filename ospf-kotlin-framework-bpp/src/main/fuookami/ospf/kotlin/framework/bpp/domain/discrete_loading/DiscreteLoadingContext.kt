package fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.service.limits.*

/** 已验证离散解。 / Independently verified discrete solution.
 * @property assignments 货物到舱位 / Cargo-to-position map
 * @property occupiedPositions 启用舱位（包含预留）/ Occupied positions, including reservations
 * @property selectedLayouts 选择组到布局 / Group-to-layout map
 */
data class DiscreteLoadingSolution(
    val assignments: Map<String, String>,
    val occupiedPositions: Set<String>,
    val selectedLayouts: Map<String, String>
)

/** 离散装载注册入口，每次完整求解使用新实例。 / Discrete-loading registration entry; use a fresh instance per solve.
 * @property problem 领域输入 / Domain input
 * @property adapter 数值适配 / Numeric adapter
 * @property aggregation 求解器注册构件 / Solver registration artifacts
 * @property conflictIndex 重用的冲突索引 / Reusable conflict index
 */
class DiscreteLoadingContext<V : FloatingNumber<V>>(
    val problem: DiscreteLoadingProblem<V>,
    val adapter: PackingValueAdapter<V>
) {
    val aggregation = LoadingAggregation()
    val conflictIndex by lazy { ConflictIndex.build(problem.conflicts) }
    private val capacities = linkedMapOf<String, Flt64>()
    private val itemWeights = linkedMapOf<String, Flt64>()
    private var registered = false

    /** 验证 ID、引用、容量和固定装载。 / Validate IDs, references, capacities and fixed loads.
     * @return 操作结果 / Operation result
     */
    fun validate(): Try {
        fun invalid(message: String): Try = Failed(ErrorCode.IllegalArgument, "装载输入无效 / Invalid loading input: $message")
        val itemIds = problem.items.map { it.id }
        val positionIds = problem.positions.map { it.id }
        if (itemIds.any { it.isBlank() } || itemIds.toSet().size != itemIds.size) return invalid("item IDs")
        if (positionIds.any { it.isBlank() } || positionIds.toSet().size != positionIds.size) return invalid("position IDs")
        if (problem.layoutGroups.map { it.id }.distinct().size != problem.layoutGroups.size) return invalid("layout group IDs")
        if (problem.allowedAssignments.any { it.itemId !in itemIds || it.positionId !in positionIds }) return invalid("assignment reference")
        if (problem.conflicts.any { it.first == it.second || it.first !in positionIds || it.second !in positionIds }) return invalid("conflict reference")
        for (group in problem.layoutGroups) {
            if (group.id.isBlank() || group.layouts.isEmpty() || group.layouts.map { it.id }.distinct().size != group.layouts.size ||
                group.layouts.any { it.id.isBlank() || !group.positionIds.containsAll(it.positionIds) } ||
                !positionIds.containsAll(group.positionIds) ||
                !group.layouts.flatMap { it.positionIds }.containsAll(group.positionIds)) return invalid("layout ${group.id}")
        }
        for (position in problem.positions) {
            val capacity = when (val result = adapter.quantity(position.maxWeight, problem.weightUnit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (capacity < Flt64.zero || position.maxCount == UInt64.zero) return invalid("capacity ${position.id}")
            capacities[position.id] = capacity
        }
        for (item in problem.items) {
            val weight = when (val result = adapter.quantity(item.weight, problem.weightUnit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (weight < Flt64.zero) return invalid("weight ${item.id}")
            itemWeights[item.id] = weight
            if (item.fixedPositionId != null && AssignmentKey(item.id, item.fixedPositionId) !in problem.allowedAssignments) return invalid("fixed assignment ${item.id}")
        }
        val fixedPositions = problem.positions.filter { it.fixedOccupied }.map { it.id }.toSet() + problem.items.mapNotNull { it.fixedPositionId }
        if (problem.conflicts.any { it.first in fixedPositions && it.second in fixedPositions }) return invalid("conflicting fixed occupancy")
        for (group in problem.layoutGroups) {
            if (group.layouts.none { it.positionIds.containsAll(fixedPositions.intersect(group.positionIds)) }) return invalid("fixed layout ${group.id}")
        }
        for (position in problem.positions) {
            val fixedItems = problem.items.filter { it.fixedPositionId == position.id }
            if (UInt64(fixedItems.size.toULong()) > position.maxCount || fixedItems.fold(Flt64.zero) { total, item -> total + itemWeights.getValue(item.id) } > capacities.getValue(position.id)) return invalid("fixed capacity ${position.id}")
        }
        return ok
    }

    /** 注册共享变量及所有通用硬约束。 / Register shared variables and generic hard constraints.
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (registered) return Failed(ErrorCode.IllegalArgument, "上下文已经注册 / Context already registered")
        val validation = validate()
        if (validation !is Ok) return validation
        val positions = problem.positions.associateBy { it.id }
        val items = problem.items.associateBy { it.id }
        val fixedPositionIds = problem.items.mapNotNull { it.fixedPositionId }.toSet()
        for (position in problem.positions) {
            val variable = BinVar("occupied_${position.id}")
            if (position.fixedOccupied || position.id in fixedPositionIds) variable.range.eq(true)
            else if (!position.enabled) variable.range.eq(false)
            aggregation.occupied[position.id] = variable
            val result = model.add(variable)
            if (result !is Ok) return result
        }
        for (item in problem.items) {
            aggregation.loaded[item.id] = LinearPolynomial()
        }
        for (position in problem.positions) {
            aggregation.counts[position.id] = LinearPolynomial()
            aggregation.weights[position.id] = LinearPolynomial()
        }
        for (key in problem.allowedAssignments.sortedWith(compareBy({ it.itemId }, { it.positionId }))) {
            val item = items.getValue(key.itemId)
            val position = positions.getValue(key.positionId)
            if ((!position.enabled || itemWeights.getValue(item.id) > capacities.getValue(position.id)) && item.fixedPositionId != position.id) continue
            if (item.fixedPositionId != null && item.fixedPositionId != position.id) continue
            val variable = BinVar("assignment_${key.itemId}_${key.positionId}")
            if (item.fixedPositionId == position.id) variable.range.eq(true)
            aggregation.assignments[key] = variable
            val result = model.add(variable)
            if (result !is Ok) return result
            aggregation.loaded[item.id] = aggregation.loaded.getValue(item.id) + LinearPolynomial(variable)
            aggregation.counts[position.id] = aggregation.counts.getValue(position.id) + LinearPolynomial(variable)
            aggregation.weights[position.id] = aggregation.weights.getValue(position.id) + LinearPolynomial(itemWeights.getValue(item.id) * variable)
        }
        for (group in problem.layoutGroups) {
            for (layout in group.layouts) {
                val variable = BinVar("layout_${group.id}_${layout.id}")
                aggregation.layouts[group.id to layout.id] = variable
                val result = model.add(variable)
                if (result !is Ok) return result
            }
        }
        var result = LoadingCapacityLimit(problem, aggregation, capacities)(model)
        if (result !is Ok) return result
        result = LoadingConflictLimit(problem, aggregation, conflictIndex)(model)
        if (result !is Ok) return result
        registered = true
        return ok
    }

    /** 追加预估/推荐重量并联动整体占位；上界来自真实舱位容量。 / Link additional weight to whole-position occupancy.
     * @param positionId 舱位 / Position
     * @param additionalWeight 统一重量单位的附加重量表达式 / Additional normalized weight expression
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun linkAdditionalWeight(
        positionId: String,
        additionalWeight: LinearPolynomial<Flt64>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        val active = aggregation.occupied[positionId]
            ?: return Failed(ErrorCode.IllegalArgument, "未知或未注册舱位 / Unknown or unregistered position: $positionId")
        val nonnegative = model.addConstraint(
            relation = additionalWeight geq Flt64.zero,
            name = "additional_weight_nonnegative_$positionId"
        )
        if (nonnegative !is Ok) return nonnegative
        return model.addConstraint(
            relation = (aggregation.weights.getValue(positionId) + additionalWeight) leq capacities.getValue(positionId) * active,
            name = "additional_weight_capacity_$positionId"
        )
    }

    /** 按模型自身 token 提取并独立复核。 / Extract using model-local tokens and independently validate.
     * @param model 元模型 / Meta model
     * @param values 求解向量 / Solver vector
     * @param additionalWeights 附加重量的已提取值，单位同问题 / Extracted additional weights in the problem unit
     * @return 已验证解或失败 / Verified solution or failure
     */
    fun extractSolution(
        model: AbstractLinearMetaModel<Flt64>,
        values: List<Flt64>,
        additionalWeights: Map<String, Flt64> = emptyMap()
    ): Ret<DiscreteLoadingSolution> {
        val byVariable = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
        val variables = aggregation.assignments.values + aggregation.occupied.values + aggregation.layouts.values
        for (variable in variables) {
            val index = model.tokens.indexOf(variable)
                ?: return Failed(ErrorCode.DataNotFound, "缺少装载变量 token / Missing loading variable token: ${variable.name}")
            val value = values.getOrNull(index)
                ?: return Failed(ErrorCode.DataNotFound, "缺少装载变量值 / Missing loading value: ${variable.name}")
            if (!value.toDouble().isFinite() || kotlin.math.min(value.abs().toDouble(), (value - Flt64.one).abs().toDouble()) > 1e-6) {
                return Failed(ErrorCode.ApplicationError, "装载变量不满足二元性 / Non-binary loading value: ${variable.name}")
            }
            byVariable[variable] = value
        }
        val selected = aggregation.assignments.filterValues { byVariable.getValue(it) > Flt64(0.5) }.keys
        if (selected.map { it.itemId }.distinct().size != selected.size) return Failed(ErrorCode.ApplicationError, "货物重复装载 / Duplicate cargo loading")
        val layoutChoices = aggregation.layouts.filterValues { byVariable.getValue(it) > Flt64(0.5) }.keys
        if (layoutChoices.groupingBy { it.first }.eachCount().any { it.value != 1 }) return Failed(ErrorCode.ApplicationError, "布局必须恰选一种 / Exactly one layout must be selected")
        val solution = DiscreteLoadingSolution(
            assignments = selected.associate { it.itemId to it.positionId },
            occupiedPositions = aggregation.occupied.filterValues { byVariable.getValue(it) > Flt64(0.5) }.keys,
            selectedLayouts = layoutChoices.associate { it.first to it.second }
        )
        val result = verify(solution, additionalWeights)
        return when (result) {
            is Ok -> Ok(solution)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /** 独立于求解器的最终复核。 / Solver-independent final verification.
     * @param solution 候选解 / Candidate solution
     * @param additionalWeights 附加重量 / Additional weights
     * @return 操作结果 / Operation result
     */
    fun verify(solution: DiscreteLoadingSolution, additionalWeights: Map<String, Flt64> = emptyMap()): Try {
        val validation = validate()
        if (validation !is Ok) return validation
        fun invalid(reason: String): Try = Failed(ErrorCode.ApplicationError, "装载解复核失败 / Loading verification failed: $reason")
        val positionIds = problem.positions.map { it.id }.toSet()
        if (!positionIds.containsAll(solution.occupiedPositions) || !positionIds.containsAll(additionalWeights.keys)) return invalid("unknown position")
        if (solution.assignments.any { AssignmentKey(it.key, it.value) !in problem.allowedAssignments }) return invalid("illegal assignment")
        if (problem.items.any { (it.required && it.id !in solution.assignments) || (it.fixedPositionId != null && solution.assignments[it.id] != it.fixedPositionId) }) return invalid("required/fixed cargo")
        if (problem.conflicts.any { it.first in solution.occupiedPositions && it.second in solution.occupiedPositions }) return invalid("overlap")
        if (solution.selectedLayouts.keys != problem.layoutGroups.map { it.id }.toSet()) return invalid("layout groups")
        for (group in problem.layoutGroups) {
            val layout = group.layouts.find { it.id == solution.selectedLayouts[group.id] } ?: return invalid("layout ${group.id}")
            if (!layout.positionIds.containsAll(solution.occupiedPositions.intersect(group.positionIds))) return invalid("layout membership ${group.id}")
        }
        for (position in problem.positions) {
            val selected = solution.assignments.filterValues { it == position.id }.keys
            val additional = additionalWeights[position.id] ?: Flt64.zero
            if (!additional.toDouble().isFinite() || additional < Flt64.zero) return invalid("additional weight ${position.id}")
            val weight = selected.fold(additional) { acc, id -> acc + itemWeights.getValue(id) }
            if (UInt64(selected.size.toULong()) > position.maxCount || weight > capacities.getValue(position.id) + Flt64(1e-6)) return invalid("capacity ${position.id}")
            if ((selected.isNotEmpty() || additional > Flt64.zero || position.fixedOccupied) && position.id !in solution.occupiedPositions) return invalid("occupancy ${position.id}")
            if (!position.enabled && position.id in solution.occupiedPositions && !position.fixedOccupied && problem.items.none { it.fixedPositionId == position.id }) return invalid("disabled occupancy ${position.id}")
            if (!position.enabled && selected.any { id -> problem.items.first { it.id == id }.fixedPositionId != position.id }) return invalid("disabled ${position.id}")
        }
        return ok
    }
}
