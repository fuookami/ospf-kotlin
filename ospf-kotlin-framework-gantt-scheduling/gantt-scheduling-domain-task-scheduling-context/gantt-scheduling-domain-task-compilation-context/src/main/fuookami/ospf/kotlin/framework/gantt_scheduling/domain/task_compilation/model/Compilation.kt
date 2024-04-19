package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface Compilation {
    val taskCancelEnabled: Boolean
    val withExecutorLeisure: Boolean

    val y: BinVariable1
    val z: BinVariable1

    val taskAssignment: LinearSymbols2
    val taskCompilation: LinearSymbols1
    val executorCompilation: LinearSymbols1

    fun register(model: LinearMetaModel): Try
}

class TaskCompilation<
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val taskCancelEnabled: Boolean = false,
    override val withExecutorLeisure: Boolean = false
) : Compilation {
    lateinit var x: BinVariable2
    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    override lateinit var taskAssignment: LinearSymbols2
    override lateinit var taskCompilation: LinearSymbols1
    override lateinit var executorCompilation: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (!::x.isInitialized) {
            x = BinVariable2("x", Shape2(tasks.size, executors.size))
            for (task in tasks) {
                for (executor in executors) {
                    x[task, executor].name = "${x.name}_${task}_${executor}"

                    if (!task.enabledExecutors.contains(executor)) {
                        x[task, executor].range.eq(UInt8.zero)
                    }
                }

                if (task.executor != null && !task.executorChangeEnabled) {
                    for (executor in executors) {
                        if (task.executor == executor) {
                            if (!task.cancelEnabled) {
                                x[task, executor].range.eq(UInt8.one)
                            }
                        } else {
                            x[task, executor].range.eq(UInt8.zero)
                        }
                    }
                }
            }
        }
        model.addVars(x)

        if (!::taskAssignment.isInitialized) {
            taskAssignment = map(
                "task_assignment",
                tasks,
                executors,
                { t, e -> LinearMonomial(x[t, e]) },
                { (_, t), (_, e) -> "${t}_$e" }
            )
            for (task in tasks) {
                for (executor in executors) {
                    taskAssignment[task, executor].range.set(ValueRange(Flt64.zero, Flt64.one))
                }
            }
        }
        model.addSymbols(taskAssignment)

        if (taskCancelEnabled) {
            if (!::y.isInitialized) {
                y = BinVariable1("y", Shape1(tasks.size))
                for (task in tasks) {
                    y[task].name = ""
                    if (!task.cancelEnabled) {
                        y[task].range.eq(UInt8.zero)
                    }
                    if (lockCancelTasks.contains(task)) {
                        y[task].range.eq(UInt8.one)
                    }
                }
            }
            model.addVars(y)
        }

        if (!::taskCompilation.isInitialized) {
            taskCompilation = flatMap(
                "task_compilation",
                tasks,
                { t ->
                    if (taskCancelEnabled) {
                        y[t] + sum(x[t, _a])
                    } else {
                        sum(x[t, _a])
                    }
                },
                { (_, t) -> "$t" }
            )
            for (task in tasks) {
                taskCompilation[task].range.set(ValueRange(Flt64.one, Flt64.one))
            }
        }
        model.addSymbols(taskCompilation)

        if (withExecutorLeisure) {
            if (!::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
                for (executor in executors) {
                    z[executor].name = "${z.name}_${executor}"
                }
            }
            model.addVars(z)
        }

        if (!::executorCompilation.isInitialized) {
            executorCompilation = LinearSymbols1(
                "executor_compilation",
                Shape1(executors.size)
            ) { (i, _) ->
                if (withExecutorLeisure) {
                    val or = OrFunction(
                        tasks.map { LinearPolynomial(x[it, executors[i]]) },
                        "executor_compilation_or_${executors[i]}"
                    )
                    model.addSymbol(or)
                    LinearExpressionSymbol(or + z[executors[i]], "executor_compilation_${executors[i]}")
                } else {
                    OrFunction(
                        tasks.map { LinearPolynomial(x[it, executors[i]]) },
                        "executor_compilation_${executors[i]}"
                    )
                }
            }
        }
        model.addSymbols(executorCompilation)

        return ok
    }
}

