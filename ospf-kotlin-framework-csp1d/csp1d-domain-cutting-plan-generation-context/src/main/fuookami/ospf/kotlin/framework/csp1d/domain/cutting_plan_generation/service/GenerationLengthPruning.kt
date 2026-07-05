package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product

internal fun <V : RealNumber<V>> generationMaxOverProduceLength(
    constraints: List<CuttingPlanConstraint<V>>
): Quantity<V>? {
    return constraints.filterIsInstance<MaxOverProduceLengthConstraint<V>>().firstOrNull()?.value
}

internal fun <V : RealNumber<V>> Product<V>.fitsGenerationLengthBound(
    maxOverProduceLength: Quantity<V>?
): Boolean {
    val maxLength = maxOverProduceLength ?: return true
    val productLength = length ?: return true
    return (productLength.value partialOrd maxLength.value) !is Order.Greater
}

internal fun <V : RealNumber<V>> GenerationWidthIndex<V>.filterByLengthBound(
    maxOverProduceLength: Quantity<V>?
): GenerationLengthBoundFilterResult<V> {
    if (maxOverProduceLength == null) {
        return GenerationLengthBoundFilterResult(
            widthIndex = this,
            prunedEntries = Int64.zero
        )
    }

    var prunedEntries = Int64.zero
    val filtered = filter { entry ->
        val accepted = entry.product.fitsGenerationLengthBound(maxOverProduceLength)
        if (!accepted) {
            prunedEntries = prunedEntries + Int64.one
        }
        accepted
    }
    return GenerationLengthBoundFilterResult(
        widthIndex = filtered,
        prunedEntries = prunedEntries
    )
}

internal fun <V : RealNumber<V>> GenerationWidthIndex<V>.filterByLengthBound(
    maxOverProduceLength: Quantity<V>?,
    collector: GenerationCollector<V>
): GenerationWidthIndex<V> {
    val result = filterByLengthBound(maxOverProduceLength)
    collector.recordLengthBoundPrunedEntries(result.prunedEntries)
    return result.widthIndex
}

/**
 * 生成长度边界过滤结果，包含过滤后的宽度索引和剪枝条目数。
 * Result of generation length bound filtering, containing the filtered width index and pruned entry count.
 *
 * @property widthIndex 过滤后的宽度索引 / The filtered width index
 * @property prunedEntries 被剪枝的条目数 / The number of pruned entries
 */
internal data class GenerationLengthBoundFilterResult<V : RealNumber<V>>(
    val widthIndex: GenerationWidthIndex<V>,
    val prunedEntries: Int64
)
