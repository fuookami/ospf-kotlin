package fuookami.ospf.kotlin.framework.bpp3d.domain.item

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class Aggregation(
    val schemes: Map<BatchNo, Scheme>,
    val bins: Map<BinType, UInt64?>
) {
    val batchNos: List<BatchNo> = schemes.asSequence()
        .sortedBy {
            if (it.key == MultiBatchNo) {
                UInt64.maximum
            } else {
                it.value.patternedItems.sumOf { (_, amount) -> amount }
            }
        }
        .map { it.key }
        .toList()

    private val usedItems = schemes.values.asSequence()
        .flatMap { it.patternedItems.map { item -> Pair(item.first, UInt64.zero) } }
        .toMap<Item, UInt64>()
        .toMutableMap()

    private val usedBins: MutableMap<BinType, UInt64> = bins.keys.asSequence()
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
    val mainBin = bins.keys.find { it.isMain } ?: usedBins.keys.maxBy { it.volume }

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

    val restBins: Map<BinType, UInt64>
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

    @JvmName("useItems")
    fun use(items: Map<Item, UInt64>) {
        for ((item, amount) in items) {
            usedItems[item] = (usedItems[item] ?: UInt64.zero) + amount
        }
    }

    fun release(bins: Map<BinType, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) - amount
        }
    }

    @JvmName("useBins")
    fun use(bins: Map<BinType, UInt64>) {
        for ((bin, amount) in bins) {
            usedBins[bin] = (usedBins[bin] ?: UInt64.zero) + amount
        }
    }
}
