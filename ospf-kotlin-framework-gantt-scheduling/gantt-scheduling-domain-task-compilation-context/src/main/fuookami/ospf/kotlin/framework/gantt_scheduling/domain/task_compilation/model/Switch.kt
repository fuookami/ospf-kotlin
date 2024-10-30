package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface Switch {
    val switch: LinearSymbols3
    val switchTime: LinearSymbols2

    fun register(model: MetaModel): Try
}

class TaskSchedulingSwitch<
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    private val timeWindow: TimeWindow,
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    private val taskTime: TaskTime? = null
) : Switch {
    private lateinit var frontOf: LinearSymbols2
    private lateinit var betweenIn: LinearSymbols3
    override lateinit var switch: LinearSymbols3
    override lateinit var switchTime: LinearSymbols2

    override fun register(model: MetaModel): Try {
        if (taskTime != null) {
            if (!::frontOf.isInitialized) {
                frontOf = LinearSymbols2(
                    "front_of",
                    Shape2(tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[0]]
                    val task2 = tasks[v[1]]
                    if (task1 == task2) {
                        LinearExpressionSymbol(LinearPolynomial(0), "front_of_${task1}_${task2}")
                    } else {
                        IfFunction(
                            taskTime.estimateStartTime[task1] leq taskTime.estimateStartTime[task2],
                            "front_of_${task1}_$task2"
                        )
                    }
                }
            }
            when (val result = model.add(frontOf)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (taskTime != null) {
            if (!::betweenIn.isInitialized) {
                betweenIn = LinearSymbols3(
                    "between_in",
                    Shape3(tasks.size, tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[1]]
                    val task2 = tasks[v[2]]
                    val task3 = tasks[v[3]]
                    if (task1 == task2 || task1 == task3 || task2 == task3) {
                        LinearExpressionSymbol(LinearPolynomial(0), "between_in_${task3}_${task1}_${task2}")
                    } else {
                        AndFunction(
                            listOf(
                                LinearPolynomial(frontOf[task1, task3]),
                                LinearPolynomial(frontOf[task3, task2])
                            ), "between_in_${task3}_${task1}_${task2}"
                        )
                    }
                }
            }
            when (val result = model.add(betweenIn)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::switch.isInitialized) {
            switch = LinearSymbols3(
                "switch",
                Shape3(executors.size, tasks.size, tasks.size)
            ) { _, v ->
                val executor = executors[v[0]]
                val task1 = tasks[v[1]]
                val task2 = tasks[v[2]]
                if (task1 == task2) {
                    LinearExpressionSymbol(LinearPolynomial(0), "front_of_${task1}_${task2}")
                } else if (taskTime != null) {
                    val conditions: MutableList<LinearPolynomial> = mutableListOf(
                        LinearPolynomial(compilation.taskAssignment[executor, task1]),
                        LinearPolynomial(compilation.taskAssignment[executor, task2]),
                        LinearPolynomial(frontOf[task1, task2])
                    )
                    for (task3 in tasks) {
                        if (task3 == task1 || task3 == task2) {
                            continue
                        }
                        conditions.add(Flt64.one - betweenIn[task3, task1, task2])
                    }
                    AndFunction(conditions, "switch_${executor}_${task1}_${task2}")
                } else {
                    if (task1.time!!.start < task2.time!!.start
                        && !tasks.any { task1.time!!.start < it.time!!.start && it.time!!.start < task2.time!!.start }
                    ) {
                        AndFunction(
                            listOf(
                                LinearPolynomial(compilation.taskAssignment[executor, task1]),
                                LinearPolynomial(compilation.taskAssignment[executor, task2])
                            ), "switch_${executor}_${task1}_${task2}"
                        )
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(0), "switch_${executor}_${task1}_${task2}")
                    }
                }
            }
        }

        if (!::switchTime.isInitialized) {
            switchTime = LinearSymbols2(
                "switch_time",
                Shape2(tasks.size, tasks.size)
            ) { _, v ->
                val task1 = tasks[v[0]]
                val task2 = tasks[v[1]]
                val thisSwitch = sum(switch[_a, task1, task2])
                thisSwitch.range.leq(Flt64.one)
                SemiFunction(
                    if (timeWindow.continues) {
                        UContinuous
                    } else {
                        UInteger
                    },
                    polynomial = taskTime?.let { it.estimateStartTime[task2] - it.estimateEndTime[task1] }
                        ?: LinearPolynomial(with(timeWindow) { (task2.time!!.start - task1.time!!.end).value }),
                    flag = thisSwitch,
                    name = "switch_time_${task1}_$task2"
                )
            }
            when (val result = model.add(switchTime)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}
