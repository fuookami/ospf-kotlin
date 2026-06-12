/**
 * 生产数量约束服务 / Produce quantity constraint service
 *
 * 本文件定义生产数量约束管道及影子价格键，用于建模产品产量的上下限约束。
 * This file defines produce quantity constraint pipeline and shadow price key for modeling product output lower/upper bound constraints.
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 生产数量影子价格键 / Produce quantity shadow price key
 *
 * @param P 产品类型 / Product type
 * @param product 产品 / Product
 */
data class ProduceQuantityShadowPriceKey<P : AbstractMaterial>(
    val product: P
) : ShadowPriceKey(ProduceQuantityShadowPriceKey::class)

/**
 * 生产数量约束 / Produce quantity constraint
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 产品类型 / Product type
 * @param products 产品与需求对列表 / List of product-demand pairs
 * @param produce 生产对象 / Produce object
 * @param shadowPriceArguments 影子价格参数提取器 / Shadow price arguments extractor
 * @param name 管道名称 / Pipeline name
 */
class ProduceQuantityConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        V
        >(
    products: List<Pair<P, MaterialDemand<V>?>>,
    private val produce: Produce,
    private val shadowPriceArguments: ((Args) -> Flt64?)? = null,
    override val name: String = "produce_quantity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> where V : RealNumber<V>, V : NumberField<V> {
    private val products = products
        .mapNotNull { (product, demand) -> demand?.let { product to it } }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((product, demand) in products) {
            if (produce.overEnabled && demand.overEnabled) {
                when (val overQuantity = produce.overQuantity[product]) {
                    is LinearFunctionSymbolAdapter -> {
                        overQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX leq demand.solverUpperBound(),
                                name = "${name}_ub_${product}",
                                args = ProduceQuantityShadowPriceKey(product)
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

                    else -> {
                        when (val result = model.addConstraint(
                            produce.quantity[product] leq demand.solverUpperBound(),
                            name = "${name}_ub_${product}",
                            args = ProduceQuantityShadowPriceKey(product)
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
                    produce.quantity[product] leq demand.solverUpperBound(),
                    name = "${name}_ub_${product}",
                    args = ProduceQuantityShadowPriceKey(product)
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

            if (produce.lessEnabled && demand.lessEnabled) {
                when (val lessQuantity = produce.lessQuantity[product]) {
                    is LinearFunctionSymbolAdapter -> {
                        lessQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX geq demand.solverLowerBound(),
                                name = "${name}_lb_${product}",
                                args = ProduceQuantityShadowPriceKey(product)
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

                    else -> {
                        when (val result = model.addConstraint(
                            produce.quantity[product] geq demand.solverLowerBound(),
                            name = "${name}_lb_${product}",
                            args = ProduceQuantityShadowPriceKey(product)
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
                    produce.quantity[product] geq demand.solverLowerBound(),
                    name = "${name}_lb_${product}",
                    args = ProduceQuantityShadowPriceKey(product)
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

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceArguments?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *, *> -> {
                            task.nonZeroProduceMaterials<P>()
                                .sumOf { map[ProduceQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                        }

                        else -> {
                            Flt64.zero
                        }
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *, *> -> {
                            task.nonZeroProduceMaterials<P>()
                                .sumOf { map[ProduceQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
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
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        val thisShadowPrices = HashMap<P, Flt64>()
        for (constraint in with(this) { model.constraintsOfGroup() }) {
            val product = shadowPriceKeyOf<ProduceQuantityShadowPriceKey<P>>(constraint.args)?.product ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                thisShadowPrices[product] = (thisShadowPrices[product] ?: Flt64.zero) + price
            }
        }
        for ((product, value) in thisShadowPrices) {
            shadowPriceMap.put(ShadowPrice(ProduceQuantityShadowPriceKey(product), value))
        }

        return ok
    }
}
