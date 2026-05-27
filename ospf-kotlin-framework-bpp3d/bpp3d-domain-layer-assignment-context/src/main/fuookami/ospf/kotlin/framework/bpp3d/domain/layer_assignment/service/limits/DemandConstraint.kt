package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.toConcreteMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandDomain
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.LayerAssignmentScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Load
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.layerAssignmentZero
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromItemRanges
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.demandEntriesFromItems
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DCGPipeline
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceArguments
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceExtractor
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractBPP3DShadowPriceMap
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.*

data class DemandShadowPriceKey(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val quantityUnit: PhysicalUnit? = null
) : ShadowPriceKey(DemandShadowPriceKey::class)

private fun asLinearPolynomial(symbol: Symbol): LinearPolynomial<LayerAssignmentScalar> {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(layerAssignmentOne(), symbol)),
        constant = layerAssignmentZero()
    )
}

private fun constantPolynomial(value: LayerAssignmentScalar): LinearPolynomial<LayerAssignmentScalar> {
    return LinearPolynomial(emptyList(), value)
}

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

private fun domainTag(domain: Bpp3dDemandDomain): String {
    return when (domain) {
        Bpp3dDemandDomain.Discrete -> "discrete"
        Bpp3dDemandDomain.Continuous -> "continuous"
    }
}

private fun demandStatistics(
    cuboid: Cuboid<*>,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (cuboid) {
        is Item -> cuboid.statistics(mode)
        is Container3<*> -> cuboid.statistics(mode)
        is Container2<*, *> -> cuboid.statistics(mode)
        else -> emptyMap()
    }
}

open class DemandConstraint<
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val load: Load,
    private val demandEntries: List<Bpp3dDemandEntry> = load.demandEntries,
    private val shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
    override val name: String = "demand"
) : AbstractBPP3DCGPipeline<Args, T> {
    companion object {
        fun <
                Args : AbstractBPP3DShadowPriceArguments<T>,
                T : Cuboid<T>
                > fromItems(
            load: Load,
            items: List<Pair<Item, UInt64>>,
            shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
            name: String = "demand"
        ): DemandConstraint<Args, T> {
            val demands = if (load.demandEntries.isNotEmpty()) {
                load.demandEntries
            } else {
                demandEntriesFromItems(items, load.demandValueAdapter)
            }
            return DemandConstraint(load, demands, shadowPriceExtractor, name)
        }

        fun <
                Args : AbstractBPP3DShadowPriceArguments<T>,
                T : Cuboid<T>
                > fromItemRanges(
            load: Load,
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
            name: String = "demand"
        ): DemandConstraint<Args, T> {
            val demands = if (load.demandEntries.isNotEmpty()) {
                load.demandEntries
            } else {
                demandEntriesFromItemRanges(items, load.demandValueAdapter)
            }
            return DemandConstraint(load, demands, shadowPriceExtractor, name)
        }
    }

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
        map: AbstractShadowPriceMap<Args, AbstractBPP3DShadowPriceMap<Args, T>>,
        demand: Bpp3dDemandEntry,
        concreteMode: Bpp3dDemandMode
    ): LayerAssignmentScalar {
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
                return shadowPrice
            }
        }
        return layerAssignmentZero()
    }

    override fun invoke(model: AbstractLinearMetaModel<LayerAssignmentScalar>): Try {
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

    override fun extractor(): AbstractBPP3DShadowPriceExtractor<Args, T>? {
        if (shadowPriceExtractor != null) {
            return { _, args -> shadowPriceExtractor.invoke(args) ?: layerAssignmentZero() }
        }

        if (demandEntries.isEmpty()) {
            return null
        }

        return { map, args ->
            var price = layerAssignmentZero()
            for (demand in demandEntries) {
                val concreteMode = demand.mode.toConcreteMode(
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
            price
        }
    }

    override fun refresh(
        shadowPriceMap: AbstractBPP3DShadowPriceMap<Args, T>,
        model: AbstractLinearMetaModel<LayerAssignmentScalar>,
        shadowPrices: MetaDualSolution
    ): Try {
        return CGPipeline.refreshByKeyAsArgs(this, shadowPriceMap, model, shadowPrices)
    }
}

@Deprecated(
    message = "Use DemandConstraint instead.",
    replaceWith = ReplaceWith("DemandConstraint<Args, T>")
)
typealias ItemDemandConstraint<Args, T> = DemandConstraint<Args, T>

@Deprecated(
    message = "Use DemandShadowPriceKey instead.",
    replaceWith = ReplaceWith("DemandShadowPriceKey(mode, key)")
)
typealias ItemDemandShadowPriceKey = DemandShadowPriceKey
