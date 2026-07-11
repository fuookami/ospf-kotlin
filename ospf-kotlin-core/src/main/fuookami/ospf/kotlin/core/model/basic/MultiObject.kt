/**
 * 多目标位置
 * Multi-objective location
*/
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 多目标函数中的子目标位置，将目标函数与其在多目标序列中的索引关联。
 * Sub-objective location within a multi-objective function, associating
 * an objective with its index in the multi-objective sequence.
 *
 * @property priority 子目标优先级 / Sub-objective priority
 * @property weight   子目标权重 / Sub-objective weight
*/
data class MultiObjectLocation<V>(
    val priority: UInt64,
    val weight: V
) where V : RealNumber<V>, V : NumberField<V>
