@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedCapacityResult
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64

typealias CapacityPreSolveSolver = suspend (AbstractLinearMetaModel) -> Ret<*>

/**
 * 分时隙产能预求解服务
 * Slot-based capacity pre-solving service
 *
 * 负责求解 capacity scheduling 问题并提取中间值。
 * Responsible for solving capacity scheduling problem and extracting intermediate values.
 *
 * 中间值用于给 bunch 生成器提供时隙约束。
 * Intermediate values are used to provide slot constraints to bunch generators.
 *
 * @param E 执行器类型 / Executor type
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
     * 执行器列表
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
     * 是否使用列生成
     * Whether to use column generation
     */
    private val useColumnGeneration: Boolean = false
) {
    /**
     * 产能编译对象
     * Capacity compilation object
     */
    private val compilation: Capacity<A> = CapacityCompilation(
        actions = actions,
        slots = slots,
        timeWindow = timeWindow
    )

    /**
     * 注册到模型
     * Register to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    fun register(model: LinearMetaModel): Try {
        return compilation.register(model)
    }
    /**
     * 执行预求解
     * Execute pre-solving
     *
     * @param model Linear meta model / 线性元模型
     * @param solver Solver / 求解器
     * @return Intermediate values / 中间值
     */
    suspend fun solve(
        model: AbstractLinearMetaModel,
        solver: CapacityPreSolveSolver
    ): Ret<CapacityIntermediateValues<A, M, R>> {
        // Solve the model
        // 求解模型
        when (val result = solver(model)) {
            is Ok<*, *> -> {}
            is Failed<*, *> -> return Failed(result.error)
            is Fatal<*, *> -> return Fatal(result.errors)
        }

        // Extract intermediate values
        // 提取中间值
        return extractIntermediateValues(model)
    }

    /**
     * 提取中间值
     * Extract intermediate values
     *
     * 从已求解的模型中提取每个时隙的产能、产量、资源使用中间值。
     * Extract capacity, produce, resource usage intermediate values for each slot from solved model.
     *
     * @param model Solved linear meta model / 已求解的线性元模型
     * @return Intermediate values / 中间值
     */
    fun extractIntermediateValues(
        model: AbstractLinearMetaModel
    ): Ret<CapacityIntermediateValues<A, M, R>> {
        val results = HashMap<TimeSlot, SlotBasedCapacityResult<A, M, R>>()

        // Extract capacity solution
        // 提取产能解
        val capacitySolution = when (val result = compilation.extractSolution(model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Group action allocations by slot
        // 按时隙分组动作分配
        val allocationsBySlot = HashMap<TimeSlot, MutableList<ActionAllocation<A>>>()
        for (allocation in capacitySolution.actionAllocations) {
            allocationsBySlot.getOrPut(allocation.slot) { mutableListOf() }.add(allocation)
        }

        // Build result for each slot
        // 为每个时隙构建结果
        for ((slotIndex, slot) in slots.withIndex()) {
            val allocations = allocationsBySlot[slot] ?: emptyList()

            // Calculate total cost for this slot
            // 计算该时隙的总成本
            val totalCost = allocations.fold(Flt64.zero) { acc, alloc ->
                acc + alloc.action.unitCost(alloc.slot.time.start) * alloc.amount.toFlt64()
            }

            // Calculate produce and consumption by material
            // 计算按物料分组的产量和消耗量
            val produceByProduct = HashMap<M, Flt64>()
            val consumptionByMaterial = HashMap<M, Flt64>()
            val resourceUsageByResource = HashMap<R, Flt64>()

            // Calculate total duration for ratio calculation
            // 计算总时长用于比例计算
            var totalDuration = 0L
            for (allocation in capacitySolution.actionAllocations) {
                totalDuration += allocation.duration.inWholeMilliseconds
            }
            var slotDuration = 0L
            for (allocation in allocations) {
                slotDuration += allocation.duration.inWholeMilliseconds
            }
            val slotRatio = if (totalDuration > 0 && slotDuration > 0) {
                Flt64(slotDuration.toDouble() / totalDuration.toDouble())
            } else {
                Flt64.zero
            }

            // Populate produce and consumption from allocations
            // 从分配中填充产量和消耗量
            for ((product, demandQuantity) in products) {
                if (demandQuantity neq Flt64.zero && slotRatio neq Flt64.zero) {
                    produceByProduct[product] = demandQuantity * slotRatio
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
