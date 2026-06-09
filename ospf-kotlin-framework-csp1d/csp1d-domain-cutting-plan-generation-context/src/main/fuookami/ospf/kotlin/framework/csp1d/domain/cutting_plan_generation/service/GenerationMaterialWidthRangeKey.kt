package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material

internal data class GenerationMaterialWidthRangeKey(
    val lowerValue: String,
    val lowerUnit: String,
    val upperValue: String,
    val upperUnit: String,
    val lowerInclusive: Boolean,
    val upperInclusive: Boolean,
    val stepValue: String,
    val stepUnit: String
)

internal fun <V : RealNumber<V>> Material<V>.generationWidthRangeKey(): GenerationMaterialWidthRangeKey {
    return GenerationMaterialWidthRangeKey(
        lowerValue = widthRange.lowerBound.value.toString(),
        lowerUnit = widthRange.lowerBound.unit.canonicalUnitKey(),
        upperValue = widthRange.upperBound.value.toString(),
        upperUnit = widthRange.upperBound.unit.canonicalUnitKey(),
        lowerInclusive = widthRange.width.lowerInclusive,
        upperInclusive = widthRange.width.upperInclusive,
        stepValue = widthRange.step.value.toString(),
        stepUnit = widthRange.step.unit.canonicalUnitKey()
    )
}
