package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * TODO
 *
 * @property y      task canceled
 * @property z      executor leisure
 */
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

class TaskCompilation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
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
                    val or = OrFunction(tasks.map { LinearPolynomial(x[it, executors[i]]) }, "executor_compilation_or_${executors[i]}")
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

        return Ok(success)
    }
}
