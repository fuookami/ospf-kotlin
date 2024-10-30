package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

abstract class AbstractBunchCompilationAggregation<
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    protected val tasks: List<T>,
    protected val executors: List<E>,
    protected val lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true
) {
    private val logger = org.apache.logging.log4j.kotlin.logger("BunchSchedulingAggregation")

    val compilation: BunchCompilation<B, T, E, A> = BunchCompilation(tasks, executors, lockCancelTasks, withExecutorLeisure)

    val bunchesIteration: List<List<B>> by compilation::bunchesIteration
    val bunches: List<B> by compilation::bunches
    val removedBunches: Set<B> by compilation::removedBunches
    val lastIterationBunches: List<B> by compilation::lastIterationBunches

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
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<List<B>> {
        val unduplicatedBunches = when (val result = compilation.addColumns(
            iteration = iteration,
            newBunches = newBunches,
            model = model
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedBunches)
    }

    open fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        reducedCost: (B) -> Flt64,
        fixedBunches: Set<B>,
        keptBunches: Set<B>,
        model: AbstractLinearMetaModel
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

    open fun extractFixedBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Ret<Set<B>> {
        return extractBunches(iteration, model) { it eq Flt64.one }
    }

    open fun extractKeptBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Ret<Set<B>> {
        return extractBunches(iteration, model) { it gr Flt64.zero }
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

    open fun globallyFix(fixedBunches: Set<B>): Try {
        for (bunch in fixedBunches) {
            assert(!removedBunches.contains(bunch))
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch].range.eq(true)
        }
        return ok
    }

    open fun locallyFix(
        iteration: UInt64,
        bar: Flt64,
        fixedBunches: Set<B>,
        model: AbstractLinearMetaModel
    ): Ret<Set<B>> {
        var flag = true
        val ret = HashSet<B>()

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

                    if (token.belongsTo(xi)) {
                        val bunch = bunchesIteration[i.toInt()][token.variable.index]
                        assert(!removedBunches.contains(bunch))

                        if ((token.result != null)
                            && (token.result!! geq bestValue)
                            && !fixedBunches.contains(bunch)
                        ) {
                            bestValue = token.result!!
                            bestIteration = i
                            bestIndex = token.variable.index
                        }
                        if ((token.result != null)
                            && (token.result!! geq bar)
                            && !fixedBunches.contains(bunch)
                        ) {
                            ret.add(bunch)
                            xi[token.variable.index].range.eq(true)
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

    open fun logBunchCost(
        iteration: UInt64,
        model: AbstractLinearMetaModel
    ): Try {
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
                y[task].range.set(ValueRange(Binary.minimum, Binary.maximum).value!!)
            }
        }
        for (i in UInt64.zero..iteration) {
            val xi = compilation.x[i.toInt()]

            for (bunch in bunchesIteration[i.toInt()]) {
                if (!removedBunches.contains(bunch)) {
                    xi[bunch].range.set(ValueRange(Binary.minimum, Binary.maximum).value!!)
                }
            }
        }
        return ok
    }

    private fun extractBunches(
        iteration: UInt64,
        model: AbstractLinearMetaModel,
        predicate: (Flt64) -> Boolean
    ): Ret<Set<B>> {
        val ret = HashSet<B>()
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

open class BunchCompilationAggregation<
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true
) : AbstractBunchCompilationAggregation<B, T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure)

open class BunchCompilationAggregationWithTime<
    B : AbstractTaskBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    timeWindow: TimeWindow,
    tasks: List<T>,
    executors: List<E>,
    lockCancelTasks: Set<T> = emptySet(),
    withExecutorLeisure: Boolean = true,
    redundancyRange: Duration? = null,
    makespanExtra: Boolean = false
) : AbstractBunchCompilationAggregation<B, T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure) {
    val taskTime: BunchSchedulingTaskTime<B, T, E, A> =
        BunchSchedulingTaskTime(timeWindow, tasks, compilation, redundancyRange)
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
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<List<B>> {
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
