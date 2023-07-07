package fuookami.ospf.kotlin.framework.gantt_scheduling.cg

import kotlin.time.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.ResourceCapacity

open class BunchCompilationAggregation<E : Executor>(
    val withExecutorLeisure: Boolean = true,
    val taskTimeNeeded: Boolean = false,
    val redundancyRange: Duration? = null,
    val makespanExtra: Boolean = false
) {
    open val compilation: Compilation<E> = Compilation(withExecutorLeisure)
    open lateinit var taskTime: TaskTime<E>
    open lateinit var makespan: Makespan<E>
    open lateinit var resourceCapacity: ResourceCapacity<E>

    private val logger = logger()

    val bunches: MutableList<TaskBunch<E>> = ArrayList()
    val removedBunches: HashSet<TaskBunch<E>> = HashSet()
    val bunchGroups: MutableList<List<TaskBunch<E>>> = ArrayList()
    fun bunches(iteration: UInt64): List<TaskBunch<E>> {
        return bunchGroups[iteration.toInt()]
    }

    open fun register(
        timeWindow: TimeWindow,
        tasks: List<Task<E>>,
        executors: List<E>,
        resources: List<Resource<E>>,
        lockCancelTasks: Set<Task<E>> = emptySet(),
        model: LinearMetaModel
    ): Try<Error> {
        when (val result = compilation.register(tasks, executors, lockCancelTasks, model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }
        if (taskTimeNeeded) {
            if (!this::taskTime.isInitialized) {
                taskTime = TaskTime(redundancyRange)
            }
            when (val result = taskTime.register(timeWindow, tasks, model)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!this::makespan.isInitialized) {
                makespan = Makespan(makespanExtra)
            }
            when (val result = makespan.register(tasks, taskTime, model)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (!this::resourceCapacity.isInitialized) {
            resourceCapacity = ResourceCapacity(timeWindow, resources)
        }
        when (val result = resourceCapacity.register(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    open suspend fun addColumns(
        iteration: UInt64,
        timeWindow: TimeWindow,
        thisBunches: List<TaskBunch<E>>,
        tasks: List<Task<E>>,
        executors: List<E>,
        model: LinearMetaModel,
        multiThread: Boolean = false
    ): Try<Error> {
        bunches.addAll(thisBunches)
        bunchGroups.add(bunches)

        return if (multiThread) {
            coroutineScope {
                val compilationPromise = when (val result = compilation.addColumns(iteration, bunches, tasks, executors, model, this)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return@coroutineScope Failed(result.error)
                    }
                }
                val taskTimePromise = when (val result = taskTime.addColumns(iteration, timeWindow, bunches, tasks, compilation, compilationPromise, this)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return@coroutineScope Failed(result.error)
                    }
                }
                val resourceCapacityPromise = when (val result = resourceCapacity.addColumns(iteration, bunches, compilation, compilationPromise, this)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return@coroutineScope Failed(result.error)
                    }
                }
                return@coroutineScope Ok(success)
            }
        } else {
            when (val result = compilation.addColumns(iteration, bunches, tasks, executors, model)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = taskTime.addColumns(iteration, timeWindow, bunches, tasks, compilation)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = resourceCapacity.addColumns(iteration, bunches, compilation)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }
            }
            Ok(success)
        }
    }

    fun removeColumns(
        maximumReducedCost: Flt64,
        maximumColumnAmount: UInt64,
        shadowPriceMap: ShadowPriceMap<E>,
        fixedBunches: Set<TaskBunch<E>>,
        keptBunches: Set<TaskBunch<E>>,
        model: LinearMetaModel
    ): Result<Flt64, Error> {
        for (bunch in bunches) {
            if (removedBunches.contains(bunch)) {
                continue
            }

            val reducedCost = shadowPriceMap.reducedCost(bunch)
            if (!(reducedCost ls maximumReducedCost)
                && !fixedBunches.contains(bunch)
                && !keptBunches.contains(bunch)
            ) {
                removedBunches.add(bunch)
            }
        }

        for (bunch in removedBunches) {
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch]!!.range.eq(UInt8.zero)
            model.remove(xi[bunch]!!)
        }

        val remainingAmount = UInt64((bunches.size - removedBunches.size).toULong())
        return if (remainingAmount > maximumColumnAmount) {
            Ok(max((maximumReducedCost.floor().toInt64() * Int64(2L) / Int64(3L)).toFlt64(), Flt64(5.0)))
        } else {
            Ok(maximumReducedCost)
        }
    }

    fun extractFixedBunches(iteration: UInt64, model: LinearMetaModel): Result<Set<TaskBunch<E>>, Error> {
        return extractBunches(iteration, model) { it eq Flt64.one }
    }

    fun extractKeptBunches(iteration: UInt64, model: LinearMetaModel): Result<Set<TaskBunch<E>>, Error> {
        return extractBunches(iteration, model) { it eq Flt64.zero }
    }

    fun extractHiddenExecutors(executors: List<E>, model: LinearMetaModel): Result<Set<E>, Error> {
        val z = compilation.z
        val ret = HashSet<E>()
        for (token in model.tokens.tokens) {
            if (token.variable.identifier == z.identifier) {
                if (token.result!! gr Flt64.zero) {
                    ret.add(executors[token.variable.index])
                }
            }
        }
        return Ok(ret)
    }

    fun globallyFix(fixedBunches: Set<TaskBunch<E>>): Try<Error> {
        for (bunch in fixedBunches) {
            assert(!removedBunches.contains(bunch))
            val xi = compilation.x[bunch.iteration.toInt()]
            xi[bunch]!!.range.eq(UInt8.one)
        }
        return Ok(success)
    }

    fun locallyFix(iteration: UInt64, bar: Flt64, fixedBunches: Set<TaskBunch<E>>, model: LinearMetaModel): Result<Set<TaskBunch<E>>, Error> {
        var flag = true
        val ret = HashSet<TaskBunch<E>>()

        var bestValue = Flt64.zero
        var bestIteration = UInt64.zero
        var bestIndex = 0

        val y = compilation.y
        for (token in model.tokens.tokens) {
            if (token.variable.identifier == y.identifier
                && (token.result!! gr bar)
            ) {
                y[token.variable.index]!!.range.eq(UInt8.one)
                flag = false
            }

            if (token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable.identifier == xi.identifier) {
                        val bunch = bunches(i)[token.variable.index]
                        assert(!removedBunches.contains(bunch))

                        if (token.result != null && (token.result!! geq bestValue) && !fixedBunches.contains(bunch)) {
                            bestValue = token.result!!
                            bestIteration = i
                            bestIndex = token.variable.index
                        }
                        if (token.result != null && (token.result!! geq bar) && !fixedBunches.contains(bunch)) {
                            ret.add(bunch)
                            xi[token.variable.index]!!.range.eq(UInt8.one)
                        }
                    }
                }
            }
        }

        // if not fix any one bunch or cancel any flight
        // fix the best if the value greater than 1e-3
        if (flag && ret.isEmpty() && (bestValue geq Flt64(1e-3))) {
            val xi = compilation.x[bestIteration.toInt()][bestIndex]!!
            ret.add(bunches(bestIteration)[bestIndex])
            xi.range.eq(UInt8.one)
        }

        return Ok(ret)
    }

    fun logResult(iteration: UInt64, model: LinearMetaModel): Try<Error> {
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

    fun logBunchCost(iteration: UInt64, model: LinearMetaModel): Try<Error> {
        for (token in model.tokens.tokens) {
            if ((token.result!! eq Flt64.one) && token.name.startsWith("x")) {
                for (i in UInt64.zero..iteration) {
                    val xi = compilation.x[i.toInt()]

                    if (token.variable.identifier == xi.identifier) {
                        val bunch = bunches(i)[token.variable.index]
                        logger.debug { "${bunch.executor} cost: ${bunch.cost.sum!!}" }
                        break
                    }
                }
            }
        }

        return Ok(success)
    }

    fun flush(iteration: UInt64, tasks: List<Task<E>>, lockCancelTasks: Set<Task<E>> = emptySet()): Try<Error> {
        val y = compilation.y
        for (task in tasks) {
            if (task.cancelEnabled && !lockCancelTasks.contains(task.originTask)) {
                y[task]!!.range.set(ValueRange(Binary.minimum, Binary.maximum, UInt8))
            }

            for (i in UInt64.zero..iteration) {
                val xi = compilation.x[i.toInt()]

                for (bunch in bunches(i)) {
                    if (!removedBunches.contains(bunch)) {
                        xi[bunch]!!.range.set(ValueRange(Binary.minimum, Binary.maximum, UInt8))
                    }
                }
            }
        }
        return Ok(success)
    }

    private fun extractBunches(iteration: UInt64, model: LinearMetaModel, predicate: (Flt64) -> Boolean): Result<Set<TaskBunch<E>>, Error> {
        val ret = HashSet<TaskBunch<E>>()
        for (token in model.tokens.tokens) {
            if (!predicate(token.result!!)) {
                continue
            }

            for (i in 0..iteration.toInt()) {
                val xi = compilation.x[i]

                if (token.variable.identifier == xi.identifier) {
                    val bunch = bunchGroups[i][token.variable.index]
                    assert(!removedBunches.contains(bunch))
                    ret.add(bunch)
                }
            }
        }
        return Ok(ret)
    }
}
