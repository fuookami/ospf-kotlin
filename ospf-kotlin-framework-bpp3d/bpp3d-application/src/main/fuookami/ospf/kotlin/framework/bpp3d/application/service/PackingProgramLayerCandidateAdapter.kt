@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.application.service

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
import fuookami.ospf.kotlin.framework.bpp3d.application.service.compat.applicationZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType

fun MaterialPackingProgramCandidate.toLayerGenerationItem(
    sequence: Int,
    materialCatalog: Map<MaterialKey, Material> = emptyMap()
): Item {
    return ActualItem(
        id = "program-candidate-$id-$sequence",
        name = itemName,
        width = program.width,
        height = program.height,
        depth = program.depth,
        weight = program.weight,
        enabledOrientations = enabledOrientations.ifEmpty { listOf(Orientation.Upright) },
        batchNo = batchNo,
        warehouse = warehouse,
        packageAttribute = packageAttribute ?: defaultProgramPackageAttribute(program.packageType),
        materialAmountsOverride = program.materialAmounts(),
        materialWeightsOverride = program.materialWeights(materialCatalog)
    )
}

private fun defaultProgramPackageAttribute(
    packageType: PackageType
): PackageAttribute {
    return PackageAttribute(
        packageType = packageType,
        weightAttribute = WeightAttribute(),
        deformationAttribute = LinearDeformationAttribute(applicationZero()),
        hangingPolicy = AbsoluteHangingPolicy(applicationZero()),
        stackingOnPolicy = FilterStackingOnPolicy()
    )
}
