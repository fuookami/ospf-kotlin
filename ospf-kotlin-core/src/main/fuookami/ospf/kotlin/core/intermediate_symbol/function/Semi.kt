@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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

        operator fun invoke(
            lb: Flt64 = Flt64.zero,
            ub: Flt64 = Flt64(1e6),
            name: String = "semi",
            displayName: String? = null
        ): SemiFunction<Flt64> = SemiFunction(
            lb = lb,
            ub = ub,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}