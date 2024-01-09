package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model

import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.ResourceCapacity
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

open class ResourceCapacity<E : Executor>(
    timeWindow: TimeWindow,
    resources: List<Resource<E>>
) {
    data class TimeSlot<E : Executor>(
        val resource: Resource<E>,
        val resourceCapacity: ResourceCapacity,
        val time: TimeRange,
        val indexInRule: UInt64,
    ) : AutoIndexed(TimeSlot::class) {
        val capacity get() = resourceCapacity.amount
        val interval get() = resourceCapacity.interval

        operator fun invoke(prevTask: Task<E>?, task: Task<E>?): Boolean {
            return resource.usedBy(prevTask, task, time)
        }

        operator fun invoke(bunch: TaskBunch<E>): UInt64 {
            return resource.usedTime(bunch, time)
        }

        override fun toString() = "${resource}_${((time.start - resourceCapacity.time.start) / interval).roundToInt()}"
    }

    val timeSlots: List<TimeSlot<E>>
    lateinit var usageAmount: LinearSymbols1
    lateinit var m: UIntVariable1

    init {
        AutoIndexed.flush<TimeSlot<E>>()

        val timeSlots = ArrayList<TimeSlot<E>>()
        for (resource in resources) {
            var index = UInt64.zero
            for (resourceCapacity in resource.capacities) {
                var beginTime = maxOf(resourceCapacity.time.start, timeWindow.window.start)
                val endTime = minOf(resourceCapacity.time.end, timeWindow.window.end)
                while (beginTime <= endTime) {
                    val thisInterval = minOf(endTime - beginTime, resourceCapacity.interval)
                    val time = TimeRange(beginTime, beginTime + thisInterval)
                    timeSlots.add(TimeSlot(resource, resourceCapacity, time, index))
                    beginTime += resourceCapacity.interval
                }
                ++index
            }
        }
        this.timeSlots = timeSlots
    }

    open fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!this::usageAmount.isInitialized) {
                usageAmount = LinearSymbols1("usage_amount", Shape1(timeSlots.size))
                for (timeSlot in timeSlots) {
                    usageAmount[timeSlot] = LinearSymbol(LinearPolynomial(), "${usageAmount.name}_${timeSlot}")
                }
            }

            if (!this::m.isInitialized) {
                m = UIntVariable1("m", Shape1(timeSlots.size))
                for (timeSlot in timeSlots) {
                    m[timeSlot]!!.name = "${m.name}_${timeSlot}"
                    m[timeSlot]!!.range.leq(timeSlot.capacity)
                }
            }
        }

        return Ok(success)
    }

    @OptIn(ObsoleteCoroutinesApi::class, DelicateCoroutinesApi::class)
    open fun addColumns(
        iteration: UInt64,
        bunches: List<TaskBunch<E>>,
        compilation: Compilation<E>,
        compilationAddingPromise: BroadcastChannel<Boolean>? = null,
        scope: CoroutineScope? = compilationAddingPromise?.let { GlobalScope }
    ): Ret<BroadcastChannel<Boolean>?> {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        for (timeSlot in timeSlots) {
            for (bunch in bunches) {
                val amount = timeSlot(bunch)
                if (amount != UInt64.zero) {
                    val usageAmount = usageAmount[timeSlot]!! as LinearSymbol
                    usageAmount.flush()
                    (usageAmount.polynomial as LinearPolynomial) += amount * xi[bunch]!!
                }
            }
        }

        return if (scope != null) {
            val promise = BroadcastChannel<Boolean>(Channel.BUFFERED)
            scope.launch {
                compilationAddingPromise?.openSubscription()?.receive()
                flush()
                promise.send(true)
            }
            Ok(promise)
        } else {
            flush()
            Ok(null)
        }
    }

    private fun flush() {
        for (timeSlot in timeSlots) {
            (usageAmount[timeSlot] as LinearSymbol).cells
        }
    }
}
