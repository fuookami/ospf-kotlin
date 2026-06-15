package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

internal class GenerationMaterialSliceTemplateCache<V : RealNumber<V>> {
    private val cache = LinkedHashMap<GenerationMaterialWidthRangeKey, List<List<CuttingPlanSlice<V>>>>()

    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): List<List<CuttingPlanSlice<V>>>? {
        val templates = cache[material.generationWidthRangeKey()]
        if (templates != null) {
            collector.recordMaterialSliceTemplateCacheHit()
        } else {
            collector.recordMaterialSliceTemplateCacheMiss()
        }
        return templates
    }

    fun put(
        material: Material<V>,
        templates: List<List<CuttingPlanSlice<V>>>
    ) {
        cache.putIfAbsent(
            material.generationWidthRangeKey(),
            templates
        )
    }
}
