/**
 * Layer generation program candidate adapters.
 * 层生成程序候选适配器。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate

/**
 * Convert a generic quantity value to a FltX-typed quantity value.
 * 将通用数量值转换为 FltX 类型的数量值。
 *
 * @param value The quantity value to convert.
 * @return The converted FltX-typed quantity value.
 * 返回转换后的 FltX 类型数量值。
*/
@Suppress("UNCHECKED_CAST")
private fun programQuantityToFltX(value: Quantity<*>): Quantity<FltX> {
    return when (value.value) {
        is FltX -> value as Quantity<FltX>
        else -> Quantity(FltX(value.value.toString().toDouble()), value.unit)
    }
}

/**
 * Get default package attribute for packing program candidate.
 * 获取包装程序候选默认包装属性。
 *
 * @param packageType Package type.
 * @return Default package attribute.
 * 返回默认包装属性。
*/
private fun defaultProgramPackageAttribute(
    packageType: PackageType
): PackageAttribute {
    return PackageAttribute(
        packageType = packageType,
        weightAttribute = WeightAttribute(),
        deformationAttribute = LinearDeformationAttribute(FltX.zero),
        hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
        stackingOnPolicy = FilterStackingOnPolicy()
    )
}

/**
 * Convert material packing program candidate to layer generation item.
 * 将物料装箱方案候选转换为层生成货物项。
 *
 * @param sequence Sequence number.
 * @param materialCatalog Material catalog.
 * @return Layer generation item.
 * 返回层生成货物项。
*/
fun MaterialPackingProgramCandidate<*>.toLayerGenerationItem(
    sequence: Int,
    materialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap()
): Item {
    return ActualItem(
        id = itemIdOf("program-candidate-$id-$sequence"),
        name = itemName,
        width = programQuantityToFltX(program.width),
        height = programQuantityToFltX(program.height),
        depth = programQuantityToFltX(program.depth),
        weight = programQuantityToFltX(program.weight),
        enabledOrientations = enabledOrientations.ifEmpty { listOf(Orientation.Upright) },
        batchNo = batchNo,
        warehouse = warehouse,
        packageAttribute = packageAttribute ?: defaultProgramPackageAttribute(program.packageType),
        shapeSpecOverride = program.shape.shapeSpec,
        materialAmountsOverride = program.materialAmounts(),
        materialWeightsOverride = program.materialWeights(materialCatalog)
    )
}

/**
 * Convert layer generation program demands to item demands.
 * 批量转换层生成程序需求为货物需求。
 *
 * @param programDemands Program demand list.
 * @param materialCatalog Material catalog.
 * @return Item demand list.
 * 返回货物需求列表。
*/
fun layerGenerationItemDemandsFromPrograms(
    programDemands: List<Pair<MaterialPackingProgramCandidate<*>, UInt64>>,
    materialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap()
): List<Pair<Item, UInt64>> {
    return programDemands.mapIndexed { index, (candidate, amount) ->
        Pair(
            candidate.toLayerGenerationItem(
                sequence = index + 1,
                materialCatalog = materialCatalog
            ),
            amount
        )
    }
}

/**
 * Convert layer generation program demands to item list.
 * 批量转换层生成程序需求为货物列表。
 *
 * @param programDemands Program demand list.
 * @param materialCatalog Material catalog.
 * @return Item list.
 * 返回货物列表。
*/
fun layerGenerationItemsFromPrograms(
    programDemands: List<Pair<MaterialPackingProgramCandidate<*>, UInt64>>,
    materialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap()
): List<Item> {
    val items = ArrayList<Item>()
    val itemDemands = layerGenerationItemDemandsFromPrograms(
        programDemands = programDemands,
        materialCatalog = materialCatalog
    )
    for ((item, amount) in itemDemands) {
        for (index in UInt64.zero until amount) {
            items.add(item)
        }
    }
    return items
}
