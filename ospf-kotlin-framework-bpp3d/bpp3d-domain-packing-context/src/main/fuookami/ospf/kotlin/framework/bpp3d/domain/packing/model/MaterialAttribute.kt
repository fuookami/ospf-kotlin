@file:Suppress("DEPRECATION")

/**
 * 物料属性模型。
 * Material attribute model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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

open class MaterialAttributeKey(
    val packageType: PackageType? = null,
    val cargoAttribute: AbstractCargoAttribute? = null,
) {
    val base: Boolean = packageType == null

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


