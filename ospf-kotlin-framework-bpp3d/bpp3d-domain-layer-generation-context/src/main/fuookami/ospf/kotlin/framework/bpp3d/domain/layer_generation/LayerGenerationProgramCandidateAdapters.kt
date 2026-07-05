/**
 * 层生成程序候选适配器。
 * Layer generation program candidate adapters.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate

/**
 * 将通用数量值转换为 FltX 类型的数量值。
 * Convert a generic quantity value to a FltX-typed quantity value.
 *
 * @param value 要转换的数量值 / the quantity value to convert
 * @return 转换后的 FltX 类型数量值 / the converted FltX-typed quantity value
 */
@Suppress("UNCHECKED_CAST")
private fun programQuantityToFltX(value: Quantity<*>): Quantity<FltX> {
    return when (value.value) {
        is FltX -> value as Quantity<FltX>
        else -> Quantity(FltX(value.value.toString().toDouble()), value.unit)
    }
}

/**
 * 获取包装程序候选默认包装属性。
 * Get default package attribute for packing program candidate.
 *
 * @param packageType 包装类型 / package type
 * @return 默认包装属性 / default package attribute
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
 * 将物料装箱方案候选转换为层生成货物项。
 * Convert material packing program candidate to layer generation item.
 *
 * @param sequence 序列号 / sequence number
 * @param materialCatalog 物料目录 / material catalog
 * @return 层生成货物项 / layer generation item
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
 * 批量转换层生成程序需求为货物需求。
 * Convert layer generation program demands to item demands.
 *
 * @param programDemands 程序需求列表 / program demand list
 * @param materialCatalog 物料目录 / material catalog
 * @return 货物需求列表 / item demand list
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
 * 批量转换层生成程序需求为货物列表。
 * Convert layer generation program demands to item list.
 *
 * @param programDemands 程序需求列表 / program demand list
 * @param materialCatalog 物料目录 / material catalog
 * @return 货物列表 / item list
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
