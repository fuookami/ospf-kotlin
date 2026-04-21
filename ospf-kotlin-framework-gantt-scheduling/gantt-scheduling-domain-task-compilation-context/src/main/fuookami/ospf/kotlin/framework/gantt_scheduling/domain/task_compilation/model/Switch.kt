@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.core.intermediate_model.ToMathLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintInput
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.MetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols2
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols3
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.AndFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray.Shape3
import fuookami.ospf.kotlin.multiarray._a

interface Switch {
    val switch: LinearIntermediateSymbols3
    val switchTime: LinearIntermediateSymbols2

    fun register(model: MetaModel<Flt64>): Try
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
    private lateinit var frontOf: LinearIntermediateSymbols2
    private lateinit var betweenIn: LinearIntermediateSymbols3
    override lateinit var switch: LinearIntermediateSymbols3
    override lateinit var switchTime: LinearIntermediateSymbols2

    override fun register(model: MetaModel<Flt64>): Try {
        if (taskTime != null) {
            if (!::frontOf.isInitialized) {
                frontOf = LinearIntermediateSymbols2(
                    name = "front_of",
                    shape = Shape2(tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[0]]
                    val task2 = tasks[v[1]]
                    val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2) {
                        LinearIntermediateSymbol.empty(
                            name = "front_of_${task1}_${task2}"
                        )
                    } else {
                        IfFunction(
                            input = LinearConstraintInput.from(
                                relation = taskTime.estimateStartTime[task1] leq taskTime.estimateStartTime[task2],
                                lhsRange = taskTime.estimateStartTime[task1].range.range!!
                            ),
                            name = "front_of_${task1}_$task2"
                        )
                    }
                    result
                }
            }
            when (val result = model.add(frontOf)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (taskTime != null) {
            if (!::betweenIn.isInitialized) {
                betweenIn = LinearIntermediateSymbols3(
                    name = "between_in",
                    shape = Shape3(tasks.size, tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[1]]
                    val task2 = tasks[v[2]]
                    val task3 = tasks[v[3]]
                    val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2 || task1 == task3 || task2 == task3) {
                        LinearIntermediateSymbol.empty(
                            name = "between_in_${task3}_${task1}_${task2}"
                        )
                    } else {
                        AndFunction(
                            polynomials = listOf(
                                frontOf[task1, task3],
                                frontOf[task3, task2]
                            ),
                            name = "between_in_${task3}_${task1}_${task2}"
                        )
                    }
                    result
                }
            }
            when (val result = model.add(betweenIn)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::switch.isInitialized) {
            switch = LinearIntermediateSymbols3(
                name = "switch",
                shape = Shape3(executors.size, tasks.size, tasks.size)
            ) { _, v ->
                val executor = executors[v[0]]
                val task1 = tasks[v[1]]
                val task2 = tasks[v[2]]
                val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2) {
                    LinearIntermediateSymbol.empty(
                        name = "front_of_${task1}_${task2}"
                    )
                } else if (taskTime != null) {
                    val conditions: MutableList<ToMathLinearPolynomial> = mutableListOf(
                        compilation.taskAssignment[executor, task1],
                        compilation.taskAssignment[executor, task2],
                        frontOf[task1, task2]
                    )
                    for (task3 in tasks) {
                        if (task3 == task1 || task3 == task2) {
                            continue
                        }
                        conditions.add(LinearExpressionSymbol(
                            polynomial = LinearPolynomial(emptyList(), Flt64.one) - betweenIn[task3, task1, task2].toMathLinearPolynomial(),
                            name = "not_between_${task3}_${task1}_${task2}"
                        ))
                    }
                    AndFunction(
                        polynomials = conditions,
                        name = "switch_${executor}_${task1}_${task2}"
                    )
                } else {
                    if (task1.time!!.start < task2.time!!.start
                        && !tasks.any { task1.time!!.start < it.time!!.start && it.time!!.start < task2.time!!.start }
                    ) {
                        AndFunction(
                            polynomials = listOf(
                                compilation.taskAssignment[executor, task1],
                                compilation.taskAssignment[executor, task2]
                            ),
                            name = "switch_${executor}_${task1}_${task2}"
                        )
                    } else {
                        LinearIntermediateSymbol.empty(
                            name = "switch_${executor}_${task1}_${task2}"
                        )
                    }
                }
                result
            }
        }

        if (!::switchTime.isInitialized) {
            switchTime = LinearIntermediateSymbols2(
                name = "switch_time",
                shape = Shape2(tasks.size, tasks.size)
            ) { _, v ->
                val task1 = tasks[v[0]]
                val task2 = tasks[v[1]]
                val thisSwitchPoly = sum(switch[_a, task1, task2].map { it.toMathLinearPolynomial() })
                val thisSwitch = LinearExpressionSymbol(
                    polynomial = thisSwitchPoly,
                    name = "this_switch_${task1}_$task2"
                )
                thisSwitch.range.leq(Flt64.one)
                val xPoly = taskTime?.let { it.estimateStartTime[task2] - it.estimateEndTime[task1] }
                    ?: LinearPolynomial(
                        emptyList(),
                        with(timeWindow) { (task2.time!!.start - task1.time!!.end).value }
                    )
                MaskingFunction(
                    x = LinearExpressionSymbol(
                        polynomial = xPoly,
                        name = "switch_time_x_${task1}_$task2"
                    ),
                    mask = thisSwitch,
                    name = "switch_time_${task1}_$task2"
                )
            }
            when (val result = model.add(switchTime)) {
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
