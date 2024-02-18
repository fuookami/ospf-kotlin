package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling

import kotlin.time.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

abstract class AbstractBunchSchedulingAggregation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = false
) {
    private val logger = logger()

    val compilation: BunchCompilation<T, E, A> =
        BunchCompilation(tasks, executors, lockCancelTasks, withExecutorLeisure)

    val bunchesIteration: List<List<AbstractTaskBunch<T, E, A>>> by compilation::bunchesIteration
    val bunches: List<AbstractTaskBunch<T, E, A>> by compilation::bunches
    val removedBunches: Set<AbstractTaskBunch<T, E, A>> by compilation::removedBunches
    val lastIterationBunches: List<AbstractTaskBunch<T, E, A>> by compilation::lastIterationBunches

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
        newBunches: List<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTaskBunch<T, E, A>>> {
        val unduplicatedBunches = when (val result = compilation.addColumns(iteration, newBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedBunches)
    }

    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (AbstractTaskBunch<T, E, A>) -> Flt64,
        fixedBunches: Set<AbstractTaskBunch<T, E, A>>,
        keptBunches: Set<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<Flt64> {
        for (bunch in bunches) {
            if (removedBunches.contains(bunch)) {
                continue
            }

            if (!(reducedCost(bunch) ls maximumReducedCost)
                && !fixedBunches.contains(bunch)
                && !keptBunches.contains(bunch)
            ) {
                compilation.aggregation.removeColumn(bunch)
            }
        }

        for (bunch in removedBunches) {
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch].range.eq(false)
            model.remove(xi[bunch])
        }

        val remainingAmount = UInt64((bunches.size - removedBunches.size).toULong())
        return if (remainingAmount > maximumColumnAmount) {
            Ok(max((maximumReducedCost.floor().toInt64() * Int64(2L) / Int64(3L)).toFlt64(), Flt64(5.0)))
        } else {
            Ok(maximumReducedCost)
        }
    }

    fun extractFixedBunches(iteration: UInt64, model: LinearMetaModel): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        return extractBunches(iteration, model) { it eq Flt64.one }
    }

    fun extractKeptBunches(iteration: UInt64, model: LinearMetaModel): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        return extractBunches(iteration, model) { it gr Flt64.zero }
    }

    fun extractHiddenExecutors(executors: List<E>, model: LinearMetaModel): Ret<Set<E>> {
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

    fun globallyFix(fixedBunches: Set<AbstractTaskBunch<T, E, A>>): Try {
        for (bunch in fixedBunches) {
            assert(!removedBunches.contains(bunch))
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch].range.eq(true)
        }
        return Ok(success)
    }

    fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        var flag = true
        val ret = HashSet<AbstractTaskBunch<T, E, A>>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token.belongsTo(y) && (token.result!! gr bar)) {
                y[token.variable.index].range.eq(true)
                flag = false
            }

            if (token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.belongsTo(xi)) {
                        val bunch = bunchesIteration[i.toInt()][token.variable.index]
                        assert(!removedBunches.contains(bunch))

                        if (token.result != null && (token.result!! geq bestValue) && !fixedBunches.contains(bunch)) {
                            bestValue = token.result!!
                            bestIteration = i
                            bestIndex = token.variable.index
                        }
                        if (token.result != null && (token.result!! geq bar) && !fixedBunches.contains(bunch)) {
                            ret.add(bunch)
                            xi[token.variable.index].range.eq(UInt8.one)
                        }
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any task
        // fix the best if the value greater than 1e-3
        if (flag && ret.isEmpty() && (bestValue geq Flt64(1e-3))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]
            ret.add(bunchesIteration[bestIteration.toInt()][bestIndex])
            xi.range.eq(UInt8.one)
        }

        return Ok(ret)
    }

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try {
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

    fun logBunchCost(iteration: UInt64, model: LinearMetaModel): Try {
        for (token in model.tokens.tokens) {
            if ((token.result!! eq Flt64.one) && token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable.belongsTo(xi)) {
                        val bunch = bunchesIteration[i.toInt()][token.variable.index]
                        logger.debug { "${bunch.executor} cost: ${bunch.cost.sum!!}" }
                        break
                    }
                }
            }
        }

        return Ok(success)
    }

    fun flush(iteration: UInt64, tasks: List<T>, lockCancelTasks: Set<T> = emptySet()): Try {
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
                y[task].range.set(ValueRange(Binary.minimum, Binary.maximum, UInt8))
            }

            for (i in UInt64.zero..iteration) {
                val xi = compilation.x[i.toInt()]

                for (bunch in bunchesIteration[i.toInt()]) {
                    if (!removedBunches.contains(bunch)) {
                        xi[bunch].range.set(ValueRange(Binary.minimum, Binary.maximum, UInt8))
                    }
                }
            }
        }
        return Ok(success)
    }

    private fun extractBunches(
        iteration: UInt64,
        model: LinearMetaModel,
        predicate: (Flt64) -> Boolean
    ): Ret<Set<AbstractTaskBunch<T, E, A>>> {
        val ret = HashSet<AbstractTaskBunch<T, E, A>>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token.belongsTo(xi)) {
                    val bunch = bunchesIteration[i][token.variable.index]
                    assert(!removedBunches.contains(bunch))
                    ret.add(bunch)
                }
            }
        }
        return Ok(ret)
    }
}

open class BunchSchedulingAggregation<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = false
) : AbstractBunchSchedulingAggregation<T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure)

open class BunchCompilationAggregationWithTime<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = false,
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractBunchSchedulingAggregation<T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure) {
    val taskTime: BunchSchedulingTaskTime<T, E, A> =
        BunchSchedulingTaskTime(timeWindow, tasks, compilation, redundancyRange)
    val makespan: Makespan<T, E, A> = Makespan(tasks, taskTime, makespanExtra)

    private val logger = logger()

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
        newBunches: List<AbstractTaskBunch<T, E, A>>,
        model: LinearMetaModel
    ): Ret<List<AbstractTaskBunch<T, E, A>>> {
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
