package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.intermediate_symbol.function.OrFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.BinVariable2
import fuookami.ospf.kotlin.core.variable.eq
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray._a

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

interface Compilation {
    val taskCancelEnabled: Boolean
    val withExecutorLeisure: Boolean

    val y: BinVariable1
    val z: BinVariable1

    val taskAssignment: LinearIntermediateSymbols2<Flt64>
    val taskCompilation: LinearIntermediateSymbols1<Flt64>
    val executorCompilation: LinearIntermediateSymbols1<Flt64>

    fun register(model: MetaModel<Flt64>): Try
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

    override lateinit var taskAssignment: LinearIntermediateSymbols2<Flt64>
    override lateinit var taskCompilation: LinearIntermediateSymbols1<Flt64>
    override lateinit var executorCompilation: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::x.isInitialized) {
            x = BinVariable2(
                "x",
                Shape2(tasks.size, executors.size)
            )
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
        when (val result = model.add(x)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::taskAssignment.isInitialized) {
            taskAssignment = LinearIntermediateSymbols2<Flt64>(
                name = "task_assignment",
                shape = Shape2(tasks.size, executors.size)
            ) { _, v ->
                LinearExpressionSymbol(
                    item = x[v],
                    name = "task_assignment_${v.joinToString("_")}"
                )
            }
            for (task in tasks) {
                for (executor in executors) {
                    taskAssignment[task, executor].range.set(
                        ValueRange(Flt64.zero, Flt64.one).value!!
                    )
                }
            }
        }
        when (val result = model.add(taskAssignment)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (taskCancelEnabled) {
            if (!::y.isInitialized) {
                y = BinVariable1(
                    "y",
                    Shape1(tasks.size)
                )
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
            when (val result = model.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::taskCompilation.isInitialized) {
            taskCompilation = LinearIntermediateSymbols1<Flt64>(
                name = "task_compilation",
                shape = Shape1(tasks.size)
            ) { t, _ ->
                LinearExpressionSymbol(
                    polynomial = if (taskCancelEnabled) {
                        LinearPolynomial(y[t]) + sum(x[t, _a].map { LinearPolynomial(it) })
                    } else {
                        sum(x[t, _a].map { LinearPolynomial(it) })
                    },
                    name = "task_compilation_${t}"
                )
            }
            for (task in tasks) {
                taskCompilation[task].range.set(ValueRange(Flt64.one, Flt64.one).value!!)
            }
        }
        when (val result = model.add(taskCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (withExecutorLeisure) {
            if (!::z.isInitialized) {
                z = BinVariable1("z", Shape1(executors.size))
                for (executor in executors) {
                    z[executor].name = "${z.name}_${executor}"
                }
            }
            when (val result = model.add(z)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::executorCompilation.isInitialized) {
            val orFunctions = ArrayList<OrFunction<Flt64>>()
            executorCompilation = LinearIntermediateSymbols1<Flt64>(
                name = "executor_compilation",
                shape = Shape1(executors.size)
            ) { i, _ ->
                val orPolynomials = tasks.map { LinearPolynomial(x[it, executors[i]]) }
                val or = OrFunction(
                    polynomials = orPolynomials,
                    converter = flt64Converter,
                    name = "executor_compilation_or_${executors[i]}"
                )
                orFunctions.add(or)
                val polynomial = if (withExecutorLeisure) {
                    LinearPolynomial(or.resultVar) + LinearPolynomial(z[executors[i]])
                } else {
                    LinearPolynomial(or.resultVar)
                }
                LinearExpressionSymbol(
                    polynomial = polynomial,
                    name = "executor_compilation_${executors[i]}"
                )
            }
            for (or in orFunctions) {
                when (val result = model.add(LinearFunctionSymbolAdapter(or, flt64Converter))) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }
        when (val result = model.add(executorCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}

open class IterativeTaskCompilation<
        IT : IterativeAbstractTask<E, A>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val originTasks: List<T>,
    private val executors: List<E>,
    private val lockedCancelTasks: Set<T> = emptySet(),
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

    lateinit var taskCost: LinearIntermediateSymbol<Flt64>
    override lateinit var taskAssignment: LinearIntermediateSymbols2<Flt64>
    override lateinit var taskCompilation: LinearIntermediateSymbols1<Flt64>
    private lateinit var xor: BinVariable1
    override lateinit var executorCompilation: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::y.isInitialized) {
            y = BinVariable1(
                name = "y",
                shape = Shape1(tasks.size)
            )
            for (task in originTasks) {
                y[task].name = "${y.name}_${task}"

                if (lockedCancelTasks.contains(task)) {
                    y[task].range.eq(true)
                }
            }
        }
        when (val result = model.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::taskCost.isInitialized) {
            taskCost = LinearExpressionSymbol(
                name = "bunch_cost"
            )
        }
        when (val result = model.add(taskCost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::taskAssignment.isInitialized) {
            taskAssignment = LinearIntermediateSymbols2<Flt64>(
                "task_assignment",
                Shape2(tasks.size, executors.size)
            ) { _, v ->
                LinearExpressionSymbol(
                    name = "task_assignment_${v.joinToString("_")}"
                )
            }
        }
        when (val result = model.add(taskAssignment)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::taskCompilation.isInitialized) {
            taskCompilation = LinearIntermediateSymbols1<Flt64>(
                "task_compilation",
                Shape1(tasks.size)
            ) { t, _ ->
                LinearExpressionSymbol(
                    item = y[t],
                    name = "task_compilation_${t}"
                )
            }
        }
        when (val result = model.add(taskCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::xor.isInitialized) {
            xor = BinVariable1("xor", Shape1(executors.size))
        }
        when (val result = model.add(xor)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::executorCompilation.isInitialized) {
            executorCompilation = LinearIntermediateSymbols1<Flt64>(
                "executor_compilation",
                Shape1(executors.size)
            ) { e, _ ->
                LinearExpressionSymbol(
                    polynomial = if (withExecutorLeisure) {
                        LinearPolynomial(xor[e]) + LinearPolynomial(z[e])
                    } else {
                        LinearPolynomial(xor[e])
                    },
                    name = "executor_compilation_${e}"
                )
            }
        }
        when (val result = model.add(executorCompilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel<Flt64>,
        cost: (IT) -> Cost,
        conflict: (IT, IT) -> Boolean
    ): Ret<List<IT>> {
        val unduplicatedTasks = aggregation.addColumns(newTasks)

        val xi = BinVariable1("x_$iteration", Shape1(unduplicatedTasks.size))
        for (task in unduplicatedTasks) {
            xi[task].name = "${xi.name}_${task.index}_${task.executor}"
        }
        when (val result = model.add(xi)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        _x.add(xi)

        taskCost.flush()
        for (task in unduplicatedTasks) {
            (taskCost as LinearExpressionSymbol<Flt64>).asMutable() += (cost(task).sum ?: Flt64.infinity) * LinearPolynomial(xi[task])
        }

        for (originTask in originTasks) {
            for (executor in executors) {
                val thisTasks = unduplicatedTasks.filter { it.key == originTask.key && it.executor == executor }
                if (thisTasks.isNotEmpty()) {
                    val assign = taskAssignment[originTask, executor]
                    assign.flush()
                    assign.asMutable() += sum(thisTasks.map { LinearPolynomial(xi[it]) })
                }
            }
        }

        for (originTask in originTasks) {
            val thisTasks = unduplicatedTasks.filter { it.key == originTask.key }
            if (thisTasks.isNotEmpty()) {
                val compilation = taskCompilation[originTask]
                compilation.flush()
                compilation.asMutable() += sum(thisTasks.map { LinearPolynomial(xi[it]) })
            }
        }

        for (executor in executors) {
            val thisTasks = unduplicatedTasks.filter { it.executor == executor }
            for (task in thisTasks) {
                when (val result = model.addConstraint(
                    xor[executor] geq xi[task],
                    name = "xor_${executor}_${iteration}_${task}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        for (task1 in unduplicatedTasks) {
            for ((otherIteration, otherTasks) in tasksIteration.withIndex()) {
                for (task2 in otherTasks) {
                    if (task1 != task2 && conflict(task1, task2)) {
                        when (val result = model.addConstraint(
                            LinearPolynomial(xi[task1]) + LinearPolynomial(x[otherIteration][task2]) leq Flt64.one,
                            name = "task_conflict_${task1}_${otherIteration}_${task2}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                    }
                }
            }
        }

        return Ok(unduplicatedTasks)
    }
}
