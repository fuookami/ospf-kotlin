package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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
