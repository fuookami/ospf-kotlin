package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*

data class ItemDemandShadowPriceKey<T : Cuboid<T>>(
    val item: T
) : ShadowPriceKey(ItemDemandShadowPriceKey::class)

class ItemDemandConstraint<
    Args : AbstractBPP3DShadowPriceArguments<T>,
    T : Cuboid<T>
>(
    private val load: Load,
    private val items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "item_demand"
) : AbstractBPP3DCGPipeline<Args, T> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        for ((item, demand, demandRange) in items) {
            if (load.overEnabled && !demandRange.fixed && demandRange.upperBound.value.unwrap() neq demand) {
                when (val overLoad = load.overLoad[item]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            overLoad.polyX leq demandRange.upperBound.value.unwrap(),
                            "${name}_ub_${item}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            load.load[item] leq demandRange.upperBound.value.unwrap(),
                            "${name}_ub_${item}"
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
                    load.load[item] leq demand,
                    "${name}_ub_${item}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (!demandRange.fixed && demandRange.lowerBound.value.unwrap() neq demand) {
                when (val lessLoad = load.lessLoad[item]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            lessLoad.polyX leq demandRange.upperBound.value.unwrap(),
                            "${name}_lb_${item}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }

                    else -> {
                        when (val result = model.addConstraint(
                            load.load[item] geq demandRange.lowerBound.value.unwrap(),
                            "${name}_ub_${item}"
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
                    load.load[item] geq demand,
                    "${name}_ub_${item}"
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

    override fun extractor(): AbstractBPP3DShadowPriceExtractor<Args, T>? {
        TODO("Not yet implemented")
    }

    override fun refresh(
        map: AbstractBPP3DShadowPriceMap<Args, T>,
        model: AbstractLinearMetaModel,
        shadowPrices: List<Flt64>
    ): Try {
        TODO("Not yet implemented")
    }
}
