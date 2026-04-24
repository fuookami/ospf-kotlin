@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.core.model.mechanism.times
import fuookami.ospf.kotlin.core.intermediate_model.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols2
import fuookami.ospf.kotlin.core.intermediate_symbol.flatMap
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_model.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.BinVariable3
import fuookami.ospf.kotlin.core.variable.UIntVariable3
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray.Shape3
import kotlin.time.Duration

/**
 * 产能编译决策对象（带顺序�?
 * Capacity Compilation Decision Object (With Order)
 *
 * Three-dimensional variables:
 * 三维变量�?
 * - x[action, slot, order] -> amount (integer)
 * - x[action, slot, order] -> 数量（整型）
 * - b[action, slot, order] -> is_selected (binary)
 * - b[action, slot, order] -> 是否选中（二进制�?
 */
class CapacityOrderCompilation<A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow,
    private val maxOrderPerSlot: UInt64
) : Capacity<A> {
    override val executors: List<Executor> = actions.map { it.executor }.distinct()

    init {
        // Index actions if they implement ManualIndexed
        // 如果动作实现�?ManualIndexed，则进行索引
        for (action in actions.filterIsInstance<ManualIndexed>()) {
            if (!action.indexed) {
                action.setIndexed()
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
     * 成本表达�?
     * Cost expression
     */
    lateinit var cost: LinearExpressionSymbol
        private set

    override lateinit var operationTime: LinearExpressionSymbols2
        private set

    override lateinit var capacity: LinearExpressionSymbols2
        private set

    /**
     * 注册到模�?
     * Register to model
     */
    fun register(model: LinearMetaModel): Try {
        // Register x variable
        // 注册 x 变量
        if (!::x.isInitialized) {
            x = UIntVariable3("x", Shape3(actions.size, slots.size, maxOrderPerSlot.toInt()))
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        x[a, s, o].name = "x_${action.id}_${s}_$o"
                        x[a, s, o].range.setUb(action.upperBound(slot, timeWindow))
                    }
                }
            }
        }
        when (val result = model.add(x)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Register b variable
        // 注册 b 变量
        if (!::b.isInitialized) {
            b = BinVariable3("b", Shape3(actions.size, slots.size, maxOrderPerSlot.toInt()))
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
            is Fatal -> return Fatal(result.errors)
        }

        // Register cost expression
        // 注册成本表达�?
        if (!::cost.isInitialized) {
            val costPoly = MutableLinearPolynomial(name = "cost")
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    val unitCost = action.unitCost(slot.time.start)
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        costPoly += unitCost * x[a, s, o]
                    }
                }
            }
            cost = LinearExpressionSymbol(costPoly, name = "cost")
        }
        when (val result = model.add(cost)) {
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
                    val a = actions.indexOf(action)
                    val s = slots.indexOf(slot)
                    val unitOperationTime = if (action.discrete && action.batchDuration != null) {
                        timeWindow.valueOf(action.batchDuration!!)
                    } else {
                        timeWindow.valueOf(timeWindow.interval)
                    }
                    val poly = MutableLinearPolynomial()
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        poly += unitOperationTime * x[a, s, o]
                    }
                    poly
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
     * 解析�?
     * Extract solution from model
     */
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>> {
        val actionAllocations = mutableListOf<ActionAllocation<A>>()

        for ((a, action) in actions.withIndex()) {
            for ((s, slot) in slots.withIndex()) {
                for (o in 0 until maxOrderPerSlot.toInt()) {
                    val token = model.tokens.find(x[a, s, o]) ?: continue
                    val amount = token.result?.round()?.toUInt64() ?: UInt64.zero
                    if (amount > UInt64.zero) {
                        val duration = if (action.discrete && action.batchDuration != null) {
                            action.batchDuration!! * amount.toLong().toDouble()
                        } else {
                            timeWindow.interval * amount.toLong().toDouble()
                        }
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
                val capValue = capacity[e, s].evaluate(model.tokens)
                val totalDuration = if (capValue != null && capValue > Flt64.zero) {
                    timeWindow.durationOf(capValue)
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



