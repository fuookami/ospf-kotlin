/** 任务束编译模型 / Bunch compilation model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Compilation

/**
 * 任务束编译 / Bunch compilation
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
 * @param bunchAggregation 任务束聚合 / Bunch aggregation
 */
open class BunchCompilation<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = true,
    bunchAggregation: BunchAggregation<B, V, T, E, A> = BunchAggregation()
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

    internal val aggregation: BunchAggregation<B, V, T, E, A> = bunchAggregation
    val bunchesIteration: List<List<B>> by aggregation::bunchesIteration
    val bunches: List<B> by aggregation::bunches
    val removedBunches: Set<B> by aggregation::removedBunches
    val lastIterationBunches: List<B> by aggregation::lastIterationBunches

    private val _x = ArrayList<BinVariable1>()
    val x: List<BinVariable1> by ::_x

    override lateinit var y: BinVariable1
    override lateinit var z: BinVariable1

    lateinit var bunchCost: LinearIntermediateSymbol<Flt64>
    override lateinit var taskAssignment: LinearIntermediateSymbols2<Flt64>
    override lateinit var taskCompilation: LinearIntermediateSymbols1<Flt64>
    override lateinit var executorCompilation: LinearIntermediateSymbols1<Flt64>

    /**
     * 注册到模型 / Register to model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    override fun register(model: MetaModel<Flt64>): Try {
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::bunchCost.isInitialized) {
            bunchCost = LinearExpressionSymbol(Flt64, name = "bunch_cost")
        }
        when (val result = model.add(bunchCost)) {
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
            ) { _, (t, e) ->
                val task = tasks[t]
                val executor = executors[e]
                LinearExpressionSymbol(
                    Flt64,
                    name = "task_assignment_${task}_${executor}"
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
                name = "task_compilation",
                shape = Shape1(tasks.size)
            ) { i, _ ->
                val task = tasks[i]
                LinearExpressionSymbol(
                    y[i],
                    Flt64,
                    name = "task_compilation_${task}"
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
                name = "executor_compilation",
                shape = Shape1(executors.size)
            ) { i, _ ->
                val executor = executors[i]
                LinearExpressionSymbol(
                    polynomial = if (withExecutorLeisure) {
                        LinearPolynomial(listOf(LinearMonomial(Flt64.one, z[i])), Flt64.zero)
                    } else {
                        LinearPolynomial(emptyList(), Flt64.zero)
                    },
                    name = "executor_compilation_${executor}"
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

    /**
     * 添加列 / Add columns
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newBunches 新任务束列表 / List of new bunches
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务束列表 / Deduplicated bunch list
     */
    open suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        _x.add(xi)

        for (bunch in unduplicatedBunches) {
            (bunchCost as LinearExpressionSymbol<Flt64>).flush()
            (bunchCost as LinearExpressionSymbol<Flt64>).asMutable() += LinearMonomial(
                bunch.cost.solverCost(Flt64.infinity),
                xi[bunch]
            )
        }

        for (task in tasks) {
            for (executor in executors) {
                val thisBunches = unduplicatedBunches.filter { it.contains(task) && it.executor == executor }
                if (thisBunches.isNotEmpty()) {
                    val assign = taskAssignment[task, executor]
                    assign.flush()
                    assign.asMutable() += sum(thisBunches.map { LinearMonomial(Flt64.one, xi[it]) })
                }
            }
        }

        for (task in tasks) {
            val thisBunches = unduplicatedBunches.filter { it.contains(task) }
            if (thisBunches.isNotEmpty()) {
                val compilation = taskCompilation[task]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { LinearMonomial(Flt64.one, xi[it]) })
            }
        }

        for (executor in executors) {
            val thisBunches = unduplicatedBunches.filter { it.executor == executor }
            if (thisBunches.isNotEmpty()) {
                val compilation = executorCompilation[executor]
                compilation.flush()
                compilation.asMutable() += sum(thisBunches.map { LinearMonomial(Flt64.one, xi[it]) })
            }
        }

        return Ok(unduplicatedBunches)
    }
}
