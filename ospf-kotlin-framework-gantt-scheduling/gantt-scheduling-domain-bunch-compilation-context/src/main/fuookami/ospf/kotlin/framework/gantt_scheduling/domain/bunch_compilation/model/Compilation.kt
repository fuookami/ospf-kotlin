package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

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
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

open class BunchCompilation<
    B : AbstractTaskBunch<T, E, A>,
    out T : AbstractTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = true
) : Compilation {
    init {
        if (!executors.all { it.indexed }) {
            ManualIndexed.flush(Executor::class)
            for (executor in executors) {
                executor.setIndexed(Executor::class)
            }
        }
        if (!tasks.all { it.indexed }) {
            ManualIndexed.flush(AbstractTask::class)
            for (task in tasks.filterIsInstance<ManualIndexed>()) {
                task.setIndexed(AbstractTask::class)
            }
        }
    }

    override val taskCancelEnabled: Boolean = true

    internal val aggregation = BunchAggregation<B, T, E, A>()
    val bunchesIteration: List<List<B>> by aggregation::bunchesIteration
    val bunches: List<B> by aggregation::bunches
    val removedBunches: Set<B> by aggregation::removedBunches
    val lastIterationBunches: List<B> by aggregation::lastIterationBunches

    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> by ::_x

    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    lateinit var bunchCost: LinearExpressionSymbol
    override lateinit var taskAssignment: LinearExpressionSymbols2
    override lateinit var taskCompilation: LinearExpressionSymbols1
    override lateinit var executorCompilation: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (!::y.isInitialized) {
            y = BinVariable1("y", Shape1(tasks.size))
            for (task in tasks) {
                y[task].name = "${y.name}_${task}"

                if (lockCancelTasks.contains(task)) {
                    y[task].range.eq(true)
                }
            }
        }
        when (val result = model.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::bunchCost.isInitialized) {
            bunchCost = LinearExpressionSymbol(LinearPolynomial(), "bunch_cost")
        }
        when (val result = model.add(bunchCost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::taskAssignment.isInitialized) {
            taskAssignment = flatMap(
                "task_assignment",
                tasks,
                executors,
                { _, _ -> LinearPolynomial() },
                { (_, t), (_, e) -> "${t}_$e" }
            )
        }
        when (val result = model.add(taskAssignment)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::taskCompilation.isInitialized) {
            taskCompilation = flatMap(
                "task_compilation",
                tasks,
                { t -> LinearPolynomial(y[t]) },
                { (_, t) -> "$t" }
            )
        }
        when (val result = model.add(taskCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (withExecutorLeisure) {
            if (!::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
            }
            when (val result = model.add(z)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!::executorCompilation.isInitialized) {
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
        when (val result = model.add(executorCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<List<B>> {
        val unduplicatedBunches = aggregation.addColumns(newBunches)

        val xi = BinVariable1("x_$iteration", Shape1(unduplicatedBunches.size))
        for (bunch in unduplicatedBunches) {
            xi[bunch].name = "${xi.name}_${bunch.index}_${bunch.executor}"
        }
        when (val result = model.add(xi)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
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
