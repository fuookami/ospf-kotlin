package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material

/**
 * 并发安全的物料等价切片模板缓存 / Thread-safe cache for material-equivalent slice templates
 *
 * 当 parallelism > 1 时使用，替代 [GenerationMaterialSliceTemplateCache]。
 * 使用 [ConcurrentHashMap] 和 [AtomicLong] 保证线程安全。
 *
 * Used when parallelism > 1, replacing [GenerationMaterialSliceTemplateCache].
 * Uses [ConcurrentHashMap] and [AtomicLong] for thread safety.
 */
internal class ConcurrentGenerationMaterialSliceTemplateCache<V : RealNumber<V>> {
    private val cache = ConcurrentHashMap<GenerationMaterialWidthRangeKey, List<List<CuttingPlanSlice<V>>>>()
    private val hits = AtomicLong(0L)
    private val misses = AtomicLong(0L)

    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): List<List<CuttingPlanSlice<V>>>? {
        val templates = cache[material.generationWidthRangeKey()]
        return if (templates != null) {
            hits.incrementAndGet()
            collector.recordMaterialSliceTemplateCacheHit()
            templates
        } else {
            misses.incrementAndGet()
            collector.recordMaterialSliceTemplateCacheMiss()
            null
        }
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

    fun totalHits(): Long = hits.get()
    fun totalMisses(): Long = misses.get()
}
