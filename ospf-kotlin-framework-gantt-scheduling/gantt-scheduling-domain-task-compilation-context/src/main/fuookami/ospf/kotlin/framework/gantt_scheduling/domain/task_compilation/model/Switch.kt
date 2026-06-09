@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 切换模型 / Switch model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.AndFunction
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.MaskingFunction
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray.Shape3
import fuookami.ospf.kotlin.multiarray._a
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/** 切换时间物理量 / Switch time quantity */
typealias SwitchTimeQuantity<V> = Quantity<V>

/** 切换接口 / Switch interface */
interface Switch {
    val switch: LinearIntermediateSymbols3<Flt64>
    val switchTime: LinearIntermediateSymbols2<Flt64>

    /**
     * 注册切换到模型 / Register switch to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    fun register(model: MetaModel<Flt64>): Try

    /**
     * 读取任务间切换时间物理量 / Read switch time between tasks as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param V 目标数值类型 / Target numeric type
     * @param from 前序任务 / Previous task
     * @param to 后序任务 / Next task
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 时间单位 / Time unit
     * @return 切换时间物理量 / Switch time quantity
     */
    fun <
            T : AbstractTask<E, A>,
            E : Executor,
            A : AssignmentPolicy<E>,
            V : RealNumber<V>
            > switchTimeQuantity(
        from: T,
        to: T,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<V>,
        unit: PhysicalUnit = NoneUnit
    ): SwitchTimeQuantity<V>? {
        val value = (switchTime[from, to] as IntermediateSymbol<Flt64>).evaluate(
            tokenTable = model.tokens,
            converter = schedulingSolverValueAdapter,
            zeroIfNone = true
        ) ?: switchTime[from, to].toLinearPolynomial().constant
        return Quantity(adapter.intoValue(value), unit)
    }
}

/**
 * 任务调度切换 / Task scheduling switch
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param timeWindow 时间窗口 / Time window
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param compilation 任务编译结果 / Task compilation result
 * @param taskTime 任务时间对象 / Task time object
 */
class TaskSchedulingSwitch<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val timeWindow: TimeWindow<Flt64>,
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    private val taskTime: TaskTime? = null
) : Switch {
    /**
     * 通过 solver 时间窗口边界创建任务调度切换 / Create task scheduling switch from a solver time-window boundary
     *
     * @param timeBoundary solver 时间窗口边界 / Solver time-window boundary
     * @param tasks 任务列表 / List of tasks
     * @param executors 执行器列表 / List of executors
     * @param compilation 任务编译结果 / Task compilation result
     * @param taskTime 任务时间对象 / Task time object
     */
    constructor(
        timeBoundary: SolverTimeWindowBoundary,
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        taskTime: TaskTime? = null
    ) : this(
        timeWindow = timeBoundary.source,
        tasks = tasks,
        executors = executors,
        compilation = compilation,
        taskTime = taskTime
    )

    private val timeBoundary = SolverTimeWindowBoundary(timeWindow)

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
                                converter = schedulingSolverValueAdapter,
                                lhsRange = taskTime.estimateStartTime[task1].range.range!!,
                                rhsConstant = Flt64.zero
                            ),
                            converter = schedulingSolverValueAdapter,
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
                            converter = schedulingSolverValueAdapter,
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
                        converter = schedulingSolverValueAdapter,
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
                            converter = schedulingSolverValueAdapter,
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
                        timeBoundary.distanceValue(
                            from = task1.time!!.end,
                            to = task2.time!!.start
                        )
                    )
                MaskingFunction.fromLinearPolynomials(
                    x = LinearExpressionSymbol(
                        polynomial = xPoly,
                        name = "switch_time_x_${task1}_$task2"
                    ),
                    mask = thisSwitch,
                    converter = schedulingSolverValueAdapter,
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
