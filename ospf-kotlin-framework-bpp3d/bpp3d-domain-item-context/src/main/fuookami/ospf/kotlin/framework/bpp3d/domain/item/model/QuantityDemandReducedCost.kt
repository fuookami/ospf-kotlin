/**
 * Quantity demand reduced cost model.
 * 泛型需求缩减成本模型。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 泛型需求影子价格键。
 * Quantity demand shadow price key.
 */
data class QuantityDemandShadowPriceKey<V : FloatingNumber<V>>(
    val mode: Bpp3dDemandMode,
    val key: QuantityBpp3dDemandKey<V>
)

private fun <V : FloatingNumber<V>> demandValueToScalar(
    value: QuantityBpp3dDemandValue<V>,
    amountToScalar: (UInt64) -> V
): V {
    return when (value) {
        is QuantityBpp3dDemandValue.Amount -> amountToScalar(value.value)
        is QuantityBpp3dDemandValue.Weight -> value.value.value
    }
}

private fun <V : FloatingNumber<V>> reducedCostByStatistics(
    statisticsOf: (Bpp3dDemandMode) -> Map<QuantityBpp3dDemandKey<V>, QuantityBpp3dDemandValue<V>>,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, QuantityBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, QuantityBpp3dDemandKey<V>) -> V,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    val activeDemandEntries = demandEntries.toSet()
    if (activeDemandEntries.isEmpty()) {
        return zero
    }

    var reducedCost = zero
    for ((mode, key) in activeDemandEntries) {
        val value = statisticsOf(mode)[key] ?: continue
        val shadowPrice = shadowPriceOf(mode, key)
        reducedCost += shadowPrice * demandValueToScalar(value, amountToScalar)
    }
    return reducedCost
}

fun <V : FloatingNumber<V>> QuantityItem<V>.reducedCost(
    demandEntries: Iterable<Pair<Bpp3dDemandMode, QuantityBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, QuantityBpp3dDemandKey<V>) -> V,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCostByStatistics(
        statisticsOf = { mode -> statistics(mode) },
        demandEntries = demandEntries,
        shadowPriceOf = shadowPriceOf,
        amountToScalar = amountToScalar,
        zero = zero
    )
}

fun <V : FloatingNumber<V>> QuantityItem<V>.reducedCost(
    demandEntries: Iterable<QuantityDemandShadowPriceKey<V>>,
    shadowPrices: Map<QuantityDemandShadowPriceKey<V>, V>,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCost(
        demandEntries = demandEntries.map { Pair(it.mode, it.key) },
        shadowPriceOf = { mode, key ->
            shadowPrices[QuantityDemandShadowPriceKey(mode, key)] ?: zero
        },
        amountToScalar = amountToScalar,
        zero = zero
    )
}

fun <V : FloatingNumber<V>> QuantityBinLayer<V>.reducedCost(
    demandEntries: Iterable<Pair<Bpp3dDemandMode, QuantityBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, QuantityBpp3dDemandKey<V>) -> V,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCostByStatistics(
        statisticsOf = { mode -> statistics(mode) },
        demandEntries = demandEntries,
        shadowPriceOf = shadowPriceOf,
        amountToScalar = amountToScalar,
        zero = zero
    )
}

fun <V : FloatingNumber<V>> QuantityBinLayer<V>.reducedCost(
    demandEntries: Iterable<QuantityDemandShadowPriceKey<V>>,
    shadowPrices: Map<QuantityDemandShadowPriceKey<V>, V>,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCost(
        demandEntries = demandEntries.map { Pair(it.mode, it.key) },
        shadowPriceOf = { mode, key ->
            shadowPrices[QuantityDemandShadowPriceKey(mode, key)] ?: zero
        },
        amountToScalar = amountToScalar,
        zero = zero
    )
}
