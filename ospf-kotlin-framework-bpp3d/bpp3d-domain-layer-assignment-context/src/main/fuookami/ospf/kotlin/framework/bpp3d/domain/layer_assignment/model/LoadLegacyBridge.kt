package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.asScalarF64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dItemDemand
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dMaterialDemand
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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

fun <V : FloatingNumber<V>> toLegacyItemDemands(
    items: List<Pair<QuantityItem<V>, Quantity<V>>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Bpp3dItemDemand> {
    return items.map { (item, quantity) ->
        Bpp3dItemDemand(
            item = item.toLegacy(materialCache, legacyItemCache),
            quantity = quantity.asScalarF64()
        )
    }
}

fun <V : FloatingNumber<V>> toLegacyMaterialDemands(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>
): List<Bpp3dMaterialDemand> {
    return materials.map { (material, demand) ->
        Bpp3dMaterialDemand(
            material = MaterialKey(
                no = material.no,
                type = material.type,
                manufacturer = material.manufacturer,
                supplier = material.supplier
            ),
            quantity = demand.asScalarF64()
        )
    }
}

fun <V : FloatingNumber<V>> toLegacyMaterialWeightDemandsByKey(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>
): List<Pair<MaterialKey, Quantity<Flt64>>> {
    return materials.map { (material, demand) ->
        Pair(
            MaterialKey(
                no = material.no,
                type = material.type,
                manufacturer = material.manufacturer,
                supplier = material.supplier
            ),
            demand.asScalarF64()
        )
    }
}



