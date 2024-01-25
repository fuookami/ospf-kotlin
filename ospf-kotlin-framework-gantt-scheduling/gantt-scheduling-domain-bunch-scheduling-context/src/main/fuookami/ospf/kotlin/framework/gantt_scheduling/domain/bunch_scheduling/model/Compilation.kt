package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

class BunchCompilation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = false
) : Compilation {
    override val taskCancelEnabled: Boolean = true

    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> get() = _x

    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    lateinit var bunchCost: LinearExpressionSymbol
    override lateinit var taskAssignment: LinearExpressionSymbols2
    override lateinit var taskCompilation: LinearExpressionSymbols1
    override lateinit var executorCompilation: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (!::taskAssignment.isInitialized) {
            taskAssignment = flatMap(
                "task_assignment",
                tasks,
                executors,
                { _, _ -> LinearPolynomial() },
                { (_, t), (_, e) -> "${t}_$e" }
            )
        }

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

    fun addColumns(
        iteration: UInt64,
        bunches: List<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Try {
        val xi = BinVariable1("x_$iteration", Shape1(bunches.size))
        for (bunch in bunches) {
            xi[bunch].name = "${xi.name}_${bunch.index}_${bunch.executor}"
        }
        model.addVars(xi)
        _x.add(xi)

        bunchCost.flush()
        for (bunch in bunches) {
            bunchCost.asMutable() += (bunch.cost.sum ?: Flt64.infinity) * xi[bunch]
        }

        for (task in tasks) {
            for (executor in executors) {
                val thisBunches = bunches.filter { it.contains(task) && it.executor == executor }
                if (thisBunches.isNotEmpty()) {
                    val assign = taskAssignment[task, executor]
                    assign.flush()
                    assign.asMutable() += sum(thisBunches.map { xi[it] })
                }
            }
        }

        for (task in tasks) {
            val thisBunches = bunches.filter { it.contains(task) }
            if (thisBunches.isNotEmpty()) {
                val compilation = taskCompilation[task]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { xi[it] })
            }
        }

        for (executor in executors) {
            val thisBunches = bunches.filter { it.executor == executor }
            if (thisBunches.isNotEmpty()) {
                val compilation = executorCompilation[executor]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { xi[it] })
            }
        }

        return Ok(success)
    }
}
