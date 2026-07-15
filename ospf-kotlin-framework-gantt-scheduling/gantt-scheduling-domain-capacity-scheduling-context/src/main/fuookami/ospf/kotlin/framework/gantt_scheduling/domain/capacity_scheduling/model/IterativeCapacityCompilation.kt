/**
 * 迭代产能编译决策对象（列生成主问题）
 * Iterative Capacity Compilation Decision Object (Column Generation Master Problem)
 *
 * 用于列生成迭代求解场景。
 * Used for column generation/iterative solving scenarios.
 *
 * 变量结构：每台设备的二维整型变量
 * Variable structure: 2D integer variable per executor
 * x[executor][iteration, columnIndex] -> amount
*/
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.math.floor
import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 迭代产能编译决策对象（列生成主问题）
 * Iterative Capacity Compilation Decision Object (Column Generation Master Problem)
 *
 * 用于列生成迭代求解场景。
 * Used for column generation/iterative solving scenarios.
 *
 * 变量结构：每台设备的二维整型变量
 * Variable structure: 2D integer variable per executor
 * x[executor][iteration, columnIndex] -> amount
 *
 * @param E 执行器类型 / Executor type
 * @param A 生产动作类型 / Production action type
*/
class IterativeCapacityCompilation<V : RealNumber<V>, E : Executor, A : ProductionAction>(

    /**
     * 执行器列表
     * List of executors
    */
    override val executors: List<E>,

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
    private val timeWindow: TimeWindow<V>
) : Capacity<A> {
    private val valueZero = timeWindow.fromDouble(0.0)

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
    internal val columnsByExecutor: Map<E, CapacityColumnAggregation<E, A, V>> =
        executors.associateWith { CapacityColumnAggregation() }
    private val executorByRef: Map<Executor, E> = executors.associateBy { it }

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
    private var _cost: LinearIntermediateSymbol<Flt64>? = null
    val cost: LinearIntermediateSymbol<Flt64>?
        get() = _cost

    override lateinit var operationTime: LinearExpressionSymbols2<Flt64>
        private set

    override lateinit var capacity: LinearExpressionSymbols2<Flt64>
        private set

    /**
     * 注册到模型
     * Register to model
     *
     * @param model Linear meta model to register variables and symbols into / 要注册变量和符号的线性元模型
     * @return Success or failure of the registration / 注册操作的成功或失败
    */
    fun register(model: LinearMetaModel<Flt64>): Try {
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
            _cost = LinearExpressionSymbol(Flt64, name = "cost")
        }
        when (val result = model.add(_cost!!)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Register operationTime symbol
        // 注册 operationTime 符号
        if (!::operationTime.isInitialized) {
            operationTime = flatMap(
                name = "operationTime",
                objs1 = actions,
                objs2 = slots,
                ctor = { action, slot ->
                    val s = slots.indexOf(slot)
                    val executor = executorByRef[action.executor]
                    val executorVar = if (executor != null) {
                        _x[executor]
                    } else {
                        null
                    }
                    if (executorVar != null && executorVar.shape[1] > 0) {
                        val unitOperationTime = if (action.discrete && action.batchDuration != null) {
                            timeWindow.valueOf(action.batchDuration!!)
                        } else {
                            timeWindow.valueOf(timeWindow.interval)
                        }
                        // Sum over all columns for this action's slot
                        // 对该动作时隙的所有列求和
                        val poly = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
                        val columnAgg = if (executor != null) {
                            columnsByExecutor[executor]
                        } else {
                            null
                        }
                        if (columnAgg != null) {
                            for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                                for ((colIdx, column) in columns.withIndex()) {
                                    if (column.slotIndex == s) {
                                        val amount = column.amountFor(action)
                                        if (amount > UInt64.zero) {
                                            val coefficient = unitOperationTime.toSolverValue() * amount.toSolverFlt64()
                                            poly += LinearMonomial(coefficient, executorVar[iterIdx, colIdx])
                                        }
                                    }
                                }
                            }
                        }
                        poly.toLinearPolynomial()
                    } else {
                        LinearPolynomial(emptyList(), Flt64.zero)
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
                    val poly = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
                    for (action in executorActions) {
                        val a = actions.indexOf(action)
                        poly += operationTime[a, s].polynomial
                    }
                    poly.toLinearPolynomial()
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
     * @param iteration Current iteration number / 当前迭代号
     * @param newColumns New columns to add / 要添加的新列
     * @param model Linear meta model / 线性元模型
     * @return Added columns (deduplicated) / 已添加的列（去重后）
    */
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A, V>>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<CapacityColumn<E, A, V>>> {
        // Group columns by executor
        // 按执行器分组
        val columnsByExec = newColumns.groupBy { it.executor }
        val allAddedColumns = mutableListOf<CapacityColumn<E, A, V>>()

        for ((executor, columns) in columnsByExec) {
            val columnAggregation = columnsByExecutor[executor] ?: continue
            val sanitizedColumns = columns.filter { column ->
                column.slotIndex in slots.indices &&
                    column.allocations.keys.all { action ->
                        action in actions && action.executor == executor
                    }
            }
            if (sanitizedColumns.isEmpty()) {
                continue
            }

            // Add columns to aggregation (with deduplication)
            // 将列添加到聚合（带去重）
            val addedColumns = columnAggregation.addColumns(iteration, sanitizedColumns)
            if (addedColumns.isEmpty()) continue

            // Get or create variable for this executor
            // 获取或创建该执行器的变量
            val existingVar = _x[executor]
            val newVar = if (existingVar == null) {
                val varShape = Shape2(
                    columnAggregation.columnsIteration.size,
                    columnAggregation.columnsIteration.maxOfOrNull { it.size } ?: 0
                )
                UIntVariable2("x_${executor.id}_v${columnAggregation.columns.size}", varShape)
            } else {
                // Resize variable to accommodate new columns
                // 调整变量大小以容纳新列
                val newShape = Shape2(
                    columnAggregation.columnsIteration.size,
                    columnAggregation.columnsIteration.maxOfOrNull { it.size } ?: 0
                )
                UIntVariable2("x_${executor.id}_v${columnAggregation.columns.size}", newShape).also {
                    // Copy existing variable bounds
                    // 复制现有变量边界
                    for (i in 0 until existingVar.shape[0]) {
                        for (j in 0 until existingVar.shape[1]) {
                            existingVar[i, j].range.range?.copy()?.let { range ->
                                it[i, j].range.set(range)
                            }
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
                    newVar[iterIdx, colIdx].range.setUb(columnUpperBound(column))
                }
            }

            if (existingVar != null) {
                for (item in existingVar) {
                    model.remove(item)
                }
            }
            when (val result = model.add(newVar)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            _x[executor] = newVar
            allAddedColumns.addAll(addedColumns)
        }

        if (allAddedColumns.isNotEmpty()) {
            when (val result = rebuildSymbols()) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return Ok(allAddedColumns)
    }

    /**
     * 定位列对应的决策变量位置
     * Locate decision variable position for a column
     *
     * @param iteration Iteration number in which the column was generated / 生成该列的迭代编号
     * @param column Capacity column to locate / 要定位的产能列
     * @return The 2D variable and column index pair, or null if not found / 二维变量与列索引对，未找到则返回 null
    */
    fun locateColumnDecision(
        iteration: UInt64,
        column: CapacityColumn<E, A, V>
    ): Pair<UIntVariable2, Int>? {
        val iterIdx = iteration.toInt()
        val executorVar = _x[column.executor] ?: return null
        val columnAgg = columnsByExecutor[column.executor] ?: return null
        val columnsInIteration = columnAgg.columnsIteration.getOrNull(iterIdx) ?: return null
        val columnIndex = columnsInIteration.indexOf(column)
        if (columnIndex < 0) {
            return null
        }
        if (iterIdx >= executorVar.shape[0] || columnIndex >= executorVar.shape[1]) {
            return null
        }
        return executorVar to columnIndex
    }

    /**
     * 计算列变量上界
     * Calculate upper bound for a column decision variable
     *
     * @param column Capacity column whose upper bound is being calculated / 正在计算上界的产能列
     * @return Maximum number of times this column can be selected / 该列可被选择的最大次数
    */
    private fun columnUpperBound(column: CapacityColumn<E, A, V>): UInt64 {
        if (column.slotIndex !in slots.indices) {
            return UInt64.zero
        }

        var columnOperationTime = valueZero
        for ((action, amount) in column.allocations) {
            if (amount <= UInt64.zero) {
                continue
            }
            val unitOperationTime = if (action.discrete && action.batchDuration != null) {
                timeWindow.valueOf(action.batchDuration!!)
            } else {
                timeWindow.valueOf(timeWindow.interval)
            }
            columnOperationTime += unitOperationTime * timeWindow.fromDouble(amount.toLong().toDouble())
        }
        if (columnOperationTime <= valueZero) {
            return UInt64.zero
        }

        val available = timeWindow.valueOf(slots[column.slotIndex].duration)
        if (available <= valueZero) {
            return UInt64.zero
        }
        val upperBound = floor(timeWindow.toDouble(available) / timeWindow.toDouble(columnOperationTime))
        return if (upperBound <= 0.0) {
            UInt64.zero
        } else {
            UInt64(upperBound.toULong())
        }
    }

/**
 * Rebuilds cost, operationTime, and capacity symbols after columns are added.
 * 在添加列后重建成本、操作时间和产能符号。
 * @return Success or failure of the symbol rebuild / 符号重建的成功或失败
*/
    private fun rebuildSymbols(): Try {
        if (_cost == null || !::operationTime.isInitialized || !::capacity.isInitialized) {
            return ok
        }

        for ((a, _) in actions.withIndex()) {
            for ((s, _) in slots.withIndex()) {
                operationTime[a, s].flush()
            }
        }

        for ((executor, columnAgg) in columnsByExecutor) {
            val executorVar = _x[executor] ?: continue
            for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                for ((colIdx, column) in columns.withIndex()) {
                    if (iterIdx >= executorVar.shape[0] || colIdx >= executorVar.shape[1]) {
                        continue
                    }
                    val variable = executorVar[iterIdx, colIdx]
                    for ((action, amount) in column.allocations) {
                        if (amount <= UInt64.zero) {
                            continue
                        }
                        val actionIndex = actions.indexOf(action)
                        if (actionIndex < 0 || column.slotIndex !in slots.indices) {
                            continue
                        }
                        val unitOperationTime = if (action.discrete && action.batchDuration != null) {
                            timeWindow.valueOf(action.batchDuration!!)
                        } else {
                            timeWindow.valueOf(timeWindow.interval)
                        }
                        val coefficient = unitOperationTime.toSolverValue() * amount.toSolverFlt64()
                        operationTime[actionIndex, column.slotIndex].asMutable() += LinearMonomial(coefficient, variable)
                    }
                }
            }
        }

        for ((e, executor) in executors.withIndex()) {
            for ((s, _) in slots.withIndex()) {
                capacity[e, s].flush()
                for ((a, action) in actions.withIndex()) {
                    if (action.executor == executor) {
                        capacity[e, s].asMutable() += operationTime[a, s].polynomial
                    }
                }
            }
        }

        _cost!!.flush()
        for ((executor, columnAgg) in columnsByExecutor) {
            val executorVar = _x[executor] ?: continue
            for ((iterIdx, columns) in columnAgg.columnsIteration.withIndex()) {
                for ((colIdx, column) in columns.withIndex()) {
                    if (iterIdx >= executorVar.shape[0] || colIdx >= executorVar.shape[1]) {
                        continue
                    }
                    (_cost as LinearExpressionSymbol).asMutable() += LinearMonomial(
                        column.columnCost.value.toSolverValue(),
                        executorVar[iterIdx, colIdx]
                    )
                }
            }
        }

        return ok
    }

    /**
     * 解析解
     * Extract solution from model
    */
    override fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<CapacitySchedulingSolution<A>> {
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
                        if (column.slotIndex !in slots.indices) {
                            continue
                        }
                        // Create action allocations from this column
                        // 从该列创建动作分配
                        for ((action, amount) in column.allocations) {
                            if (amount > UInt64.zero) {
                                val actualAmount = amount * value
                                val duration = if (action.discrete && action.batchDuration != null) {
                                    action.batchDuration!! * actualAmount.toLong().toDouble()
                                } else {
                                    timeWindow.interval * actualAmount.toLong().toDouble()
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
                val capValue = capacity[e, s].evaluate(model.tokens, schedulingSolverValueAdapter)
                val totalDuration = if (capValue != null && capValue > Flt64.zero) {
                    timeWindow.durationOf(timeWindow.fromDouble(capValue.toDouble()))
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
