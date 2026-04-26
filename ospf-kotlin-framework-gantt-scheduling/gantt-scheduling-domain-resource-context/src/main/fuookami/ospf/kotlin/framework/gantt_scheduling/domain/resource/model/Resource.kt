@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelF64
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.refresh
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration

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
) : AbstractResourceCapacity {
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

    fun register(model: MetaModelF64): Try
}

abstract class AbstractResourceUsage<
        out S : ResourceTimeSlot<R, C>,
        out R : Resource<C>,
        out C : AbstractResourceCapacity
        > : ResourceUsage<S, R, C> {
    override lateinit var overQuantity: LinearIntermediateSymbols1
    override lateinit var lessQuantity: LinearIntermediateSymbols1

    override fun register(model: MetaModelF64): Try {
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
                                @Suppress("UNCHECKED_CAST")
                                (slack.helperVariables.last().range as ExpressionRange<Flt64>).setUb(it)
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

                    is Fatal -> {
                        return Fatal(result.errors)
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
                                @Suppress("UNCHECKED_CAST")
                                (slack.helperVariables.first().range as ExpressionRange<Flt64>).setUb(it)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }

    /**
     * 提取影子价格
     * Extract shadow prices from slack variables
     *
     * @param Map              影子价格表类�?
     * @param shadowPriceMap   影子价格�?/ Shadow price map
     * @param shadowPrices     原始影子价格（对偶变量的解）/ Raw shadow prices (dual solution)
     * @return                 成功与否 / Success or failure
     */
    fun <Map : AbstractShadowPriceMap<*, Map>> refresh(
        shadowPriceMap: Map,
        shadowPrices: MetaDualSolution
    ): Try {
        if (::overQuantity.isInitialized) {
            for (overQuantity in this.overQuantity) {
                when (val result = overQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
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

        if (::lessQuantity.isInitialized) {
            for (lessQuantity in this.lessQuantity) {
                when (val result = lessQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
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

        return ok
    }
}




