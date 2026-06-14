package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
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
    private val hits = AtomicReference(Int64.zero)
    private val misses = AtomicReference(Int64.zero)

    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): List<List<CuttingPlanSlice<V>>>? {
        val templates = cache[material.generationWidthRangeKey()]
        return if (templates != null) {
            hits.updateAndGet { it + Int64.one }
            collector.recordMaterialSliceTemplateCacheHit()
            templates
        } else {
            misses.updateAndGet { it + Int64.one }
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

    fun totalHits(): Int64 = hits.get()
    fun totalMisses(): Int64 = misses.get()
}
