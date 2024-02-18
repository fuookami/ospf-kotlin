package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

data class ResourceCapacityShadowPriceKey<R : Resource<C>, C : ResourceCapacity>(
    val slot: ResourceTimeSlot<R, C>
) : ShadowPriceKey(ResourceCapacityShadowPriceKey::class)

class ResourceCapacityConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>, S : ResourceTimeSlot<R, C>, R : StorageResource<C>, C : ResourceCapacity>(
    private val usage: ResourceUsage<S, R, C>,
    override val name: String = "resource_capacity"
) : GanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: LinearMetaModel): Try {
        for (slot in usage.timeSlots) {
            if (usage.overEnabled && slot.resourceCapacity.overEnabled) {
                when (val overQuantity = usage.overQuantity[slot]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            overQuantity.polyX leq slot.resourceCapacity.quantity.upperBound.toFlt64(),
                            "${usage.name}_${name}_ub_$slot"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            usage.quantity[slot] leq slot.resourceCapacity.quantity.upperBound.toFlt64(),
                            "${usage.name}_${name}_ub_$slot"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    usage.quantity[slot] leq slot.resourceCapacity.quantity.upperBound.toFlt64(),
                    "${usage.name}_${name}_ub_$slot"
                )
            }

            if (usage.lessEnabled && slot.resourceCapacity.lessEnabled) {
                when (val lessQuantity = usage.lessQuantity[slot]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            lessQuantity.polyX geq slot.resourceCapacity.quantity.lowerBound.toFlt64(),
                            "${usage.name}_${name}_ub_$slot"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            usage.quantity[slot] geq slot.resourceCapacity.quantity.lowerBound.toFlt64(),
                            "${usage.name}_${name}_ub_$slot"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    usage.quantity[slot] geq slot.resourceCapacity.quantity.lowerBound.toFlt64(),
                    "${usage.name}_${name}_lb_$slot"
                )
            }
        }

        return Ok(success)
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>> {
        return { map, args ->
            args.thisTask?.let { task ->
                val slots = usage.timeSlots.filter { it.relatedTo(args.prevTask, task) }
                slots.sumOf(Flt64) { map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero }
            } ?: Flt64.zero
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: LinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val thisShadowPrices = HashMap<ResourceTimeSlot<R, C>, Flt64>()
        val indices = model.indicesOfConstraintGroup(name)
            ?: model.constraints.indices
        val iteratorLb = usage.timeSlots.iterator()
        val iteratorUb = usage.timeSlots.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith("${usage.name}_${name}_lb")) {
                val slot = iteratorLb.next()
                thisShadowPrices[slot] = (thisShadowPrices[slot] ?: Flt64.zero) + shadowPrices[j]
            }

            if (model.constraints[j].name.startsWith("${usage.name}_${name}_ub")) {
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

        return Ok(success)
    }
}
