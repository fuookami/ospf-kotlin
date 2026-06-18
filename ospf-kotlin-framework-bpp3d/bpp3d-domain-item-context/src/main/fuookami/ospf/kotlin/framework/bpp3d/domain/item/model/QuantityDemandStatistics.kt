/**
 * 泛型需求统计模型。
 * Quantity demand statistics model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 泛型需求键。
 * Quantity demand key.
 */
sealed interface QuantityBpp3dDemandKey<V : FloatingNumber<V>> {
    data class Item<V : FloatingNumber<V>>(val item: QuantityItem<V>) : QuantityBpp3dDemandKey<V>
    data class Material<V : FloatingNumber<V>>(val material: MaterialKey) : QuantityBpp3dDemandKey<V>
}

/**
 * 泛型需求值。
 * Quantity demand value.
 */
sealed interface QuantityBpp3dDemandValue<V : FloatingNumber<V>> {
    data class Amount<V : FloatingNumber<V>>(val value: UInt64) : QuantityBpp3dDemandValue<V>
    data class Weight<V : FloatingNumber<V>>(val value: Quantity<V>) : QuantityBpp3dDemandValue<V>
}

private fun <V : FloatingNumber<V>> QuantityMaterial<V>.materialKey(): MaterialKey {
    return MaterialKey(
        no = no,
        type = type,
        manufacturer = manufacturer,
        supplier = supplier
    )
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityPlus(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Ret<Quantity<V>> {
    return when (lhs.value) {
        is FltX -> ok(((lhs as Quantity<FltX>) + (rhs as Quantity<FltX>)) as Quantity<V>)
        else -> Failed(ErrorCode.IllegalArgument, "Unsupported numeric type: ${lhs.value::class.simpleName}")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityScale(
    value: Quantity<V>,
    amount: UInt64
): Ret<Quantity<V>> {
    val scalarF64 = FltX(amount.toULong().toDouble())
    return when (value.value) {
        is FltX -> ok(((value as Quantity<FltX>) * scalarF64) as Quantity<V>)
        else -> Failed(ErrorCode.IllegalArgument, "Unsupported numeric type: ${value.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> scaleDemandValue(
    value: QuantityBpp3dDemandValue<V>,
    multiplier: UInt64
): QuantityBpp3dDemandValue<V> {
    return when (value) {
        is QuantityBpp3dDemandValue.Amount -> QuantityBpp3dDemandValue.Amount(value.value * multiplier)
        is QuantityBpp3dDemandValue.Weight -> QuantityBpp3dDemandValue.Weight(quantityScale(value.value, multiplier).value!!)
    }
}

private fun <V : FloatingNumber<V>> mergeDemandValue(
    lhs: QuantityBpp3dDemandValue<V>,
    rhs: QuantityBpp3dDemandValue<V>
): Ret<QuantityBpp3dDemandValue<V>> {
    return when {
        lhs is QuantityBpp3dDemandValue.Amount && rhs is QuantityBpp3dDemandValue.Amount -> {
            ok(QuantityBpp3dDemandValue.Amount(lhs.value + rhs.value))
        }

        lhs is QuantityBpp3dDemandValue.Weight && rhs is QuantityBpp3dDemandValue.Weight -> {
            ok(QuantityBpp3dDemandValue.Weight(quantityPlus(lhs.value, rhs.value).value!!))
        }

        else -> {
            Failed(ErrorCode.IllegalArgument, "Incompatible demand values: $lhs vs $rhs")
        }
    }
}

private fun <V : FloatingNumber<V>> MutableMap<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>>.mergeDemand(
    key: QuantityBpp3dDemandKey<V>,
    value: QuantityBpp3dDemandValue<V>
) {
    this[key] = this[key]?.let { mergeDemandValue(it, value).value!! } ?: value
}

private fun <V : FloatingNumber<V>> Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>>.scale(
    multiplier: UInt64
): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    if (multiplier == UInt64.zero) {
        return emptyMap()
    }
    return mapValues { (_, value) -> scaleDemandValue(value, multiplier) }
}

fun <V : FloatingNumber<V>> QuantityItem<V>.statistics(mode: Bpp3dDemandMode): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    return when (mode) {
        is Bpp3dDemandMode.Item -> {
            mapOf(QuantityBpp3dDemandKey.Item(this) to QuantityBpp3dDemandValue.Amount(UInt64.one))
        }

        is Bpp3dDemandMode.Material -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .mapKeys { (material, _) -> QuantityBpp3dDemandKey.Material<V>(material.materialKey()) }
                .mapValues { (_, value) -> QuantityBpp3dDemandValue.Amount<V>(value) }
        }

        is Bpp3dDemandMode.ItemAmount -> {
            mapOf(QuantityBpp3dDemandKey.Item(this) to QuantityBpp3dDemandValue.Amount(UInt64.one))
        }

        is Bpp3dDemandMode.ItemWeight -> {
            mapOf(QuantityBpp3dDemandKey.Item(this) to QuantityBpp3dDemandValue.Weight(weight))
        }

        is Bpp3dDemandMode.ItemMaterialAmount -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .mapKeys { (material, _) -> QuantityBpp3dDemandKey.Material<V>(material.materialKey()) }
                .mapValues { (_, value) -> QuantityBpp3dDemandValue.Amount<V>(value) }
        }

        is Bpp3dDemandMode.ItemMaterialWeight -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .map { (material, amount) ->
                    Pair(
                        QuantityBpp3dDemandKey.Material<V>(material.materialKey()),
                        QuantityBpp3dDemandValue.Weight<V>(
                            quantityScale(material.weight, amount).value!!
                        )
                    )
                }
                .toMap()
        }
    }
}

fun <V : FloatingNumber<V>> QuantityItem<V>.statistics(
    mode: Bpp3dDemandMode,
    amount: UInt64
): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    return statistics(mode).scale(amount)
}

fun <V : FloatingNumber<V>> QuantityItemPlacement<V>.statistics(mode: Bpp3dDemandMode): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    return item.statistics(mode)
}

fun <V : FloatingNumber<V>> QuantityBinLayer<V>.statistics(mode: Bpp3dDemandMode): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    val counter = mutableMapOf<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>>()
    for (unit in units) {
        for ((key, value) in unit.statistics(mode)) {
            counter.mergeDemand(key, value)
        }
    }
    return counter
}

fun <V : FloatingNumber<V>> Iterable<QuantityBinLayer<V>>.statistics(mode: Bpp3dDemandMode): Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>> {
    val counter = mutableMapOf<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>>()
    for (layer in this) {
        for ((key, value) in layer.statistics(mode)) {
            counter.mergeDemand(key, value)
        }
    }
    return counter
}
