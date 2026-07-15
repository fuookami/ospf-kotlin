package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
/**
 * GenerationMaterialSliceTemplateCache class.
 * GenerationMaterialSliceTemplateCache类。
*/
internal class GenerationMaterialSliceTemplateCache<V : RealNumber<V>> {
    private val cache = LinkedHashMap<GenerationMaterialWidthRangeKey, List<List<CuttingPlanSlice<V>>>>()

/**
 * get.
 * get。
 * @param material the material whose width range key is used for cache lookup / 用于通过宽度范围键查找缓存的物料
 * @param collector the generation collector for recording cache hit/miss statistics / 用于记录缓存命中与未命中统计的生成收集器
 * @return the cached slice templates for the material's width range, or null if not cached / 该物料宽度范围对应的缓存切割方案模板，若未缓存则返回null
*/
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

/**
 * put.
 * put。
 * @param material the material whose width range key is used as the cache key / 用于以其宽度范围键作为缓存键的物料
 * @param templates the cutting plan slice templates to cache / 要缓存的切割方案切片模板
*/
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
