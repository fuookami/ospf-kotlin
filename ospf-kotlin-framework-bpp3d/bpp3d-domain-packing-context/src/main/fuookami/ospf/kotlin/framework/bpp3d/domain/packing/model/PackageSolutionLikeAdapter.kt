@file:Suppress("DEPRECATION")

/**
 * 包装解决方案适配器。
 * Package solution like adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgramMaterialValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.mergePackingProgramMaterialValues
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

sealed interface PackageSolutionLikeQuantity {
    data class Amount(val value: UInt64) : PackageSolutionLikeQuantity
    data class Weight<V : FloatingNumber<V>>(val value: Quantity<V>) : PackageSolutionLikeQuantity
    data class AmountAndWeight<V : FloatingNumber<V>>(
        val amount: UInt64,
        val weight: Quantity<V>
    ) : PackageSolutionLikeQuantity
}

data class PackageSolutionLikeMaterialItem(
    val material: MaterialKey,
    val quantity: PackageSolutionLikeQuantity
)

data class PackageSolutionLikeNode(
    val shape: PackageShape<InfraNumber>,
    val materialItems: List<PackageSolutionLikeMaterialItem> = emptyList(),
    val children: List<PackageSolutionLikeNode> = emptyList()
)

private fun mergeMaterialValue(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): PackingProgramMaterialValue {
    return mergePackingProgramMaterialValues(lhs, rhs)
}

private fun PackageSolutionLikeQuantity.toMaterialValue(): PackingProgramMaterialValue {
    return when (this) {
        is PackageSolutionLikeQuantity.Amount -> PackingProgramMaterialValue(amount = value)
        is PackageSolutionLikeQuantity.Weight<*> -> PackingProgramMaterialValue(weight = value)
        is PackageSolutionLikeQuantity.AmountAndWeight<*> -> PackingProgramMaterialValue(
            amount = amount,
            weight = weight
        )
    }
}

fun PackageSolutionLikeNode.toPackingProgram(): PackingProgram<InfraNumber> {
    val childPrograms = children.map { child -> child.toPackingProgram() }
    val mergedMaterials = LinkedHashMap<MaterialKey, PackingProgramMaterialValue>()
    for (child in childPrograms) {
        for ((material, value) in child.materials) {
            mergedMaterials[material] = mergeMaterialValue(
                lhs = mergedMaterials[material],
                rhs = value
            )
        }
    }
    for (materialItem in materialItems) {
        mergedMaterials[materialItem.material] = mergeMaterialValue(
            lhs = mergedMaterials[materialItem.material],
            rhs = materialItem.quantity.toMaterialValue()
        )
    }
    return PackingProgram(
        shape = shape,
        packages = childPrograms.ifEmpty { null },
        materials = mergedMaterials
    )
}
