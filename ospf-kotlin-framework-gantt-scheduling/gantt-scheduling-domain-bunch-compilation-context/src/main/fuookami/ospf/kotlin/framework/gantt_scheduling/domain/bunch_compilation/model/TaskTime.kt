package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

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
    override lateinit var estSlack: LinearSymbols1

    override lateinit var estimateStartTime: LinearExpressionSymbols1
    override lateinit var estimateEndTime: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
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
            }
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
            estSlack = LinearSymbols1(
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
        bunches: List<B>,
        model: AbstractLinearMetaModel
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x.last()
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
                    est.asMutable() += with(timeWindow) { time.start.value } * xi[bunch]
                    eet.asMutable() += with(timeWindow) { time.end.value } * xi[bunch]
                }
            }
        }

        return ok
    }
}
