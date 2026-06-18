/**
 * 需求统计模型。
 * Demand statistics model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

sealed interface Bpp3dDemandMode {
    object Item : Bpp3dDemandMode
    object Material : Bpp3dDemandMode
    object ItemAmount : Bpp3dDemandMode
    object ItemWeight : Bpp3dDemandMode
    object ItemMaterialAmount : Bpp3dDemandMode
    object ItemMaterialWeight : Bpp3dDemandMode
}

fun Bpp3dDemandMode.toConcreteMode(isDiscrete: Boolean): Bpp3dDemandMode {
    return when (this) {
        is Bpp3dDemandMode.Item -> if (isDiscrete) Bpp3dDemandMode.ItemAmount else Bpp3dDemandMode.ItemWeight
        is Bpp3dDemandMode.Material -> if (isDiscrete) Bpp3dDemandMode.ItemMaterialAmount else Bpp3dDemandMode.ItemMaterialWeight
        else -> this
    }
}

sealed interface Bpp3dDemandKey {
    data class Item(val item: fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item) : Bpp3dDemandKey
    data class Material(val material: MaterialKey) : Bpp3dDemandKey
}

fun Bpp3dDemandKey.toConcreteMode(isDiscrete: Boolean): Bpp3dDemandMode {
    return when (this) {
        is Bpp3dDemandKey.Item -> if (isDiscrete) Bpp3dDemandMode.ItemAmount else Bpp3dDemandMode.ItemWeight
        is Bpp3dDemandKey.Material -> if (isDiscrete) Bpp3dDemandMode.ItemMaterialAmount else Bpp3dDemandMode.ItemMaterialWeight
    }
}

fun Bpp3dDemandMode.toConcreteMode(
    key: Bpp3dDemandKey,
    isDiscrete: Boolean
): Bpp3dDemandMode {
    val concreteByMode = toConcreteMode(isDiscrete)
    return when (key) {
        is Bpp3dDemandKey.Item -> when (concreteByMode) {
            is Bpp3dDemandMode.ItemAmount,
            is Bpp3dDemandMode.ItemWeight -> concreteByMode

            else -> key.toConcreteMode(isDiscrete)
        }

        is Bpp3dDemandKey.Material -> when (concreteByMode) {
            is Bpp3dDemandMode.ItemMaterialAmount,
            is Bpp3dDemandMode.ItemMaterialWeight -> concreteByMode

            else -> key.toConcreteMode(isDiscrete)
        }
    }
}

sealed interface Bpp3dDemandValue {
    data class Amount(val value: UInt64) : Bpp3dDemandValue
    data class Weight(val value: Quantity<FltX>) : Bpp3dDemandValue
}

private fun mergeDemandValue(lhs: Bpp3dDemandValue, rhs: Bpp3dDemandValue): Ret<Bpp3dDemandValue> {
    return when {
        lhs is Bpp3dDemandValue.Amount && rhs is Bpp3dDemandValue.Amount -> ok(Bpp3dDemandValue.Amount(lhs.value + rhs.value))
        lhs is Bpp3dDemandValue.Weight && rhs is Bpp3dDemandValue.Weight -> ok(Bpp3dDemandValue.Weight(lhs.value + rhs.value))
        else -> Failed(ErrorCode.IllegalArgument, "Incompatible demand values: $lhs vs $rhs")
    }
}

private fun scaleDemandValue(value: Bpp3dDemandValue, multiplier: UInt64): Bpp3dDemandValue {
    return when (value) {
        is Bpp3dDemandValue.Amount -> Bpp3dDemandValue.Amount(value.value * multiplier)
        is Bpp3dDemandValue.Weight -> Bpp3dDemandValue.Weight(value.value * FltX(multiplier.toULong().toDouble()))
    }
}

private fun MutableMap<Bpp3dDemandKey, Bpp3dDemandValue>.mergeDemand(
    key: Bpp3dDemandKey,
    value: Bpp3dDemandValue
) {
    this[key] = this[key]?.let { mergeDemandValue(it, value).value!! } ?: value
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
    unit: Any,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (unit) {
        is Item -> unit.statistics(mode)
        is ItemContainer<*> -> unit.statistics(mode)
        is Container3<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (unit as Container3<*, FltX>).statistics(mode)
        }
        is Container2<*, *, *> -> {
            @Suppress("UNCHECKED_CAST")
            (unit as Container2<*, FltX, *>).statistics(mode)
        }
        else -> emptyMap()
    }
}

fun Item.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (mode) {
        is Bpp3dDemandMode.Item -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Amount(UInt64.one)
        )

        is Bpp3dDemandMode.Material -> materialAmounts
            .mapKeys { (key, _) -> Bpp3dDemandKey.Material(key) }
            .mapValues { (_, value) -> Bpp3dDemandValue.Amount(value) }

        is Bpp3dDemandMode.ItemAmount -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Amount(UInt64.one)
        )

        is Bpp3dDemandMode.ItemWeight -> mapOf(
            Bpp3dDemandKey.Item(this) to Bpp3dDemandValue.Weight(weight)
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

fun QuantityPlacement2<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in toPlacement3()) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun QuantityPlacement3<*, FltX>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return statisticsOf(unit, mode)
}

fun Projection<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (this) {
        is PlaneProjection<*, FltX, *> -> statisticsOf(unit, mode)
        is PileProjection<*, FltX, *> -> statisticsOf(unit, mode).scale(layer)
        is MultiPileProjection<*, FltX, *> -> {
            val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
            for (view in views) {
                counter.mergeDemand(statisticsOf(view.unit, mode))
            }
            counter
        }
    }
}

fun Container2<*, FltX, *>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun Container3<*, FltX>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in units) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun ItemContainer<*>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return (this as Container3<*, FltX>).statistics(mode)
}

fun Iterable<QuantityPlacement3<*, FltX>>.statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
    for (placement in this) {
        counter.mergeDemand(placement.statistics(mode))
    }
    return counter
}

fun noWeightDemandValue(): Bpp3dDemandValue.Weight = Bpp3dDemandValue.Weight(FltX.zero * Kilogram)
