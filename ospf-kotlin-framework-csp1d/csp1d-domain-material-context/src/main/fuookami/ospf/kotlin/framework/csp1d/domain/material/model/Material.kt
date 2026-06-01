package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 分切物料 / Cutting material
 *
 * @param V 数值类型 / Numeric value type
 * @property id 物料标识 / Material identifier
 * @property name 物料名称 / Material name
 * @property widthRange 可用幅宽范围 / Available width range
 * @property length 卷长 / Coil length
 * @property unitWeight 单位重量 / Unit weight
 * @property machineId 设备标识 / Machine identifier
 * @property availableBatches 可用批次数 / Available batches
 */
data class Material<V : RealNumber<V>>(
    override val id: String,
    val name: String,
    val widthRange: WidthRange<V>,
    override val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null,
    val machineId: String? = null,
    val availableBatches: UInt64 = UInt64.maximum
) : Production<V>, ManualIndexed() {
    override val width: List<Quantity<V>> by lazy {
        listOf(
            widthRange.lowerBound,
            widthRange.upperBound
        )
    }

    /**
     * 判断切割方案是否满足物料基础约束 / Check whether a cutting plan satisfies material basic constraints
     *
     * @param plan 切割方案 / Cutting plan
     * @return 是否满足 / Whether enabled
     */
    fun enabled(plan: CuttingPlan<V>): Boolean {
        if (machineId != null && plan.machineId != null && machineId != plan.machineId) {
            return false
        }
        val usedWidth = plan.usedWidth ?: return false
        if (!widthRange.width.contains(usedWidth)) {
            return false
        }
        return true
    }
}

