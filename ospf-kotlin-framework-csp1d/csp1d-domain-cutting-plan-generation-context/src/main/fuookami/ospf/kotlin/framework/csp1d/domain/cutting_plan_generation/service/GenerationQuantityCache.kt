package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic

internal class GenerationQuantityCache<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>
) {
    private val repeatWidthCache = HashMap<RepeatWidthKey<V>, Quantity<V>>()
    private val maxRepeatCountCache = HashMap<MaxRepeatCountKey<V>, UInt64>()

    var repeatWidthHits: Int64 = Int64.zero
        private set
    var repeatWidthMisses: Int64 = Int64.zero
        private set
    var maxRepeatCountHits: Int64 = Int64.zero
        private set
    var maxRepeatCountMisses: Int64 = Int64.zero
        private set

    val totalHits: Int64 get() = repeatWidthHits + maxRepeatCountHits
    val totalMisses: Int64 get() = repeatWidthMisses + maxRepeatCountMisses

    fun repeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        val key = RepeatWidthKey(width, times)
        val existing = repeatWidthCache[key]
        if (existing != null) {
            repeatWidthHits = repeatWidthHits + Int64.one
            return existing
        }
        repeatWidthMisses = repeatWidthMisses + Int64.one
        val result = computeRepeatWidth(width, times)
        repeatWidthCache[key] = result
        return result
    }

    fun maxRepeatCount(width: Quantity<V>, availableWidth: Quantity<V>): UInt64 {
        val key = MaxRepeatCountKey(width, availableWidth)
        val existing = maxRepeatCountCache[key]
        if (existing != null) {
            maxRepeatCountHits = maxRepeatCountHits + Int64.one
            return existing
        }
        maxRepeatCountMisses = maxRepeatCountMisses + Int64.one
        val result = computeMaxRepeatCount(width, availableWidth)
        maxRepeatCountCache[key] = result
        return result
    }

    private fun computeRepeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(width.unit)
        repeat(times.toInt()) {
            result = arithmetic.add(result, width)
        }
        return result
    }

    private fun computeMaxRepeatCount(width: Quantity<V>, availableWidth: Quantity<V>): UInt64 {
        if ((availableWidth.value partialOrd width.value) is Order.Less) {
            return UInt64.zero
        }
        var remaining = availableWidth
        var count = UInt64.zero
        while ((remaining.value partialOrd width.value) !is Order.Less) {
            remaining = arithmetic.subtract(remaining, width)
            count = count + UInt64.one
        }
        return count
    }

    private data class RepeatWidthKey<V : RealNumber<V>>(
        val width: Quantity<V>,
        val times: UInt64
    )

    private data class MaxRepeatCountKey<V : RealNumber<V>>(
        val width: Quantity<V>,
        val availableWidth: Quantity<V>
    )
}
