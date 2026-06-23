/**
 * 包装解决方案适配器。
 * Package solution like adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.*

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

/**
 * 合并两个物料值，将右值累加到左值上。左值为空时直接返回右值。
 * Merge two material values by accumulating the right value onto the left. Returns the right value directly when the left is null.
 *
 * @param lhs 已有的物料值，可为空 / existing material value, nullable
 * @param rhs 待合并的物料值 / material value to merge
 * @return 合并后的物料值，可能失败 / merged material value, may fail
 */
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

/**
 * 将包装解决方案节点树递归转换为装箱程序。递归处理子节点并合并所有物料值。
 * Recursively convert the package solution node tree into a packing program. Processes child nodes recursively and merges all material values.
 *
 * @return 装箱程序，可能失败 / packing program, may fail
 */
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
