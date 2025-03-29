package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface TaskTime {
    val delayEnabled: Boolean
    val overMaxDelayEnabled: Boolean
    val advanceEnabled: Boolean
    val overMaxAdvanceEnabled: Boolean

    val delayLastEndTimeEnabled: Boolean
    val advanceEarliestEndTimeEnabled: Boolean

    val estimateStartTime: LinearIntermediateSymbols1
    val estimateEndTime: LinearIntermediateSymbols1

    val delayTime: LinearIntermediateSymbols1
    val advanceTime: LinearIntermediateSymbols1
    val overMaxDelayTime: LinearIntermediateSymbols1
    val overMaxAdvanceTime: LinearIntermediateSymbols1
    val delayLastEndTime: LinearIntermediateSymbols1
    val advanceEarliestEndTime: LinearIntermediateSymbols1

    val onTime: LinearIntermediateSymbols1
    val notOnTime: LinearIntermediateSymbols1

    fun register(model: MetaModel): Try
}

abstract class TaskTimeImpl<
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    protected val timeWindow: TimeWindow,
    protected val tasks: List<T>
) : TaskTime {
    abstract val compilation: Compilation
    protected abstract var estSlack: LinearIntermediateSymbols1

    override lateinit var delayTime: LinearIntermediateSymbols1
    override lateinit var advanceTime: LinearIntermediateSymbols1

    override lateinit var overMaxDelayTime: LinearIntermediateSymbols1
    override lateinit var overMaxAdvanceTime: LinearIntermediateSymbols1
    override lateinit var delayLastEndTime: LinearIntermediateSymbols1
    override lateinit var advanceEarliestEndTime: LinearIntermediateSymbols1

    private lateinit var onLastEndTime: LinearIntermediateSymbols1
    private lateinit var onEarliestEndTime: LinearIntermediateSymbols1
    override lateinit var onTime: LinearIntermediateSymbols1

    private lateinit var notOnLastEndTime: LinearIntermediateSymbols1
    private lateinit var notOnEarliestEndTime: LinearIntermediateSymbols1
    override lateinit var notOnTime: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        if (delayEnabled) {
            if (!::delayTime.isInitialized) {
                delayTime = LinearIntermediateSymbols1(
                    "delay_time",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is AbstractSlackFunction<*> -> {
                            LinearPolynomial(slack.pos!!)
                        }

                        else -> {
                            LinearPolynomial()
                        }
                    }
                    if (compilation.taskCancelEnabled) {
                        SemiFunction(
                            if (timeWindow.continues) {
                                UContinuous
                            } else {
                                UInteger
                            },
                            x = polynomial,
                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                            name = "delay_time_$task"
                        )
                    } else {
                        LinearExpressionSymbol(polynomial, "delay_time_$task")
                    }
                }
                for (task in tasks) {
                    delayTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                delayTime[task].range.leq(Flt64.zero)
                            } else {
                                delayTime[task].range.leq(with(timeWindow) { (timeWindow.end - time.start).value })
                            }
                        }
                    }
                    task.maxDelay?.let {
                        delayTime[task].range.leq(with(timeWindow) { it.value })
                    }
                }
            }
            when (val result = model.add(delayTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (advanceEnabled) {
            if (!::advanceTime.isInitialized) {
                advanceTime = LinearIntermediateSymbols1(
                    "advance_time",
                    Shape1(tasks.size),
                ) { i, _ ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is AbstractSlackFunction<*> -> {
                            LinearPolynomial(slack.neg!!)
                        }

                        else -> {
                            LinearPolynomial()
                        }
                    }
                    if (compilation.taskCancelEnabled) {
                        SemiFunction(
                            if (timeWindow.continues) {
                                UContinuous
                            } else {
                                UInteger
                            },
                            x = polynomial,
                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                            name = "advance_time_$task"
                        )
                    } else {
                        LinearExpressionSymbol(polynomial, "delay_time_$task")
                    }
                }
                for (task in tasks) {
                    advanceTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceTime[task].range.leq(with(timeWindow) { (time.start - timeWindow.start).value })
                            }
                        }
                    }
                    task.maxAdvance?.let {
                        advanceTime[task].range.leq(with(timeWindow) { it.value })
                    }
                }
            }
            when (val result = model.add(advanceTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (overMaxDelayEnabled) {
            if (!::overMaxDelayTime.isInitialized) {
                try {
                    overMaxDelayTime = LinearIntermediateSymbols1(
                        "over_max_delay_time",
                        Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.delayEnabled) {
                            LinearExpressionSymbol(LinearPolynomial(), "over_max_delay_time_$task")
                        } else {
                            when (val maxDelayTime = task.maxDelay) {
                                null -> {
                                    LinearExpressionSymbol(LinearPolynomial(), "over_max_delay_time_$task")
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        x = LinearPolynomial(delayTime[task]),
                                        y = LinearPolynomial(with(timeWindow) { maxDelayTime.value }),
                                        withNegative = false,
                                        name = "over_max_delay_time_$task"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "over_max_delay_time_slack_$task"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw ApplicationException(result.error)
                                            }
                                        }
                                        SemiFunction(
                                            if (timeWindow.continues) {
                                                UContinuous
                                            } else {
                                                UInteger
                                            },
                                            x = LinearPolynomial(slack),
                                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                                            name = "over_max_delay_time_$task"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: ApplicationException) {
                    return Failed(e.error)
                }
                for (task in tasks) {
                    overMaxDelayTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val maxDelayTime = task.maxDelay) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxDelayTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxDelayTime[task].range.leq(with(timeWindow) { maxDelayTime.value })
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
            }
        }

        if (overMaxAdvanceEnabled) {
            if (!::overMaxAdvanceTime.isInitialized) {
                try {
                    overMaxAdvanceTime = LinearIntermediateSymbols1(
                        "over_max_advance_time",
                        Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.advanceEnabled) {
                            LinearExpressionSymbol(LinearPolynomial(), "over_max_advance_time_$task")
                        } else {
                            when (val maxAdvanceTime = task.maxAdvance) {
                                null -> {
                                    LinearExpressionSymbol(LinearPolynomial(), "over_max_advance_time_$task")
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        x = LinearPolynomial(advanceTime[task]),
                                        y = LinearPolynomial(with(timeWindow) { maxAdvanceTime.value }),
                                        withNegative = false,
                                        name = "over_max_advance_time_$task"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "over_max_delay_time_slack_$task"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw ApplicationException(result.error)
                                            }
                                        }
                                        SemiFunction(
                                            if (timeWindow.continues) {
                                                UContinuous
                                            } else {
                                                UInteger
                                            },
                                            x = LinearPolynomial(slack),
                                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                                            name = "over_max_advance_time_$task"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: ApplicationException) {
                    return Failed(e.error)
                }
                for (task in tasks) {
                    overMaxAdvanceTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val maxAdvanceTime = task.maxAdvance) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxAdvanceTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxAdvanceTime[task].range.leq(with(timeWindow) { maxAdvanceTime.value })
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
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::delayLastEndTime.isInitialized) {
                try {
                    delayLastEndTime = LinearIntermediateSymbols1(
                        "delay_last_end_time",
                        Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.delayEnabled) {
                            LinearExpressionSymbol(LinearPolynomial(), "delay_last_end_time_$task")
                        } else {
                            when (val lastEndTime = task.lastEndTime) {
                                null -> {
                                    LinearExpressionSymbol(LinearPolynomial(), "delay_last_end_time_$task")
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        x = LinearPolynomial(estimateEndTime[task]),
                                        y = LinearPolynomial(with(timeWindow) { lastEndTime.value }),
                                        withNegative = false,
                                        name = "delay_last_end_time_$task"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "delay_last_end_time_slack_$task"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw ApplicationException(result.error)
                                            }
                                        }
                                        SemiFunction(
                                            if (timeWindow.continues) {
                                                UContinuous
                                            } else {
                                                UInteger
                                            },
                                            x = LinearPolynomial(slack),
                                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                                            name = "delay_last_end_time_$task"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: ApplicationException) {
                    return Failed(e.error)
                }
                for (task in tasks) {
                    delayLastEndTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val lastEndTime = task.lastEndTime) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                delayLastEndTime[task].range.leq(Flt64.zero)
                            } else {
                                delayLastEndTime[task].range.leq(with(timeWindow) { (timeWindow.end - lastEndTime).value })
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
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::advanceEarliestEndTime.isInitialized) {
                try {
                    advanceEarliestEndTime = LinearIntermediateSymbols1(
                        "advance_earliest_end_time",
                        Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.advanceEnabled) {
                            LinearExpressionSymbol(LinearPolynomial(), "advance_earliest_end_time_$task")
                        } else {
                            when (val earliestEndTime = task.earliestEndTime) {
                                null -> {
                                    LinearExpressionSymbol(LinearPolynomial(), "advance_earliest_end_time_$task")
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        x = LinearPolynomial(estimateEndTime[task]),
                                        y = LinearPolynomial(with(timeWindow) { earliestEndTime.value }),
                                        withPositive = false,
                                        name = "advance_earliest_end_time_$task"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "advance_earliest_end_time_slack_$task"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw ApplicationException(result.error)
                                            }
                                        }
                                        SemiFunction(
                                            if (timeWindow.continues) {
                                                UContinuous
                                            } else {
                                                UInteger
                                            },
                                            x = LinearPolynomial(slack),
                                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                                            name = "advance_earliest_end_time_$task"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: ApplicationException) {
                    return Failed(e.error)
                }
                for (task in tasks) {
                    advanceEarliestEndTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val earliestStartTime = task.earliestStartTime) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceEarliestEndTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceEarliestEndTime[task].range.leq(with(timeWindow) { (earliestStartTime - timeWindow.start).value })
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
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::onLastEndTime.isInitialized) {
                onLastEndTime = LinearIntermediateSymbols1(
                    "on_last_end_time",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(1), "on_last_end_time_${task}")
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(1), "on_last_end_time_${task}")
                            }

                            else -> {
                                IfFunction(
                                    estimateEndTime[task] leq with(timeWindow) { lastEndTime.value },
                                    "on_last_end_time_${task}"
                                )
                            }
                        }
                    }
                }
            }
            when (val result = model.add(onLastEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::onEarliestEndTime.isInitialized) {
                onEarliestEndTime = LinearIntermediateSymbols1(
                    "on_earliest_end_time",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(1), "on_earliest_end_time_${task}")
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(1), "on_earliest_end_time_${task}")
                            }

                            else -> {
                                IfFunction(
                                    estimateEndTime[task] geq with(timeWindow) { earliestEndTime.value },
                                    "on_earliest_end_time_${task}"
                                )
                            }
                        }
                    }
                }
            }
        }
        when (val result = model.add(onEarliestEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::onTime.isInitialized) {
                onTime = flatMap(
                    "on_time",
                    tasks,
                    { t ->
                        if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            onLastEndTime[t] + onEarliestEndTime[t]
                        } else if (delayLastEndTimeEnabled) {
                            LinearPolynomial(onLastEndTime[t])
                        } else if (advanceEarliestEndTimeEnabled) {
                            LinearPolynomial(onEarliestEndTime[t])
                        } else {
                            LinearPolynomial(1)
                        }
                    },
                    { (_, t) -> "$t" }
                )
            }
            when (val result = model.add(onTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (delayLastEndTimeEnabled) {
            if (!::notOnLastEndTime.isInitialized) {
                notOnLastEndTime = LinearIntermediateSymbols1(
                    "not_on_last_end_time",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(0), "not_on_last_end_time_${task}")
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(0), "not_on_last_end_time_${task}")
                            }

                            else -> {
                                IfFunction(
                                    estimateEndTime[task] geq with(timeWindow) { (lastEndTime + timeWindow.duration).value },
                                    "not_on_last_end_time_${task}"
                                )
                            }
                        }
                    }
                }
            }
            when (val result = model.add(notOnLastEndTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::notOnEarliestEndTime.isInitialized) {
                notOnEarliestEndTime = LinearIntermediateSymbols1(
                    "on_earliest_end_time",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(0), "not_on_earliest_end_time_${task}")
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(0), "not_on_earliest_end_time_${task}")
                            }

                            else -> {
                                IfFunction(
                                    estimateEndTime[task] leq with(timeWindow) { (earliestEndTime - timeWindow.duration).value },
                                    "not_on_earliest_end_time_${task}"
                                )
                            }
                        }
                    }
                }
            }
        }
        when (val result = model.add(notOnEarliestEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::notOnTime.isInitialized) {
                notOnTime = flatMap(
                    "not_on_time",
                    tasks,
                    { t ->
                        if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            notOnLastEndTime[t] + notOnEarliestEndTime[t]
                        } else if (delayLastEndTimeEnabled) {
                            LinearPolynomial(notOnLastEndTime[t])
                        } else if (advanceEarliestEndTimeEnabled) {
                            LinearPolynomial(notOnEarliestEndTime[t])
                        } else {
                            LinearPolynomial(0)
                        }
                    },
                    { (_, t) -> "$t" }
                )
            }
            when (val result = model.add(notOnTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}

class TaskSchedulingTaskTime<
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    override val compilation: TaskCompilation<T, E, A>,
    private val estimateEndTimeCalculator: (T, LinearPolynomial) -> LinearPolynomial,
    override val delayEnabled: Boolean = false,
    override val overMaxDelayEnabled: Boolean = false,
    override val advanceEnabled: Boolean = false,
    override val overMaxAdvanceEnabled: Boolean = false,
    override val delayLastEndTimeEnabled: Boolean = false,
    override val advanceEarliestEndTimeEnabled: Boolean = false
) : TaskTimeImpl<T, E, A>(timeWindow, tasks) {
    lateinit var est: Variable1<*>
    override lateinit var estSlack: LinearIntermediateSymbols1

    override lateinit var estimateStartTime: LinearIntermediateSymbols1
    override lateinit var estimateEndTime: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        if (!::est.isInitialized) {
            est = if (timeWindow.continues) {
                val est = URealVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    val variable = est[task]
                    variable.name = "${est.name}_${task}"
                    variable.range.leq(with(timeWindow) { end.value })

                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!advanceEnabled || !task.advanceEnabled) {
                                variable.range.geq(with(timeWindow) { time.start.value })
                            }
                            if (!delayEnabled || !task.delayEnabled) {
                                variable.range.leq(with(timeWindow) { time.start.value })
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
                    variable.range.leq(with(timeWindow) { end.value }.floor().toUInt64())

                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!advanceEnabled || !task.advanceEnabled) {
                                variable.range.geq(with(timeWindow) { time.start.value }.floor().toUInt64())
                            }
                            if (!delayEnabled || !task.delayEnabled) {
                                variable.range.leq(with(timeWindow) { time.start.value }.floor().toUInt64())
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
        }

        if (delayEnabled || advanceEnabled) {
            if (!::estSlack.isInitialized) {
                estSlack = LinearIntermediateSymbols1(
                    "est_slack",
                    Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled && !task.advanceEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                    } else {
                        when (val time = task.time) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                            }

                            else -> {
                                val y = if (timeWindow.continues) {
                                    with(timeWindow) { time.start.value }
                                } else {
                                    with(timeWindow) { time.start.value }.floor()
                                }
                                val slack = SlackFunction(
                                    if (timeWindow.continues) {
                                        UContinuous
                                    } else {
                                        UInteger
                                    },
                                    x = LinearPolynomial(est[task]),
                                    y = LinearPolynomial(y),
                                    withNegative = advanceEnabled && task.advanceEnabled,
                                    withPositive = delayEnabled && task.delayEnabled,
                                    name = "est_slack_$task"
                                )
                                slack.range.set(ValueRange(-y, with(timeWindow) { end.value } - y).value!!)
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
            }
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = map(
                "estimate_start_time",
                tasks,
                { t -> LinearMonomial(est[t]) },
                { (_, t) -> "$t" }
            )
            for (task in tasks) {
                estimateStartTime[task].range.leq(with(timeWindow) { end.value })
                when (val time = task.time) {
                    null -> {}

                    else -> {
                        if (!advanceEnabled || !task.advanceEnabled) {
                            estimateStartTime[task].range.geq(with(timeWindow) { time.start.value })
                        }
                        if (!delayEnabled || !task.delayEnabled) {
                            estimateStartTime[task].range.leq(with(timeWindow) { time.start.value })
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
        }

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = flatMap(
                "estimate_end_time",
                tasks,
                { t -> estimateEndTimeCalculator(t, LinearPolynomial(estimateStartTime[t])) },
                { (_, t) -> "$t" }
            )
            for (task in tasks) {
                estimateEndTime[task].range.leq(with(timeWindow) { end.value })
                when (val lastEndTime = task.lastEndTime) {
                    null -> {}

                    else -> {
                        if (!delayLastEndTimeEnabled || !task.delayEnabled) {
                            estimateEndTime[task].range.leq(with(timeWindow) { lastEndTime.value })
                        }
                    }
                }
                when (val earliestEndTime = task.earliestEndTime) {
                    null -> {}

                    else -> {
                        if (!advanceEarliestEndTimeEnabled || !task.advanceEnabled) {
                            estimateEndTime[task].range.geq(with(timeWindow) { earliestEndTime.value })
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
        }

        return super.register(model)
    }
}

open class IterativeTaskSchedulingTaskTime<
    IT: IterativeAbstractTask<E, A>,
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    override val compilation: IterativeTaskCompilation<IT, T, E, A>,
    private val redundancyRange: Duration? = null
) : TaskTimeImpl<T, E, A>(timeWindow, tasks) {
    override val delayEnabled: Boolean = true
    override val overMaxDelayEnabled: Boolean = true
    override val advanceEnabled: Boolean = true
    override val overMaxAdvanceEnabled: Boolean = true

    override val delayLastEndTimeEnabled: Boolean = true
    override val advanceEarliestEndTimeEnabled: Boolean = true

    private val withRedundancy get() = redundancyRange != null

    private lateinit var estRedundancy: Variable1<*>
    override lateinit var estSlack: LinearIntermediateSymbols1

    override lateinit var estimateStartTime: LinearExpressionSymbols1
    override lateinit var estimateEndTime: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (withRedundancy) {
            TODO("NOT IMPLEMENT YET")
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = flatMap(
                "estimate_start_time",
                tasks,
                { t ->
                    if (!::estRedundancy.isInitialized) {
                        LinearPolynomial(estRedundancy[t])
                    } else {
                        LinearPolynomial()
                    }
                },
                { (_, t) -> "$t" }
            )
        }
        when (val result = model.add(estimateStartTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = flatMap(
                "estimate_end_time",
                tasks,
                { t ->
                    if (!::estRedundancy.isInitialized) {
                        LinearPolynomial(estRedundancy[t])
                    } else {
                        LinearPolynomial()
                    }
                },
                { (_, t) -> "$t" }
            )
        }
        when (val result = model.add(estimateEndTime)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::estSlack.isInitialized) {
            estSlack = LinearIntermediateSymbols1(
                "est_slack",
                Shape1(tasks.size)
            ) { i, _ ->
                val task = tasks[i]
                if (!task.delayEnabled && !task.advanceEnabled) {
                    LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                } else {
                    when (val time = task.time) {
                        null -> {
                            LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                        }

                        else -> {
                            val y = if (timeWindow.continues) {
                                with(timeWindow) { time.start.value }
                            } else {
                                with(timeWindow) { time.start.value }.floor()
                            }
                            val slack = SlackFunction(
                                if (timeWindow.continues) {
                                    UContinuous
                                } else {
                                    UInteger
                                },
                                x = LinearPolynomial(estimateStartTime[task]),
                                y = LinearPolynomial(y),
                                withNegative = advanceEnabled && task.advanceEnabled,
                                withPositive = delayEnabled && task.delayEnabled,
                                name = "est_slack_$task"
                            )
                            slack.range.set(ValueRange(-y, with(timeWindow) { end.value } - y).value!!)
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
        }

        return super.register(model)
    }

    open fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel
    ): Try {
        assert(tasks.isNotEmpty())

        val xi = compilation.x.last()
        for (task in tasks) {
            val thisNewTasks = newTasks.filter { it.key == task.key }
            if (thisNewTasks.isNotEmpty()) {
                val est = estimateStartTime[task]
                val eet = estimateEndTime[task]

                est.flush()
                eet.flush()

                for (newTask in thisNewTasks) {
                    val time = newTask.time!!
                    est.asMutable() += with(timeWindow) { time.start.value } * xi[newTask]
                    eet.asMutable() += with(timeWindow) { time.end.value } * xi[newTask]
                }
            }
        }

        return ok
    }
}
