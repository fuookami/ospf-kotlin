package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
/**
 * GenerationProductWidthEntry data class.
 * GenerationProductWidthEntry数据类。
*/
internal data class GenerationProductWidthEntry<V : RealNumber<V>>(
    val product: Product<V>,
    val width: Quantity<V>,
    val demandUnit: PhysicalUnit
)

/**
 * GenerationWidthIndex class.
 * GenerationWidthIndex类。
*/
internal class GenerationWidthIndex<V : RealNumber<V>> private constructor(
    val entries: List<GenerationProductWidthEntry<V>>
) {
    private val suffixMinWidthByUnit: List<Map<String, Quantity<V>>> = buildSuffixMinWidthByUnit(entries)
    private val demandUnitIndex: Map<EntryKey, PhysicalUnit> = buildDemandUnitIndex(entries)

    val isEmpty: Boolean get() = entries.isEmpty()

/**
 * filter.
 * filter。
 * @param predicate 用于筛选条目的条件 / the condition to select entries
 * @return 仅包含匹配谓词条目的新GenerationWidthIndex / a new GenerationWidthIndex containing only the entries that match the predicate
*/
    fun filter(predicate: (GenerationProductWidthEntry<V>) -> Boolean): GenerationWidthIndex<V> {
        return GenerationWidthIndex(entries.filter(predicate))
    }

/**
 * Checks if has fittableFrom.
 * 检查是否具有FittableFrom。
 * @param startIndex 排序条目列表中开始检查的起始索引 / the index in the sorted entries list from which to begin checking
 * @param remainingWidth 当前原材料的剩余可用宽度 / the remaining usable width of the current stock
 * @return 若从startIndex起至少有一个条目能放入剩余宽度则返回true / true if at least one entry from startIndex onward can fit within the remaining width
*/
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

/**
 * maxRepeatableFrom.
 * maxRepeatableFrom。
 * @param startIndex 排序条目列表中开始检查的起始索引 / the index in the sorted entries list from which to begin checking
 * @param remainingWidth 当前原材料的剩余可用宽度 / the remaining usable width of the current stock
 * @param quantityCache 用于计算重复次数的缓存 / the cache for computing repeat counts
 * @return 从startIndex起最窄条目在剩余宽度内可重复的最大次数 / the maximum number of times the narrowest entry from startIndex onward can repeat within the remaining width
*/
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

/**
 * demandUnitFor.
 * demandUnitFor。
 * @param product 要查找的产品 / the product to look up
 * @param width 产品的特定宽度 / the specific width of the product
 * @return 与给定产品和宽度关联的需求单位，未找到则返回null / the demand unit associated with the given product and width, or null if not found
*/
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

/**
 * EntryKey data class.
 * EntryKey数据类。
*/
    private data class EntryKey(
        val productId: ProductId,
        val widthValue: String,
        val widthUnit: String
    )

/**
 * DemandEntryKey data class.
 * DemandEntryKey数据类。
*/
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

/**
 * PhysicalUnit.
 * PhysicalUnit。
 * @return 此物理单位的规范字符串键（符号、名称或字符串表示） / the canonical string key for this physical unit (symbol, name, or string representation)
*/
internal fun PhysicalUnit.canonicalUnitKey(): String {
    return symbol ?: name ?: toString()
}
