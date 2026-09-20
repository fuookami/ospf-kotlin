/**
 * 机制模型基础层 / Mechanism model base layer
*/
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionConstraintRegion

/**
 * 机制模型层级结构的基础层：展开的变量和约束（无目标函数）。 / Base layer of the mechanism model hierarchy: expanded variables + constraints (no objective).
 *
 * 对应 Rust 实现中的 `BasicMechanismModel<V>`。
 * `MechanismModel<V>` 在此基础上扩展了目标函数和 Benders cut 生成。
 *
 * This corresponds to `BasicMechanismModel<V>` in the Rust implementation.
 * `MechanismModel<V>` extends this with the objective function and Benders cut generation.
 *
 * @property name 模型名称 / Model name
 * @property tokens 符号表 / Token table
*/
open class BasicMechanismModel<V>(
    open val name: String,
    open val tokens: AbstractTokenTable<V>,
    val functionExpansionPolicy: FunctionExpansionPolicy = FunctionExpansionPolicy.EAGER,
    deferredFunctionStructures: List<DeferredFunctionStructure> = emptyList()
) where V : RealNumber<V>, V : NumberField<V> {

    val deferredFunctionStructures: List<DeferredFunctionStructure> = deferredFunctionStructures.toList()
    private val _deferredFunctionConstraintRegions = mutableListOf<DeferredFunctionConstraintRegion>()
    val deferredFunctionConstraintRegions: List<DeferredFunctionConstraintRegion>
        get() = _deferredFunctionConstraintRegions.toList()

    internal fun recordDeferredFunctionConstraintRegion(
        structure: DeferredFunctionStructure,
        firstConstraintIndex: Int,
        constraintCount: Int
    ) {
        if (constraintCount > 0) {
            _deferredFunctionConstraintRegions += DeferredFunctionConstraintRegion(
                structure = structure,
                firstConstraintIndex = firstConstraintIndex,
                constraintCount = constraintCount
            )
        }
    }

    internal fun rollbackDeferredFunctionConstraintRegions(constraintCount: Int) {
        _deferredFunctionConstraintRegions.removeAll { it.lastConstraintIndex > constraintCount }
    }

    // Query helpers / 查询辅助方法

    /** 变量数量。 / Number of variables. */
    val numVariables: Int get() = tokens.tokens.size
}
