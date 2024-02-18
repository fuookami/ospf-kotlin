package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

data class BunchAggregation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val bunchesIteration: MutableList<List<AbstractTaskBunch<T, E, A>>> = ArrayList(),
    val bunches: MutableList<AbstractTaskBunch<T, E, A>> = ArrayList(),
    val removedBunches: MutableSet<AbstractTaskBunch<T, E, A>> = HashSet()
) {
    val lastIterationBunches: List<AbstractTaskBunch<T, E, A>>
        get() =
            bunchesIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    suspend fun addColumns(newBunches: List<AbstractTaskBunch<T, E, A>>): List<AbstractTaskBunch<T, E, A>> {
        val unduplicatedBunches = coroutineScope {
            val promises = ArrayList<Deferred<AbstractTaskBunch<T, E, A>?>>()
            for (bunch in newBunches) {
                promises.add(async(Dispatchers.Default) {
                    if (bunches.all { bunch neq it }) {
                        bunch
                    } else {
                        null
                    }
                })
            }
            promises.mapNotNull { it.await() }
        }

        ManualIndexed.flush<AbstractTaskBunch<T, E, A>>()
        for (bunch in unduplicatedBunches) {
            bunch.setIndexed()
        }
        bunchesIteration.add(unduplicatedBunches)
        bunches.addAll(unduplicatedBunches)

        return unduplicatedBunches
    }

    fun removeColumn(bunch: AbstractTaskBunch<T, E, A>) {
        if (!removedBunches.contains(bunch)) {
            removedBunches.add(bunch)
            bunches.remove(bunch)
        }
    }

    fun removeColumns(bunches: List<AbstractTaskBunch<T, E, A>>) {
        for (bunch in bunches) {
            removeColumn(bunch)
        }
    }
}

open class BunchCompilation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = false
) : Compilation {
    override val taskCancelEnabled: Boolean = true

    internal val aggregation = BunchAggregation<T, E, A>()
    val bunchesIteration: List<List<AbstractTaskBunch<T, E, A>>> by aggregation::bunchesIteration
    val bunches: List<AbstractTaskBunch<T, E, A>> by aggregation::bunches
    val removedBunches: Set<AbstractTaskBunch<T, E, A>> by aggregation::removedBunches
    val lastIterationBunches: List<AbstractTaskBunch<T, E, A>> by aggregation::lastIterationBunches

    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> get() = _x

    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    lateinit var bunchCost: LinearExpressionSymbol
    override lateinit var taskAssignment: LinearExpressionSymbols2
    override lateinit var taskCompilation: LinearExpressionSymbols1
    override lateinit var executorCompilation: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (!::y.isInitialized) {
            y = BinVariable1("y", Shape1(tasks.size))
            for (task in tasks) {
                y[task].name = "${y.name}_${task}"

                if (lockCancelTasks.contains(task)) {
                    y[task].range.eq(UInt8.one)
                }
            }
        }
        model.addVars(y)

        if (!::bunchCost.isInitialized) {
            bunchCost = LinearExpressionSymbol(LinearPolynomial(), "bunch_cost")
        }
        model.addSymbol(bunchCost)

        if (!::taskAssignment.isInitialized) {
            taskAssignment = flatMap(
                "task_assignment",
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

        if (withExecutorLeisure) {
            if (!::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
            }
            model.addVars(z)
        }

        if (::executorCompilation.isInitialized) {
            executorCompilation = flatMap(
                "executor_compilation",
                executors,
                { e ->
                    if (withExecutorLeisure) {
                        LinearPolynomial(z[e])
                    } else {
                        LinearPolynomial()
                    }
                },
                { e -> "$e" }
            )
        }
        model.addSymbols(executorCompilation)

        return Ok(success)
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTaskBunch<T, E, A>>> {
        val unduplicatedBunches = aggregation.addColumns(newBunches)

        val xi = BinVariable1("x_$iteration", Shape1(unduplicatedBunches.size))
        for (bunch in unduplicatedBunches) {
            xi[bunch].name = "${xi.name}_${bunch.index}_${bunch.executor}"
        }
        model.addVars(xi)
        _x.add(xi)

        bunchCost.flush()
        for (bunch in unduplicatedBunches) {
            bunchCost.asMutable() += (bunch.cost.sum ?: Flt64.infinity) * xi[bunch]
        }

        for (task in tasks) {
            for (executor in executors) {
                val thisBunches = unduplicatedBunches.filter { it.contains(task) && it.executor == executor }
                if (thisBunches.isNotEmpty()) {
                    val assign = taskAssignment[task, executor]
                    assign.flush()
                    assign.asMutable() += sum(thisBunches.map { xi[it] })
                }
            }
        }

        for (task in tasks) {
            val thisBunches = unduplicatedBunches.filter { it.contains(task) }
            if (thisBunches.isNotEmpty()) {
                val compilation = taskCompilation[task]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { xi[it] })
            }
        }

        for (executor in executors) {
            val thisBunches = unduplicatedBunches.filter { it.executor == executor }
            if (thisBunches.isNotEmpty()) {
                val compilation = executorCompilation[executor]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { xi[it] })
            }
        }

        return Ok(unduplicatedBunches)
    }
}
