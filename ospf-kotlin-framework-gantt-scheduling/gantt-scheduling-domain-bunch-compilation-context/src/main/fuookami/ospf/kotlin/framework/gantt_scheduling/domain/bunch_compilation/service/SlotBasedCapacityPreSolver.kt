@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 分时隙产能预求解服务 / Slot-based capacity pre-solving service */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
/**
 * Type alias for CapacityPreSolveSolver.
 * CapacityPreSolveSolver的类型别名。
*/
typealias CapacityPreSolveSolver = suspend (AbstractLinearMetaModel<Flt64>) -> Ret<*>

/**
 * 将 wildcard 错误收敛为 ErrorCode 错误。
 * 当前预求解链路约定 solver 返回 ErrorCode；若出现非 ErrorCode，降级为 Unknown 并保留原始信息。
 *
 * Normalizes wildcard errors to ErrorCode errors.
 * The pre-solve pipeline expects solver errors to use ErrorCode; if not,
 * it degrades to Unknown while preserving original error details.
 *
 * @param error Solver error to normalize / 要规范化的求解器错误
 * @return Error normalized to ErrorCode type / 规范化为 ErrorCode 类型的错误
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

/**
 * Normalizes a list of wildcard errors to ErrorCode errors.
 * 将通配符错误列表规范化为 ErrorCode 错误列表。
 * @param errors List of errors to normalize / 要规范化的错误列表
 * @return List of errors normalized to ErrorCode type / 规范化为 ErrorCode 类型的错误列表
*/
private fun errorCodeErrorsOf(errors: List<Error<*>>): List<Error<ErrorCode>> {
    return errors.map(::errorCodeErrorOf)
}

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
 * @param V 业务数值类型 / Business numeric type
 * @param E 执行器类型 / Executor type
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @property actions 生产动作列表 / List of production actions
 * @property executors 执行器列表 / List of executors
 * @property slots 时隙列表 / List of time slots
 * @property timeWindow 时间窗口 / Time window
 * @property products 产品列表及其需求量 / Products with their demand quantities
 * @property resourceCapacities 资源容量列表 / List of resource capacities
 * @property useColumnGeneration 是否使用列生成 / Whether to use column generation
 * @property unitProduceOfAction 计算动作单位操作时间的产品产量 / Calculate product produce per unit operation time for action
 * @property unitConsumptionOfAction 计算动作单位操作时间的原料消耗 / Calculate material consumption per unit operation time for action
 * @property unitResourceUsageOfAction 计算动作在时隙内单位操作时间的资源使用量 / Calculate resource usage per unit operation time for action in slot
 * @property materials 原料列表 / Material list
