/**
 * 需求约束与影子价格提取。
 * Demand constraint and shadow price extraction.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.model.*

private val shadowPriceConverter = IntoValue.fromConverter(FltX)

/**
 * 需求影子价格键，用于标识需求约束的影子价格。
 * Demand shadow price key, used to identify shadow prices of demand constraints.
 *
 * @property mode 需求模式 / demand mode
 * @property key 需求键 / demand key
 * @property quantityUnit 量纲单位（可选） / quantity unit (optional)
 */
data class DemandShadowPriceKey(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val quantityUnit: PhysicalUnit? = null
) : ShadowPriceKey(DemandShadowPriceKey::class)

/**
 * 符号线性多项式构造。
 * Symbol linear polynomial construction.
 */
private fun asLinearPolynomial(symbol: Symbol): LinearPolynomial<FltX> {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(layerAssignmentOne(), symbol)),
        constant = layerAssignmentZero()
    )
}

/**
 * 常量多项式构造。
 * Constant polynomial construction.
 */
private fun constantPolynomial(value: FltX): LinearPolynomial<FltX> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * 需求模式标签。
 * Demand mode tag.
 */
private fun modeTag(mode: Bpp3dDemandMode): String {
    return when (mode) {
        is Bpp3dDemandMode.Item -> "item"
        is Bpp3dDemandMode.Material -> "material"
        is Bpp3dDemandMode.ItemAmount -> "item_amount"
        is Bpp3dDemandMode.ItemWeight -> "item_weight"
        is Bpp3dDemandMode.ItemMaterialAmount -> "material_amount"
        is Bpp3dDemandMode.ItemMaterialWeight -> "material_weight"
    }
}

/**
 * 需求域标签。
 * Demand domain tag.
 */
private fun domainTag(domain: Bpp3dDemandDomain): String {
    return when (domain) {
        Bpp3dDemandDomain.Discrete -> "discrete"
        Bpp3dDemandDomain.Continuous -> "continuous"
    }
}

/**
 * 获取需求统计。
 * Get demand statistics.
 *
 * 使用 Any 参数代替基础设施层通配 Cuboid 类型：when-dispatch 本身即为运行时类型检查，
 * Any 等价且更通用，减少 domain 层对基础设施层几何兼容类型的绑定。
 * Uses Any parameter instead of the infrastructure wildcard Cuboid type: when-dispatch is runtime type checking,
 * Any is equivalent and more general, reducing domain-layer binding to infrastructure geometry compatibility types.
 */
private fun demandStatistics(
    cuboid: Any,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (cuboid) {
        is Item -> cuboid.statistics(mode)
        is Container3<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (cuboid as Container3<*, FltX>).statistics(mode)
        }
        is Container2<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            (cuboid as Container2<*, FltX, *>).statistics(mode)
        }
        else -> emptyMap()
    }
}

/**
 * 需求约束，确保装载量满足需求并提取影子价格。
 * Demand constraint, ensures load satisfies demands and extracts shadow prices.
 *
 * @param Args 影子价格参数类型 / shadow price arguments type
 * @param T 立方体类型 / cuboid type
 * @property load 负载符号 / load symbols
 * @property demandEntries 需求条目列表 / demand entry list
 * @property shadowPriceExtractor 自定义影子价格提取器（可选） / custom shadow price extractor (optional)
 * @property name 约束名称 / constraint name
 */
