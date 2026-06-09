@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.Flt64CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.Flt64SlotBasedCapacityResult
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.toGeneric
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.Capacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityColumn
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.IterativeCapacityCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.SolverTimeWindowBoundary
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.PlusGroup
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

typealias CapacityPreSolveSolver = suspend (AbstractLinearMetaModel<Flt64>) -> Ret<*>

private fun UInt64.solverAllocationAmount() = Flt64(toLong().toDouble())

/**
 * 将 wildcard 错误收敛为 ErrorCode 错误。
 * 当前预求解链路约定 solver 返回 ErrorCode；若出现非 ErrorCode，降级为 Unknown 并保留原始信息。
 *
 * Normalizes wildcard errors to ErrorCode errors.
 * The pre-solve pipeline expects solver errors to use ErrorCode; if not,
 * it degrades to Unknown while preserving original error details.
 */
private fun errorCodeErrorOf(error: Error<*>): Error<ErrorCode> {
    val code = error.code as? ErrorCode
    return if (code != null) {
        if (error.withValue) {
            ExErr(code, error.message, error.value)
        } else {
            Err(code, error.message)
        }
    } else {
        ExErr(
            ErrorCode.Unknown,
            "slot_based_capacity_presolver.solve received non-ErrorCode solver error.",
            error.toString()
        )
    }
}

private fun errorCodeErrorsOf(errors: List<Error<*>>): List<Error<ErrorCode>> {
    return errors.map(::errorCodeErrorOf)
}

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
    private val timeWindow: TimeWindow<Flt64>,

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
     * 通过 solver 时间窗口边界创建分时隙产能预求解服务 /
     * Create slot-based capacity pre-solving service from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param actions 生产动作列表 / List of production actions
     * @param executors 执行器列表 / List of executors
     * @param slots 时隙列表 / List of time slots
     * @param products 产品列表及其需求量 / Products with demand quantities
     * @param resourceCapacities 资源容量列表 / List of resource capacities
     * @param useColumnGeneration 是否使用列生成 / Whether to use column generation
     * @param unitProduceOfAction 动作单位操作时间产品产量 / Product production per unit operation time
     * @param unitConsumptionOfAction 动作单位操作时间原料消耗 / Material consumption per unit operation time
     * @param unitResourceUsageOfAction 动作在时隙内单位操作时间资源使用量 / Resource usage per unit operation time in slot
     * @param materials 原料列表 / Material list
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        actions: List<A>,
        executors: List<E>,
        slots: List<TimeSlot>,
        products: List<Pair<M, Flt64>> = emptyList(),
        resourceCapacities: List<R> = emptyList(),
        useColumnGeneration: Boolean = false,
        unitProduceOfAction: ((A, M) -> Flt64)? = null,
        unitConsumptionOfAction: ((A, M) -> Flt64)? = null,
        unitResourceUsageOfAction: ((A, R, TimeSlot) -> Flt64)? = null,
        materials: List<M> = emptyList()
    ) : this(
        actions = actions,
        executors = executors,
        slots = slots,
        timeWindow = timeBoundary.source,
        products = products,
        resourceCapacities = resourceCapacities,
        useColumnGeneration = useColumnGeneration,
        unitProduceOfAction = unitProduceOfAction,
        unitConsumptionOfAction = unitConsumptionOfAction,
        unitResourceUsageOfAction = unitResourceUsageOfAction,
        materials = materials
    )

    /**
     * 产能编译对象
     * Capacity compilation object
     */
    private val capacityCompilation: CapacityCompilation<Flt64, A>? = if (!useColumnGeneration) {
        CapacityCompilation(
            actions = actions,
            slots = slots,
            timeWindow = timeWindow
        )
    } else {
        null
    }

    private val iterativeCompilation: IterativeCapacityCompilation<Flt64, E, A>? = if (useColumnGeneration) {
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
    fun register(model: LinearMetaModel<Flt64>): Try {
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
        columns: List<CapacityColumn<E, A, Flt64>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CapacityColumn<E, A, Flt64>>> {
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
        model: AbstractLinearMetaModel<Flt64>,
        solver: CapacityPreSolveSolver,
        initialColumnsByIteration: Map<UInt64, List<CapacityColumn<E, A, Flt64>>> = emptyMap()
    ): Ret<Flt64CapacityIntermediateValues<A, M, R>> {
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
            is Ok<*, *, *> -> {}
            is Failed<*, *, *> -> return Failed(errorCodeErrorOf(result.error))
            is Fatal<*, *, *> -> return Fatal(errorCodeErrorsOf(result.errors))
        }

        // Extract intermediate values
        // 提取中间�?
        return extractIntermediateValues(model)
    }

    /**
     * 执行预求解并转换为泛型物理量结果 / Execute pre-solving and convert to generic quantity results
     *
     * @param V 目标数值类型 / Target numeric type
     * @param model 线性元模型 / Linear meta model
     * @param solver 求解器 / Solver
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param initialColumnsByIteration 按迭代分组的初始列 / Initial columns grouped by iteration
     * @return 泛型产能中间值 / Generic capacity intermediate values
     */
    suspend fun <V> solveGeneric(
        model: AbstractLinearMetaModel<Flt64>,
        solver: CapacityPreSolveSolver,
        adapter: SchedulingSolverValueAdapter<V>,
        initialColumnsByIteration: Map<UInt64, List<CapacityColumn<E, A, Flt64>>> = emptyMap()
    ): Ret<CapacityIntermediateValues<A, M, R, V>> where V : RealNumber<V>, V : PlusGroup<V> {
        return when (val result = solve(
            model = model,
            solver = solver,
            initialColumnsByIteration = initialColumnsByIteration
        )) {
            is Ok -> {
                Ok(result.value.toGeneric(adapter))
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
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
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Flt64CapacityIntermediateValues<A, M, R>> {
        val results = HashMap<TimeSlot, Flt64SlotBasedCapacityResult<A, M, R>>()

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
                acc + alloc.action.unitCost(alloc.slot.time.start) * alloc.amount.solverAllocationAmount()
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

            results[slot] = Flt64SlotBasedCapacityResult(
                slot = slot,
                slotIndex = slotIndex,
                actionAllocations = allocations,
                totalCost = totalCost,
                produceByProduct = produceByProduct,
                consumptionByMaterial = consumptionByMaterial,
                resourceUsageByResource = resourceUsageByResource
            )
        }

        return Ok(Flt64CapacityIntermediateValues(slots, results))
    }

    /**
     * 提取中间值并转换为泛型物理量结果 / Extract intermediate values and convert to generic quantity results
     *
     * @param V 目标数值类型 / Target numeric type
     * @param model 已求解的线性元模型 / Solved linear meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @return 泛型产能中间值 / Generic capacity intermediate values
     */
    fun <V> extractIntermediateValuesGeneric(
        model: AbstractLinearMetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>
    ): Ret<CapacityIntermediateValues<A, M, R, V>> where V : RealNumber<V>, V : PlusGroup<V> {
        return when (val result = extractIntermediateValues(model)) {
            is Ok -> {
                Ok(result.value.toGeneric(adapter))
            }

            is Failed -> {
                Failed(result.error)
            }

            is Fatal -> {
                Fatal(result.errors)
            }
        }
    }
}

