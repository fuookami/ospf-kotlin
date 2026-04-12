@file:Suppress("DEPRECATION")

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
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray.Shape2
import kotlin.time.Duration

/**
 * 产能编译决策对象（无顺序�?
 * Capacity Compilation Decision Object (No Order)
 *
 * Two-dimensional integer variable: x[action, slot] -> amount
 * 二维整型变量：x[action, slot] -> 数量
 */
class CapacityCompilation<A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow
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
     * 二维整型变量
     * 2D integer variable
     * x[action, slot] -> amount
     */
    lateinit var x: UIntVariable2
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
            x = UIntVariable2("x", Shape2(actions.size, slots.size))
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    x[a, s].name = "x_${action.id}_$s"
                    x[a, s].range.setUb(action.upperBound(slot, timeWindow))
                }
            }
        }
        when (val result = model.add(x)) {
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
                    costPoly += unitCost * x[a, s]
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
            operationTime = map(
                name = "operationTime",
                objs1 = actions,
                objs2 = slots,
                ctor = { action, slot ->
                    val unitOperationTime = if (action.discrete && action.batchDuration != null) {
                        timeWindow.valueOf(action.batchDuration!!)
                    } else {
                        timeWindow.valueOf(timeWindow.interval)
                    }
                    unitOperationTime * x[actions.indexOf(action), slots.indexOf(slot)]
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
                val token = model.tokens.find(x[a, s]) ?: continue
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
                            duration = duration
                        )
                    )
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



