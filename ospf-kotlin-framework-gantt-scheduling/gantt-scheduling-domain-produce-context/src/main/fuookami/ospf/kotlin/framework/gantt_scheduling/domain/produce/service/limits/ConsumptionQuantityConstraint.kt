package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Consumption
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.ProductionTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.nonZeroConsumptionMaterials
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.solverLowerBound
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.solverUpperBound
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel

/**
 * 消费数量影子价格键 / Consumption quantity shadow price key
 *
 * @param C 材料类型 / Material type
 * @param material 材料 / Material
 */
data class ConsumptionQuantityShadowPriceKey<C : AbstractMaterial>(
    val material: C,
) : ShadowPriceKey(ConsumptionQuantityShadowPriceKey::class)

/**
 * 消费数量约束 / Consumption quantity constraint
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param C 材料类型 / Material type
 * @param materials 材料与储备对列表 / List of material-reserve pairs
 * @param consumption 消费对象 / Consumption object
 * @param shadowPriceArguments 影子价格参数提取器 / Shadow price arguments extractor
 * @param name 管道名称 / Pipeline name
 */
class ConsumptionQuantityConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        C : AbstractMaterial,
        V
        >(
    materials: List<Pair<C, MaterialReserves<V>?>>,
    private val consumption: Consumption,
    private val shadowPriceArguments: ((Args) -> Flt64?)? = null,
    override val name: String = "consumption_quantity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> where V : RealNumber<V>, V : NumberField<V> {
    private val materials = materials
        .mapNotNull { (material, reserve) -> reserve?.let { material to it } }

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((material, reserve) in materials) {
            if (consumption.overEnabled && reserve.overEnabled) {
                when (val overQuantity = consumption.overQuantity[material]) {
                    is LinearFunctionSymbolAdapter -> {
                        overQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX leq reserve.solverUpperBound(),
                                name = "${name}_ub_$material",
                                args = ConsumptionQuantityShadowPriceKey(material)
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
                            consumption.quantity[material] leq reserve.solverUpperBound(),
                            name = "${name}_ub_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
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
                    consumption.quantity[material] leq reserve.solverUpperBound(),
                    name = "${name}_ub_$material",
                    args = ConsumptionQuantityShadowPriceKey(material)
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

            if (consumption.lessEnabled && reserve.lessEnabled) {
                when (val lessQuantity = consumption.lessQuantity[material]) {
                    is LinearFunctionSymbolAdapter -> {
                        lessQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX geq reserve.solverLowerBound(),
                                name = "${name}_lb_$material",
                                args = ConsumptionQuantityShadowPriceKey(material)
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
                            consumption.quantity[material] geq reserve.solverLowerBound(),
                            name = "${name}_lb_$material",
                            args = ConsumptionQuantityShadowPriceKey(material)
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
                    consumption.quantity[material] geq reserve.solverLowerBound(),
                    name = "${name}_lb_$material",
                    args = ConsumptionQuantityShadowPriceKey(material)
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
                            task.nonZeroConsumptionMaterials<C>()
                                .sumOf { map[ConsumptionQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
                        }

                        else -> {
                            Flt64.zero
                        }
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    when (val task = args.task) {
                        is ProductionTask<*, *, *, *, *> -> {
                            task.nonZeroConsumptionMaterials<C>()
                                .sumOf { map[ConsumptionQuantityShadowPriceKey(it)]?.price ?: Flt64.zero }
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
        val thisShadowPrices = HashMap<C, Flt64>()
        for (constraint in with(this) { model.constraintsOfGroup() }) {
            val material = shadowPriceKeyOf<ConsumptionQuantityShadowPriceKey<C>>(constraint.args)?.material ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                thisShadowPrices[material] = (thisShadowPrices[material] ?: Flt64.zero) + price
            }
        }
        for ((material, value) in thisShadowPrices) {
            shadowPriceMap.put(ShadowPrice(ConsumptionQuantityShadowPriceKey(material), value))
        }

        return ok
    }
}
