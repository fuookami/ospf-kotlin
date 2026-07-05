package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

internal data class GenerationProductWidthEntry<V : RealNumber<V>>(
    val product: Product<V>,
    val width: Quantity<V>,
    val demandUnit: PhysicalUnit
)

internal class GenerationWidthIndex<V : RealNumber<V>> private constructor(
    val entries: List<GenerationProductWidthEntry<V>>
) {
    private val suffixMinWidthByUnit: List<Map<String, Quantity<V>>> = buildSuffixMinWidthByUnit(entries)
    private val demandUnitIndex: Map<EntryKey, PhysicalUnit> = buildDemandUnitIndex(entries)

    val isEmpty: Boolean get() = entries.isEmpty()

    fun filter(predicate: (GenerationProductWidthEntry<V>) -> Boolean): GenerationWidthIndex<V> {
        return GenerationWidthIndex(entries.filter(predicate))
    }

    fun hasFittableFrom(
        startIndex: Int,
        remainingWidth: Quantity<V>
    ): Boolean {
        if (startIndex >= entries.size) {
            return false
        }
        val minWidth = suffixMinWidthByUnit[startIndex][remainingWidth.unit.canonicalUnitKey()] ?: return false
        return (remainingWidth.value partialOrd minWidth.value) !is Order.Less
    }

    fun maxRepeatableFrom(
        startIndex: Int,
        remainingWidth: Quantity<V>,
        quantityCache: GenerationQuantityCache<V>
    ): UInt64 {
        if (startIndex >= entries.size) {
            return UInt64.zero
        }
        val minWidth = suffixMinWidthByUnit[startIndex][remainingWidth.unit.canonicalUnitKey()] ?: return UInt64.zero
        return quantityCache.maxRepeatCount(minWidth, remainingWidth)
    }

    fun demandUnitFor(
        product: Product<V>,
        width: Quantity<V>
    ): PhysicalUnit? {
        return demandUnitIndex[
            EntryKey(
                productId = product.id,
                widthValue = width.value.toString(),
                widthUnit = width.unit.canonicalUnitKey()
            )
        ]
    }

    private data class EntryKey(
        val productId: ProductId,
        val widthValue: String,
        val widthUnit: String
    )

    private data class DemandEntryKey(
        val productId: ProductId,
        val widthValue: String,
        val widthUnit: String,
        val demandUnit: String
    )

    companion object {
        fun <V : RealNumber<V>> fromDemands(demands: List<ProductDemand<V>>): GenerationWidthIndex<V> {
            val seen = LinkedHashSet<DemandEntryKey>()
            val entries = ArrayList<GenerationProductWidthEntry<V>>()
            for (demand in demands) {
                for (width in demand.product.width) {
                    val key = DemandEntryKey(
                        productId = demand.product.id,
                        widthValue = width.value.toString(),
                        widthUnit = width.unit.canonicalUnitKey(),
                        demandUnit = demand.quantity.unit.canonicalUnitKey()
                    )
                    if (!seen.add(key)) {
                        continue
                    }
                    entries.add(
                        GenerationProductWidthEntry(
                            product = demand.product,
                            width = width,
                            demandUnit = demand.quantity.unit
                        )
                    )
                }
            }
            return GenerationWidthIndex(entries)
        }

        private fun <V : RealNumber<V>> buildSuffixMinWidthByUnit(
            entries: List<GenerationProductWidthEntry<V>>
        ): List<Map<String, Quantity<V>>> {
            val suffix = MutableList(entries.size) {
                emptyMap<String, Quantity<V>>()
            }
            val current = HashMap<String, Quantity<V>>()
            for (index in entries.indices.reversed()) {
                val entry = entries[index]
                val unitKey = entry.width.unit.canonicalUnitKey()
                val existing = current[unitKey]
                if (existing == null || (entry.width.value partialOrd existing.value) is Order.Less) {
                    current[unitKey] = entry.width
                }
                suffix[index] = HashMap(current)
            }
            return suffix
        }

        private fun <V : RealNumber<V>> buildDemandUnitIndex(
            entries: List<GenerationProductWidthEntry<V>>
        ): Map<EntryKey, PhysicalUnit> {
            val index = LinkedHashMap<EntryKey, PhysicalUnit>()
            for (entry in entries) {
                index.putIfAbsent(
                    entryKey(entry),
                    entry.demandUnit
                )
            }
            return index
        }

        private fun <V : RealNumber<V>> entryKey(entry: GenerationProductWidthEntry<V>): EntryKey {
            return EntryKey(
                productId = entry.product.id,
                widthValue = entry.width.value.toString(),
                widthUnit = entry.width.unit.canonicalUnitKey()
            )
        }
    }
}

internal fun PhysicalUnit.canonicalUnitKey(): String {
    return symbol ?: name ?: toString()
}
