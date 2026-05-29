@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemCuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.itemInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.itemNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.itemOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.itemTwo
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.itemZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.quantities.unit.Kilogram







enum class MaterialType {
    RawMaterial,
    SemiFinishedProduct,
    FinishedProduct
}

open class MaterialKey(
    val no: MaterialNo,
    val type: MaterialType,
    val manufacturer: String? = null,
    val supplier: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaterialKey) return false

        if (no != other.no) return false
        if (type != other.type) return false
        if (manufacturer != other.manufacturer) return false
        if (supplier != other.supplier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = no.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (manufacturer?.hashCode() ?: 0)
        result = 31 * result + (supplier?.hashCode() ?: 0)
        return result
    }
}

open class Material(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null,
    val weight: Quantity<InfraNumber> = Quantity(itemZero(), Kilogram)
) {
    open val key: MaterialKey
        get() = MaterialKey(
            no = no,
            type = type,
            manufacturer = manufacturer,
            supplier = supplier
        )
}



