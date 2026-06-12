@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

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
 * 产能编译决策对象（带顺序）
 * Capacity Compilation Decision Object (With Order)
 *
 * 三维变量：
 * Three-dimensional variables:
 * - x[action, slot, order] -> 数量（整型）
 * - x[action, slot, order] -> amount (integer)
 * - b[action, slot, order] -> 是否选中（二进制）
 * - b[action, slot, order] -> is_selected (binary)
 *
 * @param V 数值类型 / Numeric type
 * @param A 生产动作类型 / Production action type
 * @property actions 生产动作列表 / List of production actions
 * @property slots 时隙列表 / List of time slots
 * @property timeWindow 时间窗口 / Time window
 * @property maxOrderPerSlot 每时隙最大顺序数 / Maximum order per slot
 */
class CapacityOrderCompilation<V : RealNumber<V>, A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow<V>,
    private val maxOrderPerSlot: UInt64
) : Capacity<A> {
    override val executors: List<Executor> = actions.map { it.executor }.distinct()

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
    lateinit var cost: LinearIntermediateSymbol<Flt64>
        private set

    override lateinit var operationTime: LinearExpressionSymbols2<Flt64>
        private set

    override lateinit var capacity: LinearExpressionSymbols2<Flt64>
        private set

    /**
     * 注册到模型
     * Register to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    fun register(model: LinearMetaModel<Flt64>): Try {
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
        // 注册成本表达式
        if (!::cost.isInitialized) {
            val costPoly = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            for ((a, action) in actions.withIndex()) {
                for ((s, slot) in slots.withIndex()) {
                    val unitCost = action.unitCostSolverValue(slot.time.start)
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        costPoly += LinearMonomial(unitCost.toSolverValue(), x[a, s, o])
                    }
                }
            }
            cost = LinearExpressionSymbol(polynomial = costPoly.toLinearPolynomial(), name = "cost")
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
                    val poly = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
                    for (o in 0 until maxOrderPerSlot.toInt()) {
                        poly += LinearMonomial(unitOperationTime.toSolverValue(), x[a, s, o])
                    }
                    poly.toLinearPolynomial()
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
     * 解析解
     * Extract solution from model
     *
     * @param model Abstract linear meta model / 抽象线性元模型
     * @return Capacity scheduling solution / 产能调度解
     */
    override fun extractSolution(model: AbstractLinearMetaModel<Flt64>): Ret<CapacitySchedulingSolution<A>> {
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
