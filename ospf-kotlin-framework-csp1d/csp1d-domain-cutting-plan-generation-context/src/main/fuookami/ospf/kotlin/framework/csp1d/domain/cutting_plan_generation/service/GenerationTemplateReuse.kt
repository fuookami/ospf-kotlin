package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice

internal fun <V : RealNumber<V>> canReuseMaterialSliceTemplates(
    constraints: List<CuttingPlanConstraint<V>>
): Boolean {
    return constraints.all { constraint ->
        constraint is MaxKnifeCountConstraint<V> ||
                constraint is MinKnifeCountConstraint<V> ||
                constraint is MaxOverProduceLengthConstraint<V> ||
                constraint is WidthUpperBoundConstraint<V>
    }
}

/**
 * GenerationSliceTemplateRecorder class.
 * GenerationSliceTemplateRecorder类。
*/
internal class GenerationSliceTemplateRecorder<V : RealNumber<V>> {
    private val recordedTemplates = ArrayList<List<CuttingPlanSlice<V>>>()

    val templates: List<List<CuttingPlanSlice<V>>> get() = recordedTemplates

/**
 * record.
 * record。
 * @param slices the list of cutting plan slices to record as a template / 要记录为模板的切割方案切片列表
*/
    fun record(slices: List<CuttingPlanSlice<V>>) {
        recordedTemplates.add(ArrayList(slices))
    }
}
