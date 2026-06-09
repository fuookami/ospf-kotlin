package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.CuttingPlanConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MaxOverProduceLengthConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.MinKnifeCountConstraint
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.WidthUpperBoundConstraint
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

internal class GenerationSliceTemplateRecorder<V : RealNumber<V>> {
    private val recordedTemplates = ArrayList<List<CuttingPlanSlice<V>>>()

    val templates: List<List<CuttingPlanSlice<V>>> get() = recordedTemplates

    fun record(slices: List<CuttingPlanSlice<V>>) {
        recordedTemplates.add(ArrayList(slices))
    }
}
