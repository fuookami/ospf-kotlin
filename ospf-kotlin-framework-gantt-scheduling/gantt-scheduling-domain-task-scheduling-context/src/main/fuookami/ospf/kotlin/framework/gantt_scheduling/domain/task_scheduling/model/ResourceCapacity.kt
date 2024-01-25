package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class ResourceTimeSlot<R : Resource<C>, C : ResourceCapacity>(
    val resource: R,
    val capacity: C,
    val time: TimeRange,
    val indexInRule: UInt64
) : AutoIndexed(ResourceTimeSlot::class) {
    val quantity by capacity::quantity
    val interval by capacity::interval

    operator fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(
        bunch: AbstractTaskBunch<T, E, A>
    ): Flt64 {
        return resource.usedQuantity(bunch, time)
    }
}

interface ResourceUsage<R : Resource<C>, C : ResourceCapacity> {
    val timeSlots: List<ResourceTimeSlot<R, C>>

    val lessUsageEnabled: Boolean
    val overUsageEnabled: Boolean

    val usage: LinearSymbols1
    val lessUsage: LinearSymbols1
    val overUsage: LinearSymbols1

    fun register(model: LinearMetaModel): Try
}

open class TaskSchedulingExecutionResourceUsage<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, R : ExecutionResource<C>, C : ResourceCapacity>(
    private val tasks: List<T>,
    private val resources: List<R>,
    private val compilation: Compilation,
    val resourceCategory: String,
    override val overUsageEnabled: Boolean = false,
    override val lessUsageEnabled: Boolean = false
) : ResourceUsage<R, C> {
    final override val timeSlots: List<ResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<ResourceTimeSlot<R, C>>()
        val timeSlots : MutableList<ResourceTimeSlot<R, C>> = ArrayList()
        this.timeSlots = timeSlots
    }

    private lateinit var u: URealVariable2
    override lateinit var usage: LinearSymbols1
    override lateinit var overUsage: LinearSymbols1
    override lateinit var lessUsage: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (!::u.isInitialized) {
            u = URealVariable2("u_$resourceCategory", Shape2(tasks.size, resources.size))
            for (task in tasks) {
                for (slot in timeSlots) {
                    u[task, slot].name = "${u.name}_${task}_${slot.index}"
                }
            }
        }
        model.addVars(u)

        if (!::usage.isInitialized) {
            usage = flatMap(
                "${resourceCategory}_usage",
                timeSlots,
                { s ->
                    sum(tasks
                        .filter { it.time != null && !it.advanceEnabled && !it.delayEnabled }
                        .sortedBy { it.time!!.start }
                        .map { u[it, s] }
                    )
                },
                { (_, s) -> "${s.index}" }
            )
        }
        model.addSymbols(usage)

        if (overUsageEnabled) {
            if (!::overUsage.isInitialized) {
                overUsage = LinearSymbols1(
                    "${resourceCategory}_over_usage",
                    Shape1(timeSlots.size)
                ) { (i, _) ->
                    val slot = timeSlots[i]
                    if (slot.capacity.overEnabled) {
                        SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(usage[slot]),
                            threshold = LinearPolynomial(slot.quantity.upperBound.toFlt64()),
                            name = "${resourceCategory}_over_usage_${slot.index}"
                        )
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "${resourceCategory}_over_usage_${slot.index}")
                    }
                }
            }
            model.addSymbols(overUsage)
        }

        if (lessUsageEnabled) {
            if (!::lessUsage.isInitialized) {
                lessUsage = LinearSymbols1(
                    "${resourceCategory}_less_usage",
                    Shape1(timeSlots.size)
                ) { (i, _) ->
                    val slot = timeSlots[i]
                    if (slot.capacity.overEnabled) {
                        SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(usage[slot]),
                            threshold = LinearPolynomial(slot.quantity.upperBound.toFlt64()),
                            withPositive = false,
                            name = "${resourceCategory}_less_usage_${slot.index}"
                        )
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "${resourceCategory}_less_usage_${slot.index}")
                    }
                }
            }
            model.addSymbols(lessUsage)
        }

        return Ok(success)
    }
}

