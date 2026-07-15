package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * 按物料维度缓存过滤后的宽度索引，避免对同一物料重复计算。
 * Caches filtered width indices by material dimension to avoid redundant computation.
 * @property baseIndex 基础宽度索引 / Base width index
 * @property maxOverProduceLength 最大超产长度约束，用于长度边界剪枝 / Maximum over-produce length constraint for length-bound pruning
 * @property widthCheck 自定义宽度检查策略，为空时回退到 material.widthRange.canCut / Custom width check strategy; falls back to material.widthRange.canCut when null
*/
internal class GenerationMaterialWidthIndexCache<V : RealNumber<V>>(
    private val baseIndex: GenerationWidthIndex<V>,
    private val maxOverProduceLength: Quantity<V>?,
    private val widthCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
) {
    private val cache = LinkedHashMap<GenerationMaterialWidthRangeKey, CachedWidthIndex<V>>()

    /**
     * 获取指定物料的过滤后宽度索引（命中缓存时直接返回）。
     * Gets the filtered width index for the specified material (returns cached entry on hit).
     * @param material 目标物料 / Target material
     * @param collector 生成过程收集器，用于记录缓存命中与剪枝统计 / Generation collector for recording cache hits and pruning statistics
     * @return 过滤后的宽度索引 / Filtered width index
    */
    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): GenerationWidthIndex<V> {
        val key = material.generationWidthRangeKey()
        val cached = synchronized(cache) {
            cache[key]?.let { existing ->
                collector.recordMaterialWidthIndexCacheHit()
                return@synchronized existing
            }

            val filtered = baseIndex
                .filter { entry ->
                    // Use domain policy width check if provided, otherwise fall back to canCut
                    if (widthCheck != null) {
                        widthCheck(material, entry.product, entry.width)
                    } else {
                        material.widthRange.canCut(entry.width)
                    }
                }
                .filterByLengthBound(maxOverProduceLength)
            val newEntry = CachedWidthIndex(
                widthIndex = filtered.widthIndex,
                lengthBoundPrunedEntries = filtered.prunedEntries
            )
            cache[key] = newEntry
            newEntry
        }
        collector.recordLengthBoundPrunedEntries(cached.lengthBoundPrunedEntries)
        return cached.widthIndex
    }

    /**
     * 缓存中的宽度索引条目，包含原始索引和长度边界剪枝信息。
     * Cached width index entry containing the original index and length-bound pruning information.
     * @property widthIndex 过滤后的宽度索引 / Filtered width index
     * @property lengthBoundPrunedEntries 被长度边界剪枝的条目数 / Number of entries pruned by length bound
    */
    private data class CachedWidthIndex<V : RealNumber<V>>(
        val widthIndex: GenerationWidthIndex<V>,
        val lengthBoundPrunedEntries: Int64
    )
}
