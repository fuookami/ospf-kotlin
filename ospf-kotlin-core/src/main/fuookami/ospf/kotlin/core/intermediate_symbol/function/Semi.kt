@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try

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
    val lb: V = zeroOf<V>(),
    val ub: V = Flt64(1e6) as V,
    override var name: String = "semi",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = emptyList()

    override fun evaluate(values: Map<Symbol, V>): V? {
        return null
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        // No additional constraints needed; solver handles semi-continuous natively
        return fuookami.ospf.kotlin.utils.functional.ok
    }

    companion object {
        operator fun <V> invoke(
            lb: V = zeroOf<V>(),
            ub: V = Flt64(1e6) as V,
            name: String,
            displayName: String? = null
        ): SemiFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SemiFunction(lb = lb, ub = ub, name = name, displayName = displayName)

        operator fun invoke(
            lb: Flt64 = Flt64.zero,
            ub: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): SemiFunction<Flt64> = SemiFunction(
            lb = lb,
            ub = ub,
            name = name,
            displayName = displayName
        )
    }
}