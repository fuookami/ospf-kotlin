/** 半连续函数符号 / Semi-continuous function symbol */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 半连续变量函数符号 / Semi-continuous variable function symbol
 *
 * 提供 [SemiFunction]，建模 y = 0 或 lb <= y <= ub 的半连续变量。
 *
 * Provides [SemiFunction] for modeling semi-continuous variables where y = 0 or lb <= y <= ub.
 */

/**
 * 半连续变量函数。
 * Semi-continuous variable function.
 *
 * 建模 y = 0 或 lb <= y <= ub 的半连续变量。
 * Models y where either y = 0 or lb <= y <= ub.
 * 这通常由求解器内置的半连续支持处理，
 * This is typically handled by the solver's built-in semi-continuous support,
 * 因此这是一个不产生额外约束的标记类。
 * so this is a marker class that produces no additional constraints.
 *
 * @param lb 激活时的下界 / lower bound when active
 * @param ub 激活时的上界 / upper bound when active
 * @param name 此函数的唯一名称 / unique name for this function
 * @param displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class SemiFunction<V>(
    lb: V? = null,
    ub: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "semi",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    val lb: V = lb ?: converter.zero
    val ub: V = ub ?: converter.intoValue(Flt64(1e6))

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = emptyList()

    override fun evaluate(values: Map<Symbol, V>): V? {
        return null
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return ok
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return ok
    }
    companion object {
        operator fun <V> invoke(
            lb: V? = null,
            ub: V? = null,
            converter: IntoValue<V>,
            name: String = "semi",
            displayName: String? = null
        ): SemiFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SemiFunction(lb, ub, converter, name, displayName)
    }
}
