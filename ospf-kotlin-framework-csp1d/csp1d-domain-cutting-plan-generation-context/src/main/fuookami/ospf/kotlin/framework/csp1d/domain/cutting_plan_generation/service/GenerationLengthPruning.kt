package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxOverProduceLengthConstraint
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

internal data class GenerationLengthBoundFilterResult<V : RealNumber<V>>(
    val widthIndex: GenerationWidthIndex<V>,
    val prunedEntries: Int64
)
