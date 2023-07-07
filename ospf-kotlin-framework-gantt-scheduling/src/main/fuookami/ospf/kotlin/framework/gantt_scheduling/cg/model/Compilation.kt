package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*

typealias CostCalculator<E> = Extractor<Flt64?, Task<E>>

open class Compilation<E : Executor>(
    val withExecutorLeisure: Boolean = false
) {
    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> get() = _x

    lateinit var y: BinVariable1
    lateinit var z: BinVariable1

    lateinit var bunchCost: LinearSymbol
    lateinit var taskCompilation: LinearSymbols1
    lateinit var executorCompilation: LinearSymbols1

    open fun register(tasks: List<Task<E>>, executors: List<E>, lockCancelTasks: Set<Task<E>> = emptySet(), model: LinearMetaModel): Try<Error> {
        if (!this::y.isInitialized) {
            y = BinVariable1("y", Shape1(tasks.size))
            for (task in tasks) {
                y[task]!!.name = "${y.name}_${task}"

                if (lockCancelTasks.contains(task)) {
                    y[task]!!.range.eq(UInt8.one)
                }
            }
        }
        model.addVars(y)

        if (!this::bunchCost.isInitialized) {
            bunchCost = LinearSymbol(LinearPolynomial(), "bunch_cost")
        }
        model.addSymbol(bunchCost)

        if (!this::taskCompilation.isInitialized) {
            taskCompilation = LinearSymbols1("task_compilation", Shape1(tasks.size))
            for (task in tasks) {
                taskCompilation[task] = LinearSymbol(
                    LinearPolynomial(y[task]!!),
                    "${taskCompilation.name}_${task}"
                )
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
                    executorCompilation[executor] = LinearSymbol(
                        LinearPolynomial(z[executor]!!),
                        "${executorCompilation.name}_${executor}"
                    )
                }
            }
            model.addSymbols(executorCompilation)
        }

        return Ok(success)
    }

    @OptIn(DelicateCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    open fun addColumns(
        iteration: UInt64,
        bunches: List<TaskBunch<E>>,
        tasks: List<Task<E>>,
        executors: List<E>,
        model: LinearMetaModel,
        scope: CoroutineScope? = GlobalScope
    ): Result<BroadcastChannel<Boolean>?, Error> {
        assert(iteration.toInt() == x.size)
        assert(bunches.isNotEmpty())

        val xi = BinVariable1("x_$iteration", Shape1(bunches.size))
        for (bunch in bunches) {
            xi[bunch]!!.name = "${xi.name}_${bunch.index}_${bunch.executor}"
        }
        model.addVars(xi)
        _x.add(xi)

        bunchCost.flush()
        for (bunch in bunches) {
            (bunchCost.polynomial as LinearPolynomial) += bunch.cost.sum!! * xi[bunch]!!
        }

        for (task in tasks) {
            bunches.asSequence()
                .filter { it.contains(task) }
                .forEach {
                    val compilation = taskCompilation[task]!! as LinearSymbol
                    compilation.flush()
                    (compilation.polynomial as LinearPolynomial) += xi[it]!!
                }
        }

        for (executor in executors) {
            bunches.asSequence()
                .filter { it.executor == executor }
                .forEach {
                    val compilation = executorCompilation[executor]!! as LinearSymbol
                    compilation.flush()
                    (compilation.polynomial as LinearPolynomial) += xi[it]!!
                }
        }

        return if (scope != null) {
            val promise = BroadcastChannel<Boolean>(Channel.BUFFERED)
            scope.launch {
                flush(tasks, executors)
                promise.send(true)
            }
            Ok(promise)
        } else {
            flush(tasks, executors)
            Ok(null)
        }
    }

    private fun flush(tasks: List<Task<E>>, executors: List<E>) {
        bunchCost.cells

        for (task in tasks) {
            (taskCompilation[task]!! as LinearSymbol).cells
        }
        for (executor in executors) {
            (executorCompilation[executor]!! as LinearSymbol).cells
        }
    }
}
