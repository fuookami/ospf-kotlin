package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

abstract class AbstractIterativeTaskCompilationAggregation<
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    tasks: List<T>,
    executors: List<E>,
    lockedCancelTasks: Set<T> = emptySet()
) {
    data class Policy<
        IT : IterativeAbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
    >(
        val cost: (IT) -> Cost,
        val conflict: (IT, IT) -> Boolean
    )

    private val logger = org.apache.logging.log4j.kotlin.logger("IterativeTaskSchedulingAggregation")

    val compilation: IterativeTaskCompilation<IT, T, E, A> = IterativeTaskCompilation(tasks, executors, lockedCancelTasks)
    abstract val policy: Policy<IT, E, A>

    val tasksIteration: List<List<IT>> by compilation::tasksIteration
    val tasks: List<IT> by compilation::tasks
    val removedTasks: Set<IT> by compilation::removedTasks
    val lastIterationTasks: List<IT> by compilation::lastIterationTasks

    open fun register(model: MetaModel): Try {
        when (val result = compilation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel
    ): Ret<List<IT>> {
        val unduplicatedTasks = when (val result = compilation.addColumns(
            iteration = iteration,
            newTasks = newTasks,
            model = model,
            cost = policy.cost,
            conflict = policy.conflict
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedTasks)
    }

    open fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (IT) -> Flt64,
        fixedTasks: Set<IT>,
        keptTasks: Set<IT>,
        model: AbstractLinearMetaModel
    ): Ret<Flt64> {
        for (task in tasks) {
            if (removedTasks.contains(task)) {
                continue
            }

            if (!(reducedCost(task) ls maximumReducedCost)
                && !fixedTasks.contains(task)
                && !keptTasks.contains(task)
            ) {
                compilation.aggregation.removeColumn(task)
            }
        }

        for (task in compilation.removedTasks) {
            val xi = compilation.x[task.iteration.toInt()]
            xi[task].range.eq(false)
            model.remove(xi[task])
        }

        val remainingAmount = UInt64((tasks.size - removedTasks.size).toULong())
        return if (remainingAmount > maximumColumnAmount) {
            Ok(max((maximumReducedCost.floor().toInt64() * Int64(2L) / Int64(3L)).toFlt64(), Flt64(5.0)))
        } else {
            Ok(maximumReducedCost)
        }
    }

    open fun extractFixedTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Ret<Set<IT>> {
        return extractTasks(iteration, model) { it eq Flt64.one }
    }

    open fun extractKeptTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Ret<Set<IT>> {
        return extractTasks(iteration, model) { it gr Flt64.zero }
    }

    open fun extractHiddenExecutors(
        executors: List<E>,
        model: AbstractLinearMetaModel
    ): Ret<Set<E>> {
        val z = compilation.z
        val ret = HashSet<E>()
        for (token in model.tokens.tokens) {
            if (token.belongsTo(z)) {
                if (token.result!! gr Flt64.zero) {
                    ret.add(executors[token.variable.index])
                }
            }
        }
        return Ok(ret)
    }

    open fun globallyFix(
        fixedTasks: Set<IT>
    ): Try {
        for (task in fixedTasks) {
            assert(!removedTasks.contains(task))
            val xi = compilation.x[task.iteration.toInt()]
            logger.debug { "globally fix: ${xi[task]}" }
            xi[task].range.eq(true)
        }
        return ok
    }

    open fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Set<IT>,
        model: AbstractLinearMetaModel
    ): Ret<Set<IT>> {
        var flag = true
        val ret = HashSet<IT>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token.belongsTo(y) && (token.result!! gr bar)) {
                logger.debug { "locally fix: ${y[token.variable.index]}" }
                y[token.variable.index].range.eq(true)
                flag = false
            }

            for (i in UInt64.zero..iteration) {
                if (token.belongsTo(compilation.x[i.toInt()])) {
                    val xi = compilation.x[i.toInt()]

                    val task = tasksIteration[i.toInt()][token.variable.index]
                    assert(!removedTasks.contains(task))

                    if ((token.result != null)
                        && (token.result!! geq bestValue)
                        && !fixedTasks.contains(task)
                    ) {
                        bestValue = token.result!!
                        bestIteration = i
                        bestIndex = token.variable.index
                    }
                    if ((token.result != null)
                        && (token.result!! geq bar)
                        && !fixedTasks.contains(task)
                    ) {
                        ret.add(task)
                        logger.debug { "locally fix: ${xi[token.variable.index]}" }
                        xi[token.variable.index].range.eq(true)
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any task
        // fix the best if the value greater than 1e-3
        if (flag && ret.isEmpty() && (bestValue geq Flt64(1e-3))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]
            ret.add(tasksIteration[bestIteration.toInt()][bestIndex])
            logger.debug { "locally fix: $xi" }
            xi.range.eq(true)
        }

        return Ok(ret)
    }

    open fun logResult(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        for (token in model.tokens.tokens) {
            if (token.result!! gr Flt64.zero) {
                logger.debug { "${token.name} = ${token.result!!}" }
            }
        }

        for (obj in model.subObjects) {
            logger.debug { "${obj.name} = ${obj.value()}" }
        }

        return ok
    }

    open fun logTaskCost(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
        for (token in model.tokens.tokens) {
            if ((token.result!! eq Flt64.one) && token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable.belongsTo(xi)) {
                        val task = tasksIteration[i.toInt()][token.variable.index]
                        logger.debug { "${task.executor} cost: ${policy.cost(task).sum!!}" }
                        break
                    }
                }
            }
        }

        return ok
    }

    fun flush(
        iteration: UInt64,
        tasks: List<T>,
        lockCancelTasks: Set<T> = emptySet()
    ): Try {
        val y = compilation.y
        for (task in tasks) {
            if (task.cancelEnabled && when (task) {
                    is AbstractPlannedTask<*, *> -> {
                        !lockCancelTasks.any { (it as AbstractPlannedTask<*, *>).plan == task.plan }
                    }

                    else -> {
                        true
                    }
                }
            ) {
                y[task].range.set(ValueRange(Binary.minimum, Binary.maximum))
            }
        }
        for (i in UInt64.zero..iteration) {
            val xi = compilation.x[i.toInt()]

            for (task in tasksIteration[i.toInt()]) {
                if (!removedTasks.contains(task)) {
                    xi[task].range.set(ValueRange(Binary.minimum, Binary.maximum))
                }
            }
        }
        return ok
    }

    private fun extractTasks(
        iteration: UInt64,
        model: AbstractLinearMetaModel,
        predicate: (Flt64) -> Boolean
    ): Ret<Set<IT>> {
        val ret = HashSet<IT>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token.belongsTo(xi)) {
                    val task = tasksIteration[i][token.variable.index]
                    assert(!removedTasks.contains(task))
                    ret.add(task)
                }
            }
        }
        return Ok(ret)
    }
}

