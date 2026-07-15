/**
 * Material attribute model.
 * 物料属性模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * Attribute values for a material, describing deformation, orientation, and stacking constraints.
 * 物料的属性值，描述变形、方向和堆叠约束。
 *
 * @property deformationCoefficient The deformation coefficient of the material, nullable.
 * 物料的变形系数，可为空。
 * @property overTypes The list of package types that can be placed on top of this material.
 * 可放置在此物料上方的包装类型列表。
 * @property enabledOrientations The list of allowed orientations for this material.
 * 此物料允许的方向列表。
 * @property sideOnTopLayer The maximum number of items that can be placed on the side of the top layer, nullable.
 * 顶层侧面可放置的最大物品数，可为空。
 * @property isBottomOnly Whether this material can only be placed at the bottom.
 * 此物料是否只能放置在底部。
 * @property isTopFlat Whether the top surface of this material is flat.
 * 此物料的顶面是否平坦。
 * @property maxLayer The maximum number of layers allowed for this material.
 * 此物料允许的最大层数。
 * @property maxHeight The maximum height allowed for this material.
 * 此物料允许的最大高度。
*/
data class MaterialAttributeValue<V : FloatingNumber<V>>(
    val deformationCoefficient: V?,
    val overTypes: List<PackageType>,
    val enabledOrientations: List<Orientation>,
    val sideOnTopLayer: UInt64?,
    val isBottomOnly: Boolean,
    val isTopFlat: Boolean,
    val maxLayer: UInt64,
    val maxHeight: Quantity<V>
)

/**
 * Key for looking up material attributes, matching by package type or as a base default.
 * 物料属性查找键，按包装类型匹配或作为基础默认值。
 *
 * @property packageType The package type to match, null for base default.
 * 要匹配的包装类型，为空表示基础默认值。
 * @property cargoAttribute The cargo attribute to match, nullable.
 * 要匹配的货物属性，可为空。
 * @property base Whether this key is a base (default) key with no package type.
 * 此键是否为基础（默认）键，无包装类型。
*/
open class MaterialAttributeKey(
    val packageType: PackageType? = null,
    val cargoAttribute: AbstractCargoAttribute? = null,
) {
    val base: Boolean = packageType == null

    /**
     * Checks whether this key matches the given package.
     * 检查此键是否匹配给定的包装。
     *
     * @param pack The package to match against.
     * 要匹配的包装。
     * @return True if this key matches the package, false otherwise.
     * 如果此键匹配包装则返回 true，否则返回 false。
    */
    open fun match(pack: Package<*>): Boolean {
        return packageType == null || pack.packageType == packageType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaterialAttributeKey) return false

        if (packageType != other.packageType) return false
        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageType?.hashCode() ?: 0
        result = 31 * result + base.hashCode()
        return result
    }
}
