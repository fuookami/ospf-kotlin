package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface TaskTime {
    val delayEnabled: Boolean
    val advanceEnabled: Boolean

    val delayLastEndTimeEnabled: Boolean
    val advanceEarliestEndTimeEnabled: Boolean

    val estimateStartTime: LinearSymbols1
    val estimateEndTime: LinearSymbols1

    val delayTime: LinearSymbols1
    val advanceTime: LinearSymbols1
    val delayLastEndTime: LinearSymbols1
    val advanceEarliestEndTime: LinearSymbols1

    fun register(model: LinearMetaModel): Try
}

class TaskSchedulingTaskTime<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val timeWindow: TimeWindow,
    private val tasks: List<T>,
    private val compilation: Compilation,
    private val estimateEndTimeCalculator: (T, LinearPolynomial) -> LinearPolynomial,
    override val delayEnabled: Boolean = false,
    override val advanceEnabled: Boolean = false,
    override val delayLastEndTimeEnabled: Boolean = false,
    override val advanceEarliestEndTimeEnabled: Boolean = false
) : TaskTime {
    lateinit var est: Variable1<*>
    override lateinit var estimateStartTime: LinearSymbols1
    override lateinit var estimateEndTime: LinearSymbols1

    private lateinit var estSlack: LinearSymbols1
    override lateinit var delayTime: LinearSymbols1
    override lateinit var advanceTime: LinearSymbols1
    override lateinit var delayLastEndTime: LinearSymbols1
    override lateinit var advanceEarliestEndTime: LinearSymbols1

    @Suppress("UNCHECKED_CAST")
    override fun register(model: LinearMetaModel): Try {
        if (!::est.isInitialized) {
            if (timeWindow.continues) {
                est = URealVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    val variable = est[task] as URealVariable
                    variable.name = "${est.name}_${task}"

                    when (val time = task.plan.time) {
                        null -> {
                            variable.range.leq(timeWindow.valueOf(timeWindow.duration))
                        }

                        else -> {
                            if (!task.advanceEnabled) {
                                variable.range.geq(timeWindow.valueOf(time.start))
                            }
                            if (!task.delayEnabled) {
                                variable.range.leq(timeWindow.valueOf(time.start))
                            }
                        }
                    }
                }
            } else {
                est = UIntVariable1("est", Shape1(tasks.size))
                for (task in tasks) {
                    val variable = est[task] as UIntVariable
                    variable.name = "${est.name}_${task}"

                    when (val time = task.plan.time) {
                        null -> {
                            variable.range.leq(timeWindow.valueOf(timeWindow.duration).floor().toUInt64())
                        }

                        else -> {
                            if (!task.advanceEnabled) {
                                variable.range.geq(timeWindow.valueOf(time.start).floor().toUInt64())
                            }
                            if (!task.delayEnabled) {
                                variable.range.leq(timeWindow.valueOf(time.start).floor().toUInt64())
                            }
                        }
                    }
                }
            }
        }
        model.addVars(est)

        if (delayEnabled || advanceEnabled) {
            if (!::estSlack.isInitialized) {
                estSlack = LinearSymbols1(
                    "est_slack",
                    Shape1(tasks.size)
                ) { (i, _) ->
                    val task = tasks[i]
                    if (!task.delayEnabled && !task.advanceEnabled) {
                        LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                    } else {
                        when (val scheduledTime = task.scheduledTime) {
                            null -> {
                                LinearExpressionSymbol(LinearPolynomial(), "est_slack_$task")
                            }

                            else -> {
                                val y = if (timeWindow.continues) {
                                    timeWindow.valueOf(scheduledTime.start)
                                } else {
                                    timeWindow.valueOf(scheduledTime.start).floor()
                                }
                                SlackFunction(
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
                            }
                        }
                    }
                }
            }
            model.addSymbols(estSlack)
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = flatMap(
                "estimate_start_time",
                tasks,
                { t ->
                    if (delayEnabled || advanceEnabled) {
                        est[t] + estSlack[t]
                    } else {
                        LinearPolynomial(est[t])
                    }
                },
                { (_, t) -> "$t" }
            )
        }
        model.addSymbols(estimateStartTime)

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = flatMap(
                "estimate_end_time",
                tasks,
                { t -> estimateEndTimeCalculator(t, LinearPolynomial(estimateStartTime[t])) },
                { (_, t) -> "$t" }
            )
        }
        model.addSymbols(estimateEndTime)

        if (delayEnabled) {
            if (!::delayTime.isInitialized) {
                delayTime = LinearSymbols1(
                    "delay_time",
                    Shape1(tasks.size)
                ) { (i, _) ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is AbstractSlackFunction<*> -> {
                            LinearPolynomial(slack.pos)
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
                            polynomial = polynomial,
                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                            name = "delay_time_$task"
                        )
                    } else {
                        LinearExpressionSymbol(polynomial, "delay_time_$task")
                    }
                }
            }
            model.addSymbols(delayTime)
        }

        if (advanceEnabled) {
            if (!::advanceTime.isInitialized) {
                advanceTime = LinearSymbols1(
                    "advance_time",
                    Shape1(tasks.size),
                ) { (i, _) ->
                    val task = tasks[i]
                    val polynomial = when (val slack = estSlack[task]) {
                        is AbstractSlackFunction<*> -> {
                            LinearPolynomial(slack.neg)
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
                            polynomial = polynomial,
                            flag = LinearPolynomial(compilation.taskCompilation[task]),
                            name = "advance_time_$task"
                        )
                    } else {
                        LinearExpressionSymbol(polynomial, "delay_time_$task")
                    }
                }
            }
            model.addSymbols(advanceTime)
        }

        if (delayLastEndTimeEnabled) {
            if (!::delayLastEndTime.isInitialized) {
                delayLastEndTime = LinearSymbols1(
                    "delay_last_end_time",
                    Shape1(tasks.size)
                ) { (i, _) ->
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
                                    y = LinearPolynomial(timeWindow.valueOf(lastEndTime)),
                                    withNegative = false,
                                    name = "delay_last_end_time_$task"
                                )
                                if (compilation.taskCancelEnabled) {
                                    slack.name = "delay_last_end_time_slack_$task"
                                    model.addSymbol(slack)
                                    SemiFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        polynomial = LinearPolynomial(slack),
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
            }
            model.addSymbols(delayLastEndTime)
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::advanceEarliestEndTime.isInitialized) {
                advanceEarliestEndTime = LinearSymbols1(
                    "advance_earliest_end_time",
                    Shape1(tasks.size)
                ) { (i, _) ->
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
                                    y = LinearPolynomial(timeWindow.valueOf(earliestEndTime)),
                                    withPositive = false,
                                    name = "advance_earliest_end_time_$task"
                                )
                                if (compilation.taskCancelEnabled) {
                                    slack.name = "advance_earliest_end_time_slack_$task"
                                    model.addSymbol(slack)
                                    SemiFunction(
                                        if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        polynomial = LinearPolynomial(slack),
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
            }
            model.addSymbols(advanceEarliestEndTime)
        }

        return Ok(success)
    }
}
