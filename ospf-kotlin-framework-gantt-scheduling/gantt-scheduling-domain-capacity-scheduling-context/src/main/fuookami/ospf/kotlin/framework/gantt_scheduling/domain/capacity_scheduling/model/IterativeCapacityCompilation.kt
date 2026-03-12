package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/**
 * 迭代产能编译决策对象（列生成主问题）
 * Iterative Capacity Compilation Decision Object (Column Generation Master Problem)
 *
 * Two-dimensional variable per executor: x[iteration, columnIndex] -> amount
 * 每设备二维变量：x[iteration, columnIndex] -> 数量
 */
class IterativeCapacityCompilation<E : Executor, A : ProductionAction>(
    private val executors: List<E>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow
) : Capacity<A> {

    init {
        if (!executors.all { it.indexed }) {
            ManualIndexed.flush(Executor::class)
            for (executor in executors) {
                executor.setIndexed(Executor::class)
            }
        }
        if (!actions.all { it.indexed }) {
            ManualIndexed.flush(ProductionAction::class)
            for (action in actions) {
                action.setIndexed(ProductionAction::class)
            }
        }
    }

    /**
     * 每台设备的列聚合（按迭代分组）
     * Column aggregation per executor (grouped by iteration)
     */
    internal val columnsByExecutor: Map<E, CapacityColumnAggregation<E, A>> = executors.associateWith {
        CapacityColumnAggregation()
    }

    /**
     * 每台设备的二维整型变量
     * 2D integer variables per executor
     * x[executor][iteration, columnIndexInIteration] -> amount
     */
    lateinit var x: Map<E, UIntVariable2>
        private set

    private val _xList = ArrayList<UIntVariable2>()

    /**
     * 成本表达式
     * Cost expression
     */
    lateinit var cost: LinearExpressionSymbol
        private set

    override lateinit var operationTime: LinearIntermediateSymbols2
        private set

    override lateinit var capacity: LinearIntermediateSymbols2
        private set

    /**
     * 注册到模型
     * Register to model
     */
    fun register(model: MetaModel): Try {
        // Initialize x variables map
        // 初始化 x 变量映射
        if (!::x.isInitialized) {
            x = emptyMap()
        }

        // Register cost expression
        // 注册成本表达式
        if (!::cost.isInitialized) {
            cost = LinearExpressionSymbol(name = "cost")
        }
        when (val result = model.add(cost)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register operationTime symbol
        // 注册 operationTime 符号
        if (!::operationTime.isInitialized) {
            operationTime = LinearIntermediateSymbols2(
                name = "operation_time",
                shape = Shape2(actions.size, slots.size)
            ) { _, (a, s) ->
                val action = actions[a]
                val slot = slots[s]
                LinearIntermediateSymbol(name = "operation_time_${action.id}_$s")
            }
        }
        when (val result = model.add(operationTime)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register capacity symbol
        // 注册 capacity 符号
        if (!::capacity.isInitialized) {
            capacity = LinearIntermediateSymbols2(
                name = "capacity",
                shape = Shape2(executors.size, slots.size)
            ) { _, (e, s) ->
                val executor = executors[e]
                val slot = slots[s]
                LinearIntermediateSymbol(name = "capacity_${executor.id}_$s")
            }
        }
        when (val result = model.add(capacity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        return ok
    }

    /**
     * 添加新列
     * Add new columns
     *
     * @param iteration Iteration number / 迭代数
     * @param newColumns New columns to add / 要添加的新列
     * @param model Linear meta model / 线性元模型
     * @return Unduplicated columns / 去重后的列
     */
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A>>,
        model: AbstractLinearMetaModel
    ): Ret<List<CapacityColumn<E, A>>> {
        val allUnduplicatedColumns = mutableListOf<CapacityColumn<E, A>>()

        for ((executor, aggregation) in columnsByExecutor) {
            val executorColumns = newColumns.filter { it.executor == executor }
            if (executorColumns.isEmpty()) continue

            val unduplicatedColumns = aggregation.addColumns(iteration, executorColumns)
            if (unduplicatedColumns.isEmpty()) continue

            allUnduplicatedColumns.addAll(unduplicatedColumns)

            // Create or update x variable for this iteration
            // 为此迭代创建或更新 x 变量
            val xi = UIntVariable1(
                "x_${executor.id}_$iteration",
                Shape1(unduplicatedColumns.size)
            )
            for ((i, column) in unduplicatedColumns.withIndex()) {
                xi[i].name = "x_${executor.id}_${iteration}_$i"
            }

            when (val result = model.add(xi)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
            }

            // Update cost expression
            // 更新成本表达式
            for ((i, column) in unduplicatedColumns.withIndex()) {
                cost.asMutable() += column.cost * xi[i]
            }

            // Update operationTime and capacity expressions
            // 更新 operationTime 和 capacity 表达式
            for ((i, column) in unduplicatedColumns.withIndex()) {
                for ((action, amount) in column.allocations) {
                    val a = actions.indexOf(action)
                    val s = column.slotIndex
                    val unitCap = Flt64(action.unitCapacity(timeWindow) / timeWindow.interval)
                    operationTime[a, s].asMutable() += unitCap * Flt64(amount.toDouble()) * xi[i]
                }

                val e = executors.indexOf(executor)
                val s = column.slotIndex
                for ((action, amount) in column.allocations) {
                    val a = actions.indexOf(action)
                    val unitCap = Flt64(action.unitCapacity(timeWindow) / timeWindow.interval)
                    capacity[e, s].asMutable() += unitCap * Flt64(amount.toDouble()) * xi[i]
                }
            }
        }

        return Ok(allUnduplicatedColumns)
    }

    /**
     * 解析解
     * Extract solution from model
     */
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>> {
        val actionAllocations = mutableListOf<ActionAllocation<A>>()
        val executorCapacities = mutableListOf<ExecutorCapacityResult>()

        // Extract allocations from columns
        // 从列中提取分配
        for ((executor, aggregation) in columnsByExecutor) {
            for ((iterationIndex, columns) in aggregation.columnsIteration.withIndex()) {
                val xi = x[executor] ?: continue

                for ((columnIndex, column) in columns.withIndex()) {
                    val amount = model.solution[xi[iterationIndex, columnIndex]]
                        ?.let { UInt64(it.toBigDecimal().toLong()) } ?: UInt64.zero

                    if (amount > UInt64.zero) {
                        for ((action, columnAmount) in column.allocations) {
                            val actualAmount = columnAmount * amount
                            if (actualAmount > UInt64.zero) {
                                val unitCap = action.unitCapacity(timeWindow)
                                val duration = unitCap * actualAmount.toDouble()
                                actionAllocations.add(
                                    ActionAllocation(
                                        action = action,
                                        slot = slots[column.slotIndex],
                                        slotIndex = column.slotIndex,
                                        amount = actualAmount,
                                        duration = duration,
                                        order = column.order
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extract executor capacities
        // 提取设备产能
        for ((e, executor) in executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                val totalDuration = model.solution[capacity[e, s]]?.let {
                    timeWindow.interval * it
                } ?: Duration.ZERO

                if (totalDuration > Duration.ZERO) {
                    executorCapacities.add(
                        ExecutorCapacityResult(
                            executor = executor,
                            slot = slot,
                            slotIndex = s,
                            totalDuration = totalDuration
                        )
                    )
                }
            }
        }

        return Ok(
            CapacitySchedulingSolution(
                actions = actions,
                slots = slots,
                actionAllocations = actionAllocations,
                executorCapacities = executorCapacities
            )
        )
    }
}