/**
 * 货物聚合。
 * Item aggregation.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

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

    private fun mergeDemandValue(
        lhs: Bpp3dDemandValue,
        rhs: Bpp3dDemandValue
    ): Ret<Bpp3dDemandValue> {
        return when {
            lhs is Bpp3dDemandValue.Amount && rhs is Bpp3dDemandValue.Amount -> ok(Bpp3dDemandValue.Amount(lhs.value + rhs.value))
            lhs is Bpp3dDemandValue.Weight && rhs is Bpp3dDemandValue.Weight -> ok(Bpp3dDemandValue.Weight(lhs.value + rhs.value))
            else -> Failed(ErrorCode.IllegalArgument, "Incompatible demand values: $lhs vs $rhs")
        }
    }

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

    fun usedDemand(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
        return aggregateDemand(usedItems, mode)
    }

    fun restDemand(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue> {
        return aggregateDemand(restItems, mode)
    }

    @JvmName("useItems")
    fun use(items: Map<Item, UInt64>) {
        for ((item, amount) in items) {
            usedItems[item] = (usedItems[item] ?: UInt64.zero) + amount
        }
    }

    fun release(bins: Map<BinType<FltX>, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) - amount
        }
    }

    @JvmName("useBins")
    fun use(bins: Map<BinType<FltX>, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) + amount
        }
    }
}
