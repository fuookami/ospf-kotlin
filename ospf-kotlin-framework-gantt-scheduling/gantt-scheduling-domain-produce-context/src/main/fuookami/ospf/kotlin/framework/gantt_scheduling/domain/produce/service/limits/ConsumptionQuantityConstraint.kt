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
    val material: Material,
) : ShadowPriceKey(ConsumptionQuantityShadowPriceKey::class)

class ConsumptionQuantityConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    materials: List<Pair<Material, MaterialReserves?>>,
    private val consumption: Consumption,
    override val name: String = "consumption_quantity"
) : GanttSchedulingCGPipeline<Args, E, A> {
    private val materials = materials.filterIsInstance<Pair<Material, MaterialReserves>>()

    override fun invoke(model: LinearMetaModel): Try {
        for ((material, reserve) in materials) {
            if (consumption.overEnabled && reserve.overEnabled) {
                when (val overQuantity = consumption.overQuantity[material]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            overQuantity.polyX leq reserve.quantity.upperBound.toFlt64(),
                            "${name}_ub_$material"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            consumption.quantity[material] leq reserve.quantity.upperBound.toFlt64(),
                            "${name}_ub_$material"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    consumption.quantity[material] leq reserve.quantity.upperBound.toFlt64(),
                    "${name}_ub_$material"
                )
            }

            if (consumption.lessEnabled && reserve.lessEnabled) {
                when (val lessQuantity = consumption.lessQuantity[material]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            lessQuantity.polyX geq reserve.quantity.lowerBound.toFlt64(),
                            "${name}_lb_$material"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            consumption.quantity[material] geq reserve.quantity.lowerBound.toFlt64(),
                            "${name}_lb_$material"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    consumption.quantity[material] geq reserve.quantity.lowerBound.toFlt64(),
                    "${name}_lb_$material"
                )
            }
        }

        return Ok(success)
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>> {
        return { map, args ->
            args.thisTask?.let { task ->
                when (task) {
                    is ProductionTask -> {
                        val materials = task.consumption.filter { it.value neq Flt64.zero }.map { it.key }
                        materials.sumOf(Flt64) { map[ConsumptionQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                    }

                    else -> {
                        Flt64.zero
                    }
                }
            } ?: Flt64.zero
        }
    }

    override fun refresh(
        map: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: LinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        val thisShadowPrices = HashMap<Material, Flt64>()
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

        return Ok(success)
    }
}
