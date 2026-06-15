package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 宽度范围，支持步进和单位一致性 / Width range with step and unit consistency
 *
 * @param V 数值类型 / Numeric value type
 * @property width 宽度值域 / Width value range
 * @property step 步进值 / Step value
 */
data class WidthRange<V : RealNumber<V>>(
    val width: QuantityRange<V>,
    val step: Quantity<V>
) {
    init {
        check(width.lowerBound.unit.quantity == step.unit.quantity)
        check(width.upperBound.unit.quantity == step.unit.quantity)
    }

    val upperBound get() = width.upperBound
    val lowerBound get() = width.lowerBound

    /**
     * 判断给定宽度是否能在该幅宽范围的原料上分切 / Check whether a given width can be cut on material with this width range
     *
     * 产品只需要宽度不超过上界即可在下界及以上幅宽的原料上分切 / A product only needs width <= upperBound to be cuttable
     *
     * @param productWidth 产品宽度 / Product width to check
     * @return 是否可分切 / Whether cuttable
     */
    fun canCut(productWidth: Quantity<V>): Boolean {
        val order = productWidth partialOrd upperBound
        return order != null && order !is Order.Greater
    }
}
