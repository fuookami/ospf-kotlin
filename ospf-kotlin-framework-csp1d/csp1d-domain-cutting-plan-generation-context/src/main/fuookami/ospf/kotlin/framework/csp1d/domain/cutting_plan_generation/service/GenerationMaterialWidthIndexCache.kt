package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material

internal class GenerationMaterialWidthIndexCache<V : RealNumber<V>>(
    private val baseIndex: GenerationWidthIndex<V>,
    private val maxOverProduceLength: Quantity<V>?
) {
    private val cache = LinkedHashMap<GenerationMaterialWidthRangeKey, CachedWidthIndex<V>>()

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
                .filter { material.widthRange.canCut(it.width) }
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

    private data class CachedWidthIndex<V : RealNumber<V>>(
        val widthIndex: GenerationWidthIndex<V>,
        val lengthBoundPrunedEntries: Long
    )
}
