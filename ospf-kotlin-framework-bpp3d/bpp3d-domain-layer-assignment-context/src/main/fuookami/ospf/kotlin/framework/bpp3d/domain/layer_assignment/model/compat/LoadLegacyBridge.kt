package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

fun <V : FloatingNumber<V>> toLegacyItems(
    items: List<Pair<QuantityItem<V>, UInt64>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Pair<Item, UInt64>> {
    return items.map { (item, demand) ->
        Pair(item.toLegacy(materialCache, legacyItemCache), demand)
    }
}

fun <V : FloatingNumber<V>> toLegacyItemRanges(
    items: List<Triple<QuantityItem<V>, UInt64, ValueRange<UInt64>>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Triple<Item, UInt64, ValueRange<UInt64>>> {
    return items.map { (item, demand, demandRange) ->
        Triple(item.toLegacy(materialCache, legacyItemCache), demand, demandRange)
    }
}

fun <V : FloatingNumber<V>> toLegacyLayers(
    layers: List<QuantityBinLayer<V>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<BinLayer> {
    return layers.map { it.toLegacy(materialCache, legacyItemCache) }
}