*/
class SlotBasedCapacityPreSolver<V, E : Executor, A : ProductionAction, M, R>(

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
    private val timeWindow: TimeWindow<V>,

    /**
     * 产品列表及其需求量
     * Products with their demand quantities
    */
    private val products: List<Pair<M, Quantity<V>>> = emptyList(),

    /**
     * 资源容量列表
     * List of resource capacities
    */
    private val resourceCapacities: List<R> = emptyList(),

    /**
     * 是否使用列生成
     * Whether to use column generation
    */
    private val useColumnGeneration: Boolean = false,

    /**
     * 计算动作单位操作时间的产品产量
     * Calculate product produce per unit operation time for action
    */
    private val unitProduceOfAction: ((A, M) -> V)? = null,

    /**
     * 计算动作单位操作时间的原料消耗
     * Calculate material consumption per unit operation time for action
    */
    private val unitConsumptionOfAction: ((A, M) -> V)? = null,

    /**
     * 计算动作在时隙内单位操作时间的资源使用量
     * Calculate resource usage per unit operation time for action in slot
    */
    private val unitResourceUsageOfAction: ((A, R, TimeSlot) -> V)? = null,

    /**
     * 原料列表
     * Material list
    */
    private val materials: List<M> = emptyList()
) where V : RealNumber<V>, V : PlusGroup<V> {
    private val solverTimeWindow = timeWindow.toFlt64Boundary()

    /**
     * 产能编译对象
     * Capacity compilation object
    */
    private val capacityCompilation: CapacityCompilation<Flt64, A>? = if (!useColumnGeneration) {
        CapacityCompilation(
            actions = actions,
            slots = slots,
            timeWindow = solverTimeWindow
        )
    } else {
        null
    }

    private val iterativeCompilation: IterativeCapacityCompilation<Flt64, E, A>? = if (useColumnGeneration) {
        IterativeCapacityCompilation(
            executors = executors,
            actions = actions,
            slots = slots,
            timeWindow = solverTimeWindow
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
     * 注册到模型
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
     * 在列生成模式下添加预求解列
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
     * 执行预求解
     * Execute pre-solving
     *
     * @param model Linear meta model / 线性元模型
     * @param solver Solver / 求解器
     * @param initialColumnsByIteration Initial columns grouped by iteration / 按迭代分组的初始列
     * @return Intermediate values / 中间值
    */
    suspend fun solve(
        model: AbstractLinearMetaModel<Flt64>,
        solver: CapacityPreSolveSolver,
        initialColumnsByIteration: Map<UInt64, List<CapacityColumn<E, A, Flt64>>> = emptyMap()
    ): Ret<CapacityIntermediateValues<A, M, R, V>> {
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
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<CapacityIntermediateValues<A, M, R, V>> {
        val results = HashMap<TimeSlot, SlotBasedCapacityResult<A, M, R, V>>()
        val zero = timeWindow.fromDouble(0.0)

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
            val totalCost = allocations.fold(zero) { acc, alloc ->
                acc + alloc.action.unitCost(
                    time = alloc.slot.time.start,
                    fromDouble = timeWindow.fromDouble
                ) * timeWindow.fromDouble(alloc.amount.toSolverFlt64().toDouble())
            }

            // Calculate produce and consumption by material
            // 计算按物料分组的产量和消耗量
            val produceByProduct = HashMap<M, V>()
            val consumptionByMaterial = HashMap<M, V>()
            val resourceUsageByResource = HashMap<R, V>()

            val productSet = products.map { it.first }.toSet()
            val materialSet = if (materials.isNotEmpty()) {
                materials.toSet()
            } else {
                productSet
            }

            for (allocation in allocations) {
                val operationTime = timeWindow.valueOf(allocation.duration)

                for (product in productSet) {
                    val unitProduce = unitProduceOfAction?.invoke(allocation.action, product) ?: zero
                    if (unitProduce neq zero) {
                        produceByProduct[product] = (produceByProduct[product] ?: zero) + (unitProduce * operationTime)
                    }
                }

                for (material in materialSet) {
                    val unitConsumption = unitConsumptionOfAction?.invoke(allocation.action, material) ?: zero
                    if (unitConsumption neq zero) {
                        consumptionByMaterial[material] = (consumptionByMaterial[material] ?: zero) + (unitConsumption * operationTime)
                    }
                }

                for (resource in resourceCapacities) {
                    val unitUsage = unitResourceUsageOfAction?.invoke(allocation.action, resource, slot) ?: zero
                    if (unitUsage neq zero) {
                        resourceUsageByResource[resource] = (resourceUsageByResource[resource] ?: zero) + (unitUsage * operationTime)
                    }
                }
            }

            results[slot] = SlotBasedCapacityResult(
                slot = slot,
                slotIndex = slotIndex,
                actionAllocations = allocations,
                totalCostQuantityValue = Quantity(totalCost, NoneUnit),
                produceQuantityByProduct = produceByProduct.mapValues { (_, v) -> Quantity(v, NoneUnit) },
                consumptionQuantityByMaterial = consumptionByMaterial.mapValues { (_, v) -> Quantity(v, NoneUnit) },
                resourceUsageQuantityByResource = resourceUsageByResource.mapValues { (_, v) -> Quantity(v, NoneUnit) }
            )
        }

        return Ok(CapacityIntermediateValues(slots, results))
    }
}
