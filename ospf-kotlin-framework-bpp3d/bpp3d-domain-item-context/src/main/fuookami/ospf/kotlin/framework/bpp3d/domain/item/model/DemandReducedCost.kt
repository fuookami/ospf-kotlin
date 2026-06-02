/**
 * 需求缩减成本模型。
 * Demand reduced cost model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.neq

private fun demandStatisticsForReducedCost(
    unit: Any,
    mode: Bpp3dDemandMode
): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
    return when (unit) {
        is Item -> unit.statistics(mode)
        is Container3<*> -> unit.statistics(mode)
        is Container2<*, *> -> unit.statistics(mode)
        else -> emptyMap()
    }
}

fun BPP3DShadowPriceMap.reducedCost(
    unit: Any,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>,
    shadowPriceOf: (Bpp3dDemandMode, Bpp3dDemandKey) -> ShadowPriceNumber,
    demandValueToScalar: (Bpp3dDemandValue) -> ShadowPriceNumber
): ShadowPriceNumber {
    val activeDemandEntries = demandEntries.toSet()
    if (activeDemandEntries.isEmpty()) {
        return ShadowPriceNumber.zero
    }

    var reducedCost = ShadowPriceNumber.zero
    for ((mode, key) in activeDemandEntries) {
        val value = demandStatisticsForReducedCost(unit, mode)[key] ?: continue
        val shadow = shadowPriceOf(mode, key)
        if (shadow neq ShadowPriceNumber.zero) {
            reducedCost += shadow * demandValueToScalar(value)
        }
    }
    return reducedCost
}