open class IterativeTaskCompilation<
    IT: IterativeAbstractTask<E, A>,
    out T: AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    private val originTasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
) : Compilation {
    init {
        if (!executors.all { it.indexed }) {
            ManualIndexed.flush(Executor::class)
            for (executor in executors) {
                executor.setIndexed(Executor::class)
            }
        }
        if (!originTasks.all { it.indexed }) {
            ManualIndexed.flush(AbstractTask::class)
            for (task in originTasks.filterIsInstance<ManualIndexed>()) {
                task.setIndexed(AbstractTask::class)
            }
        }
    }

    override val withExecutorLeisure: Boolean = true
    override val taskCancelEnabled: Boolean = true

    internal val aggregation = TaskAggregation<IT, E, A>()
    val tasksIteration: List<List<IT>> by aggregation::tasksIteration
    val tasks: List<IT> by aggregation::tasks
    val removedTasks: Set<IT> by aggregation::removedTasks
    val lastIterationTasks: List<IT> by aggregation::lastIterationTasks

    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> by ::_x

    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    lateinit var taskCost: LinearExpressionSymbol
    override lateinit var taskAssignment: LinearExpressionSymbols2
    override lateinit var taskCompilation: LinearExpressionSymbols1
    private lateinit var xor: BinVariable1
    override lateinit var executorCompilation: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (!::y.isInitialized) {
            y = BinVariable1("y", Shape1(tasks.size))
            for (task in originTasks) {
                y[task].name = "${y.name}_${task}"

                if (lockCancelTasks.contains(task)) {
                    y[task].range.eq(true)
                }
            }
        }
        model.addVars(y)

        if (!::taskCost.isInitialized) {
            taskCost = LinearExpressionSymbol(LinearPolynomial(), "bunch_cost")
        }
        model.addSymbol(taskCost)

        if (!::taskAssignment.isInitialized) {
            taskAssignment = flatMap(
                "task_compilation",
                tasks,
                executors,
                { _, _ -> LinearPolynomial() },
                { (_, t), (_, e) -> "${t}_$e" }
            )
        }
        model.addSymbols(taskAssignment)

        if (!::taskCompilation.isInitialized) {
            taskCompilation = flatMap(
                "task_compilation",
                tasks,
                { t -> LinearPolynomial(y[t]) },
                { (_, t) -> "$t" }
            )
        }
        model.addSymbols(taskCompilation)

        if (!::xor.isInitialized) {
            xor = BinVariable1("xor", Shape1(executors.size))
        }
        model.addVars(xor)

        if (withExecutorLeisure) {
            if (!::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
            }
            model.addVars(z)
        }

        if (!::executorCompilation.isInitialized) {
            executorCompilation = flatMap(
                "executor_compilation",
                executors,
                { e ->
                    if (withExecutorLeisure) {
                        LinearPolynomial(xor[e] + z[e])
                    } else {
                        LinearPolynomial(xor[e])
                    }
                },
                { e -> "$e" }
            )
        }
        model.addSymbols(executorCompilation)

        return ok
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: LinearMetaModel,
        cost: (IT) -> Cost,
        conflict: (IT, IT) -> Boolean
    ): Ret<List<IT>> {
        val unduplicatedTasks = aggregation.addColumns(newTasks)

        val xi = BinVariable1("x_$iteration", Shape1(unduplicatedTasks.size))
        for (task in unduplicatedTasks) {
            xi[task].name = "${xi.name}_${task.index}_${task.executor}"
        }
        model.addVars(xi)
        _x.add(xi)

        taskCost.flush()
        for (task in unduplicatedTasks) {
            taskCost.asMutable() += (cost(task).sum ?: Flt64.infinity) * xi[task]
        }

        for (originTask in originTasks) {
            for (executor in executors) {
                val thisTasks = unduplicatedTasks.filter { it.key == originTask.key && it.executor == executor }
                if (thisTasks.isNotEmpty()) {
                    val assign = taskAssignment[originTask, executor]
                    assign.flush()
                    assign.asMutable() += sum(thisTasks.map { xi[it] })
                }
            }
        }

        for (originTask in originTasks) {
            val thisTasks = unduplicatedTasks.filter { it.key == originTask.key }
            if (thisTasks.isNotEmpty()) {
                val compilation = taskCompilation[originTask]
                compilation.flush()
                compilation.asMutable() += sum(thisTasks.map { xi[it] })
            }
        }

        for (executor in executors) {
            val thisTasks = unduplicatedTasks.filter { it.executor == executor }
            for (task in thisTasks) {
                model.addConstraint(
                    xor[executor] geq xi[task],
                    "xor_${executor}_${iteration}_${task}"
                )
            }
        }

        for (task1 in unduplicatedTasks) {
            for ((otherIteration, otherTasks) in tasksIteration.withIndex()) {
                for (task2 in otherTasks) {
                    if (task1 != task2 && conflict(task1, task2)) {
                        model.addConstraint(
                            xi[task1] + x[otherIteration][task2] leq Flt64.one,
                            "task_conflict_${task1}_${otherIteration}_${task2}"
                        )
                    }
                }
            }
        }

        return Ok(unduplicatedTasks)
    }
}
