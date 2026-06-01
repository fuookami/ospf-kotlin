package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 分切设备 / Slitting machine
 *
 * @param V 数值类型 / Numeric value type
 * @property id 设备标识 / Machine identifier
 * @property name 设备名称 / Machine name
 * @property maxBatchCount 最大批次数 / Maximum batch count
 * @property maxSwitchCount 最大换料次数 / Maximum material switch count
 * @property widthRange 可加工幅宽范围 / Processable width range
 * @property capacity 产能（按业务单位）/ Capacity in business unit
 */
data class Machine<V : RealNumber<V>>(
    val id: String,
    val name: String,
    val maxBatchCount: UInt64? = null,
    val maxSwitchCount: UInt64? = null,
    val widthRange: WidthRange<V>? = null,
    val capacity: Quantity<V>? = null
) : ManualIndexed() {
    /**
     * 判断是否可加工给定物料 / Check whether the machine can process the material
     *
     * @param material 物料 / Material
     * @return 是否可加工 / Whether enabled
     */
    fun enabled(material: Material<V>): Boolean {
        val thisWidthRange = widthRange ?: return true
        return thisWidthRange.width.contains(material.widthRange.lowerBound)
                && thisWidthRange.width.contains(material.widthRange.upperBound)
    }
}

