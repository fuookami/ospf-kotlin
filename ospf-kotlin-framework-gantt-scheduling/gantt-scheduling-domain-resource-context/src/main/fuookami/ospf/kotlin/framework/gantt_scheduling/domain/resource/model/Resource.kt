package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface AbstractResourceCapacity {
    val time: TimeRange
    val quantity: ValueRange<Flt64>
    val lessQuantity: Flt64? get() = null
    val overQuantity: Flt64? get() = null
    val interval: Duration
    val name: String? get() = null
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

open class ResourceCapacity(
    override val time: TimeRange,
    override val quantity: ValueRange<Flt64>,
    override val lessQuantity: Flt64? = null,
    override val overQuantity: Flt64? = null,
    override val interval: Duration = Duration.INFINITE,
    override val name: String? = null
): AbstractResourceCapacity {
    override fun toString() = name ?: "${quantity}_${interval}"
}

abstract class Resource<out C : AbstractResourceCapacity> : ManualIndexed() {
    abstract val id: String
    abstract val name: String
    abstract val capacities: List<C>
    abstract val initialQuantity: Flt64

    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A>,
        time: TimeRange
    ): Flt64
}

interface ResourceTimeSlot<
    out R : Resource<C>,
    out C : AbstractResourceCapacity
> : TimeSlot, Indexed {
    val origin: TimeSlot
    val resource: R
    val resourceCapacity: C
    override val time: TimeRange get() = origin.time
    val indexInRule: UInt64

    fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        return relationTo(prevTask, task) neq Flt64.zero
    }

    fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Flt64 {
        return Flt64.zero
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(
        bunch: AbstractTaskBunch<T, E, A>
    ): Flt64 {
        return resource.usedQuantity(bunch, time)
    }
}

interface ResourceUsage<
    out S : ResourceTimeSlot<R, C>,
    out R : Resource<C>,
    out C : AbstractResourceCapacity
> {
    val name: String

    val timeSlots: List<S>
    val quantity: LinearIntermediateSymbols1
    val overQuantity: LinearIntermediateSymbols1
    val lessQuantity: LinearIntermediateSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: MetaModel): Try
}

abstract class AbstractResourceUsage<
    out S : ResourceTimeSlot<R, C>,
    out R : Resource<C>,
    out C : AbstractResourceCapacity
> : ResourceUsage<S, R, C> {
    override lateinit var overQuantity: LinearIntermediateSymbols1
    override lateinit var lessQuantity: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (overEnabled) {
                if (!::overQuantity.isInitialized) {
                    overQuantity = LinearIntermediateSymbols1(
                        name = "${name}_over_quantity",
                        shape = Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.overEnabled) {
                            val slack = SlackFunction(
                                x = quantity[slot],
                                threshold = slot.resourceCapacity.quantity.upperBound.value.unwrap(),
                                type = UContinuous,
                                constraint = false,
                                name = "${name}_over_quantity_$slot"
                            )
                            slot.resourceCapacity.overQuantity?.let {
                                slack.pos!!.range.leq(it)
                            }
                            slack
                        } else {
                            LinearIntermediateSymbol.empty(
                                name = "${name}_over_quantity_$slot"
                            )
                        }
                    }
                }
                when (val result = model.add(overQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (lessEnabled) {
                if (!::lessQuantity.isInitialized) {
                    lessQuantity = LinearIntermediateSymbols1(
                        name = "${name}_less_quantity",
                        shape = Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.lessEnabled) {
                            val slack = SlackFunction(
                                x = quantity[slot],
                                threshold = slot.resourceCapacity.quantity.lowerBound.value.unwrap(),
                                withPositive = false,
                                constraint = false,
                                name = "${name}_less_quantity_$slot"
                            )
                            slot.resourceCapacity.lessQuantity?.let {
                                slack.neg!!.range.leq(it)
                            }
                            slack
                        } else {
                            LinearIntermediateSymbol.empty(
                                name = "${name}_less_quantity_$slot"
                            )
                        }
                    }
                }
                when (val result = model.add(lessQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return ok
    }
}
