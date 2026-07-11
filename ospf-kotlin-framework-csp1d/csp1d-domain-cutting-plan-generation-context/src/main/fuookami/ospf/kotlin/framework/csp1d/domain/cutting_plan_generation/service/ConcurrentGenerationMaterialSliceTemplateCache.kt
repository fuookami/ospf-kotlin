package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

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

    /**
     * Retrieves cached slice templates for the given material.
     * 获取给定物料的缓存切片模板。
     *
     * @param material the material to look up slice templates for / 要查找切片模板的物料
     * @param collector the generation collector for recording cache statistics / 用于记录缓存统计的方案收集器
     * @return the cached slice templates, or null if not present / 缓存的切片模板，若不存在则返回 null
    */
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

    /**
     * Stores slice templates for the given material if not already cached.
     * 若给定物料尚未缓存，则存储其切片模板。
     *
     * @param material the material to cache slice templates for / 要缓存切片模板的物料
     * @param templates the slice templates to cache / 要缓存的切片模板
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

    /**
     * 获取缓存命中总数 / Get the total number of cache hits
     *
     * @return 缓存命中总数 / total cache hits
    */
    fun totalHits(): Int64 = hits.get()

    /**
     * 获取缓存未命中总数 / Get the total number of cache misses
     *
     * @return 缓存未命中总数 / total cache misses
    */
    fun totalMisses(): Int64 = misses.get()
}
