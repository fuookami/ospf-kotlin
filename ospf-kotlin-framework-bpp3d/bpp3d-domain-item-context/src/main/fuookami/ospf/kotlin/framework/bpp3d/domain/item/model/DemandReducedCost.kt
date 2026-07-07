/**
 * Demand reduced cost model.
 * 需求缩减成本模型。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

private fun demandStatisticsForReducedCost(
    unit: Any,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (unit) {
        is Item -> unit.statistics(mode)
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

fun AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>.reducedCost(
    unit: Any,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>,
    shadowPriceOf: (Bpp3dDemandMode, Bpp3dDemandKey) -> FltX,
    demandValueToScalar: (Bpp3dDemandValue) -> FltX
): FltX {
    val activeDemandEntries = demandEntries.toSet()
    if (activeDemandEntries.isEmpty()) {
        return FltX.zero
    }

    var reducedCost = FltX.zero
    for ((mode, key) in activeDemandEntries) {
        val value = demandStatisticsForReducedCost(unit, mode)[key] ?: continue
        val shadow = shadowPriceOf(mode, key)
        if (shadow neq FltX.zero) {
            reducedCost += shadow * demandValueToScalar(value)
        }
    }
    return reducedCost
}
