package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*

data class ConsumptionQuantityShadowPriceKey<C : AbstractMaterial>(
    val material: C,
) : ShadowPriceKey(ConsumptionQuantityShadowPriceKey::class)

class ConsumptionQuantityConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    C : AbstractMaterial
>(
    materials: List<Pair<C, MaterialReserves?>>,
    private val consumption: Consumption,
    private val shadowPriceArguments: ((Args) -> Flt64?)? = null,
    override val name: String = "consumption_quantity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val materials = materials
        .filter { it.second != null }
        .filterIsInstance<Pair<C, MaterialReserves>>()

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for ((material, reserve) in materials) {
            if (consumption.overEnabled && reserve.overEnabled) {
                when (val overQuantity = consumption.overQuantity[material]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            overQuantity.polyX leq reserve.quantity.upperBound.value.unwrap(),
                            name = "${name}_ub_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            consumption.quantity[material] leq reserve.quantity.upperBound.value.unwrap(),
                            name = "${name}_ub_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
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
                    consumption.quantity[material] leq reserve.quantity.upperBound.value.unwrap(),
                    name = "${name}_ub_$material",
                    args = ConsumptionQuantityShadowPriceKey(material)
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (consumption.lessEnabled && reserve.lessEnabled) {
                when (val lessQuantity = consumption.lessQuantity[material]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            lessQuantity.polyX geq reserve.quantity.lowerBound.value.unwrap(),
                            name = "${name}_lb_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            consumption.quantity[material] geq reserve.quantity.lowerBound.value.unwrap(),
                            name = "${name}_lb_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
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
                    consumption.quantity[material] geq reserve.quantity.lowerBound.value.unwrap(),
                    name = "${name}_lb_$material",
                    args = ConsumptionQuantityShadowPriceKey(material)
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

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceArguments?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *> -> {
                            val materials = task.consumption.filter { it.value neq Flt64.zero }.map { it.key }
                            materials.sumOf { map[ConsumptionQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                        }

                        else -> {
                            Flt64.zero
                        }
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *> -> {
                            val materials = task.consumption.filter { it.value neq Flt64.zero }.map { it.key }
                            materials.sumOf { map[ConsumptionQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                        }

                        else -> {
                            Flt64.zero
                        }
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
        val thisShadowPrices = HashMap<C, Flt64>()
        for (constraint in model.constraintsOfGroup()) {
            val material = (constraint.args as? ConsumptionQuantityShadowPriceKey<C>)?.material ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                thisShadowPrices[material] = (thisShadowPrices[material] ?: Flt64.zero) + price
            }
        }
        for ((material, value) in thisShadowPrices) {
            map.put(ShadowPrice(ConsumptionQuantityShadowPriceKey(material), value))
        }

        return ok
    }
}
