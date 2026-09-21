package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 零容差带及带外方向选择。 / Zero-tolerance band and outside direction selection.
 *
 * @param V 数值类型 / Numeric type
 * @property tolerance 带内绝对值上界 / Inside-band absolute-value limit
 * @property sideVariable 保留的方向列，带外 1 为正侧、0 为负侧，带内不限定 / Retained side column: positive outside on one, negative on zero, unrestricted inside
 */
data class ZeroBand<V>(
    val tolerance: V,
    val sideVariable: AbstractVariableItem<*, *>
) where V : RealNumber<V>, V : NumberField<V>
