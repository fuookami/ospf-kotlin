@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.service.limits

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_model.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.functional.sumOf

data class ResourceCapacityShadowPriceKey<R : Resource<C>, C : AbstractResourceCapacity>(
    val slot: ResourceTimeSlot<R, C>
) : ShadowPriceKey(ResourceCapacityShadowPriceKey::class)

class ResourceCapacityConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        S : ResourceTimeSlot<R, C>,
        R : StorageResource<C>,
        C : AbstractResourceCapacity
        >(
    private val usage: ResourceUsage<S, R, C>,
    private val quantity: Extractor<ValueRange<Flt64>, S> = { it.resourceCapacity.quantity },
    private val withSlack: Boolean = true,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "${usage.name}_resource_capacity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for (slot in usage.timeSlots) {
            val thisQuantity = quantity(slot)
            if (withSlack && usage.overEnabled && slot.resourceCapacity.overEnabled) {
                when (val overQuantity = usage.overQuantity[slot]) {
                    is LinearFunctionSymbolAdapter -> {
                        overQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX leq thisQuantity.upperBound.value.unwrap(),
                                name = "${name}_ub_$slot",
                                args = ResourceCapacityShadowPriceKey(slot)
                            )) {
                                is Ok<*, *, *> -> {}

                                is Failed<*, *, *> -> {
                                    return Failed(result.error)
                                }

                                is Fatal -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] leq thisQuantity.upperBound.value.unwrap(),
                            name = "${name}_ub_$slot",
                            args = ResourceCapacityShadowPriceKey(slot)
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
            } else {
                when (val result = model.addConstraint(
                    usage.quantity[slot] leq thisQuantity.upperBound.value.unwrap(),
                    name = "${usage.name}_${name}_ub_$slot",
                    args = ResourceCapacityShadowPriceKey(slot)
                )) {
                    is Ok<*, *, *> -> {}

                    is Failed<*, *, *> -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }

            if (withSlack && usage.lessEnabled && slot.resourceCapacity.lessEnabled) {
                when (val lessQuantity = usage.lessQuantity[slot]) {
                    is LinearFunctionSymbolAdapter -> {
                        lessQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX geq thisQuantity.lowerBound.value.unwrap(),
                                name = "${name}_lb_$slot",
                                args = ResourceCapacityShadowPriceKey(slot)
                            )) {
                                is Ok<*, *, *> -> {}

                                is Failed<*, *, *> -> {
                                    return Failed(result.error)
                                }

                                is Fatal -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] geq thisQuantity.lowerBound.value.unwrap(),
                            name = "${name}_lb_$slot",
                            args = ResourceCapacityShadowPriceKey(slot)
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
            } else {
                when (val result = model.addConstraint(
                    usage.quantity[slot] geq thisQuantity.lowerBound.value.unwrap(),
                    name = "${name}_lb_$slot",
                    args = ResourceCapacityShadowPriceKey(slot)
                )) {
                    is Ok<*, *, *> -> {}

                    is Failed<*, *, *> -> {
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

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    usage.timeSlots.sumOf {
                        it.relationTo(null, args.task) * (map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero)
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        usage.timeSlots.sumOf {
                            it.relationTo(null, args.task) * (map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero)
                        }
                    } else {
                        Flt64.zero
                    }
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: MetaDualSolution
    ): Try {
        val thisShadowPrices = HashMap<ResourceTimeSlot<R, C>, Flt64>()
        for (constraint in model.constraintsOfGroup()) {
            val slot = (constraint.args as? ResourceCapacityShadowPriceKey<R, C>)?.slot ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                thisShadowPrices[slot] = (thisShadowPrices[slot] ?: Flt64.zero) + price
            }
        }
        for ((slot, value) in thisShadowPrices) {
            map.put(ShadowPrice(ResourceCapacityShadowPriceKey(slot), value))
        }

        return ok
    }
}




