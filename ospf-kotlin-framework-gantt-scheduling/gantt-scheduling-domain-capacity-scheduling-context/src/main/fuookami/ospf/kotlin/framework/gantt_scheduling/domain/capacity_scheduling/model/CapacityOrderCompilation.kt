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
 * 产能编译决策对象（有顺序）
 * Capacity Compilation Decision Object (With Order)
 *
 * Three-dimensional integer variable: x[action, slot, order] -> amount
 * 三维整型变量：x[action, slot, order] -> 数量
 */
class CapacityOrderCompilation<A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow,
    private val maxOrderPerSlot: UInt64
) : Capacity<A> {

    init {
        if (!actions.all { it.indexed }) {
            ManualIndexed.flush(ProductionAction::class)
            for (action in actions) {
                action.setIndexed(ProductionAction::class)
            }
        }
    }

    /**
     * 三维整型变量
     * 3D integer variable
     * x[action, slot, order] -> amount
     */
    lateinit var x: UIntVariable3
        private set

    /**
     * 三维二元变量（顺序占用标记）
     * 3D binary variable for order occupation
     */
    lateinit var b: BinVariable3
        private set

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
    fun register(model: LinearMetaModel): Try {
        // Register x variable
        // 注册 x 变量
        if (!::x.isInitialized) {
            x = UIntVariable1("x", Shape3(actions.size, slots.size, maxOrderPerSlot.toInt()))
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        x[a, s, o].name = "x_${action.id}_${s}_$o"
                        x[a, s, o].range.leq(action.upperBound(slot, timeWindow))
                    }
                }
            }
        }
        when (val result = model.add(x)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register b variable
        // 注册 b 变量
        if (!::b.isInitialized) {
            b = BinVariable1("b", Shape3(actions.size, slots.size, maxOrderPerSlot.toInt()))
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        b[a, s, o].name = "b_${action.id}_${s}_$o"
                    }
                }
            }
        }
        when (val result = model.add(b)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register cost expression
        // 注册成本表达式
        if (!::cost.isInitialized) {
            cost = LinearExpressionSymbol(name = "cost")
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    val unitCost = action.unitCost(slot.time)
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        cost.asMutable() += unitCost * x[a, s, o]
                    }
                }
            }
        }
        when (val result = model.add(cost)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register operationTime symbol (aggregated over orders)
        // 注册 operationTime 符号（在顺序维度上聚合）
        if (!::operationTime.isInitialized) {
            operationTime = LinearIntermediateSymbols2(
                name = "operation_time",
                shape = Shape2(actions.size, slots.size)
            ) { _, (a, s) ->
                val action = actions[a]
                val slot = slots[s]
                LinearIntermediateSymbol(name = "operation_time_${action.id}_$s")
            }

            // Build operationTime expression: sum over orders
            // 构建 operationTime 表达式：在顺序维度上求和
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    val opTime = operationTime[a, s]
                    opTime.flush()
                    val unitCap = Flt64(action.unitCapacity(timeWindow) / timeWindow.interval)
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        opTime.asMutable() += unitCap * x[a, s, o]
                    }
                }
            }
        }
        when (val result = model.add(operationTime)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        // Register capacity symbol
        // 注册 capacity 符号
        val executors = actions.map { it.executor }.distinct()
        if (!::capacity.isInitialized) {
            capacity = LinearIntermediateSymbols2(
                name = "capacity",
                shape = Shape2(executors.size, slots.size)
            ) { _, (e, s) ->
                val executor = executors[e]
                val slot = slots[s]
                LinearIntermediateSymbol(name = "capacity_${executor.id}_$s")
            }

            // Build capacity expression: sum of operationTime for each executor's actions
            // 构建 capacity 表达式：每个设备的所有动作 operationTime 之和
            for ((e, executor) in executors.withIndex()) {
                val executorActions = actions.filter { it.executor == executor }
                for ((s, slot) in slots.withIndex()) {
                    val cap = capacity[e, s]
                    cap.flush()
                    for (action in executorActions) {
                        val a = actions.indexOf(action)
                        cap.asMutable() += operationTime[a, s]
                    }
                }
            }
        }
        when (val result = model.add(capacity)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
        }

        return ok
    }

    /**
     * 解析解
     * Extract solution from model
     */
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>> {
        val actionAllocations = mutableListOf<ActionAllocation<A>>()
        val executors = actions.map { it.executor }.distinct()

        for ((a, action) in actions.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                for (o in 0 until maxOrderPerSlot.toInt()) {
                    val amount = model.solution[x[a, s, o]]?.let { UInt64(it.toBigDecimal().toLong()) } ?: UInt64.zero
                    if (amount > UInt64.zero) {
                        val unitCap = action.unitCapacity(timeWindow)
                        val duration = unitCap * amount.toDouble()
                        actionAllocations.add(
                            ActionAllocation(
                                action = action,
                                slot = slot,
                                slotIndex = s,
                                amount = amount,
                                duration = duration,
                                order = o
                            )
                        )
                    }
                }
            }
        }

        val executorCapacities = mutableListOf<ExecutorCapacityResult>()
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