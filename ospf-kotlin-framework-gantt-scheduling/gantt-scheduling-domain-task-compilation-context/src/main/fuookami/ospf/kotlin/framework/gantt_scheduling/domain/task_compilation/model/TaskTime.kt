@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.IterativeAbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration

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

    fun register(model: MetaModel<Flt64>): Try
}

abstract class TaskTimeImpl<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    protected val timeWindow: TimeWindow,
    protected val tasks: List<T>
) : TaskTime {
    protected abstract val compilation: Compilation
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

    override fun register(model: MetaModel<Flt64>): Try {
        if (delayEnabled) {
            if (!::delayTime.isInitialized) {
                delayTime = LinearIntermediateSymbols1(
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
                    delayTime[task].range.leq(with(timeWindow) { duration.value })
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                delayTime[task].range.leq(Flt64.zero)
                            } else {
                                delayTime[task].range.leq(
                                    with(timeWindow) { (timeWindow.end - time.start).value }
                                )
                            }
                        }
                    }
                    task.maxDelay?.let {
                        delayTime[task].range.leq(
                            with(timeWindow) { it.value }
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
                advanceTime = LinearIntermediateSymbols1(
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
                        with(timeWindow) { duration.value }
                    )
                    when (val time = task.time) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceTime[task].range.leq(
                                    with(timeWindow) { (time.start - timeWindow.start).value }
                                )
                            }
                        }
                    }
                    task.maxAdvance?.let {
                        advanceTime[task].range.leq(
                            with(timeWindow) { it.value }
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
                try {
                    overMaxDelayTime = LinearIntermediateSymbols1(
                        name = "over_max_delay_time",
                        shape = Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.delayEnabled) {
                            LinearIntermediateSymbol.empty(
                                name = "over_max_delay_time_${task}"
                            )
                        } else {
                            when (val maxDelayTime = task.maxDelay) {
                                null -> {
                                    LinearExpressionSymbol(
                                        name = "over_max_delay_time_${task}"
                                    )
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        x = delayTime[task],
                                        y = with(timeWindow) { maxDelayTime.value },
                                        type = if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        withNegative = false,
                                        name = "over_max_delay_time_${task}"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "over_max_delay_time_slack_${task}"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw IllegalStateException(result.error.message)
                                            }

                                            is Fatal -> {
                                                throw IllegalStateException(result.errors.joinToString(", ") { it.message })
                                            }
                                        }
                                        MaskingFunction.fromLinearPolynomials(
                                            x = slack,
                                            mask = compilation.taskCompilation[task],
                                            name = "over_max_delay_time_${task}"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    return Failed(Err(ErrorCode.ApplicationError, e.message ?: "Unknown error"))
                }
                for (task in tasks) {
                    overMaxDelayTime[task].range.leq(
                        with(timeWindow) { duration.value }
                    )
                    when (val maxDelayTime = task.maxDelay) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxDelayTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxDelayTime[task].range.leq(
                                    with(timeWindow) { maxDelayTime.value }
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
                try {
                    overMaxAdvanceTime = LinearIntermediateSymbols1(
                        name = "over_max_advance_time",
                        shape = Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.advanceEnabled) {
                            LinearIntermediateSymbol.empty(
                                name = "over_max_advance_time_${task}"
                            )
                        } else {
                            when (val maxAdvanceTime = task.maxAdvance) {
                                null -> {
                                    LinearIntermediateSymbol.empty(
                                        name = "over_max_advance_time_${task}"
                                    )
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        x = advanceTime[task],
                                        y = with(timeWindow) { maxAdvanceTime.value },
                                        type = if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        withNegative = false,
                                        name = "over_max_advance_time_${task}"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "over_max_delay_time_slack_${task}"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw IllegalStateException(result.error.message)
                                            }

                                            is Fatal -> {
                                                throw IllegalStateException(result.errors.joinToString(", ") { it.message })
                                            }
                                        }
                                        MaskingFunction.fromLinearPolynomials(
                                            x = slack,
                                            mask = compilation.taskCompilation[task],
                                            name = "over_max_advance_time_${task}"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    return Failed(Err(ErrorCode.ApplicationError, e.message ?: "Unknown error"))
                }
                for (task in tasks) {
                    overMaxAdvanceTime[task].range.leq(
                        with(timeWindow) { duration.value }
                    )
                    when (val maxAdvanceTime = task.maxAdvance) {
                        null -> {}

                        else -> {
                            if (!task.delayEnabled) {
                                overMaxAdvanceTime[task].range.leq(Flt64.zero)
                            } else {
                                overMaxAdvanceTime[task].range.leq(
                                    with(timeWindow) { maxAdvanceTime.value }
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
                try {
                    delayLastEndTime = LinearIntermediateSymbols1(
                        name = "delay_last_end_time",
                        shape = Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.delayEnabled) {
                            LinearIntermediateSymbol.empty(
                                name = "delay_last_end_time_${task}"
                            )
                        } else {
                            when (val lastEndTime = task.lastEndTime) {
                                null -> {
                                    LinearIntermediateSymbol.empty(
                                        name = "delay_last_end_time_${task}"
                                    )
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        x = estimateEndTime[task],
                                        y = with(timeWindow) { lastEndTime.value },
                                        type = if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        withNegative = false,
                                        name = "delay_last_end_time_${task}"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "delay_last_end_time_slack_${task}"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw IllegalStateException(result.error.message)
                                            }

                                            is Fatal -> {
                                                throw IllegalStateException(result.errors.joinToString(", ") { it.message })
                                            }
                                        }
                                        MaskingFunction.fromLinearPolynomials(
                                            x = slack,
                                            mask = compilation.taskCompilation[task],
                                            name = "delay_last_end_time_${task}"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    return Failed(Err(ErrorCode.ApplicationError, e.message ?: "Unknown error"))
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::advanceEarliestEndTime.isInitialized) {
                try {
                    advanceEarliestEndTime = LinearIntermediateSymbols1(
                        name = "advance_earliest_end_time",
                        shape = Shape1(tasks.size)
                    ) { i, _ ->
                        val task = tasks[i]
                        if (!task.advanceEnabled) {
                            LinearIntermediateSymbol.empty(
                                name = "advance_earliest_end_time_${task}"
                            )
                        } else {
                            when (val earliestEndTime = task.earliestEndTime) {
                                null -> {
                                    LinearIntermediateSymbol.empty(
                                        name = "advance_earliest_end_time_${task}"
                                    )
                                }

                                else -> {
                                    val slack = SlackFunction(
                                        x = estimateEndTime[task],
                                        y = with(timeWindow) { earliestEndTime.value },
                                        type = if (timeWindow.continues) {
                                            UContinuous
                                        } else {
                                            UInteger
                                        },
                                        withPositive = false,
                                        name = "advance_earliest_end_time_${task}"
                                    )
                                    if (compilation.taskCancelEnabled) {
                                        slack.name = "advance_earliest_end_time_slack_${task}"
                                        when (val result = model.add(slack)) {
                                            is Ok -> {}

                                            is Failed -> {
                                                throw IllegalStateException(result.error.message)
                                            }

                                            is Fatal -> {
                                                throw IllegalStateException(result.errors.joinToString(", ") { it.message })
                                            }
                                        }
                                        MaskingFunction.fromLinearPolynomials(
                                            x = slack,
                                            mask = compilation.taskCompilation[task],
                                            name = "advance_earliest_end_time_${task}"
                                        )
                                    } else {
                                        slack
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    return Failed(Err(ErrorCode.ApplicationError, e.message ?: "Unknown error"))
                }
                for (task in tasks) {
                    advanceEarliestEndTime[task].range.leq(
                        with(timeWindow) { duration.value }
                    )
                    when (val earliestStartTime = task.earliestStartTime) {
                        null -> {}

                        else -> {
                            if (!task.advanceEnabled) {
                                advanceEarliestEndTime[task].range.leq(Flt64.zero)
                            } else {
                                advanceEarliestEndTime[task].range.leq(
                                    with(timeWindow) { (earliestStartTime - timeWindow.start).value }
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
                onLastEndTime = LinearIntermediateSymbols1(
                    name = "on_last_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            name = "on_last_end_time_${task}"
                        )
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    name = "on_last_end_time_${task}"
                                )
                            }

                            else -> {
                                IfFunction(
                                    inequality = LinearConstraintInput.from(
                                        relation = estimateEndTime[task] leq with(timeWindow) { lastEndTime.value },
                                        lhsRange = estimateEndTime[task].range.range!!
                                    ),
                                    name = "on_last_end_time_${task}"
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::onEarliestEndTime.isInitialized) {
                onEarliestEndTime = LinearIntermediateSymbols1(
                    name = "on_earliest_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            name = "on_earliest_end_time_${task}"
                        )
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    name = "on_earliest_end_time_${task}"
                                )
                            }

                            else -> {
                                IfFunction(
                                    inequality = LinearConstraintInput.from(
                                        relation = estimateEndTime[task] geq with(timeWindow) { earliestEndTime.value },
                                        lhsRange = estimateEndTime[task].range.range!!
                                    ),
                                    name = "on_earliest_end_time_${task}"
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::onTime.isInitialized) {
                onTime = LinearIntermediateSymbols1(
                    name = "on_time",
                    shape = Shape1(tasks.size)
                ) { t, _ ->
                    val task = tasks[t]
                    LinearExpressionSymbol(
                        polynomial = if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            onLastEndTime[t].toMathLinearPolynomial() + onEarliestEndTime[t].toMathLinearPolynomial()
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
                notOnLastEndTime = LinearIntermediateSymbols1(
                    name = "not_on_last_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled) {
                        LinearIntermediateSymbol.empty(
                            name = "not_on_last_end_time_${task}"
                        )
                    } else {
                        when (val lastEndTime = task.lastEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    name = "not_on_last_end_time_${task}"
                                )
                            }

                            else -> {
                                IfFunction(
                                    inequality = LinearConstraintInput.from(
                                        relation = estimateEndTime[task] geq with(timeWindow) { (lastEndTime + timeWindow.duration).value },
                                        lhsRange = estimateEndTime[task].range.range!!
                                    ),
                                    name = "not_on_last_end_time_${task}"
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (advanceEarliestEndTimeEnabled) {
            if (!::notOnEarliestEndTime.isInitialized) {
                notOnEarliestEndTime = LinearIntermediateSymbols1(
                    name = "on_earliest_end_time",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            name = "not_on_earliest_end_time_${task}"
                        )
                    } else {
                        when (val earliestEndTime = task.earliestEndTime) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    name = "not_on_earliest_end_time_${task}"
                                )
                            }

                            else -> {
                                IfFunction(
                                    inequality = LinearConstraintInput.from(
                                        relation = estimateEndTime[task] leq with(timeWindow) { (earliestEndTime - timeWindow.duration).value },
                                        lhsRange = estimateEndTime[task].range.range!!
                                    ),
                                    name = "not_on_earliest_end_time_${task}"
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayLastEndTimeEnabled || advanceEarliestEndTimeEnabled) {
            if (!::notOnTime.isInitialized) {
                notOnTime = LinearIntermediateSymbols1(
                    name = "not_on_time",
                    shape = Shape1(tasks.size)
                ) { t, _ ->
                    val task = tasks[t]
                    LinearExpressionSymbol(
                        polynomial = if (delayLastEndTimeEnabled && advanceEarliestEndTimeEnabled) {
                            notOnLastEndTime[t].toMathLinearPolynomial() + notOnEarliestEndTime[t].toMathLinearPolynomial()
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

class TaskSchedulingTaskTime<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    timeWindow: TimeWindow,
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
    lateinit var est: Variable1<*>
    override lateinit var estSlack: LinearIntermediateSymbols1

    override lateinit var estimateStartTime: LinearIntermediateSymbols1
    override lateinit var estimateEndTime: LinearIntermediateSymbols1

    override fun register(model: MetaModel<Flt64>): Try {
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (delayEnabled || advanceEnabled) {
            if (!::estSlack.isInitialized) {
                estSlack = LinearIntermediateSymbols1(
                    name = "est_slack",
                    shape = Shape1(tasks.size)
                ) { i, _ ->
                    val task = tasks[i]
                    if (!task.delayEnabled && !task.advanceEnabled) {
                        LinearIntermediateSymbol.empty(
                            name = "est_slack_${task}"
                        )
                    } else {
                        when (val time = task.time) {
                            null -> {
                                LinearIntermediateSymbol.empty(
                                    name = "est_slack_${task}"
                                )
                            }

                            else -> {
                                val y = if (timeWindow.continues) {
                                    with(timeWindow) { time.start.value }
                                } else {
                                    with(timeWindow) { time.start.value }.floor()
                                }
                                val slack = SlackFunction(
                                    x = est[task],
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
                                slack.range.set(
                                    ValueRange(
                                        -y,
                                        with(timeWindow) { end.value } - y
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
            estimateStartTime = LinearIntermediateSymbols1(
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::estimateEndTime.isInitialized) {
            estimateEndTime = LinearIntermediateSymbols1(
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }
}

open class IterativeTaskSchedulingTaskTime<
        IT : IterativeAbstractTask<E, A>,
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

    override lateinit var estimateStartTime: LinearIntermediateSymbols1
    override lateinit var estimateEndTime: LinearIntermediateSymbols1

    override fun register(model: MetaModel<Flt64>): Try {
        if (withRedundancy) {
            TODO("NOT IMPLEMENT YET")
        }

        if (!::estimateStartTime.isInitialized) {
            estimateStartTime = LinearIntermediateSymbols1(
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
            estimateEndTime = LinearIntermediateSymbols1(
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
            estSlack = LinearIntermediateSymbols1(
                name = "est_slack",
                shape = Shape1(tasks.size)
            ) { i, _ ->
                val task = tasks[i]
                if (!task.delayEnabled && !task.advanceEnabled) {
                    LinearIntermediateSymbol.empty(
                        name = "est_slack_${task}"
                    )
                } else {
                    when (val time = task.time) {
                        null -> {
                            LinearIntermediateSymbol.empty(
                                name = "est_slack_${task}"
                            )
                        }

                        else -> {
                            val y = if (timeWindow.continues) {
                                with(timeWindow) { time.start.value }
                            } else {
                                with(timeWindow) { time.start.value }.floor()
                            }
                            val slack = LinearFunctionSymbolAdapter(SlackFunction(
                                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, estimateStartTime[task])), Flt64.zero),
                                y = LinearPolynomial(emptyList(), y),
                                type = if (timeWindow.continues) {
                                    UContinuous
                                } else {
                                    UInteger
                                },
                                withNegative = advanceEnabled && task.advanceEnabled,
                                withPositive = delayEnabled && task.delayEnabled,
                                name = "est_slack_${task}"
                            ))
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
                    est.asMutable() += LinearMonomial(with(timeWindow) { time.start.value }, xi[newTask])
                    eet.asMutable() += LinearMonomial(with(timeWindow) { time.end.value }, xi[newTask])
                }
            }
        }

        return ok
    }
}




