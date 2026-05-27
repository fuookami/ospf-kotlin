@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgram
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackingProgramMaterialValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.plus

sealed interface PackageSolutionLikeQuantity {
    data class Amount(val value: UInt64) : PackageSolutionLikeQuantity
    data class Weight(val value: Quantity<Flt64>) : PackageSolutionLikeQuantity
    data class AmountAndWeight(
        val amount: UInt64,
        val weight: Quantity<Flt64>
    ) : PackageSolutionLikeQuantity
}

data class PackageSolutionLikeMaterialItem(
    val material: MaterialKey,
    val quantity: PackageSolutionLikeQuantity
)

data class PackageSolutionLikeNode(
    val shape: PackageShape,
    val materialItems: List<PackageSolutionLikeMaterialItem> = emptyList(),
    val children: List<PackageSolutionLikeNode> = emptyList()
)

private fun mergeMaterialValue(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): PackingProgramMaterialValue {
    val lhsAmount = lhs?.amount
    val rhsAmount = rhs.amount
    val amount = when {
        lhsAmount == null -> rhsAmount
        rhsAmount == null -> lhsAmount
        else -> lhsAmount + rhsAmount
    }
    val lhsWeight = lhs?.weight
    val rhsWeight = rhs.weight
    val weight = when {
        lhsWeight == null -> rhsWeight
        rhsWeight == null -> lhsWeight
        else -> lhsWeight + rhsWeight
    }
    return PackingProgramMaterialValue(
        amount = amount,
        weight = weight
    )
}

private fun PackageSolutionLikeQuantity.toMaterialValue(): PackingProgramMaterialValue {
    return when (this) {
        is PackageSolutionLikeQuantity.Amount -> PackingProgramMaterialValue(amount = value)
        is PackageSolutionLikeQuantity.Weight -> PackingProgramMaterialValue(weight = value)
        is PackageSolutionLikeQuantity.AmountAndWeight -> PackingProgramMaterialValue(
            amount = amount,
            weight = weight
        )
    }
}

fun PackageSolutionLikeNode.toPackingProgram(): PackingProgram {
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
