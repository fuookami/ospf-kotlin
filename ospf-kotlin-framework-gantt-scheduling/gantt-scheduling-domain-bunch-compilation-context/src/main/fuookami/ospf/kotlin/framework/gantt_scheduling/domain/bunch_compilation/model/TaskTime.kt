@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务束时间模型 / Bunch time model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskTimeImpl
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration

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
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}

/**
 * 任务束调度时间 / Bunch scheduling task time
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param compilation 任务束编译结果 / Bunch compilation result
 * @param redundancyRange 冗余范围 / Redundancy range
 */
open class BunchSchedulingTaskTime<
        B : AbstractTaskBunch<T, E, A>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow,
    tasks: List<T>,
    override val compilation: BunchCompilation<B, T, E, A>,
    private val redundancyRange: Duration? = null,
) : TaskTimeImpl<T, E, A>(timeWindow, tasks) {
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

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    override fun register(model: MetaModel<Flt64>): Try {
        if (withRedundancy) {
            if (!::estRedundancy.isInitialized) {
                estRedundancy = if (timeWindow.continues) {
                    val redundancyValue = with(timeWindow) { redundancyRange!!.value }
                    val est = RealVariable1("est_redundancy", Shape1(tasks.size))
                    for (task in tasks) {
                        val variable = est[task]
                        variable.name = "${est.name}_${task}"
                        variable.range.geq(-redundancyValue)
                        variable.range.leq(redundancyValue)

                        if (task.time != null) {
                            if (!task.advanceEnabled) {
                                variable.range.geq(Flt64.zero)
                            }
                            if (!task.delayEnabled) {
                                variable.range.leq(Flt64.zero)
                            }
                        }
                    }
                    est
                } else {
                    val redundancyValue = with(timeWindow) { redundancyRange!!.value }.floor().toInt64()
                    val est = IntVariable1("est", Shape1(tasks.size))
                    for (task in tasks) {
                        val variable = est[task]
                        variable.name = "${est.name}_${task}"
                        variable.range.geq(-redundancyValue)
                        variable.range.leq(redundancyValue)

                        if (task.time != null) {
                            if (!task.advanceEnabled) {
                                variable.range.geq(Int64.zero)
                            }
                            if (!task.delayEnabled) {
                                variable.range.leq(Int64.zero)
                            }
                        }
                    }
                    est
                }
            }
            when (val result = model.add(estRedundancy)) {
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
            ) { i, _ ->
                val task = tasks[i]
                LinearExpressionSymbol(
                    polynomial = if (::estRedundancy.isInitialized) {
                        LinearPolynomial(listOf(LinearMonomial(Flt64.one, estRedundancy[task])), Flt64.zero)
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
            ) { i, _ ->
                val task = tasks[i]
                LinearExpressionSymbol(
                    polynomial = if (::estRedundancy.isInitialized) {
                        LinearPolynomial(listOf(LinearMonomial(Flt64.one, estRedundancy[task])), Flt64.zero)
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
                            val y = if (timeWindow.continues) {
                                with(timeWindow) { time.start.value }
                            } else {
                                with(timeWindow) { time.start.value }.floor()
                            }
                            val slack = slackSymbol(
                                x = estimateStartTime[task],
                                y = y,
                                type = if (timeWindow.continues) {
                                    UContinuous
                                } else {
                                    UInteger
                                },
                                withNegative = advanceEnabled && task.advanceEnabled,
                                withPositive = delayEnabled && task.delayEnabled,
                                name = "est_slack_${task}"
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

        return super.register(model)
    }

    /**
     * 添加列 / Add columns
     *
     * @param iteration 迭代次数 / Iteration count
     * @param bunches 任务束列表 / List of bunches
     * @param model 线性元模型 / Linear meta model
     * @return 操作结果 / Operation result
     */
    open fun addColumns(
        iteration: UInt64,
        bunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x.getOrNull(iteration.toInt())
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "TaskTime.addColumns iteration $iteration is out of range."
            )
        for (task in tasks) {
            val thisBunches = bunches.filter { it.contains(task) }
            if (thisBunches.isNotEmpty()) {
                val est = estimateStartTime[task]
                val eet = estimateEndTime[task]

                est.flush()
                eet.flush()

                for (bunch in thisBunches) {
                    val actualTask = bunch.get(task) ?: continue
                    val time = actualTask.time!!
                    est.asMutable() += LinearMonomial(with(timeWindow) { time.start.value }, xi[bunch])
                    eet.asMutable() += LinearMonomial(with(timeWindow) { time.end.value }, xi[bunch])
                }
            }
        }

        return ok
    }
}



