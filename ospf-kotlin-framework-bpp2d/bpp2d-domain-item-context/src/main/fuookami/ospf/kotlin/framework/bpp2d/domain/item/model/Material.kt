/**
 * 物料模型。 / Material model.
 *
 * 与 BPP3D 的 `Material` 对齐：物料以编号、类型、名称与单位面积重量描述，作为面积或重量需求的载体。
 * / Mirrors BPP3D `Material`: a material is described by its number, type, name and unit-area weight,
 * and carries area or weight demands.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 物料类型。 / Material type.
*/
enum class MaterialType {
    /** 原材料 / raw material */
    RawMaterial,

    /** 半成品 / semi-finished product */
    SemiFinishedProduct,

    /** 成品 / finished product */
    FinishedProduct
}

/**
 * 物料键：按编号、类型与来源唯一识别物料。 / Material key uniquely identifying a material.
 *
 * @property no 物料编号 / material number
 * @property type 物料类型 / material type
 * @property manufacturer 制造商 / manufacturer
 * @property supplier 供应商 / supplier
*/
open class MaterialKey(
    open val no: MaterialNo,
    val type: MaterialType,
    val manufacturer: String? = null,
    val supplier: String? = null
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

    override fun toString(): String = "$no-$type"
}

/**
 * 物料。 / Material.
 *
 * @param V 数值类型 / numeric type
 * @property no 物料编号 / material number
 * @property type 物料类型 / material type
 * @property name 物料名称 / material name
 * @property weight 单件重量 / per-piece weight
 * @property manufacturer 制造商 / manufacturer
 * @property supplier 供应商 / supplier
 * @property warehouse 仓库 / warehouse
*/
open class Material<V : FloatingNumber<V>>(
    open val no: MaterialNo,
    val type: MaterialType,
    val name: String,
    val weight: Quantity<V>,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null
) {
    /** 物料键 / material key */
    open val key: MaterialKey
        get() = MaterialKey(
            no = no,
            type = type,
            manufacturer = manufacturer,
            supplier = supplier
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Material<*>) return false
        if (key != other.key) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "$no-$name"
}