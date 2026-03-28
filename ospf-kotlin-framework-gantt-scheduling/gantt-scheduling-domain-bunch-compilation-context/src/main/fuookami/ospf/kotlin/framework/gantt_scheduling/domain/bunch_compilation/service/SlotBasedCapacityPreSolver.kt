@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedCapacityResult
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.IterativeCapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64

typealias CapacityPreSolveSolver = suspend (AbstractLinearMetaModel) -> Ret<*>

/**
 * 分时隙产能预求解服务
 * Slot-based capacity pre-solving service
 *
 * 负责求解 capacity scheduling 问题并提取中间值�?
 * Responsible for solving capacity scheduling problem and extracting intermediate values.
 *
 * 中间值用于给 bunch 生成器提供时隙约束�?
 * Intermediate values are used to provide slot constraints to bunch generators.
 *
 * @param E 执行器类�?/ Executor type
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 */
class SlotBasedCapacityPreSolver<E : Executor, A : ProductionAction, M, R>(
    /**
     * 生产动作列表
     * List of production actions
     */
    private val actions: List<A>,

    /**
     * 执行器列�?
     * List of executors
     */
    private val executors: List<E>,

    /**
     * 时隙列表
     * List of time slots
     */
    private val slots: List<TimeSlot>,

    /**
     * 时间窗口
     * Time window
     */
    private val timeWindow: TimeWindow,

    /**
     * 产品列表及其需求量
     * Products with their demand quantities
     */
    private val products: List<Pair<M, Flt64>> = emptyList(),

    /**
     * 资源容量列表
     * List of resource capacities
     */
    private val resourceCapacities: List<R> = emptyList(),

    /**
     * 是否使用列生�?
     * Whether to use column generation
     */
    private val useColumnGeneration: Boolean = false,

    /**
     * 计算动作单位操作时间的产品产�?
     * Calculate product produce per unit operation time for action
     */
    private val unitProduceOfAction: ((A, M) -> Flt64)? = null,

    /**
     * 计算动作单位操作时间的原料消�?
     * Calculate material consumption per unit operation time for action
     */
    private val unitConsumptionOfAction: ((A, M) -> Flt64)? = null,

    /**
     * 计算动作在时隙内单位操作时间的资源使用量
     * Calculate resource usage per unit operation time for action in slot
     */
    private val unitResourceUsageOfAction: ((A, R, TimeSlot) -> Flt64)? = null,

    /**
     * 原料列表
     * Material list
     */
    private val materials: List<M> = emptyList()
) {
    /**
     * 产能编译对象
     * Capacity compilation object
     */
    private val capacityCompilation: CapacityCompilation<A>? = if (!useColumnGeneration) {
        CapacityCompilation(
            actions = actions,
            slots = slots,
            timeWindow = timeWindow
        )
    } else {
        null
    }

    private val iterativeCompilation: IterativeCapacityCompilation<E, A>? = if (useColumnGeneration) {
        IterativeCapacityCompilation(
            executors = executors,
            actions = actions,
            slots = slots,
            timeWindow = timeWindow
        )
    } else {
        null
    }

    private val compilation: Capacity<A>
        get() = if (useColumnGeneration) {
            iterativeCompilation!!
        } else {
            capacityCompilation!!
        }

    /**
     * 注册到模�?
     * Register to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    fun register(model: LinearMetaModel): Try {
        return if (useColumnGeneration) {
            iterativeCompilation!!.register(model)
        } else {
            capacityCompilation!!.register(model)
        }
    }

    /**
     * 在列生成模式下添加预求解�?
     * Add pre-solving columns in column-generation mode
     *
     * @param iteration Iteration number / 迭代编号
     * @param columns Columns to add / 待添加列
     * @param model Linear meta model / 线性元模型
     * @return Added columns / 实际添加的列
     */
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        model: AbstractLinearMetaModel
    ): Ret<List<CapacityColumn<E, A>>> {
        if (!useColumnGeneration) {
            return Failed(
                ErrorCode.IllegalArgument,
                "slot_based_capacity_presolver.addColumns is only available when useColumnGeneration=true."
            )
        }
        if (columns.isEmpty()) {
            return Ok(emptyList())
        }
        return iterativeCompilation!!.addColumns(iteration, columns, model)
    }

    /**
     * 执行预求�?
     * Execute pre-solving
     *
     * @param model Linear meta model / 线性元模型
     * @param solver Solver / 求解�?
     * @param initialColumnsByIteration Initial columns grouped by iteration / 按迭代分组的初始�?
     * @return Intermediate values / 中间�?
     */
    suspend fun solve(
        model: AbstractLinearMetaModel,
        solver: CapacityPreSolveSolver,
        initialColumnsByIteration: Map<UInt64, List<CapacityColumn<E, A>>> = emptyMap()
    ): Ret<CapacityIntermediateValues<A, M, R>> {
        if (useColumnGeneration && initialColumnsByIteration.isNotEmpty()) {
            val initialColumnEntries = initialColumnsByIteration.entries.sortedBy { it.key }
            for ((iteration, columns) in initialColumnEntries) {
                when (val result = addColumns(iteration, columns, model)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        // Solve the model
        // 求解模型
        when (val result = solver(model)) {
            is Ok<*, *> -> {}
            is Failed<*, *> -> return Failed(result.error)
            is Fatal<*, *> -> return Fatal(result.errors)
        }

        // Extract intermediate values
        // 提取中间�?
        return extractIntermediateValues(model)
    }

    /**
     * 提取中间�?
     * Extract intermediate values
     *
     * 从已求解的模型中提取每个时隙的产能、产量、资源使用中间值�?
     * Extract capacity, produce, resource usage intermediate values for each slot from solved model.
     *
     * @param model Solved linear meta model / 已求解的线性元模型
     * @return Intermediate values / 中间�?
     */
    fun extractIntermediateValues(
        model: AbstractLinearMetaModel
    ): Ret<CapacityIntermediateValues<A, M, R>> {
        val results = HashMap<TimeSlot, SlotBasedCapacityResult<A, M, R>>()

        // Extract capacity solution
        // 提取产能�?
        val capacitySolution = when (val result = compilation.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Group action allocations by slot
        // 按时隙分组动作分�?
        val allocationsBySlot = HashMap<TimeSlot, MutableList<ActionAllocation<A>>>()
        for (allocation in capacitySolution.actionAllocations) {
            allocationsBySlot.getOrPut(allocation.slot) { mutableListOf() }.add(allocation)
        }

        // Build result for each slot
        // 为每个时隙构建结�?
        for ((slotIndex, slot) in slots.withIndex()) {
            val allocations = allocationsBySlot[slot] ?: emptyList()

            // Calculate total cost for this slot
            // 计算该时隙的总成�?
            val totalCost = allocations.fold(Flt64.zero) { acc, alloc ->
                acc + alloc.action.unitCost(alloc.slot.time.start) * alloc.amount.toFlt64()
            }

            // Calculate produce and consumption by material
            // 计算按物料分组的产量和消耗量
            val produceByProduct = HashMap<M, Flt64>()
            val consumptionByMaterial = HashMap<M, Flt64>()
            val resourceUsageByResource = HashMap<R, Flt64>()

            val productSet = products.map { it.first }.toSet()
            val materialSet = if (materials.isNotEmpty()) {
                materials.toSet()
            } else {
                productSet
            }

            for (allocation in allocations) {
                val operationTime = timeWindow.valueOf(allocation.duration)

                for (product in productSet) {
                    val unitProduce = unitProduceOfAction?.invoke(allocation.action, product) ?: Flt64.zero
                    if (unitProduce neq Flt64.zero) {
                        produceByProduct[product] = (produceByProduct[product] ?: Flt64.zero) + (unitProduce * operationTime)
                    }
                }

                for (material in materialSet) {
                    val unitConsumption = unitConsumptionOfAction?.invoke(allocation.action, material) ?: Flt64.zero
                    if (unitConsumption neq Flt64.zero) {
                        consumptionByMaterial[material] = (consumptionByMaterial[material] ?: Flt64.zero) + (unitConsumption * operationTime)
                    }
                }

                for (resource in resourceCapacities) {
                    val unitUsage = unitResourceUsageOfAction?.invoke(allocation.action, resource, slot) ?: Flt64.zero
                    if (unitUsage neq Flt64.zero) {
                        resourceUsageByResource[resource] = (resourceUsageByResource[resource] ?: Flt64.zero) + (unitUsage * operationTime)
                    }
                }
            }

            results[slot] = SlotBasedCapacityResult(
                slot = slot,
                slotIndex = slotIndex,
                actionAllocations = allocations,
                totalCost = totalCost,
                produceByProduct = produceByProduct,
                consumptionByMaterial = consumptionByMaterial,
                resourceUsageByResource = resourceUsageByResource
            )
        }

        return Ok(CapacityIntermediateValues(slots, results))
    }
}



