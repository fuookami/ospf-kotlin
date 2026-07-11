/**
 * Demand reduced cost model.
 * 需求缩减成本模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
/**
 * demandStatisticsForReducedCost.
 * demandStatisticsForReducedCost。
 * @param unit packing unit (Item, Container3, or Container2) / 装箱单元（货物、三维容器或二维容器）
 * @param mode demand mode for statistics / 需求统计模式
 * @return demand statistics map for the given unit and mode / 给定单元和模式的需求统计映射
*/
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

/**
 * AbstractBPP3DShadowPriceMap.
 * AbstractBPP3DShadowPriceMap。
 * @param unit packing unit (Item, Container3, or Container2) / 装箱单元（货物、三维容器或二维容器）
 * @param demandEntries active demand mode-key pairs / 活跃的需求模式-键对
 * @param shadowPriceOf function to look up shadow price by mode and key / 按模式和键查找影子价格的函数
 * @return reduced cost value / 缩减成本值
*/
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
