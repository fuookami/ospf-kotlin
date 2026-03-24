@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbols2
import fuookami.ospf.kotlin.core.frontend.expression.symbol.flatMap
import fuookami.ospf.kotlin.core.frontend.expression.symbol.map
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.UIntVariable2
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.UInt64
import fuookami.ospf.kotlin.utils.multi_array.Shape2
import kotlin.time.Duration

/**
 * 迭代产能编译决策对象（列生成主问题）
 * Iterative Capacity Compilation Decision Object (Column Generation Master Problem)
 *
 * 用于列生成/迭代求解场景。
 * Used for column generation/iterative solving scenarios.
 *
 * 变量结构：每台设备的二维整型变量
 * Variable structure: 2D integer variable per executor
 * x[executor][iteration, columnIndex] -> amount
 *
 * @param E 执行器类型 / Executor type
 * @param A 生产动作类型 / Production action type
 */
class IterativeCapacityCompilation<E : Executor, A : ProductionAction>(
    /**
     * 执行器列表
     * List of executors
     */
    private val executors: List<E>,

    /**
     * 生产动作列表
     * List of production actions
     */
    private val actions: List<A>,

    /**
     * 时隙列表
     * List of time slots
     */
    private val slots: List<TimeSlot>,

    /**
     * 时间窗口
     * Time window
     */
    private val timeWindow: TimeWindow
) : Capacity<A> {

    init {
        // Index actions if they implement ManualIndexed
        // 如果动作实现了 ManualIndexed，则进行索引
        for (action in actions.filterIsInstance<ManualIndexed>()) {
            if (!action.indexed) {
                action.setIndexed()
            }
        }
    }

    /**
     * 每台设备的列聚合（按迭代分组）
     * Column aggregation per executor (grouped by iteration)
     */
    internal val columnsByExecutor: Map<E, CapacityColumnAggregation<E, A>> =
        executors.associateWith { CapacityColumnAggregation() }

    /**
     * 每台设备的二维整型变量
     * 2D integer variables per executor
     * x[executor][iteration, columnIndexInIteration] -> amount
     */
    private val _x: MutableMap<E, UIntVariable2> = HashMap()
    val x: Map<E, UIntVariable2>
        get() = _x.toMap()

    /**
     * 成本表达式
     * Cost expression
     */
    private var _cost: LinearExpressionSymbol? = null
    val cost: LinearExpressionSymbol?
        get() = _cost

    override lateinit var operationTime: LinearExpressionSymbols2
        private set

    override lateinit var capacity: LinearExpressionSymbols2
        private set

    /**
     * 注册到模型
     * Register to model
     */
    fun register(model: LinearMetaModel): Try {
        // Initialize variables for each executor
        // 为每个执行器初始化变量
        for (executor in executors) {
            if (!_x.containsKey(executor)) {
                val executorActions = actions.filter { it.executor == executor }
                if (executorActions.isEmpty()) continue

                val variable = UIntVariable2(
                    "x_${executor.id}",
                    Shape2(0, 0)  // Initially empty, will be expanded as columns are added
                )
                _x[executor] = variable
            }
        }

        // Register cost expression
        // 注册成本表达式
        if (_cost == null) {
            val costPoly = MutableLinearPolynomial(name = "cost")
            _cost = LinearExpressionSymbol(costPoly, name = "cost")
        }
        when (val result = model.add(_cost!!)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Register operationTime symbol
        // 注册 operationTime 符号
        if (!::operationTime.isInitialized) {
            operationTime = map(
                name = "operationTime",
                objs1 = actions,
                objs2 = slots,
                ctor = { action, slot ->
                    val a = actions.indexOf(action)
                    val s = slots.indexOf(slot)
                    val executorVar = _x[action.executor]
                    if (executorVar != null && executorVar.shape[1] > 0) {
                        val unitCap = action.unitCapacity(timeWindow) /
                            Flt64(timeWindow.interval.inWholeMilliseconds.toDouble())
                        // Sum over all columns for this action's slot
                        // 对该动作时隙的所有列求和
                        val poly = MutableLinearPolynomial()
                        val columnAgg = columnsByExecutor[action.executor]
                        if (columnAgg != null) {
                            for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                                for ((colIdx, column) in columns.withIndex()) {
                                    if (column.slotIndex == s) {
                                        val amount = column.amountFor(action)
                                        if (amount > UInt64.zero) {
                                            poly += unitCap * executorVar[iterIdx, colIdx]
                                        }
                                    }
                                }
                            }
                        }
                        poly
                    } else {
                        MutableLinearPolynomial()
                    }
                }
            )
        }
        when (val result = model.add(operationTime)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Register capacity symbol
        // 注册 capacity 符号
        if (!::capacity.isInitialized) {
            capacity = flatMap(
                name = "capacity",
                objs1 = executors,
                objs2 = slots,
                ctor = { executor, slot ->
                    val s = slots.indexOf(slot)
                    val executorActions = actions.filter { it.executor == executor }
                    val poly = MutableLinearPolynomial()
                    for (action in executorActions) {
                        val a = actions.indexOf(action)
                        poly += operationTime[a, s].toLinearPolynomial()
                    }
                    poly
                }
            )
        }
        when (val result = model.add(capacity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    /**
     * 添加新列
     * Add new columns
     *
     * 将新生成的列添加到主问题中。
     * Adds newly generated columns to the master problem.
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param newColumns New columns to add / 要添加的新列
     * @param model Linear meta model / 线性元模型
     * @return Added columns (deduplicated) / 已添加的列（去重后）
     */
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A>>,
        model: AbstractLinearMetaModel
    ): Ret<List<CapacityColumn<E, A>>> {
        // Group columns by executor
        // 按执行器分组列
        val columnsByExec = newColumns.groupBy { it.executor }
        val allAddedColumns = mutableListOf<CapacityColumn<E, A>>()

        for ((executor, columns) in columnsByExec) {
            val columnAggregation = columnsByExecutor[executor] ?: continue

            // Add columns to aggregation (with deduplication)
            // 将列添加到聚合（带去重）
            val addedColumns = columnAggregation.addColumns(iteration, columns)
            if (addedColumns.isEmpty()) continue

            // Get or create variable for this executor
            // 获取或创建该执行器的变量
            val existingVar = _x[executor]
            val newVar = if (existingVar == null) {
                val varShape = Shape2(
                    columnAggregation.columnsIteration.size,
                    columnAggregation.columnsIteration.maxOfOrNull { it.size } ?: 0
                )
                UIntVariable2("x_${executor.id}", varShape)
            } else {
                // Resize variable to accommodate new columns
                // 调整变量大小以容纳新列
                val newShape = Shape2(
                    columnAggregation.columnsIteration.size,
                    columnAggregation.columnsIteration.maxOfOrNull { it.size } ?: 0
                )
                UIntVariable2("x_${executor.id}", newShape).also {
                    // Copy existing variable bounds
                    // 复制现有变量边界
                    for (i in 0 until existingVar.shape[0]) {
                        for (j in 0 until existingVar.shape[1]) {
                            it[i, j].range.setUb(existingVar[i, j].range.ub)
                        }
                    }
                }
            }

            // Set bounds for new columns
            // 为新列设置边界
            for (column in addedColumns) {
                val iterIdx = iteration.toInt()
                val colIdx = columnAggregation.columnsIteration[iterIdx].indexOf(column)
                if (colIdx >= 0) {
                    newVar[iterIdx, colIdx].name = "x_${executor.id}_${iterIdx}_$colIdx"
                    newVar[iterIdx, colIdx].range.setUb(UInt64(1000))  // Default upper bound
                }
            }

            _x[executor] = newVar
            allAddedColumns.addAll(addedColumns)
        }

        // Update cost expression with new columns
        // 用新列更新成本表达式
        if (allAddedColumns.isNotEmpty() && _cost != null) {
            val costPoly = MutableLinearPolynomial(name = "cost")
            for ((executor, columnAgg) in columnsByExecutor) {
                val executorVar = _x[executor] ?: continue
                for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                    for ((colIdx, column) in columns.withIndex()) {
                        costPoly += column.cost * executorVar[iterIdx, colIdx]
                    }
                }
            }
            // Note: In practice, you may need to update the model's cost expression
            // 注意：实践中，可能需要更新模型的成本表达式
        }

        return Ok(allAddedColumns)
    }

    /**
     * 解析解
     * Extract solution from model
     */
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>> {
        val actionAllocations = mutableListOf<ActionAllocation<A>>()
        val executorCapacities = mutableListOf<ExecutorCapacityResult>()

        // Extract allocations from column values
        // 从列值中提取分配
        for ((executor, columnAgg) in columnsByExecutor) {
            val executorVar = _x[executor] ?: continue

            for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                for ((colIdx, column) in columns.withIndex()) {
                    if (colIdx >= executorVar.shape[1]) continue

                    val token = model.tokens.find(executorVar[iterIdx, colIdx])
                    val value = token?.result?.round()?.toUInt64() ?: UInt64.zero

                    if (value > UInt64.zero) {
                        // Create action allocations from this column
                        // 从该列创建动作分配
                        for ((action, amount) in column.allocations) {
                            if (amount > UInt64.zero) {
                                val actualAmount = amount * value.toLong()
                                val duration = if (action.discrete && action.batchDuration != null) {
                                    action.batchDuration!! * actualAmount.toDouble()
                                } else {
                                    timeWindow.interval * actualAmount.toDouble()
                                }

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

        // Calculate executor capacities from operationTime
        // 从 operationTime 计算执行器产能
        for ((e, executor) in executors.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                val capValue = capacity[e, s].evaluate(model.tokens)
                val totalDuration = if (capValue != null && capValue > Flt64.zero) {
                    timeWindow.interval * capValue.toDouble()
                } else {
                    Duration.ZERO
                }
                if (totalDuration.inWholeMilliseconds > 0) {
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