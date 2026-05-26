package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.neq

private fun demandStatisticsForReducedCost(
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

fun BPP3DShadowPriceMap.reducedCost(
    cuboid: Cuboid<*>,
    demandEntries: Iterable<Pair<Bpp3dDemandMode, Bpp3dDemandKey>>,
    shadowPriceOf: (Bpp3dDemandMode, Bpp3dDemandKey) -> ShadowPriceScalar,
    demandValueToScalar: (Bpp3dDemandValue) -> ShadowPriceScalar
): ShadowPriceScalar {
    val activeDemandEntries = demandEntries.toSet()
    if (activeDemandEntries.isEmpty()) {
        return ShadowPriceScalar.zero
    }

    var reducedCost = ShadowPriceScalar.zero
    for ((mode, key) in activeDemandEntries) {
        val value = demandStatisticsForReducedCost(cuboid, mode)[key] ?: continue
        val shadow = shadowPriceOf(mode, key)
        if (shadow neq ShadowPriceScalar.zero) {
            reducedCost += shadow * demandValueToScalar(value)
        }
    }
    return reducedCost
}
