/**
 * 包装解决方案适配器。
 * Package solution like adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

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
    val shape: PackageShape<FltX>,
    val materialItems: List<PackageSolutionLikeMaterialItem> = emptyList(),
    val children: List<PackageSolutionLikeNode> = emptyList()
)

private fun mergeMaterialValue(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): Ret<PackingProgramMaterialValue> {
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

fun PackageSolutionLikeNode.toPackingProgram(): Ret<PackingProgram<FltX>> {
    val childPrograms = ArrayList<PackingProgram<FltX>>()
    for (child in children) {
        when (val result = child.toPackingProgram()) {
            is Ok -> childPrograms.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val mergedMaterials = LinkedHashMap<MaterialKey, PackingProgramMaterialValue>()
    for (child in childPrograms) {
        for ((material, value) in child.materials) {
            mergedMaterials[material] = when (val result = mergeMaterialValue(
                lhs = mergedMaterials[material],
                rhs = value
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }
    for (materialItem in materialItems) {
        mergedMaterials[materialItem.material] = when (val result = mergeMaterialValue(
            lhs = mergedMaterials[materialItem.material],
            rhs = materialItem.quantity.toMaterialValue()
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok(PackingProgram(
        shape = shape,
        packages = childPrograms.ifEmpty { null },
        materials = mergedMaterials
    ))
}
