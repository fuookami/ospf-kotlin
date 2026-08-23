/**
 * Item aggregation.
 * 货物聚合。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
/**
 * Aggregation class.
 * Aggregation类。
*/
class Aggregation(
    val schemes: Map<BatchNo, Scheme>,
    val bins: Map<BinType<FltX>, UInt64?>
) {
    val batchNos: List<BatchNo> = schemes.asSequence()
        .sortedBy {
            if (it.key == MultiBatchNo) {
                UInt64.maximum
            } else {
                it.value.patternedItems.fold(UInt64.zero) { acc, (_, amount) -> acc + amount }
            }
        }
        .map { it.key }
        .toList()

    private val usedItems = schemes.values.asSequence()
        .flatMap { it.patternedItems.map { item -> Pair(item.first, UInt64.zero) } }
        .toMap<Item, UInt64>()
        .toMutableMap()

    private val usedBins: MutableMap<BinType<FltX>, UInt64> = bins.keys.asSequence()
        .associateWith { UInt64.zero }
        .toSortedMapWithThreeWayComparator { lhs, rhs ->
            if (lhs.isMain) {
                Order.Less()
            } else if (rhs.isMain) {
                Order.Greater()
            } else {
                rhs.volume ord lhs.volume
            }
        }
    val mainBin = bins.keys.find { it.isMain } ?: usedBins.keys.maxByQuantity { it.volume }

/**
 * Merges two demand values of the same type (Amount+Amount or Weight+Weight).
 * 合并两个同类型的需求值（数量+数量或重量+重量）。
 *
 * @param lhs 左侧需求值 / left-hand demand value
 * @param rhs 右侧需求值 / right-hand demand value
 * @return 合并后的需求值，类型不兼容时返回错误 / merged demand value, or error if types are incompatible
*/
    private fun mergeDemandValue(
        lhs: Bpp3dDemandValue,
        rhs: Bpp3dDemandValue
    ): Ret<Bpp3dDemandValue> {
        return when {
            lhs is Bpp3dDemandValue.Amount && rhs is Bpp3dDemandValue.Amount -> ok(Bpp3dDemandValue.Amount(lhs.value + rhs.value))
            lhs is Bpp3dDemandValue.Weight && rhs is Bpp3dDemandValue.Weight -> {
                lhs.value.plusSafe(rhs.value).map { Bpp3dDemandValue.Weight(it) }
            }
            else -> Failed(ErrorCode.IllegalArgument, "Incompatible demand values: $lhs vs $rhs")
        }
    }

/**
 * Aggregates demand statistics for a set of items under the given mode.
 * 在给定模式下聚合一组货物的需求统计。
 *
 * @param items 待聚合的货物到数量映射 / item-to-quantity map to aggregate
 * @param mode 指定聚合维度的需求模式 / demand mode specifying which dimension to aggregate
 * @return 聚合后的需求键值统计映射 / aggregated demand key-value statistics
*/
    private fun aggregateDemand(
        items: Map<Item, UInt64>,
        mode: Bpp3dDemandMode
    ): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
        val counter = mutableMapOf<Bpp3dDemandKey, Bpp3dDemandValue>()
        for ((item, amount) in items) {
            val statistics = item.statistics(mode, amount)
            for ((key, value) in statistics) {
                counter[key] = counter[key]?.let { mergeDemandValue(it, value).value!! } ?: value
            }
        }
        return counter
    }

    val restItems: Map<Item, UInt64>
        get() = schemes.values.asSequence()
            .flatMap {
                it.patternedItems.map { item ->
                    val restAmount = if (item.second leq usedItems[item.first]!!) {
                        UInt64.zero
                    } else {
                        item.second - usedItems[item.first]!!
                    }
                    Pair(item.first, restAmount)
                }
            }
            .toMap()

    val restBins: Map<BinType<FltX>, UInt64>
        get() = bins.asSequence()
            .map {
                val amount = if (it.value == null) {
                    UInt64.maximum
                } else {
                    it.value!! - usedBins[it.key]!!
                }
                Pair(it.key, amount)
            }
            .filter { it.second > UInt64.zero }
            .associate { it }
            .toSortedMapWithThreeWayComparator { lhs, rhs ->
                if (lhs.isMain) {
                    Order.Less()
                } else if (rhs.isMain) {
                    Order.Greater()
                } else {
                    rhs.volume ord lhs.volume
                }
            }

/**
 * Returns the demand statistics for already-used items under the given mode.
 * 返回已使用货物在给定模式下的需求统计。
 *
 * @param mode 指定聚合维度的需求模式 / demand mode specifying which dimension to aggregate
 * @return 已使用货物的需求键值统计映射 / demand key-value statistics for used items
*/
    fun usedDemand(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
        return aggregateDemand(usedItems, mode)
    }

/**
 * Returns the demand statistics for remaining (unused) items under the given mode.
 * 返回剩余（未使用）货物在给定模式下的需求统计。
 *
 * @param mode 指定聚合维度的需求模式 / demand mode specifying which dimension to aggregate
 * @return 剩余货物的需求键值统计映射 / demand key-value statistics for remaining items
*/
    fun restDemand(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
        return aggregateDemand(restItems, mode)
    }

/**
 * use.
 * use。
 * @param items 货物到数量的映射 / item-to-quantity map
*/
    @JvmName("useItems")
    fun use(items: Map<Item, UInt64>) {
        for ((item, amount) in items) {
            usedItems[item] = (usedItems[item] ?: UInt64.zero) + amount
        }
    }

/**
 * Releases previously used bins, decreasing their used count.
 * 释放已使用的箱，减少其已用计数。
 *
 * @param bins 待释放的箱型到数量映射 / bin type to quantity map to release
*/
    fun release(bins: Map<BinType<FltX>, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) - amount
        }
    }

/**
 * Marks bins as used, increasing their used count.
 * 标记箱为已使用，增加其已用计数。
 *
 * @param bins 待使用的箱型到数量映射 / bin type to quantity map to consume
*/
    @JvmName("useBins")
    fun use(bins: Map<BinType<FltX>, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) + amount
        }
    }
}
