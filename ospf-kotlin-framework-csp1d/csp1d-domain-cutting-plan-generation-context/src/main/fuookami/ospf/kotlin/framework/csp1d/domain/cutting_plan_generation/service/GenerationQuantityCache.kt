package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityArithmetic
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 生成量缓存，用于缓存重复宽度和最大重复次数的计算结果，避免重复计算。
 * A cache for generation quantities, caching computed repeat width and max repeat count results to avoid redundant computation.
 *
 * @property arithmetic 量算术运算器，用于执行量的加法和减法操作 / The quantity arithmetic used to perform addition and subtraction on quantities.
*/
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

    /**
     * 获取或计算重复宽度，即宽度乘以重复次数。若缓存中存在则直接返回，否则计算并缓存。
     * Get or compute the repeat width, i.e., width multiplied by the number of times. Returns the cached value if present, otherwise computes and caches it.
     *
     * @param width 单次宽度 / The width per occurrence.
     * @param times 重复次数 / The number of repetitions.
     * @return 重复后的总宽度 / The total width after repetition.
    */
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

    /**
     * 获取或计算在可用宽度内可重复的最大次数。若缓存中存在则直接返回，否则计算并缓存。
     * Get or compute the maximum number of repetitions within the available width. Returns the cached value if present, otherwise computes and caches it.
     *
     * @param width 单次宽度 / The width per occurrence.
     * @param availableWidth 可用总宽度 / The total available width.
     * @return 最大重复次数 / The maximum number of repetitions.
    */
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

    /**
     * 计算重复宽度，通过累加指定次数得到总宽度。
     * Compute the repeat width by accumulating the width for the given number of times.
     *
     * @param width 单次宽度 / The width per occurrence.
     * @param times 重复次数 / The number of repetitions.
     * @return 重复后的总宽度 / The total width after repetition.
    */
    private fun computeRepeatWidth(width: Quantity<V>, times: UInt64): Quantity<V> {
        var result = arithmetic.zero(width.unit)
        repeat(times.toInt()) {
            result = arithmetic.addOrNull(result, width) ?: return result
        }
        return result
    }

    /**
     * 计算在可用宽度内可重复的最大次数，通过循环减法得到结果。
     * Compute the maximum number of repetitions within the available width via repeated subtraction.
     *
     * @param width 单次宽度 / The width per occurrence.
     * @param availableWidth 可用总宽度 / The total available width.
     * @return 最大重复次数 / The maximum number of repetitions.
    */
    private fun computeMaxRepeatCount(width: Quantity<V>, availableWidth: Quantity<V>): UInt64 {
        if ((availableWidth.value partialOrd width.value) is Order.Less) {
            return UInt64.zero
        }
        var remaining = availableWidth
        var count = UInt64.zero
        while ((remaining.value partialOrd width.value) !is Order.Less) {
            remaining = arithmetic.subtractOrNull(remaining, width) ?: return count
            count = count + UInt64.one
        }
        return count
    }

    /**
     * 重复宽度缓存键，由宽度和重复次数组成。
     * Key for the repeat width cache, composed of width and repetition count.
     *
     * @property width 单次宽度 / The width per occurrence.
     * @property times 重复次数 / The number of repetitions.
    */
    private data class RepeatWidthKey<V : RealNumber<V>>(
        val width: Quantity<V>,
        val times: UInt64
    )

    /**
     * 最大重复次数缓存键，由单次宽度和可用宽度组成。
     * Key for the max repeat count cache, composed of width and available width.
     *
     * @property width 单次宽度 / The width per occurrence.
     * @property availableWidth 可用总宽度 / The total available width.
    */
    private data class MaxRepeatCountKey<V : RealNumber<V>>(
        val width: Quantity<V>,
        val availableWidth: Quantity<V>
    )
}
