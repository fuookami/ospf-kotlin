package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

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
)

open class Material(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null,
    val weight: Flt64 = Flt64.zero
) {
    open val key: MaterialKey
        get() = MaterialKey(
            no = no,
            type = type,
            manufacturer = manufacturer,
            supplier = supplier
        )
}
