@file:Suppress("DEPRECATION")

/**
 * 泛型需求缩减成本模型。
 * Generic demand reduced cost model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 泛型需求影子价格键。
 * Generic demand shadow price key.
 */
data class GenericDemandShadowPriceKey<V : FloatingNumber<V>>(
    val mode: Bpp3dDemandMode,
    val key: GenericBpp3dDemandKey<V>
)

private fun <V : FloatingNumber<V>> demandValueToScalar(
    value: GenericBpp3dDemandValue<V>,
    amountToScalar: (UInt64) -> V
): V {
    return when (value) {
        is GenericBpp3dDemandValue.Amount -> amountToScalar(value.value)
        is GenericBpp3dDemandValue.Weight -> value.value.value
    }
}

private fun <V : FloatingNumber<V>> reducedCostByStatistics(
    statisticsOf: (Bpp3dDemandMode) -> Map<GenericBpp3dDemandKey<V>, GenericBpp3dDemandValue<V>>,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, GenericBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, GenericBpp3dDemandKey<V>) -> V,
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

fun <V : FloatingNumber<V>> GenericItem<V>.reducedCost(
    demandEntries: Iterable<Pair<Bpp3dDemandMode, GenericBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, GenericBpp3dDemandKey<V>) -> V,
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

fun <V : FloatingNumber<V>> GenericItem<V>.reducedCost(
    demandEntries: Iterable<GenericDemandShadowPriceKey<V>>,
    shadowPrices: Map<GenericDemandShadowPriceKey<V>, V>,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCost(
        demandEntries = demandEntries.map { Pair(it.mode, it.key) },
        shadowPriceOf = { mode, key ->
            shadowPrices[GenericDemandShadowPriceKey(mode, key)] ?: zero
        },
        amountToScalar = amountToScalar,
        zero = zero
    )
}

fun <V : FloatingNumber<V>> GenericBinLayer<V>.reducedCost(
    demandEntries: Iterable<Pair<Bpp3dDemandMode, GenericBpp3dDemandKey<V>>>,
    shadowPriceOf: (Bpp3dDemandMode, GenericBpp3dDemandKey<V>) -> V,
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

fun <V : FloatingNumber<V>> GenericBinLayer<V>.reducedCost(
    demandEntries: Iterable<GenericDemandShadowPriceKey<V>>,
    shadowPrices: Map<GenericDemandShadowPriceKey<V>, V>,
    amountToScalar: (UInt64) -> V,
    zero: V
): V {
    return reducedCost(
        demandEntries = demandEntries.map { Pair(it.mode, it.key) },
        shadowPriceOf = { mode, key ->
            shadowPrices[GenericDemandShadowPriceKey(mode, key)] ?: zero
        },
        amountToScalar = amountToScalar,
        zero = zero
    )
}