open class DemandConstraint<
        Args : AbstractBPP3DShadowPriceArguments<FltX, T>,
        T : Cuboid<T, FltX>
        > protected constructor(
    private val load: Load<FltX>,
    private val demandEntries: List<Bpp3dDemandEntry<FltX>> = load.demandEntries,
    private val shadowPriceExtractor: ((Args) -> FltX?)? = null,
    override val name: String = "demand"
) : CGPipeline<Args, AbstractLinearMetaModel<FltX>, AbstractBPP3DShadowPriceMap<Args, FltX, T>> {
    private fun symbolAt(index: Int): Symbol {
        return (runCatching { load.load[index] as Symbol }.getOrNull()
            ?: throw IllegalStateException("Missing load symbol at index $index"))
    }

    private fun upperSymbolAt(index: Int): Symbol {
        return runCatching { load.overLoad[index] as Symbol }.getOrDefault(symbolAt(index))
    }

    private fun lowerSymbolAt(index: Int): Symbol {
        return runCatching { load.lessLoad[index] as Symbol }.getOrDefault(symbolAt(index))
    }

    private fun resolveShadowPrice(
        map: AbstractShadowPriceMap<Args, AbstractBPP3DShadowPriceMap<Args, FltX, T>>,
        demand: Bpp3dDemandEntry<FltX>,
        concreteMode: Bpp3dDemandMode
    ): FltX {
        val keys = LinkedHashSet<DemandShadowPriceKey>()
        keys.add(
            DemandShadowPriceKey(
                mode = demand.mode,
                key = demand.key,
                quantityUnit = demand.quantityUnit
            )
        )
        keys.add(
            DemandShadowPriceKey(
                mode = demand.mode,
                key = demand.key,
                quantityUnit = null
            )
        )
        if (concreteMode != demand.mode) {
            keys.add(
                DemandShadowPriceKey(
                    mode = concreteMode,
                    key = demand.key,
                    quantityUnit = demand.quantityUnit
                )
            )
            keys.add(
                DemandShadowPriceKey(
                    mode = concreteMode,
                    key = demand.key,
                    quantityUnit = null
                )
            )
        }
        for (key in keys) {
            val shadowPrice = map[key]?.price
            if (shadowPrice != null) {
                return FltX(shadowPrice.toDouble())
            }
        }
        return layerAssignmentZero()
    }

    override fun invoke(model: AbstractLinearMetaModel<FltX>): Try {
        for ((i, demand) in demandEntries.withIndex()) {
            val upperBound = demand.demandRange.upperBound.value.unwrap()
            val lowerBound = demand.demandRange.lowerBound.value.unwrap()
            val priceKey = DemandShadowPriceKey(
                mode = demand.mode,
                key = demand.key,
                quantityUnit = demand.quantityUnit
            )
            val tag = "${modeTag(demand.mode)}_${domainTag(demand.quantityDomain)}"

            if (load.overEnabled && !demand.demandRange.fixed && upperBound neq demand.demand) {
                when (val result = model.addConstraint(
                    relation = LinearInequality(
                        asLinearPolynomial(upperSymbolAt(i)),
                        constantPolynomial(upperBound),
                        Comparison.LE
                    ),
                    name = "${name}_${tag}_ub_$i",
                    args = priceKey
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                when (val result = model.addConstraint(
                    relation = LinearInequality(
                        asLinearPolynomial(symbolAt(i)),
                        constantPolynomial(demand.demand),
                        Comparison.LE
                    ),
                    name = "${name}_${tag}_ub_$i",
                    args = priceKey
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            if (!demand.demandRange.fixed && lowerBound neq demand.demand) {
                when (val result = model.addConstraint(
                    relation = LinearInequality(
                        asLinearPolynomial(lowerSymbolAt(i)),
                        constantPolynomial(lowerBound),
                        Comparison.GE
                    ),
                    name = "${name}_${tag}_lb_$i",
                    args = priceKey
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else {
                when (val result = model.addConstraint(
                    relation = LinearInequality(
                        asLinearPolynomial(symbolAt(i)),
                        constantPolynomial(demand.demand),
                        Comparison.GE
                    ),
                    name = "${name}_${tag}_lb_$i",
                    args = priceKey
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    override fun extractor(): ShadowPriceExtractor<Args, AbstractBPP3DShadowPriceMap<Args, FltX, T>>? {
        if (shadowPriceExtractor != null) {
            return { _, args ->
                shadowPriceConverter.fromValue(shadowPriceExtractor.invoke(args) ?: layerAssignmentZero())
            }
        }

        if (demandEntries.isEmpty()) {
            return null
        }

        return { map, args ->
            var price = layerAssignmentZero()
            for (demand in demandEntries) {
                val concreteMode = demand.mode.toConcreteMode(
                    key = demand.key,
                    isDiscrete = demand.quantityDomain == Bpp3dDemandDomain.Discrete
                )
                val statistics = demandStatistics(args.cuboid, concreteMode)
                val value = statistics[demand.key] ?: continue
                val shadow = resolveShadowPrice(
                    map = map,
                    demand = demand,
                    concreteMode = concreteMode
                )
                if (shadow neq layerAssignmentZero()) {
                    price += shadow * load.demandValueAdapter.toSolver(value)
                }
            }
            shadowPriceConverter.fromValue(price)
        }
    }

    override fun refresh(
        shadowPriceMap: AbstractBPP3DShadowPriceMap<Args, FltX, T>,
        model: AbstractLinearMetaModel<FltX>,
        shadowPrices: MetaDualSolution
    ): Try {
        return CGPipeline.refreshByKeyAsArgs(this, shadowPriceMap, model, shadowPrices)
    }
}

/**
 * 创建 Item 专用需求约束，供业务调用侧避开泛型基类入口。
 * Build item-only demand constraint so business callers avoid the quantity-polymorphic base entry.
 *
 * @param load 负载符号 / load symbols
 * @param demandEntries 需求条目列表 / demand entry list
 * @param shadowPriceExtractor 自定义影子价格提取器（可选） / custom shadow price extractor (optional)
 * @param name 约束名称 / constraint name
 * @return Item 专用需求约束 / item-only demand constraint
 */
fun itemDemandConstraint(
    load: Load<FltX>,
    demandEntries: List<Bpp3dDemandEntry<FltX>> = load.demandEntries,
    shadowPriceExtractor: ((BPP3DShadowPriceArguments) -> FltX?)? = null,
    name: String = "demand"
): ItemDemandConstraint {
    return ItemDemandConstraint(
        load = load,
        demandEntries = demandEntries,
        shadowPriceExtractor = shadowPriceExtractor,
        name = name
    )
}

/**
 * Item 专用需求约束，不暴露底层泛型 cuboid 约束。
 * Item-only demand constraint, does not expose the underlying quantity-polymorphic cuboid constraint.
 *
 * @property load 负载符号 / load symbols
 * @property demandEntries 需求条目列表 / demand entry list
 * @property shadowPriceExtractor 自定义影子价格提取器（可选） / custom shadow price extractor (optional)
 * @property name 约束名称 / constraint name
 */
class ItemDemandConstraint(
    load: Load<FltX>,
    demandEntries: List<Bpp3dDemandEntry<FltX>> = load.demandEntries,
    shadowPriceExtractor: ((BPP3DShadowPriceArguments) -> FltX?)? = null,
    name: String = "demand"
) : DemandConstraint<BPP3DShadowPriceArguments, Item>(
    load = load,
    demandEntries = demandEntries,
    shadowPriceExtractor = shadowPriceExtractor,
    name = name
)
