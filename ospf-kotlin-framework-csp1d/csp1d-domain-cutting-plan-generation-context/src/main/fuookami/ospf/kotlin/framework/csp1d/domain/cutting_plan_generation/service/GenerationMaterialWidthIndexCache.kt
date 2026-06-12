package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product

internal class GenerationMaterialWidthIndexCache<V : RealNumber<V>>(
    private val baseIndex: GenerationWidthIndex<V>,
    private val maxOverProduceLength: Quantity<V>?,
    private val widthCheck: ((Material<V>, Product<V>, Quantity<V>) -> Boolean)? = null
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

    private data class CachedWidthIndex<V : RealNumber<V>>(
        val widthIndex: GenerationWidthIndex<V>,
        val lengthBoundPrunedEntries: Long
    )
}
