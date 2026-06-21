@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务时间模型 / Task time model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 任务时间物理量 / Task time quantity */
typealias TaskTimeQuantity<V> = Quantity<V>

private fun captureLinearConstraintInput(
    result: Ret<LinearConstraintInput<Flt64>>,
    onFailure: (Try) -> Unit
): LinearConstraintInput<Flt64>? {
    return when (result) {
        is Ok -> result.value
        is Failed -> {
            onFailure(Failed(result.error))
            null
        }

        is Fatal -> {
            onFailure(Fatal(result.errors))
            null
        }
    }
}

/**
 * 松弛符号构建 / Slack symbol construction
 *
 * @param x 线性多项式 / Linear polynomial
 * @param y 目标值 / Target value
 * @param type 变量类型 / Variable type
 * @param withNegative 是否包含负松弛 / Whether to include negative slack
 * @param withPositive 是否包含正松弛 / Whether to include positive slack
 * @param name 名称 / Name
 * @return 线性函数符号适配器 / Linear function symbol adapter
 */
private fun slackSymbol(
    x: ToLinearPolynomial<Flt64>,
    y: Flt64,
    type: VariableTypeKind,
    withNegative: Boolean = true,
    withPositive: Boolean = true,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = LinearPolynomial(emptyList(), y),
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            converter = schedulingSolverValueAdapter,
            name = name
        ),
        converter = schedulingSolverValueAdapter
    )
}

/** 任务时间接口 / Task time interface */
interface TaskTime {
    val delayEnabled: Boolean
    val overMaxDelayEnabled: Boolean
    val advanceEnabled: Boolean
    val overMaxAdvanceEnabled: Boolean

    val delayLastEndTimeEnabled: Boolean
    val advanceEarliestEndTimeEnabled: Boolean

    val estimateStartTime: LinearIntermediateSymbols1<Flt64>
    val estimateEndTime: LinearIntermediateSymbols1<Flt64>

    val delayTime: LinearIntermediateSymbols1<Flt64>
    val advanceTime: LinearIntermediateSymbols1<Flt64>
    val overMaxDelayTime: LinearIntermediateSymbols1<Flt64>
    val overMaxAdvanceTime: LinearIntermediateSymbols1<Flt64>
    val delayLastEndTime: LinearIntermediateSymbols1<Flt64>
    val advanceEarliestEndTime: LinearIntermediateSymbols1<Flt64>

