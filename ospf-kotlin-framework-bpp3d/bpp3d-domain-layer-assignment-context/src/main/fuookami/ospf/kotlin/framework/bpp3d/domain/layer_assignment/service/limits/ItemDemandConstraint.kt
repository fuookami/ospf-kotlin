package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandEntry
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
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.*

data class ItemDemandShadowPriceKey(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey
) : ShadowPriceKey(ItemDemandShadowPriceKey::class)

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
        is Bpp3dDemandMode.ItemAmount -> "item_amount"
        is Bpp3dDemandMode.ItemMaterialAmount -> "material_amount"
        is Bpp3dDemandMode.ItemMaterialWeight -> "material_weight"
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

class ItemDemandConstraint<
        Args : AbstractBPP3DShadowPriceArguments<T>,
        T : Cuboid<T>
        >(
    private val load: Load,
    private val demandEntries: List<Bpp3dDemandEntry> = load.demandEntries,
    private val shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
    override val name: String = "item_demand"
) : AbstractBPP3DCGPipeline<Args, T> {
    companion object {
        fun <
                Args : AbstractBPP3DShadowPriceArguments<T>,
                T : Cuboid<T>
                > fromItems(
            load: Load,
            items: List<Pair<Item, UInt64>>,
            shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
            name: String = "item_demand"
        ): ItemDemandConstraint<Args, T> {
            val demands = if (load.demandEntries.isNotEmpty()) {
                load.demandEntries
            } else {
                demandEntriesFromItems(items, load.demandValueAdapter)
            }
            return ItemDemandConstraint(load, demands, shadowPriceExtractor, name)
        }

        fun <
                Args : AbstractBPP3DShadowPriceArguments<T>,
                T : Cuboid<T>
                > fromItemRanges(
            load: Load,
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            shadowPriceExtractor: ((Args) -> LayerAssignmentScalar?)? = null,
            name: String = "item_demand"
        ): ItemDemandConstraint<Args, T> {
            val demands = if (load.demandEntries.isNotEmpty()) {
                load.demandEntries
            } else {
                demandEntriesFromItemRanges(items, load.demandValueAdapter)
            }
            return ItemDemandConstraint(load, demands, shadowPriceExtractor, name)
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

    override fun invoke(model: AbstractLinearMetaModel<LayerAssignmentScalar>): Try {
        for ((i, demand) in demandEntries.withIndex()) {
            val upperBound = demand.demandRange.upperBound.value.unwrap()
            val lowerBound = demand.demandRange.lowerBound.value.unwrap()
            val priceKey = ItemDemandShadowPriceKey(demand.mode, demand.key)
            val tag = modeTag(demand.mode)

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

        val modes = demandEntries.asSequence().map { it.mode }.toSet()
        if (modes.isEmpty()) {
            return null
        }

        return { map, args ->
            var price = layerAssignmentZero()
            for (mode in modes) {
                val statistics = demandStatistics(args.cuboid, mode)
                for ((key, value) in statistics) {
                    val shadow = map[ItemDemandShadowPriceKey(mode, key)]?.price ?: layerAssignmentZero()
                    if (shadow neq layerAssignmentZero()) {
                        price += shadow * load.demandValueAdapter.toSolver(value)
                    }
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

