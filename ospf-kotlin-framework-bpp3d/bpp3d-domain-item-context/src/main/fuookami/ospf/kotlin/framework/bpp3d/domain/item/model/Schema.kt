package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

class Scheme(
    val actualItems: List<Triple<ActualItem, UInt64, ValueRange<UInt64>>> = emptyList(),
) {
    val patternedItems: List<Triple<PatternedItem, UInt64, ValueRange<UInt64>>>

    init {
        val patternedItems = ArrayList<Pair<ItemPattern, MutableList<Triple<ActualItem, UInt64, ValueRange<UInt64>>>>>()
        for (item in actualItems.sortedBy { it.first.weight }) {
            val thisPattern = item.first.pattern

            var flag = false
            for ((pattern, thisPatternItems) in patternedItems) {
                if (pattern == thisPattern) {
                    thisPatternItems.add(item)
                    flag = true
                    break
                }
            }
            if (!flag) {
                patternedItems.add(Pair(thisPattern, arrayListOf(item)))
            }
        }
        this.patternedItems = patternedItems.map { PatternedItem(it.first, it.second) }
    }
}

fun <T : Item> List<Triple<T, UInt64, UInt64>>.pack(): Map<T, UInt64> {
    return this.associate { Pair(it.first, it.second) }
}
