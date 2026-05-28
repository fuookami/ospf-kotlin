package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

private fun <V : FloatingNumber<V>> Quantity<V>.toFlt64Quantity(): Quantity<InfraNumber> {
    return Quantity(infraScalar(this.value.toString().toDouble()), this.unit)
}

data class LegacyDemandSlices(
    val itemDemands: List<Pair<Item, UInt64>>,
    val materialAmountDemands: List<Pair<Material, UInt64>>,
    val materialWeightDemands: List<Pair<Material, Quantity<InfraNumber>>>,
    val initialColumns: List<BinLayer>
)

fun <V : FloatingNumber<V>> toLegacyDemandSlices(
    itemDemands: List<Pair<QuantityItem<V>, UInt64>>,
    materialAmountDemands: List<Pair<QuantityMaterial<V>, UInt64>>,
    materialWeightDemands: List<Pair<QuantityMaterial<V>, Quantity<V>>>,
    initialColumns: List<BinLayer>,
    quantityInitialColumns: List<QuantityBinLayer<V>>,
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
    itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
): LegacyDemandSlices {
    val legacyItemDemands = itemDemands.map { (item, amount) ->
        Pair(itemCache.getOrPut(item) { item.toLegacyModel(materialCache, itemCache) }, amount)
    }
    val legacyMaterialAmountDemands = materialAmountDemands.map { (material, amount) ->
        Pair(materialCache.getOrPut(material) { material.toLegacyModel() }, amount)
    }
    val legacyMaterialWeightDemands = materialWeightDemands.map { (material, weight) ->
        Pair(materialCache.getOrPut(material) { material.toLegacyModel() }, weight.toFlt64Quantity())
    }
    val legacyInitialColumns = initialColumns + quantityInitialColumns.map { quantityLayer ->
        quantityLayer.toLegacyModel(
            materialCache = materialCache,
            itemCache = itemCache
        )
    }

    return LegacyDemandSlices(
        itemDemands = legacyItemDemands,
        materialAmountDemands = legacyMaterialAmountDemands,
        materialWeightDemands = legacyMaterialWeightDemands,
        initialColumns = legacyInitialColumns
    )
}


