@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.AndFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray.Shape3
import fuookami.ospf.kotlin.multiarray._a

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

interface Switch {
    val switch: LinearIntermediateSymbols3<Flt64>
    val switchTime: LinearIntermediateSymbols2<Flt64>

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
    private lateinit var frontOf: LinearIntermediateSymbols2<Flt64>
    private lateinit var betweenIn: LinearIntermediateSymbols3<Flt64>
    override lateinit var switch: LinearIntermediateSymbols3<Flt64>
    override lateinit var switchTime: LinearIntermediateSymbols2<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (taskTime != null) {
            if (!::frontOf.isInitialized) {
                frontOf = LinearIntermediateSymbols2<Flt64>(
                    name = "front_of",
                    shape = Shape2(tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[0]]
                    val task2 = tasks[v[1]]
                    val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "front_of_${task1}_${task2}"
                        )
                    } else {
                        IfFunction.from(
                            inequality = LinearConstraintInput.from(
                                relation = taskTime.estimateStartTime[task1] leq taskTime.estimateStartTime[task2],
                                converter = flt64Converter,
                                lhsRange = taskTime.estimateStartTime[task1].range.range!!,
                                rhsConstant = Flt64.zero
                            ),
                            converter = flt64Converter,
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
                betweenIn = LinearIntermediateSymbols3<Flt64>(
                    name = "between_in",
                    shape = Shape3(tasks.size, tasks.size, tasks.size)
                ) { _, v ->
                    val task1 = tasks[v[1]]
                    val task2 = tasks[v[2]]
                    val task3 = tasks[v[3]]
                    val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2 || task1 == task3 || task2 == task3) {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "between_in_${task3}_${task1}_${task2}"
                        )
                    } else {
                        AndFunction.fromLinearPolynomials(
                            polynomials = listOf(
                                frontOf[task1, task3],
                                frontOf[task3, task2]
                            ),
                            converter = flt64Converter,
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
            switch = LinearIntermediateSymbols3<Flt64>(
                name = "switch",
                shape = Shape3(executors.size, tasks.size, tasks.size)
            ) { _, v ->
                val executor = executors[v[0]]
                val task1 = tasks[v[1]]
                val task2 = tasks[v[2]]
                val result: LinearIntermediateSymbol<Flt64> = if (task1 == task2) {
                    LinearIntermediateSymbol.empty(
                        Flt64,
                        name = "front_of_${task1}_${task2}"
                    )
                } else if (taskTime != null) {
                    val conditions: MutableList<LinearIntermediateSymbol<Flt64>> = mutableListOf(
                        compilation.taskAssignment[executor, task1],
                        compilation.taskAssignment[executor, task2],
                        frontOf[task1, task2]
                    )
                    for (task3 in tasks) {
                        if (task3 == task1 || task3 == task2) {
                            continue
                        }
                        conditions.add(LinearExpressionSymbol(
                            polynomial = LinearPolynomial(emptyList(), Flt64.one) - betweenIn[task3, task1, task2].toLinearPolynomial(),
                            name = "not_between_${task3}_${task1}_${task2}"
                        ))
                    }
                    AndFunction.fromLinearPolynomials(
                        polynomials = conditions,
                        converter = flt64Converter,
                        name = "switch_${executor}_${task1}_${task2}"
                    )
                } else {
                    if (task1.time!!.start < task2.time!!.start
                        && !tasks.any { task1.time!!.start < it.time!!.start && it.time!!.start < task2.time!!.start }
                    ) {
                        AndFunction.fromLinearPolynomials(
                            polynomials = listOf(
                                compilation.taskAssignment[executor, task1],
                                compilation.taskAssignment[executor, task2]
                            ),
                            converter = flt64Converter,
                            name = "switch_${executor}_${task1}_${task2}"
                        )
                    } else {
                        LinearIntermediateSymbol.empty(
                            Flt64,
                            name = "switch_${executor}_${task1}_${task2}"
                        )
                    }
                }
                result
            }
        }

        if (!::switchTime.isInitialized) {
            switchTime = LinearIntermediateSymbols2<Flt64>(
                name = "switch_time",
                shape = Shape2(tasks.size, tasks.size)
            ) { _, v ->
                val task1 = tasks[v[0]]
                val task2 = tasks[v[1]]
                val thisSwitchPoly = sum(switch[_a, task1, task2].map { it.toLinearPolynomial() })
                val thisSwitch = LinearExpressionSymbol(
                    polynomial = thisSwitchPoly,
                    name = "this_switch_${task1}_$task2"
                )
                thisSwitch.range.leq(Flt64.one)
                val xPoly = taskTime?.let { it.estimateStartTime[task2].toLinearPolynomial() - it.estimateEndTime[task1].toLinearPolynomial() }
                    ?: LinearPolynomial(
                        emptyList(),
                        with(timeWindow) { (task2.time!!.start - task1.time!!.end).value }
                    )
                MaskingFunction.fromLinearPolynomials(
                    x = LinearExpressionSymbol(
                        polynomial = xPoly,
                        name = "switch_time_x_${task1}_$task2"
                    ),
                    mask = thisSwitch,
                    converter = flt64Converter,
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
