package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

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
}
