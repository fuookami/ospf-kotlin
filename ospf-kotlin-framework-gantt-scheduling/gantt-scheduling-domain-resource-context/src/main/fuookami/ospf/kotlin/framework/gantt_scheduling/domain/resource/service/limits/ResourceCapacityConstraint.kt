@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.service.limits

import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel

/**
 * 资源容量影子价格键 / Resource capacity shadow price key
 *
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param slot 资源时间槽 / Resource time slot
 */
data class ResourceCapacityShadowPriceKey<R : Resource<C, V>, C : AbstractResourceCapacity<V>, V>(
    val slot: ResourceTimeSlot<R, C, V>
) : ShadowPriceKey(ResourceCapacityShadowPriceKey::class) where V : RealNumber<V>, V : NumberField<V>

/**
 * 资源容量约束 / Resource capacity constraint
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param S 资源时间槽类型 / Resource time slot type
 * @param R 存储资源类型 / Storage resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param usage 资源使用对象 / Resource usage object
 * @param quantity 数量提取器 / Quantity extractor
 * @param withSlack 是否使用松弛 / Whether to use slack
 * @param shadowPriceExtractor 影子价格提取器 / Shadow price extractor
 * @param name 管道名称 / Pipeline name
 */
class ResourceCapacityConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    private val usage: ResourceUsage<S, R, C, V>,
    private val quantity: Extractor<ValueRange<V>, S> = { it.resourceCapacity.quantityRangeValue.value },
    private val withSlack: Boolean = true,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "${usage.name}_resource_capacity"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> where V : RealNumber<V>, V : NumberField<V> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (slot in usage.timeSlots) {
            val thisQuantity = quantity(slot)
            if (withSlack && usage.overEnabled && slot.resourceCapacity.overEnabled) {
                when (val overQuantity = usage.overQuantity[slot]) {
                    is LinearFunctionSymbolAdapter -> {
                        overQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX leq thisQuantity.solverUpperBound(),
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

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] leq thisQuantity.solverUpperBound(),
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
                    usage.quantity[slot] leq thisQuantity.solverUpperBound(),
                    name = "${usage.name}_${name}_ub_$slot",
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

            if (withSlack && usage.lessEnabled && slot.resourceCapacity.lessEnabled) {
                when (val lessQuantity = usage.lessQuantity[slot]) {
                    is LinearFunctionSymbolAdapter -> {
                        lessQuantity.polyX?.let { polyX ->
                            when (val result = model.addConstraint(
                                polyX geq thisQuantity.solverLowerBound(),
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

                    else -> {
                        when (val result = model.addConstraint(
                            usage.quantity[slot] geq thisQuantity.solverLowerBound(),
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
                    usage.quantity[slot] geq thisQuantity.solverLowerBound(),
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

        return ok
    }

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is TaskGanttSchedulingShadowPriceArguments<*, *> -> {
                    usage.timeSlots.sumOf {
                        it.relationTo(null, args.task).solverResourceQuantity() *
                                (map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero)
                    }
                }

                is BunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task != null) {
                        usage.timeSlots.sumOf {
                            it.relationTo(null, args.task).solverResourceQuantity() *
                                    (map[ResourceCapacityShadowPriceKey(it)]?.price ?: Flt64.zero)
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
    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        val thisShadowPrices = HashMap<ResourceTimeSlot<R, C, V>, Flt64>()
        for (constraint in model.constraintsOfGroup()) {
            val slot = shadowPriceKeyOf<ResourceCapacityShadowPriceKey<R, C, V>>(constraint.args)?.slot ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                thisShadowPrices[slot] = (thisShadowPrices[slot] ?: Flt64.zero) + price
            }
        }
        for ((slot, value) in thisShadowPrices) {
            shadowPriceMap.put(ShadowPrice(ResourceCapacityShadowPriceKey(slot), value))
        }

        return ok
    }
}
