package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*

data class ConsumptionQuantityShadowPriceKey(
    val material: RawMaterial,
) : ShadowPriceKey(ConsumptionQuantityShadowPriceKey::class)

class ConsumptionQuantityConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    materials: List<Pair<RawMaterial, RawMaterialReserves?>>,
    private val consumption: Consumption,
    private val shadowPriceArguments: ((Args) -> Flt64?)? = null,
    override val name: String = "consumption_quantity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val materials = materials.filterIsInstance<Pair<RawMaterial, RawMaterialReserves>>()

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for ((material, reserve) in materials) {
            if (consumption.overEnabled && reserve.overEnabled) {
                when (val overQuantity = consumption.overQuantity[material]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            overQuantity.polyX leq reserve.quantity.upperBound.value.unwrap(),
                            "${name}_ub_$material"
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
                            "${name}_ub_$material"
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
                    "${name}_ub_$material"
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
                            "${name}_lb_$material"
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
                            "${name}_lb_$material"
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
                    "${name}_lb_$material"
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
                        is ProductionTask<*, *> -> {
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
                        is ProductionTask<*, *> -> {
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

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val thisShadowPrices = HashMap<RawMaterial, Flt64>()
        val indices = model.indicesOfConstraintGroup(name)
            ?: model.constraints.indices
        val iteratorLb = materials.iterator()
        val iterableUb = materials.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith("${name}_lb")) {
                val material = iteratorLb.next().first
                thisShadowPrices[material] = (thisShadowPrices[material] ?: Flt64.zero) + shadowPrices[j]
            }

            if (model.constraints[j].name.startsWith("${name}_ub")) {
                val material = iterableUb.next().first
                thisShadowPrices[material] = (thisShadowPrices[material] ?: Flt64.zero) + shadowPrices[j]
            }

            if (!iteratorLb.hasNext() && !iterableUb.hasNext()) {
                break
            }
        }
        for ((material, value) in thisShadowPrices) {
            map.put(ShadowPrice(ConsumptionQuantityShadowPriceKey(material), value))
        }

        return ok
    }
}
