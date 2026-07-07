package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * Checks whether the minimum knife count is unreachable from the current search state.
 * 检查从当前搜索状态是否无法达到最小刀数。
 *
 * @param minKnifeCount the minimum knife count constraint, or null if not constrained
 * @param minKnifeCount 最小刀数约束，若不受约束则为 null
 * @param currentCuts the current number of cuts
 * @param currentCuts 当前的刀数
 * @param startIndex the current width index position
 * @param startIndex 当前的宽度索引位置
 * @param remainingWidth the remaining width available
 * @param remainingWidth 剩余可用宽度
 * @param widthIndex the width index for checking fittable entries
 * @param widthIndex 用于检查可放入条目的宽度索引
 * @param quantityCache the quantity cache for max repeat count computation
 * @param quantityCache 用于计算最大重复次数的数量缓存
 * @param remainingCutCapacity the remaining cut capacity, or null if unlimited
 * @param remainingCutCapacity 剩余切割容量，若无限制则为 null
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
