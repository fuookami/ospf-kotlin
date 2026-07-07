/**
 * Package solution like adapter.
 * 包装解决方案适配器。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Sealed interface representing the quantity of a package solution, which can be by amount, weight, or both.
 * 密封接口，表示包装解决方案的数量，可按数量、重量或两者混合。
 */
sealed interface PackageSolutionLikeQuantity {
    /**
     * Quantity specified by item amount.
     * 按物品数量指定的数量。
     *
     * @property value The amount value.
     * 数量值。
     */
    data class Amount(val value: UInt64) : PackageSolutionLikeQuantity

    /**
     * Quantity specified by weight.
     * 按重量指定的数量。
     *
     * @property value The weight value.
     * 重量值。
     */
    data class Weight<V : FloatingNumber<V>>(val value: Quantity<V>) : PackageSolutionLikeQuantity

    /**
     * Quantity specified by both amount and weight.
     * 按数量和重量混合指定的数量。
     *
     * @property amount The amount value.
     * 数量值。
     * @property weight The weight value.
     * 重量值。
     */
    data class AmountAndWeight<V : FloatingNumber<V>>(
        val amount: UInt64,
        val weight: Quantity<V>
    ) : PackageSolutionLikeQuantity
}

/**
 * A material item in a package solution with its material key and quantity.
 * 包装解决方案中的物料项，包含物料键和数量。
 *
 * @property material The material key identifying the material.
 * 标识物料的物料键。
 * @property quantity The quantity of the material.
 * 物料的数量。
 */
data class PackageSolutionLikeMaterialItem(
    val material: MaterialKey,
    val quantity: PackageSolutionLikeQuantity
)

/**
 * 包装解决方案节点，包含形状、物料项和子节点。
 * Package solution like node, containing shape, material items and child nodes.
 *
 * @property shape 包装形状 / package shape
 * @property materialItems 物料项列表 / list of material items
 * @property children 子节点列表 / list of child nodes
 */
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

/**
 * 将包装解决方案数量转换为装箱程序物料值。
 * Convert the package solution like quantity to a packing program material value.
 *
 * @return 装箱程序物料值 / packing program material value
 */
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
