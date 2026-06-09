package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material

internal class GenerationMaterialWidthIndexCache<V : RealNumber<V>>(
    private val baseIndex: GenerationWidthIndex<V>,
    private val maxOverProduceLength: Quantity<V>?
) {
    private val cache = LinkedHashMap<MaterialWidthRangeKey, CachedWidthIndex<V>>()

    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): GenerationWidthIndex<V> {
        val key = material.widthRangeKey()
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

    private fun Material<V>.widthRangeKey(): MaterialWidthRangeKey {
        return MaterialWidthRangeKey(
            lowerValue = widthRange.lowerBound.value.toString(),
            lowerUnit = widthRange.lowerBound.unit.canonicalUnitKey(),
            upperValue = widthRange.upperBound.value.toString(),
            upperUnit = widthRange.upperBound.unit.canonicalUnitKey(),
            lowerInclusive = widthRange.width.lowerInclusive,
            upperInclusive = widthRange.width.upperInclusive,
            stepValue = widthRange.step.value.toString(),
            stepUnit = widthRange.step.unit.canonicalUnitKey()
        )
    }

    private data class CachedWidthIndex<V : RealNumber<V>>(
        val widthIndex: GenerationWidthIndex<V>,
        val lengthBoundPrunedEntries: Long
    )

    private data class MaterialWidthRangeKey(
        val lowerValue: String,
        val lowerUnit: String,
        val upperValue: String,
        val upperUnit: String,
        val lowerInclusive: Boolean,
        val upperInclusive: Boolean,
        val stepValue: String,
        val stepUnit: String
    )
}
