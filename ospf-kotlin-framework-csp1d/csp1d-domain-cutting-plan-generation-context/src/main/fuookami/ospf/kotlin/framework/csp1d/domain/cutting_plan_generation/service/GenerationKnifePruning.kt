package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * Checks whether the minimum knife count is unreachable from the current search state.
 * 检查从当前搜索状态是否无法达到最小刀数。
 *
 * @param minKnifeCount the minimum knife count constraint, or null if not constrained
 * @param currentCuts the current number of cuts
 * @param startIndex the current width index position
 * @param remainingWidth the remaining width available
 * @param widthIndex the width index for checking fittable entries
 * @param quantityCache the quantity cache for max repeat count computation
 * @param remainingCutCapacity the remaining cut capacity, or null if unlimited
 * @return true if the minimum knife count cannot be reached
 * @return 若无法达到最小刀数则返回 true
*/
internal fun <V : RealNumber<V>> isMinKnifeCountUnreachable(
    minKnifeCount: UInt64?,
    currentCuts: UInt64,
    startIndex: Int,
    remainingWidth: Quantity<V>,
    widthIndex: GenerationWidthIndex<V>,
    quantityCache: GenerationQuantityCache<V>,
    remainingCutCapacity: UInt64? = null
): Boolean {
    val requiredCuts = minKnifeCount ?: return false
    if (currentCuts >= requiredCuts) {
        return false
    }

    val widthLimitedCuts = widthIndex.maxRepeatableFrom(
        startIndex = startIndex,
        remainingWidth = remainingWidth,
        quantityCache = quantityCache
    )
    val maxAdditionalCuts = remainingCutCapacity?.let { capacity ->
        minOf(widthLimitedCuts, capacity)
    } ?: widthLimitedCuts
    return currentCuts + maxAdditionalCuts < requiredCuts
}

/**
 * remainingGenerationCutCapacity.
 * remainingGenerationCutCapacity。
 * @param maxKnifeCount the maximum allowed number of cuts, or null if unlimited / 允许的最大刀数，为 null 时表示无上限
 * @param currentCuts the number of cuts already made / 已使用的刀数
 * @param searchCutCapacity the remaining cut capacity at the search level, or null if not constrained / 搜索层级的剩余可用刀数，为 null 时表示不受限
 * @return the remaining cut capacity constrained by both the max knife count and search capacity, or null if neither is constrained / 受最大刀数和搜索容量共同约束的剩余可用刀数，两者均无限时返回 null
*/
internal fun remainingGenerationCutCapacity(
    maxKnifeCount: UInt64?,
    currentCuts: UInt64,
    searchCutCapacity: UInt64? = null
): UInt64? {
    val maxKnifeCapacity = maxKnifeCount?.let { max ->
        if (currentCuts >= max) {
            UInt64.zero
        } else {
            max - currentCuts
        }
    }
    return when {
        maxKnifeCapacity != null && searchCutCapacity != null -> minOf(maxKnifeCapacity, searchCutCapacity)
        maxKnifeCapacity != null -> maxKnifeCapacity
        else -> searchCutCapacity
    }
}
