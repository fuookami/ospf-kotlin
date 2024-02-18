package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*

data class ProduceQuantityShadowPriceKey(
    val product: Product
) : ShadowPriceKey(ProduceQuantityShadowPriceKey::class)

class ProduceQuantityConstraint<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A: AssignmentPolicy<E>>(
    products: List<Pair<Product, ProductDemand?>>,
    private val produce: Produce,
    override val name: String = "produce_quantity"
) : GanttSchedulingCGPipeline<Args, E, A> {
    private val products = products.filterIsInstance<Pair<Product, ProductDemand>>()

    override fun invoke(model: LinearMetaModel): Try {
        for ((product, demand) in products) {
            if (produce.overEnabled && demand.overEnabled) {
                when (val overQuantity = produce.overQuantity[product]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            overQuantity.polyX leq demand.quantity.upperBound.toFlt64(),
                            "${name}_ub_$product"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            produce.quantity[product] leq demand.quantity.upperBound.toFlt64(),
                            "${name}_ub_$product"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    produce.quantity[product] leq demand.quantity.upperBound.toFlt64(),
                    "${name}_ub_$product"
                )
            }

            if (produce.lessEnabled && demand.lessEnabled) {
                when (val lessQuantity = produce.lessQuantity[product]) {
                    is AbstractSlackFunction<*> -> {
                        model.addConstraint(
                            lessQuantity.polyX geq demand.quantity.lowerBound.toFlt64(),
                            "${name}_lb_$product"
                        )
                    }

                    else -> {
                        model.addConstraint(
                            produce.quantity[product] geq demand.quantity.lowerBound.toFlt64(),
                            "${name}_lb_$product"
                        )
                    }
                }
            } else {
                model.addConstraint(
                    produce.quantity[product] geq demand.quantity.lowerBound.toFlt64(),
                    "${name}_lb_$product"
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
                        val products = task.produce.filter { it.value neq Flt64.zero }.map { it.key }
                        products.sumOf(Flt64) { map[ProduceQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
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
        val thisShadowPrices = HashMap<Product, Flt64>()
        val indices = model.indicesOfConstraintGroup(name)
            ?: model.constraints.indices
        val iteratorLb = products.iterator()
        val iteratorUb = products.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith("${name}_lb")) {
                val product = iteratorLb.next().first
                thisShadowPrices[product] = (thisShadowPrices[product]?: Flt64.zero) + shadowPrices[j]
            }

            if (model.constraints[j].name.startsWith("${name}_ub")) {
                val product = iteratorUb.next().first
                thisShadowPrices[product] = (thisShadowPrices[product]?: Flt64.zero) + shadowPrices[j]
            }

            if (!iteratorLb.hasNext() && !iteratorUb.hasNext()) {
                break
            }
        }
        for ((product, value) in thisShadowPrices) {
            map.put(ShadowPrice(ProduceQuantityShadowPriceKey(product), value))
        }

        return Ok(success)
    }
}