//open class ResourceCapacity<E : Executor>(
//    timeWindow: TimeWindow,
//    resources: List<Resource<E>>
//) {
//    data class TimeSlot<E : Executor>(
//        val resource: Resource<E>,
//        val resourceCapacity: ResourceCapacity,
//        val time: TimeRange,
//        val indexInRule: UInt64,
//    ) : AutoIndexed(TimeSlot::class) {
//        val capacity get() = resourceCapacity.amount
//        val interval get() = resourceCapacity.interval
//
//        operator fun invoke(prevTask: Task<E>?, task: Task<E>?): Boolean {
//            return resource.usedBy(prevTask, task, time)
//        }
//
//        operator fun invoke(bunch: TaskBunch<E>): UInt64 {
//            return resource.usedTime(bunch, time)
//        }
//
//        override fun toString() = "${resource}_${((time.start - resourceCapacity.time.start) / interval).roundToInt()}"
//    }
//
//    val timeSlots: List<TimeSlot<E>>
//    lateinit var usageAmount: LinearSymbols1
//    lateinit var m: UIntVariable1
//
//    init {
//        AutoIndexed.flush<TimeSlot<E>>()
//
//        val timeSlots = ArrayList<TimeSlot<E>>()
//        for (resource in resources) {
//            var index = UInt64.zero
//            for (resourceCapacity in resource.capacities) {
//                var beginTime = maxOf(resourceCapacity.time.start, timeWindow.window.start)
//                val endTime = minOf(resourceCapacity.time.end, timeWindow.window.end)
//                while (beginTime <= endTime) {
//                    val thisInterval = minOf(endTime - beginTime, resourceCapacity.interval)
//                    val time = TimeRange(beginTime, beginTime + thisInterval)
//                    timeSlots.add(TimeSlot(resource, resourceCapacity, time, index))
//                    beginTime += resourceCapacity.interval
//                }
//                ++index
//            }
//        }
//        this.timeSlots = timeSlots
//    }
//
//    open fun register(model: LinearMetaModel): Try {
//        if (timeSlots.isNotEmpty()) {
//            if (!this::usageAmount.isInitialized) {
//                usageAmount = LinearSymbols1("usage_amount", Shape1(timeSlots.size))
//                for (timeSlot in timeSlots) {
//                    usageAmount[timeSlot] = LinearExpressionSymbol(LinearPolynomial(), "${usageAmount.name}_${timeSlot}")
//                }
//            }
//
//            if (!this::m.isInitialized) {
//                m = UIntVariable1("m", Shape1(timeSlots.size))
//                for (timeSlot in timeSlots) {
//                    m[timeSlot]!!.name = "${m.name}_${timeSlot}"
//                    m[timeSlot]!!.range.leq(timeSlot.capacity)
//                }
//            }
//        }
//
//        return Ok(success)
//    }
//
//    @OptIn(ObsoleteCoroutinesApi::class, DelicateCoroutinesApi::class)
//    open fun addColumns(
//        iteration: UInt64,
//        bunches: List<TaskBunch<E>>,
//        compilation: Compilation<E>,
//        compilationAddingPromise: BroadcastChannel<Boolean>? = null,
//        scope: CoroutineScope? = compilationAddingPromise?.let { GlobalScope }
//    ): Ret<BroadcastChannel<Boolean>?> {
//        assert(bunches.isNotEmpty())
//
//        val xi = compilation.x[iteration.toInt()]
//
//        for (timeSlot in timeSlots) {
//            for (bunch in bunches) {
//                val amount = timeSlot(bunch)
//                if (amount != UInt64.zero) {
//                    val usageAmount = usageAmount[timeSlot]!! as LinearExpressionSymbol
//                    usageAmount.flush()
//                    (usageAmount.polynomial as LinearPolynomial) += amount * xi[bunch]!!
//                }
//            }
//        }
//
//        return if (scope != null) {
//            val promise = BroadcastChannel<Boolean>(Channel.BUFFERED)
//            scope.launch {
//                compilationAddingPromise?.openSubscription()?.receive()
//                flush()
//                promise.send(true)
//            }
//            Ok(promise)
//        } else {
//            flush()
//            Ok(null)
//        }
//    }
//
//    private fun flush() {
//        for (timeSlot in timeSlots) {
//            (usageAmount[timeSlot] as LinearExpressionSymbol).cells
//        }
//    }
//}
