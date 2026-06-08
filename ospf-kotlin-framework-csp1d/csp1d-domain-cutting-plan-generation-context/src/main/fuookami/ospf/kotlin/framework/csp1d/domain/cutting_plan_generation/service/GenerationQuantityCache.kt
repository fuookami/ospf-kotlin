package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic

internal class GenerationQuantityCache<V : RealNumber<V>>(
    private val arithmetic: QuantityArithmetic<V>
) {
    private val repeatWidthCache = HashMap<RepeatWidthKey<V>, Quantity<V>>()
    private val maxRepeatCountCache = HashMap<MaxRepeatCountKey<V>, UInt64>()

    fun repeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        return repeatWidthCache.getOrPut(RepeatWidthKey(width, times)) {
            var result = arithmetic.zero(width.unit)
            repeat(times.toInt()) {
                result = arithmetic.add(result, width)
            }
            result
        }
    }

    fun maxRepeatCount(width: Quantity<V>, availableWidth: Quantity<V>): UInt64 {
        return maxRepeatCountCache.getOrPut(MaxRepeatCountKey(width, availableWidth)) {
            if ((availableWidth.value partialOrd width.value) is Order.Less) {
                return@getOrPut UInt64.zero
            }
            var remaining = availableWidth
            var count = UInt64.zero
            while ((remaining.value partialOrd width.value) !is Order.Less) {
                remaining = arithmetic.subtract(remaining, width)
                count = count + UInt64.one
            }
            count
        }
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