open class IterativeTaskCompilationAggregation<
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    tasks: List<T>,
    executors: List<E>,
    override val policy: Policy<IT, E, A>,
    lockCancelTask: Set<T> = emptySet()
) : AbstractIterativeTaskCompilationAggregation<IT, T, E, A>(tasks, executors, lockCancelTask)

open class IterativeTaskCompilationAggregationWithTime<
    IT : IterativeAbstractTask<E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    override val policy: Policy<IT, E, A>,
    lockCancelTasks: Set<T> = emptySet(),
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractIterativeTaskCompilationAggregation<IT, T, E, A>(tasks, executors, lockCancelTasks) {
    val taskTime: IterativeTaskSchedulingTaskTime<IT, T, E, A> =
        IterativeTaskSchedulingTaskTime(timeWindow, tasks, compilation, redundancyRange)
    val makespan: Makespan<T, E, A> = Makespan(tasks, taskTime, makespanExtra)

    override fun register(model: MetaModel): Try {
        when (val result = super.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = taskTime.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = makespan.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<IT>,
        model: AbstractLinearMetaModel
    ): Ret<List<IT>> {
        val unduplicatedBunches = when (val result = super.addColumns(iteration, newTasks, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = taskTime.addColumns(iteration, unduplicatedBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedBunches)
    }
}
