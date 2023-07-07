package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

typealias CostCalculator<E> = Extractor<Flt64?, Task<E>>

class Compilation<E : Executor>(
    val taskCancelEnabled: Boolean,
    val withExecutorLeisure: Boolean = false
) {
    lateinit var x: BinVariable2
    lateinit var y: BinVariable1
    lateinit var z: BinVariable1

    lateinit var taskCompilation: LinearSymbols1
    lateinit var executorCompilation: LinearSymbols1

    fun register(tasks: List<Task<E>>, executors: List<E>, lockCancelTasks: Set<Task<E>> = emptySet(), model: LinearMetaModel): Try<Error> {
        if (!this::x.isInitialized) {
            x = BinVariable2("x", Shape2(tasks.size, executors.size))
            for (task in tasks) {
                for (executor in executors) {
                    x[task, executor]!!.name = "${x.name}_${task}_${executor}"
                }

                if (task.executor != null && !task.executorChangeEnabled) {
                    for (executor in executors) {
                        if (task.executor == executor) {
                            if (!task.cancelEnabled) {
                                x[task, executor]!!.range.eq(UInt8.one)
                            }
                        } else {
                            x[task, executor]!!.range.eq(UInt8.zero)
                        }
                    }
                }
            }
        }
        model.addVars(x)

        if (taskCancelEnabled) {
            if (!this::y.isInitialized) {
                y = BinVariable1("y", Shape1(tasks.size))
                for (task in tasks) {
                    y[task]!!.name = ""
                    if (!task.cancelEnabled) {
                        y[task]!!.range.eq(UInt8.zero)
                    }
                    if (lockCancelTasks.contains(task)) {
                        y[task]!!.range.eq(UInt8.one)
                    }
                }
            }
            model.addVars(y)
        }

        if (!this::taskCompilation.isInitialized) {
            taskCompilation = LinearSymbols1("task_compilation", Shape1(tasks.size))
            for (task in tasks) {
                val poly = LinearPolynomial()
                if (taskCancelEnabled) {
                    poly += y[task]!!
                }
                executors.forEach { poly += x[task, it]!! }
                taskCompilation[task] = LinearSymbol(poly, "${taskCompilation.name}_${task}")
            }
        }
        model.addSymbols(taskCompilation)

        if (withExecutorLeisure) {
            if (!this::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
                for (executor in executors) {
                    z[executor]!!.name = "${z.name}_${executor}"
                }
            }
            model.addVars(z)

            if (!this::executorCompilation.isInitialized) {
                executorCompilation = LinearSymbols1("executor_compilation", Shape1(executors.size))
                for (executor in executors) {
                    val executorAssigned = OrFunction(tasks.map { LinearPolynomial(x[it, executor]!!) }, "executor_assigned_${executor}")
                    model.addSymbol(executorAssigned)

                    executorCompilation[executor] = LinearSymbol(
                        executorAssigned + z[executor]!!,
                        "${executorCompilation.name}_${executor}"
                    )
                }
            }
            model.addSymbols(executorCompilation)
        }

        return Ok(success)
    }
}
