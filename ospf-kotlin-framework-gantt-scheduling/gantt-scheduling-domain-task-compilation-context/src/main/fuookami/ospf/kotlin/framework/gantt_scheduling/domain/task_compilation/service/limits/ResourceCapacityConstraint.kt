package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data class ResourceCapacityShadowPriceKey<R : Resource<C>, C : ResourceCapacity>(
    val slot: ResourceTimeSlot<R, C>
) : ShadowPriceKey(ResourceCapacityShadowPriceKey::class)

class ResourceCapacityConstraint<
    Args : GanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    S : ResourceTimeSlot<R, C>,
    R : StorageResource<C>,
    C : ResourceCapacity
>(
    private val usage: ResourceUsage<S, R, C>,
    override val name: String = "${usage.name}_resource_capacity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (slot in usage.timeSlots) {
            if (usage.overEnabled && slot.resourceCapacity.overEnabled) {
                when (val overQuantity = usage.overQuantity[slot]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            overQuantity.polyX leq slot.resourceCapacity.quantity.upperBound.value.unwrap(),
                            "${name}_ub_$slot"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] leq slot.resourceCapacity.quantity.upperBound.value.unwrap(),
                            "${name}_ub_$slot"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    usage.quantity[slot] leq slot.resourceCapacity.quantity.upperBound.value.unwrap(),
                    "${usage.name}_${name}_ub_$slot"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (usage.lessEnabled && slot.resourceCapacity.lessEnabled) {
                when (val lessQuantity = usage.lessQuantity[slot]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            lessQuantity.polyX geq slot.resourceCapacity.quantity.lowerBound.value.unwrap(),
                            "${name}_lb_$slot"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] geq slot.resourceCapacity.quantity.lowerBound.value.unwrap(),
                            "${name}_lb_$slot"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    usage.quantity[slot] geq slot.resourceCapacity.quantity.lowerBound.value.unwrap(),
                    "${name}_lb_$slot"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return ok
    }

    @Suppress("UNCHECKED_CAST")
    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            when (args) {
                is ResourceGanttSchedulingShadowPriceArguments<*, *, *, *> -> {
                    val slots = usage.timeSlots.filter {
                        it.resource == args.resource && it.time.withIntersection(args.time)
                    }
                    slots.sumOf { map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero }
                }

                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    val slots = usage.timeSlots.filter {
                        it.relatedTo(args.prevTask as? AbstractTask<E, A>?, args.thisTask as? AbstractTask<E, A>?)
                    }
                    slots.sumOf { map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero }
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val thisShadowPrices = HashMap<ResourceTimeSlot<R, C>, Flt64>()
        val indices = model.indicesOfConstraintGroup(name)
            ?: model.constraints.indices
        val iteratorLb = usage.timeSlots.iterator()
        val iteratorUb = usage.timeSlots.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith("${name}_lb")) {
                val slot = iteratorLb.next()
                thisShadowPrices[slot] = (thisShadowPrices[slot] ?: Flt64.zero) + shadowPrices[j]
            }

            if (model.constraints[j].name.startsWith("${name}_ub")) {
                val slot = iteratorUb.next()
                thisShadowPrices[slot] = (thisShadowPrices[slot] ?: Flt64.zero) + shadowPrices[j]
            }

            if (!iteratorLb.hasNext() && !iteratorUb.hasNext()) {
                break
            }
        }
        for ((slot, value) in thisShadowPrices) {
            map.put(ShadowPrice(ResourceCapacityShadowPriceKey(slot), value))
        }

        return ok
    }
}
