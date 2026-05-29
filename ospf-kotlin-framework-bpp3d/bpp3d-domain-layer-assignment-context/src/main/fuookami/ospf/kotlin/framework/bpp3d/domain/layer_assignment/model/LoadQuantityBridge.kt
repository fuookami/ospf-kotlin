package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

private fun <V : FloatingNumber<V>> Quantity<V>.toInfraQuantity(): Quantity<InfraNumber> {
    return Quantity(infraScalar(this.value.toString().toDouble()), this.unit)
}

fun <V : FloatingNumber<V>> toModelItems(
    items: List<Pair<QuantityItem<V>, UInt64>>,
    itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Pair<Item, UInt64>> {
    return items.map { (item, demand) ->
        Pair(item.toModel(materialCache, itemCache), demand)
    }
}

fun <V : FloatingNumber<V>> toModelItemRanges(
    items: List<Triple<QuantityItem<V>, UInt64, ValueRange<UInt64>>>,
    itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Triple<Item, UInt64, ValueRange<UInt64>>> {
    return items.map { (item, demand, demandRange) ->
        Triple(item.toModel(materialCache, itemCache), demand, demandRange)
    }
}

fun <V : FloatingNumber<V>> toModelLayers(
    layers: List<QuantityBinLayer<V>>,
    itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<BinLayer> {
    return layers.map { it.toModel(materialCache, itemCache) }
}

fun <V : FloatingNumber<V>> toModelItemDemands(
    items: List<Pair<QuantityItem<V>, Quantity<V>>>,
    itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap()
): List<Bpp3dItemDemand> {
    return items.map { (item, quantity) ->
        Bpp3dItemDemand(
            item = item.toModel(materialCache, itemCache),
            quantity = quantity.toInfraQuantity()
        )
    }
}

fun <V : FloatingNumber<V>> toModelMaterialDemands(
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
            quantity = demand.toInfraQuantity()
        )
    }
}

fun <V : FloatingNumber<V>> toModelMaterialWeightDemandsByKey(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>
): List<Pair<MaterialKey, Quantity<InfraNumber>>> {
    return materials.map { (material, demand) ->
        Pair(
            MaterialKey(
                no = material.no,
                type = material.type,
                manufacturer = material.manufacturer,
                supplier = material.supplier
            ),
            demand.toInfraQuantity()
        )
    }
}
