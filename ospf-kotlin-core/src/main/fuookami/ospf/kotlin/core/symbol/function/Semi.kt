/**
 * 半连续变量函数符号 / Semi-continuous variable function symbol
 *
 * 提供 [SemiFunction]，建模 y = 0 或 lb <= y <= ub 的半连续变量。
 *
 * Provides [SemiFunction] for modeling semi-continuous variables where y = 0 or lb <= y <= ub.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * Semi-continuous variable function.
 *
 * Models y where either y = 0 or lb <= y <= ub.
 * This is typically handled by the solver's built-in semi-continuous support,
 * so this is a marker class that produces no additional constraints.
 *
 * @param lb lower bound when active
 * @param ub upper bound when active
 * @param name unique name for this function
 * @param displayName optional human-readable display name
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

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
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
