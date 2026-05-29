@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.toFltX
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times





sealed interface GenericBpp3dDemandKey<V : FloatingNumber<V>> {
    data class Item<V : FloatingNumber<V>>(val item: fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item<V>) : GenericBpp3dDemandKey<V>
    data class Material<V : FloatingNumber<V>>(val material: MaterialKey) : GenericBpp3dDemandKey<V>
}

sealed interface GenericBpp3dDemandValue<V : FloatingNumber<V>> {
    data class Amount<V : FloatingNumber<V>>(val value: UInt64) : GenericBpp3dDemandValue<V>
    data class Weight<V : FloatingNumber<V>>(val value: Quantity<V>) : GenericBpp3dDemandValue<V>
}

private fun <V : FloatingNumber<V>> Material<V>.materialKey(): MaterialKey {
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
): Quantity<V> {
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) + (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) + (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported numeric type: ${lhs.value::class.simpleName}")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityScale(
    value: Quantity<V>,
    amount: UInt64
): Quantity<V> {
    val scalarF64 = itemScalar(amount)
    return when (value.value) {
        is InfraNumber -> ((value as Quantity<InfraNumber>) * scalarF64) as Quantity<V>
        is FltX -> ((value as Quantity<FltX>) * scalarF64.toFltX()) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported numeric type: ${value.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> scaleDemandValue(
    value: GenericBpp3dDemandValue<V>,
    multiplier: UInt64
): GenericBpp3dDemandValue<V> {
    return when (value) {
        is GenericBpp3dDemandValue.Amount -> GenericBpp3dDemandValue.Amount(value.value * multiplier)
        is GenericBpp3dDemandValue.Weight -> GenericBpp3dDemandValue.Weight(quantityScale(value.value, multiplier))
    }
}

private fun <V : FloatingNumber<V>> mergeDemandValue(
    lhs: GenericBpp3dDemandValue<V>,
    rhs: GenericBpp3dDemandValue<V>
): GenericBpp3dDemandValue<V> {
    return when {
        lhs is GenericBpp3dDemandValue.Amount && rhs is GenericBpp3dDemandValue.Amount -> {
            GenericBpp3dDemandValue.Amount(lhs.value + rhs.value)
        }

        lhs is GenericBpp3dDemandValue.Weight && rhs is GenericBpp3dDemandValue.Weight -> {
            GenericBpp3dDemandValue.Weight(quantityPlus(lhs.value, rhs.value))
        }

        else -> {
            throw IllegalArgumentException("Incompatible demand values: $lhs vs $rhs")
        }
    }
}

private fun <V : FloatingNumber<V>> MutableMap<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>>.mergeDemand(
    key: GenericBpp3dDemandKey<V>,
    value: GenericBpp3dDemandValue<V>
) {
    this[key] = this[key]?.let { mergeDemandValue(it, value) } ?: value
}

private fun <V : FloatingNumber<V>> Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>>.scale(
    multiplier: UInt64
): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    if (multiplier == UInt64.zero) {
        return emptyMap()
    }
    return mapValues { (_, value) -> scaleDemandValue(value, multiplier) }
}

fun <V : FloatingNumber<V>> Item<V>.statistics(mode: Bpp3dDemandMode): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    return when (mode) {
        is Bpp3dDemandMode.Item -> {
            mapOf(GenericBpp3dDemandKey.Item(this) to GenericBpp3dDemandValue.Amount(UInt64.one))
        }

        is Bpp3dDemandMode.Material -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .mapKeys { (material, _) -> GenericBpp3dDemandKey.Material<V>(material.materialKey()) }
                .mapValues { (_, value) -> GenericBpp3dDemandValue.Amount<V>(value) }
        }

        is Bpp3dDemandMode.ItemAmount -> {
            mapOf(GenericBpp3dDemandKey.Item(this) to GenericBpp3dDemandValue.Amount(UInt64.one))
        }

        is Bpp3dDemandMode.ItemWeight -> {
            mapOf(GenericBpp3dDemandKey.Item(this) to GenericBpp3dDemandValue.Weight(weight))
        }

        is Bpp3dDemandMode.ItemMaterialAmount -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .mapKeys { (material, _) -> GenericBpp3dDemandKey.Material<V>(material.materialKey()) }
                .mapValues { (_, value) -> GenericBpp3dDemandValue.Amount<V>(value) }
        }

        is Bpp3dDemandMode.ItemMaterialWeight -> {
            val materials = pack?.materials ?: emptyMap()
            materials
                .map { (material, amount) ->
                    Pair(
                        GenericBpp3dDemandKey.Material<V>(material.materialKey()),
                        GenericBpp3dDemandValue.Weight<V>(
                            quantityScale(material.weight, amount)
                        )
                    )
                }
                .toMap()
        }
    }
}

fun <V : FloatingNumber<V>> Item<V>.statistics(
    mode: Bpp3dDemandMode,
    amount: UInt64
): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    return statistics(mode).scale(amount)
}

fun <V : FloatingNumber<V>> ItemPlacement<V>.statistics(mode: Bpp3dDemandMode): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    return item.statistics(mode)
}

fun <V : FloatingNumber<V>> BinLayer<V>.statistics(mode: Bpp3dDemandMode): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    val counter = mutableMapOf<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>>()
    for (unit in units) {
        for ((key, value) in unit.statistics(mode)) {
            counter.mergeDemand(key, value)
        }
    }
    return counter
}

fun <V : FloatingNumber<V>> Iterable<BinLayer<V>>.statistics(mode: Bpp3dDemandMode): Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>> {
    val counter = mutableMapOf<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>>()
    for (layer in this) {
        for ((key, value) in layer.statistics(mode)) {
            counter.mergeDemand(key, value)
        }
    }
    return counter
}

