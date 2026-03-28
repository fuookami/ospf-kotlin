package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.AbstractSlackFunction
import fuookami.ospf.kotlin.core.frontend.inequality.geq
import fuookami.ospf.kotlin.core.frontend.inequality.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Load
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ValueRange

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
                            name = "${name}_ub_${item}"
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

                    else -> {
                        when (val result = model.addConstraint(
                            load.load[item] leq demandRange.upperBound.value.unwrap(),
                            name = "${name}_ub_${item}"
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
                    load.load[item] leq demand,
                    name = "${name}_ub_${item}"
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

            if (!demandRange.fixed && demandRange.lowerBound.value.unwrap() neq demand) {
                when (val lessLoad = load.lessLoad[item]) {
                    is AbstractSlackFunction<*> -> {
                        when (val result = model.addConstraint(
                            lessLoad.polyX leq demandRange.upperBound.value.unwrap(),
                            name = "${name}_lb_${item}"
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

                    else -> {
                        when (val result = model.addConstraint(
                            load.load[item] geq demandRange.lowerBound.value.unwrap(),
                            name = "${name}_ub_${item}"
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
                    load.load[item] geq demand,
                    name = "${name}_ub_${item}"
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

    override fun extractor(): AbstractBPP3DShadowPriceExtractor<Args, T>? {
        TODO("Not yet implemented")
    }

    override fun refresh(
        map: AbstractBPP3DShadowPriceMap<Args, T>,
        model: AbstractLinearMetaModel,
        shadowPrices: MetaDualSolution
    ): Try {
        TODO("Not yet implemented")
    }
}




