/**
 * 机制模型基础层
 * Mechanism model base layer
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.AbstractTokenTable

/**
 * 机制模型层级结构的基础层：展开的变量和约束（无目标函数）。
 * Base layer of the mechanism model hierarchy: expanded variables + constraints (no objective).
 *
 * 对应 Rust 实现中的 `BasicMechanismModel<V>`。
 * `MechanismModel<V>` 在此基础上扩展了目标函数和 Benders cut 生成。
 *
 * This corresponds to `BasicMechanismModel<V>` in the Rust implementation.
 * `MechanismModel<V>` extends this with the objective function and Benders cut generation.
 */
open class BasicMechanismModel<V>(
    open val name: String,
    open val tokens: AbstractTokenTable<V>
) where V : RealNumber<V>, V : NumberField<V> {

    // Query helpers

    val numVariables: Int get() = tokens.tokens.size
}
