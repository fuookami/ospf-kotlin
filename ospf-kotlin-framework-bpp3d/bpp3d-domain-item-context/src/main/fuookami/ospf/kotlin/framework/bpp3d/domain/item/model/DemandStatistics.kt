@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.Kilogram

sealed interface Bpp3dDemandMode {
    object ItemAmount : Bpp3dDemandMode
    object ItemMaterialAmount : Bpp3dDemandMode
    object ItemMaterialWeight : Bpp3dDemandMode
}

sealed interface Bpp3dDemandKey {
    data class Item(val item: fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item) : Bpp3dDemandKey
    data class Material(val material: MaterialKey) : Bpp3dDemandKey
}

sealed interface Bpp3dDemandValue {
    data class Amount(val value: UInt64) : Bpp3dDemandValue
    data class Weight(val value: Quantity<Flt64>) : Bpp3dDemandValue
}

private fun mergeDemandValue(lhs: Bpp3dDemandValue, rhs: Bpp3dDemandValue): Bpp3dDemandValue {
    return when {
        lhs is Bpp3dDemandValue.Amount && rhs is Bpp3dDemandValue.Amount -> Bpp3dDemandValue.Amount(lhs.value + rhs.value)
        lhs is Bpp3dDemandValue.Weight && rhs is Bpp3dDemandValue.Weight -> Bpp3dDemandValue.Weight(lhs.value + rhs.value)
        else -> throw IllegalArgumentException("Incompatible demand values: $lhs vs $rhs")
    }
}

private fun scaleDemandValue(value: Bpp3dDemandValue, multiplier: UInt64): Bpp3dDemandValue {
    return when (value) {
        is Bpp3dDemandValue.Amount -> Bpp3dDemandValue.Amount(value.value * multiplier)
        is Bpp3dDemandValue.Weight -> Bpp3dDemandValue.Weight(value.value * multiplier.asScalarF64())
    }
}

private fun MutableMap<Bpp3dDemandKey, Bpp3dDemandValue>.mergeDemand(
    key: Bpp3dDemandKey,
    value: Bpp3dDemandValue
) {
    this[key] = this[key]?.let { mergeDemandValue(it, value) } ?: value
}

private fun MutableMap<Bpp3dDemandKey, Bpp3dDemandValue>.mergeDemand(
    values: Map<Bpp3dDemandKey, Bpp3dDemandValue>
) {
    for ((key, value) in values) {
        mergeDemand(key, value)
    }
}

private fun Map<Bpp3dDemandKey, Bpp3dDemandValue>.scale(
    multiplier: UInt64
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    if (multiplier == UInt64.zero) {
        return emptyMap()
    }
    return mapValues { (_, value) -> scaleDemandValue(value, multiplier) }
}

private fun statisticsOf(
    unit: AbstractCuboid<Flt64>,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (unit) {
        is Item -> unit.statistics(mode)
        is ItemContainer<*> -> unit.statistics(mode)
        is Container3<*> -> unit.statistics(mode)
        is Container2<*, *> -> unit.statistics(mode)
        else -> emptyMap()
    }
}

fun Item.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (mode) {
        is Bpp3dDemandMode.ItemAmount -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Amount(UInt64.one)
        )

        is Bpp3dDemandMode.ItemMaterialAmount -> materialAmounts
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Amount(value) }

        is Bpp3dDemandMode.ItemMaterialWeight -> materialWeights
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Weight(value) }
    }
}

fun Item.statistics(
    mode: Bpp3dDemandMode,
    amount: UInt64
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return statistics(mode).scale(amount)
}

fun ItemView.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return unit.statistics(mode)
}

fun Placement2<*, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in toPlacement3()) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun Placement3<*>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return statisticsOf(unit, mode)
}

fun Projection<*, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (this) {
        is PlaneProjection<*, *> -> statisticsOf(unit, mode)
        is PileProjection<*, *> -> statisticsOf(unit, mode).scale(layer)
        is MultiPileProjection<*, *> -> {
            val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
            for (view in views) {
                counter.mergeDemand(statisticsOf(view.unit, mode))
            }
            counter
        }
    }
}

fun Container2<*, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun Container3<*>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun ItemContainer<*>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return (this as Container3<*>).statistics(mode)
}

fun Iterable<Placement3<*>>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in this) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun noWeightDemandValue(): Bpp3dDemandValue.Weight = Bpp3dDemandValue.Weight(Flt64.zero * Kilogram)


