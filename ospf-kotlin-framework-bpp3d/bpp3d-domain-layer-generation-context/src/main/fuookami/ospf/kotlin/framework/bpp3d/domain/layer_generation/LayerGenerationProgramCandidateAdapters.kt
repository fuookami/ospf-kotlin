@file:Suppress("DEPRECATION")

/**
 * 层生成程序候选适配器。
 * Layer generation program candidate adapters.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

@Suppress("UNCHECKED_CAST")
private fun programQuantityToInfra(value: Quantity<*>): Quantity<InfraNumber> {
    return when (value.value) {
        is InfraNumber -> value as Quantity<InfraNumber>
        else -> Quantity(InfraNumber(value.value.toString().toDouble()), value.unit)
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
        deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
        hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
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
    materialCatalog: Map<MaterialKey, Material<InfraNumber>> = emptyMap()
): Item {
    return ActualItem(
        id = "program-candidate-$id-$sequence",
        name = itemName,
        width = programQuantityToInfra(program.width),
        height = programQuantityToInfra(program.height),
        depth = programQuantityToInfra(program.depth),
        weight = programQuantityToInfra(program.weight),
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
    materialCatalog: Map<MaterialKey, Material<InfraNumber>> = emptyMap()
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
    materialCatalog: Map<MaterialKey, Material<InfraNumber>> = emptyMap()
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
