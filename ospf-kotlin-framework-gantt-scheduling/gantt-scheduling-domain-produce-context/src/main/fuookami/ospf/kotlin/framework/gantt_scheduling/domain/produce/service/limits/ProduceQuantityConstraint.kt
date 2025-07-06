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
    val product: AbstractMaterial
) : ShadowPriceKey(ProduceQuantityShadowPriceKey::class)

class ProduceQuantityConstraint<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    P : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    private val produce: Produce,
    private val shadowPriceArguments: ((Args) -> Flt64?)? = null,
    override val name: String = "produce_quantity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val products = products
        .filter { it.second != null }
        .filterIsInstance<Pair<P, MaterialDemand>>()

    override fun invoke(model: AbstractLinearMetaModel): Try {
        for ((product, demand) in products) {
            if (produce.overEnabled && demand.overEnabled) {
                when (val overQuantity = produce.overQuantity[product]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            overQuantity.polyX leq demand.quantity.upperBound.value.unwrap(),
                            "${name}_ub_$product"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            produce.quantity[product] leq demand.quantity.upperBound.value.unwrap(),
                            "${name}_ub_$product"
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
                    produce.quantity[product] leq demand.quantity.upperBound.value.unwrap(),
                    "${name}_ub_$product"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (produce.lessEnabled && demand.lessEnabled) {
                when (val lessQuantity = produce.lessQuantity[product]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            lessQuantity.polyX geq demand.quantity.lowerBound.value.unwrap(),
                            "${name}_lb_$product"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            produce.quantity[product] geq demand.quantity.lowerBound.value.unwrap(),
                            "${name}_lb_$product"
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
                    produce.quantity[product] geq demand.quantity.lowerBound.value.unwrap(),
                    "${name}_lb_$product"
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
                            val materials = task.produce.filter { it.value neq Flt64.zero }.map { it.key }
                            materials.sumOf { map[ProduceQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                        }

                        else -> {
                            Flt64.zero
                        }
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *> -> {
                            val materials = task.produce.filter { it.value neq Flt64.zero }.map { it.key }
                            materials.sumOf { map[ProduceQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
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
        val thisShadowPrices = HashMap<P, Flt64>()
        val indices = model.indicesOfConstraintGroup(name)
            ?: model.constraints.indices
        val iteratorLb = products.iterator()
        val iteratorUb = products.iterator()
        for (j in indices) {
            if (model.constraints[j].name.startsWith("${name}_lb")) {
                val product = iteratorLb.next().first
                thisShadowPrices[product] = (thisShadowPrices[product] ?: Flt64.zero) + shadowPrices[j]
            }

            if (model.constraints[j].name.startsWith("${name}_ub")) {
                val product = iteratorUb.next().first
                thisShadowPrices[product] = (thisShadowPrices[product] ?: Flt64.zero) + shadowPrices[j]
            }

            if (!iteratorLb.hasNext() && !iteratorUb.hasNext()) {
                break
            }
        }
        for ((product, value) in thisShadowPrices) {
            map.put(ShadowPrice(ProduceQuantityShadowPriceKey(product), value))
        }

        return ok
    }
}
