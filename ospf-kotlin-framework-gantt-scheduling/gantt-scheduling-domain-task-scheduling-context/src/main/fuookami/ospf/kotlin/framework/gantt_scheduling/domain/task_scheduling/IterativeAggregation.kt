package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling

import kotlin.time.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

abstract class AbstractIterativeTaskSchedulingAggregation<E : Executor, A : AssignmentPolicy<E>>(
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    lockedCancelTasks: Set<AbstractTask<E, A>> = emptySet()
) {
    data class Policy<E : Executor, A : AssignmentPolicy<E>>(
        val cost: (AbstractTask<E, A>) -> Cost,
        val conflict: (AbstractTask<E, A>, AbstractTask<E, A>) -> Boolean
    )

    private val logger = logger()

    val compilation: IterativeTaskCompilation<E, A> = IterativeTaskCompilation(tasks, executors, lockedCancelTasks)
    abstract val policy: Policy<E, A>

    val tasksIteration: List<List<AbstractTask<E, A>>> by compilation::tasksIteration
    val tasks: List<AbstractTask<E, A>> by compilation::tasks
    val removedTasks: Set<AbstractTask<E, A>> by compilation.removedTasks::keys
    val lastIterationTasks: List<AbstractTask<E, A>> by compilation::lastIterationTasks

    open fun register(model: LinearMetaModel): Try {
        when (val result = compilation.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }

    open suspend fun addColumns(
        iteration: UInt64,
        newTasks: List<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTask<E, A>>> {
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

    open fun removedColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (AbstractTask<E, A>) -> Flt64,
        fixedTasks: Set<AbstractTask<E, A>>,
        keptTasks: Set<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Ret<Flt64> {
        for ((iteration, tasks) in tasksIteration.withIndex()) {
            for (task in tasks) {
                if (removedTasks.contains(task)) {
                    continue
                }

                if (!(reducedCost(task) ls maximumReducedCost)
                    && !fixedTasks.contains(task)
                    && !keptTasks.contains(task)
                ) {
                    compilation.aggregation.removeColumn(UInt64(iteration), task)
                }
            }
        }

        for ((task, iteration) in compilation.removedTasks) {
            val xi = compilation.x[iteration.toInt()]
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
        model: LinearMetaModel
    ): Ret<Map<AbstractTask<E, A>, UInt64>> {
        return extractTasks(iteration, model) { it eq Flt64.one }
    }

    open fun extractKeptTasks(
        iteration: UInt64,
        model: LinearMetaModel
    ): Ret<Map<AbstractTask<E, A>, UInt64>> {
        return extractTasks(iteration, model) { it gr Flt64.zero }
    }

    open fun extractHiddenExecutors(
        executors: List<E>,
        model: LinearMetaModel
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
        fixedTasks: Map<AbstractTask<E, A>, UInt64>
    ): Try {
        for ((task, iteration) in fixedTasks) {
            assert(!removedTasks.contains(task))
            val xi = compilation.x[iteration.toInt()]
            xi[task].range.eq(true)
        }
        return Ok(success)
    }

    open fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedTasks: Map<AbstractTask<E, A>, UInt64>,
        model: LinearMetaModel
    ): Ret<Map<AbstractTask<E, A>, UInt64>> {
        var flag = true
        val ret = HashMap<AbstractTask<E, A>, UInt64>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token.belongsTo(y) && (token.result!! gr bar)) {
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
                        ret[task] = iteration
                        xi[token.variable.index].range.eq(true)
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any task
        // fix the best if the value greater than 1e-3
        if (flag && ret.isEmpty() && (bestValue geq Flt64(1e-3))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]
            ret[tasksIteration[bestIteration.toInt()][bestIndex]] = bestIteration
            xi.range.eq(true)
        }

        return Ok(ret)
    }

    open fun logResult(
        iteration: UInt64,
        model: LinearMetaModel
    ): Try {
        for (token in model.tokens.tokens) {
            if (token.result!! gr Flt64.zero) {
                logger.debug { "${token.name} = ${token.result!!}" }
            }
        }

        for (obj in model.subObjects) {
            logger.debug { "${obj.name} = ${obj.value()}" }
        }

        return Ok(success)
    }

    open fun logTaskCost(
        iteration: UInt64,
        model: LinearMetaModel
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

        return Ok(success)
    }

    fun flush(
        iteration: UInt64,
        tasks: List<AbstractTask<E, A>>,
        lockCancelTasks: Set<AbstractTask<E, A>> = emptySet()
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
        return Ok(success)
    }

    private fun extractTasks(
        iteration: UInt64,
        model: LinearMetaModel,
        predicate: (Flt64) -> Boolean
    ): Ret<Map<AbstractTask<E, A>, UInt64>> {
        val ret = HashMap<AbstractTask<E, A>, UInt64>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token.belongsTo(xi)) {
                    val task = tasksIteration[i][token.variable.index]
                    assert(!removedTasks.contains(task))
                    ret[task] = UInt64(i)
                }
            }
        }
        return Ok(ret)
    }
}

open class IterativeTaskSchedulingAggregation<E : Executor, A : AssignmentPolicy<E>>(
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    override val policy: Policy<E, A>,
    lockCancelTask: Set<AbstractTask<E, A>> = emptySet()
) : AbstractIterativeTaskSchedulingAggregation<E, A>(tasks, executors, lockCancelTask)

open class IterativeTaskSchedulingAggregationWithTime<E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<AbstractTask<E, A>>,
    executors: List<E>,
    override val policy: Policy<E, A>,
    lockCancelTasks: Set<AbstractTask<E, A>> = emptySet(),
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractIterativeTaskSchedulingAggregation<E, A>(tasks, executors, lockCancelTasks) {
    val taskTime: IterativeTaskSchedulingTaskTime<E, A> =
        IterativeTaskSchedulingTaskTime(timeWindow, tasks, compilation, redundancyRange)
    val makespan: Makespan<E, A> = Makespan(tasks, taskTime, makespanExtra)

    override fun register(model: LinearMetaModel): Try {
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

        return Ok(success)
    }

    override suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<AbstractTask<E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTask<E, A>>> {
        val unduplicatedBunches = when (val result = super.addColumns(iteration, newBunches, model)) {
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