    val onTime: LinearIntermediateSymbols1<Flt64>
    val notOnTime: LinearIntermediateSymbols1<Flt64>

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: MetaModel<Flt64>): Try

    /**
     * 读取预估开始时间物理量 / Read estimate start time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 预估开始时间物理量 / Estimate start time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > estimateStartTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return estimateStartTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取预估结束时间物理量 / Read estimate end time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 预估结束时间物理量 / Estimate end time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > estimateEndTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return estimateEndTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取延迟时间物理量 / Read delay time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 延迟时间物理量 / Delay time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > delayTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return delayTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取提前时间物理量 / Read advance time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 提前时间物理量 / Advance time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > advanceTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return advanceTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取延迟最晚结束时间物理量 / Read delay last-end-time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 延迟最晚结束时间物理量 / Delay last-end-time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > delayLastEndTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return delayLastEndTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取提前最早结束时间物理量 / Read advance earliest-end-time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 提前最早结束时间物理量 / Advance earliest-end-time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > advanceEarliestEndTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return advanceEarliestEndTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取超最大延迟时间物理量 / Read over-max-delay time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 超最大延迟时间物理量 / Over-max-delay time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > overMaxDelayTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return overMaxDelayTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取超最大提前时间物理量 / Read over-max-advance time as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param task 任务 / Task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 超最大提前时间物理量 / Over-max-advance time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > overMaxAdvanceTimeQuantity(
        task: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): TaskTimeQuantity<V>? {
        return overMaxAdvanceTime[task].quantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }
}

private fun <V : RealNumber<V>> LinearIntermediateSymbol<Flt64>.quantityOf(
    model: MetaModel<Flt64>,
    adapter: SchedulingSolverValueAdapter<V>,
    unit: PhysicalUnit
): TaskTimeQuantity<V>? {
    val value = (this as IntermediateSymbol<Flt64>).evaluate(
        tokenTable = model.tokens,
        converter = schedulingSolverValueAdapter,
        zeroIfNone = true
    ) ?: toLinearPolynomial().constant
    return Quantity(adapter.intoValue(value), unit)
}

/**
 * 任务时间实现基类 / Task time implementation base class
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 */
abstract class TaskTimeImpl<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    protected val timeWindow: TimeWindow<Flt64>,
    protected val tasks: List<T>
) : TaskTime {
    protected val timeBoundary = SolverTimeWindowBoundary(timeWindow)

    protected abstract val compilation: Compilation
    protected abstract var estSlack: LinearIntermediateSymbols1<Flt64>

    override lateinit var delayTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var advanceTime: LinearIntermediateSymbols1<Flt64>

    override lateinit var overMaxDelayTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var overMaxAdvanceTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var delayLastEndTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var advanceEarliestEndTime: LinearIntermediateSymbols1<Flt64>

    private lateinit var onLastEndTime: LinearIntermediateSymbols1<Flt64>
    private lateinit var onEarliestEndTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var onTime: LinearIntermediateSymbols1<Flt64>

    private lateinit var notOnLastEndTime: LinearIntermediateSymbols1<Flt64>
    private lateinit var notOnEarliestEndTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var notOnTime: LinearIntermediateSymbols1<Flt64>

    private fun maskedSlackResult(
        model: MetaModel<Flt64>,
        task: @UnsafeVariance T,
        slack: LinearFunctionSymbolAdapter<Flt64>,
        slackName: String,
        symbolName: String
    ): Ret<LinearIntermediateSymbol<Flt64>> {
        if (!compilation.taskCancelEnabled) {
            return ok(slack)
        }
        slack.name = slackName
        when (val result = model.add(slack)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        return ok(MaskingFunction.fromLinearPolynomials(
            x = slack,
            mask = compilation.taskCompilation[task],
            converter = schedulingSolverValueAdapter,
            name = symbolName
        ))
    }

    override fun register(model: MetaModel<Flt64>): Try {
        var constructionFailure: Try = ok

        if (delayEnabled) {
            if (!::delayTime.isInitialized) {
                delayTime = LinearIntermediateSymbols1<Flt64>(
                    name = "delay_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is LinearFunctionSymbolAdapter -> {
                            slack.pos ?: LinearPolynomial(emptyList(), Flt64.zero)
                        }
                        else -> {
                            LinearPolynomial(emptyList(), Flt64.zero)
                        }
                    }
                    if (compilation.taskCancelEnabled) {
                        MaskingFunction.fromLinearPolynomials(
                            x = LinearExpressionSymbol(polynomial),
                            mask = compilation.taskCompilation[task],
                            converter = schedulingSolverValueAdapter,
                            name = "delay_time_${task}"
                        )
                    } else {
                        LinearExpressionSymbol(
                            polynomial = polynomial,
                            name = "delay_time_${task}"
                        )
                    }
                }
                for (task in tasks) {
                    delayTime[task].range.leq(timeBoundary.durationValue)
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                delayTime[task].range.leq(Flt64.zero)
                            } else {
                                delayTime[task].range.leq(
                                    timeBoundary.remainingValueAfter(time.start)
                                )
                            }
                        }
                    }
                    task.maxDelay?.let {
                        delayTime[task].range.leq(
                            timeBoundary.valueOf(it)
                        )
                    }
                }
            }
            when (val result = model.add(delayTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEnabled) {
            if (!::advanceTime.isInitialized) {
                advanceTime = LinearIntermediateSymbols1<Flt64>(
                    name = "advance_time",
                    shape = Shape1(tasks.size),
                ) { i, _ ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is LinearFunctionSymbolAdapter -> {
                            slack.neg ?: LinearPolynomial(emptyList(), Flt64.zero)
                        }
                        else -> {
                            LinearPolynomial(emptyList(), Flt64.zero)
                        }
                    }
                    if (compilation.taskCancelEnabled) {
                        MaskingFunction.fromLinearPolynomials(
                            x = LinearExpressionSymbol(polynomial),
                            mask = compilation.taskCompilation[task],
                            converter = schedulingSolverValueAdapter,
                            name = "advance_time_${task}"
                        )
                    } else {
                        LinearExpressionSymbol(
                            polynomial = polynomial,
                            name = "delay_time_${task}"
                        )
                    }
                }
                for (task in tasks) {
                    advanceTime[task].range.leq(
                        timeBoundary.durationValue
                    )
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceTime[task].range.leq(
                                    timeBoundary.elapsedValueBefore(time.start)
                                )
                            }
                        }
                    }
                    task.maxAdvance?.let {
                        advanceTime[task].range.leq(
                            timeBoundary.valueOf(it)
                        )
                    }
                }
            }
            when (val result = model.add(advanceTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (overMaxDelayEnabled) {
            if (!::overMaxDelayTime.isInitialized) {
                val symbols = ArrayList<LinearIntermediateSymbol<Flt64>>()
                for (task in tasks) {
                    val symbol = if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "over_max_delay_time_${task}"
                        )
                    } else {
                        when (val maxDelayTime = task.maxDelay) {
                            null -> {
                                LinearExpressionSymbol(
                                    Flt64,
                                    name = "over_max_delay_time_${task}"
                                )
                            }

                            else -> {
                                val slack = slackSymbol(
                                    x = delayTime[task],
                                    y = timeBoundary.valueOf(maxDelayTime),
                                    type = if (timeBoundary.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    withNegative = false,
                                    name = "over_max_delay_time_${task}"
                                )
                                when (val result = maskedSlackResult(
                                    model = model,
                                    task = task,
                                    slack = slack,
                                    slackName = "over_max_delay_time_slack_${task}",
                                    symbolName = "over_max_delay_time_${task}"
                                )) {
                                    is Ok -> result.value
                                    is Failed -> return Failed(result.error)
                                    is Fatal -> return Fatal(result.errors)
                                }
                            }
                        }
                    }
                    symbols.add(symbol)
                }
                overMaxDelayTime = LinearIntermediateSymbols1<Flt64>(
                    name = "over_max_delay_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    symbols[i]
                }
                for (task in tasks) {
                    overMaxDelayTime[task].range.leq(
                        timeBoundary.durationValue
                    )
                    when (val maxDelayTime = task.maxDelay) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxDelayTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxDelayTime[task].range.leq(
                                    timeBoundary.valueOf(maxDelayTime)
                                )
                            }
                        }
                    }
                }
            }
            when (val result = model.add(overMaxDelayTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (overMaxAdvanceEnabled) {
            if (!::overMaxAdvanceTime.isInitialized) {
                val symbols = ArrayList<LinearIntermediateSymbol<Flt64>>()
                for (task in tasks) {
                    val symbol = if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "over_max_advance_time_${task}"
                        )
                    } else {
                        when (val maxAdvanceTime = task.maxAdvance) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "over_max_advance_time_${task}"
                                )
                            }

                            else -> {
                                val slack = slackSymbol(
                                    x = advanceTime[task],
                                    y = timeBoundary.valueOf(maxAdvanceTime),
                                    type = if (timeBoundary.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    withNegative = false,
                                    name = "over_max_advance_time_${task}"
                                )
                                when (val result = maskedSlackResult(
                                    model = model,
                                    task = task,
                                    slack = slack,
                                    slackName = "over_max_advance_time_slack_${task}",
                                    symbolName = "over_max_advance_time_${task}"
                                )) {
                                    is Ok -> result.value
                                    is Failed -> return Failed(result.error)
                                    is Fatal -> return Fatal(result.errors)
                                }
                            }
                        }
                    }
                    symbols.add(symbol)
                }
                overMaxAdvanceTime = LinearIntermediateSymbols1<Flt64>(
                    name = "over_max_advance_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    symbols[i]
                }
                for (task in tasks) {
                    overMaxAdvanceTime[task].range.leq(
                        timeBoundary.durationValue
                    )
                    when (val maxAdvanceTime = task.maxAdvance) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxAdvanceTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxAdvanceTime[task].range.leq(
                                    timeBoundary.valueOf(maxAdvanceTime)
                                )
                            }
                        }
                    }
                }
            }
            when (val result = model.add(overMaxAdvanceTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::delayLastEndTime.isInitialized) {
                val symbols = ArrayList<LinearIntermediateSymbol<Flt64>>()
                for (task in tasks) {
                    val symbol = if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "delay_last_end_time_${task}"
                        )
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "delay_last_end_time_${task}"
                                )
                            }

                            else -> {
                                val slack = slackSymbol(
                                    x = estimateEndTime[task],
                                    y = timeBoundary.valueOf(lastEndTime),
                                    type = if (timeBoundary.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    withNegative = false,
                                    name = "delay_last_end_time_${task}"
                                )
                                when (val result = maskedSlackResult(
                                    model = model,
                                    task = task,
                                    slack = slack,
                                    slackName = "delay_last_end_time_slack_${task}",
                                    symbolName = "delay_last_end_time_${task}"
                                )) {
                                    is Ok -> result.value
                                    is Failed -> return Failed(result.error)
                                    is Fatal -> return Fatal(result.errors)
                                }
                            }
                        }
                    }
                    symbols.add(symbol)
                }
                delayLastEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "delay_last_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    symbols[i]
                }
                for (task in tasks) {
                    delayLastEndTime[task].range.leq(timeBoundary.durationValue)
                    when (val lastEndTime = task.lastEndTime) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                delayLastEndTime[task].range.leq(Flt64.zero)
                            } else {
                                delayLastEndTime[task].range.leq(timeBoundary.remainingValueAfter(lastEndTime))
                            }
                        }
                    }
                }
            }
            when (val result = model.add(delayLastEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::advanceEarliestEndTime.isInitialized) {
                val symbols = ArrayList<LinearIntermediateSymbol<Flt64>>()
                for (task in tasks) {
                    val symbol = if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "advance_earliest_end_time_${task}"
                        )
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "advance_earliest_end_time_${task}"
                                )
                            }

                            else -> {
                                val slack = slackSymbol(
                                    x = estimateEndTime[task],
                                    y = timeBoundary.valueOf(earliestEndTime),
                                    type = if (timeBoundary.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    withPositive = false,
                                    name = "advance_earliest_end_time_${task}"
                                )
                                when (val result = maskedSlackResult(
                                    model = model,
                                    task = task,
                                    slack = slack,
                                    slackName = "advance_earliest_end_time_slack_${task}",
                                    symbolName = "advance_earliest_end_time_${task}"
                                )) {
                                    is Ok -> result.value
                                    is Failed -> return Failed(result.error)
                                    is Fatal -> return Fatal(result.errors)
                                }
                            }
                        }
                    }
                    symbols.add(symbol)
                }
                advanceEarliestEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "advance_earliest_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    symbols[i]
                }
                for (task in tasks) {
                    advanceEarliestEndTime[task].range.leq(
                        timeBoundary.durationValue
                    )
                    when (val earliestStartTime = task.earliestStartTime) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceEarliestEndTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceEarliestEndTime[task].range.leq(
                                    timeBoundary.elapsedValueBefore(earliestStartTime)
                                )
                            }
                        }
                    }
                }
            }
            when (val result = model.add(advanceEarliestEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::onLastEndTime.isInitialized) {
                onLastEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "on_last_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "on_last_end_time_${task}"
                        )
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "on_last_end_time_${task}"
                                )
                            }

                            else -> {
                                val input = captureLinearConstraintInput(
                                    LinearConstraintInput.from(
                                        relation = estimateEndTime[task] leq timeBoundary.valueOf(lastEndTime),
                                        converter = schedulingSolverValueAdapter,
                                        lhsRange = estimateEndTime[task].range.range!!,
                                        rhsConstant = Flt64.zero
                                    )
                                ) { constructionFailure = it }
                                if (input == null) {
                                    LinearIntermediateSymbol.empty(
                                        Flt64,
                                        name = "on_last_end_time_${task}"
                                    )
                                } else {
                                    IfThenFunction.from(
                                        inequality = input,
                                        converter = schedulingSolverValueAdapter,
                                        name = "on_last_end_time_${task}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            when (val failure = constructionFailure) {
                is Ok -> {}
                is Failed -> return Failed(failure.error)
                is Fatal -> return Fatal(failure.errors)
            }
            when (val result = model.add(onLastEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::onEarliestEndTime.isInitialized) {
                onEarliestEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "on_earliest_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "on_earliest_end_time_${task}"
                        )
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "on_earliest_end_time_${task}"
                                )
                            }

                            else -> {
                                val input = captureLinearConstraintInput(
                                    LinearConstraintInput.from(
                                        relation = estimateEndTime[task] geq timeBoundary.valueOf(earliestEndTime),
                                        converter = schedulingSolverValueAdapter,
                                        lhsRange = estimateEndTime[task].range.range!!,
                                        rhsConstant = Flt64.zero
                                    )
                                ) { constructionFailure = it }
                                if (input == null) {
                                    LinearIntermediateSymbol.empty(
                                        Flt64,
                                        name = "on_earliest_end_time_${task}"
                                    )
                                } else {
                                    IfThenFunction.from(
                                        inequality = input,
                                        converter = schedulingSolverValueAdapter,
                                        name = "on_earliest_end_time_${task}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        when (val failure = constructionFailure) {
            is Ok -> {}
            is Failed -> return Failed(failure.error)
            is Fatal -> return Fatal(failure.errors)
        }
        when (val result = model.add(onEarliestEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::onTime.isInitialized) {
                onTime = LinearIntermediateSymbols1<Flt64>(
                    name = "on_time",
                    shape = Shape1(tasks.size)
                ) { t, _ ->
                    val task = tasks[t]
                    LinearExpressionSymbol(
                        polynomial = if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            onLastEndTime[t].toLinearPolynomial() + onEarliestEndTime[t].toLinearPolynomial()
                        } else if (delayLastEndTimeEnabled) {
                            LinearPolynomial(listOf(LinearMonomial(Flt64.one, onLastEndTime[t])), Flt64.zero)
                        } else if (advanceEarliestEndTimeEnabled) {
                            LinearPolynomial(listOf(LinearMonomial(Flt64.one, onEarliestEndTime[t])), Flt64.zero)
                        } else {
                            LinearPolynomial(emptyList(), Flt64.one)
                        },
                        name = "on_time_${task}"
                    )
                }
            }
            when (val result = model.add(onTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::notOnLastEndTime.isInitialized) {
                notOnLastEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "not_on_last_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "not_on_last_end_time_${task}"
                        )
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "not_on_last_end_time_${task}"
                                )
                            }

                            else -> {
                                val input = captureLinearConstraintInput(
                                    LinearConstraintInput.from(
                                        relation = estimateEndTime[task] geq timeBoundary.afterWindowDurationValue(lastEndTime),
                                        converter = schedulingSolverValueAdapter,
                                        lhsRange = estimateEndTime[task].range.range!!,
                                        rhsConstant = Flt64.zero
                                    )
                                ) { constructionFailure = it }
                                if (input == null) {
                                    LinearIntermediateSymbol.empty(
                                        Flt64,
                                        name = "not_on_last_end_time_${task}"
                                    )
                                } else {
                                    IfThenFunction.from(
                                        inequality = input,
                                        converter = schedulingSolverValueAdapter,
                                        name = "not_on_last_end_time_${task}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            when (val failure = constructionFailure) {
                is Ok -> {}
                is Failed -> return Failed(failure.error)
                is Fatal -> return Fatal(failure.errors)
            }
            when (val result = model.add(notOnLastEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::notOnEarliestEndTime.isInitialized) {
                notOnEarliestEndTime = LinearIntermediateSymbols1<Flt64>(
                    name = "on_earliest_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "not_on_earliest_end_time_${task}"
                        )
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "not_on_earliest_end_time_${task}"
                                )
                            }

                            else -> {
                                val input = captureLinearConstraintInput(
                                    LinearConstraintInput.from(
                                        relation = estimateEndTime[task] leq timeBoundary.beforeWindowDurationValue(earliestEndTime),
                                        converter = schedulingSolverValueAdapter,
                                        lhsRange = estimateEndTime[task].range.range!!,
                                        rhsConstant = Flt64.zero
                                    )
                                ) { constructionFailure = it }
                                if (input == null) {
                                    LinearIntermediateSymbol.empty(
                                        Flt64,
                                        name = "not_on_earliest_end_time_${task}"
                                    )
                                } else {
                                    IfThenFunction.from(
                                        inequality = input,
                                        converter = schedulingSolverValueAdapter,
                                        name = "not_on_earliest_end_time_${task}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        when (val failure = constructionFailure) {
            is Ok -> {}
            is Failed -> return Failed(failure.error)
            is Fatal -> return Fatal(failure.errors)
        }
        when (val result = model.add(notOnEarliestEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::notOnTime.isInitialized) {
                notOnTime = LinearIntermediateSymbols1<Flt64>(
                    name = "not_on_time",
                    shape = Shape1(tasks.size)
                ) { t, _ ->
                    val task = tasks[t]
                    LinearExpressionSymbol(
                        polynomial = if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            notOnLastEndTime[t].toLinearPolynomial() + notOnEarliestEndTime[t].toLinearPolynomial()
                        } else if (delayLastEndTimeEnabled) {
                            LinearPolynomial(listOf(LinearMonomial(Flt64.one, notOnLastEndTime[t])), Flt64.zero)
                        } else if (advanceEarliestEndTimeEnabled) {
                            LinearPolynomial(listOf(LinearMonomial(Flt64.one, notOnEarliestEndTime[t])), Flt64.zero)
                        } else {
                            LinearPolynomial(emptyList(), Flt64.zero)
                        },
                        name = "not_on_time_${task}"
                    )
                }
            }
            when (val result = model.add(notOnTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}

/**
 * 任务调度时间 / Task scheduling time
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param compilation 任务编译结果 / Task compilation result
 * @param estimateEndTimeCalculator 预估结束时间计算器 / Estimate end time calculator
 * @param delayEnabled 是否启用延迟 / Whether delay is enabled
 * @param overMaxDelayEnabled 是否启用超最大延迟 / Whether over-max delay is enabled
 * @param advanceEnabled 是否启用提前 / Whether advance is enabled
 * @param overMaxAdvanceEnabled 是否启用超最大提前 / Whether over-max advance is enabled
 * @param delayLastEndTimeEnabled 是否启用延迟最后结束时间 / Whether delay last end time is enabled
 * @param advanceEarliestEndTimeEnabled 是否启用提前最早结束时间 / Whether advance earliest end time is enabled
 */
class TaskSchedulingTaskTime<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow<Flt64>,
    tasks: List<T>,
    override val compilation: TaskCompilation<T, E, A>,
    private val estimateEndTimeCalculator: (T, LinearPolynomial<Flt64>) -> LinearPolynomial<Flt64>,
    override val delayEnabled: Boolean = false,
    override val overMaxDelayEnabled: Boolean = false,
    override val advanceEnabled: Boolean = false,
    override val overMaxAdvanceEnabled: Boolean = false,
    override val delayLastEndTimeEnabled: Boolean = false,
    override val advanceEarliestEndTimeEnabled: Boolean = false
) : TaskTimeImpl<T, E, A>(timeWindow, tasks) {
    /**
     * 通过 solver 时间窗口边界创建任务调度时间 / Create task scheduling time from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param compilation 任务编译结果 / Task compilation result
     * @param estimateEndTimeCalculator 预估结束时间计算器 / Estimate end time calculator
     * @param delayEnabled 是否启用延迟 / Whether delay is enabled
     * @param overMaxDelayEnabled 是否启用超最大延迟 / Whether over-max delay is enabled
     * @param advanceEnabled 是否启用提前 / Whether advance is enabled
     * @param overMaxAdvanceEnabled 是否启用超最大提前 / Whether over-max advance is enabled
     * @param delayLastEndTimeEnabled 是否启用延迟最后结束时间 / Whether delay last end time is enabled
     * @param advanceEarliestEndTimeEnabled 是否启用提前最早结束时间 / Whether advance earliest end time is enabled
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        compilation: TaskCompilation<T, E, A>,
        estimateEndTimeCalculator: (T, LinearPolynomial<Flt64>) -> LinearPolynomial<Flt64>,
        delayEnabled: Boolean = false,
        overMaxDelayEnabled: Boolean = false,
        advanceEnabled: Boolean = false,
        overMaxAdvanceEnabled: Boolean = false,
        delayLastEndTimeEnabled: Boolean = false,
        advanceEarliestEndTimeEnabled: Boolean = false
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        compilation = compilation,
        estimateEndTimeCalculator = estimateEndTimeCalculator,
        delayEnabled = delayEnabled,
        overMaxDelayEnabled = overMaxDelayEnabled,
        advanceEnabled = advanceEnabled,
        overMaxAdvanceEnabled = overMaxAdvanceEnabled,
        delayLastEndTimeEnabled = delayLastEndTimeEnabled,
        advanceEarliestEndTimeEnabled = advanceEarliestEndTimeEnabled
    )

    lateinit var est: Variable1<*>
    override lateinit var estSlack: LinearIntermediateSymbols1<Flt64>

    override lateinit var estimateStartTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var estimateEndTime: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::est.isInitialized) {
            est = if (timeBoundary.continues) {
                val est = URealVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    val variable = est[task]
                    variable.name = "${est.name}_${task}"
                    variable.range.leq(timeBoundary.endValue)

                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!advanceEnabled || !task.advanceEnabled) {
                                variable.range.geq(timeBoundary.valueOf(time.start))
                            }
                            if (!delayEnabled || !task.delayEnabled) {
                                variable.range.leq(timeBoundary.valueOf(time.start))
                            }
                        }
                    }
                }
                est
            } else {
                val est = UIntVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    val variable = est[task]
                    variable.name = "${est.name}_${task}"
                    variable.range.leq(timeBoundary.endValue.floor().toUInt64())

                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!advanceEnabled || !task.advanceEnabled) {
                                variable.range.geq(timeBoundary.unsignedFlooredValueOf(time.start))
                            }
                            if (!delayEnabled || !task.delayEnabled) {
                                variable.range.leq(timeBoundary.unsignedFlooredValueOf(time.start))
                            }
                        }
                    }
                }
                est
            }
        }
        when (val result = model.add(est)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayEnabled || advanceEnabled) {
            if (!::estSlack.isInitialized) {
                estSlack = LinearIntermediateSymbols1<Flt64>(
                    name = "est_slack",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled && !task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "est_slack_${task}"
                        )
                    } else {
                        when (val time = task.time) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    Flt64,
                                    name = "est_slack_${task}"
                                )
                            }

                            else -> {
                                val y = if (timeBoundary.continues) {
                                    timeBoundary.valueOf(time.start)
                                } else {
                                    timeBoundary.flooredValueOf(time.start)
                                }
                                val slack = slackSymbol(
                                    x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, est[task])), Flt64.zero),
                                    y = y,
                                    type = if (timeBoundary.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    withNegative = advanceEnabled && task.advanceEnabled,
                                    withPositive = delayEnabled && task.delayEnabled,
                                    name = "est_slack_${task}"
                                )
                                slack.range.set(
                                    ValueRange(
                                        -y,
                                        timeBoundary.endValue - y
                                    ).value!!
                                )
                                slack
                            }
                        }
                    }
                }
            }
            when (val result = model.add(estSlack)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = LinearIntermediateSymbols1<Flt64>(
                name = "estimate_start_time",
                shape = Shape1(tasks.size)
            ) { t, _ ->
                val task = tasks[t]
                LinearExpressionSymbol(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, est[t])), Flt64.zero),
                    name = "estimate_start_time_${task}"
                )
            }
            for (task in tasks) {
                estimateStartTime[task].range.leq(timeBoundary.endValue)
                when (val time = task.time) {
                    null -> {}

                    else -> {
                        if (!advanceEnabled || !task.advanceEnabled) {
                            estimateStartTime[task].range.geq(timeBoundary.valueOf(time.start))
                        }
                        if (!delayEnabled || !task.delayEnabled) {
                            estimateStartTime[task].range.leq(timeBoundary.valueOf(time.start))
                        }
                    }
                }
            }
        }
        when (val result = model.add(estimateStartTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = LinearIntermediateSymbols1<Flt64>(
                name = "estimate_end_time",
                shape = Shape1(tasks.size)
            ) { t, _ ->
                val task = tasks[t]
                LinearExpressionSymbol(
                    estimateEndTimeCalculator(task, LinearPolynomial(listOf(LinearMonomial(Flt64.one, estimateStartTime[t])), Flt64.zero)),
                    name = "estimate_end_time_${task}"
                )
            }
            for (task in tasks) {
                estimateEndTime[task].range.leq(timeBoundary.endValue)
                when (val lastEndTime = task.lastEndTime) {
                    null -> {}

                    else -> {
                        if (!delayLastEndTimeEnabled || !task.delayEnabled) {
                            estimateEndTime[task].range.leq(timeBoundary.valueOf(lastEndTime))
                        }
                    }
                }
                when (val earliestEndTime = task.earliestEndTime) {
                    null -> {}

                    else -> {
                        if (!advanceEarliestEndTimeEnabled || !task.advanceEnabled) {
                            estimateEndTime[task].range.geq(timeBoundary.valueOf(earliestEndTime))
                        }
                    }
                }
            }
        }
        when (val result = model.add(estimateEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }
}

/**
 * 迭代任务调度时间 / Iterative task scheduling time
 *
 * @param IT 迭代任务类型 / Iterative task type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param compilation 迭代任务编译结果 / Iterative task compilation result
 * @param redundancyRange 冗余范围 / Redundancy range
 */
open class IterativeTaskSchedulingTaskTime<
        IT : IterativeAbstractTask<E, A>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow<Flt64>,
    tasks: List<T>,
    override val compilation: IterativeTaskCompilation<IT, T, E, A>,
    private val redundancyRange: Duration? = null
) : TaskTimeImpl<T, E, A>(timeWindow, tasks) {
    /**
     * 通过 solver 时间窗口边界创建迭代任务调度时间 / Create iterative task scheduling time from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param compilation 迭代任务编译结果 / Iterative task compilation result
     * @param redundancyRange 冗余范围 / Redundancy range
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        compilation: IterativeTaskCompilation<IT, T, E, A>,
        redundancyRange: Duration? = null
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        compilation = compilation,
        redundancyRange = redundancyRange
    )

    override val delayEnabled: Boolean = true
    override val overMaxDelayEnabled: Boolean = true
    override val advanceEnabled: Boolean = true
    override val overMaxAdvanceEnabled: Boolean = true

    override val delayLastEndTimeEnabled: Boolean = true
    override val advanceEarliestEndTimeEnabled: Boolean = true

    private val withRedundancy get() = redundancyRange != null

    private lateinit var estRedundancy: Variable1<*>
    override lateinit var estSlack: LinearIntermediateSymbols1<Flt64>

    override lateinit var estimateStartTime: LinearIntermediateSymbols1<Flt64>
    override lateinit var estimateEndTime: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (withRedundancy) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "IterativeTaskTime.register 暂未实现 withRedundancy 路径，请先关闭 redundancyRange 或补充冗余建模逻辑。"
            )
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = LinearIntermediateSymbols1<Flt64>(
                name = "estimate_start_time",
                shape = Shape1(tasks.size)
            ) { t, _ ->
                val task = tasks[t]
                LinearExpressionSymbol(
                    if (::estRedundancy.isInitialized) {
                        LinearPolynomial(listOf(LinearMonomial(Flt64.one, estRedundancy[t])), Flt64.zero)
                    } else {
                        LinearPolynomial(emptyList(), Flt64.zero)
                    },
                    name = "estimate_start_time_${task}"
                )
            }
        }
        when (val result = model.add(estimateStartTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = LinearIntermediateSymbols1<Flt64>(
                name = "estimate_end_time",
                shape = Shape1(tasks.size)
            ) { t, _ ->
                val task = tasks[t]
                LinearExpressionSymbol(
                    if (::estRedundancy.isInitialized) {
                        LinearPolynomial(listOf(LinearMonomial(Flt64.one, estRedundancy[t])), Flt64.zero)
                    } else {
                        LinearPolynomial(emptyList(), Flt64.zero)
                    },
                    name = "estimate_end_time_${task}"
                )
            }
        }
        when (val result = model.add(estimateEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::estSlack.isInitialized) {
            estSlack = LinearIntermediateSymbols1<Flt64>(
                name = "est_slack",
                shape = Shape1(tasks.size)
            ) { i, _ ->
                val task = tasks[i]
                if (!task.delayEnabled && !task.advanceEnabled) {
                    LinearIntermediateSymbol.empty(
                        Flt64,
                        name = "est_slack_${task}"
                    )
                } else {
                    when (val time = task.time) {
                        null -> {
                            LinearIntermediateSymbol.empty(
                                Flt64,
                                name = "est_slack_${task}"
                            )
                        }

                        else -> {
                            val y = if (timeBoundary.continues) {
                                timeBoundary.valueOf(time.start)
                            } else {
                                timeBoundary.flooredValueOf(time.start)
                            }
                            val slack = slackSymbol(
                                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, estimateStartTime[task])), Flt64.zero),
                                y = y,
                                type = if (timeBoundary.continues) {
                                    UContinuous
                                } else {
                                    UInteger
                                },
                                withNegative = advanceEnabled && task.advanceEnabled,
                                withPositive = delayEnabled && task.delayEnabled,
                                name = "est_slack_${task}"
                            )
                            slack.range.set(ValueRange(-y, timeBoundary.endValue - y).value!!)
                            slack
                        }
                    }
                }
            }
        }
        when (val result = model.add(estSlack)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }

    open fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        assert(tasks.isNotEmpty())

        val xi = compilation.x.getOrNull(iteration.toInt())
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "TaskTime.addColumns iteration $iteration is out of range."
            )
        for (task in tasks) {
            val thisNewTasks = newTasks.filter { it.key == task.key }
            if (thisNewTasks.isNotEmpty()) {
                val est = estimateStartTime[task]
                val eet = estimateEndTime[task]

                est.flush()
                eet.flush()

                for (newTask in thisNewTasks) {
                    val time = newTask.time!!
                    est.asMutable() += LinearMonomial(timeBoundary.valueOf(time.start), xi[newTask])
                    eet.asMutable() += LinearMonomial(timeBoundary.valueOf(time.end), xi[newTask])
                }
            }
        }

        return ok
    }
}
