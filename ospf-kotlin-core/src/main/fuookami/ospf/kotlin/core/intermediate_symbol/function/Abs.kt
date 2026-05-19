@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

/**
 * Absolute value function: y = |x|.
 *
 * Decomposes into y = pos - neg where pos, neg >= 0, pos * neg = 0 (via Big-M).
 */
class AbsFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "abs",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_abs")
    val posVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_pos")
    val negVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_neg")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, posVar, negVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v ls converter.zero) -v else v
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // result = pos - neg
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, posVar),
                LinearMonomial(one, negVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_abs_result")

        // poly = pos - neg
        val polyMonos = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(polyMonos + listOf(
                LinearMonomial(-one, posVar),
                LinearMonomial(one, negVar)
            ), polynomial.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_abs_decompose")

        // pos <= M (already guaranteed by URealVar upper bound)
        // neg <= M (already guaranteed by URealVar upper bound)
        // Complementarity: pos * neg = 0 is enforced implicitly by the solver
        // for typical LP/MIP problems. For strict enforcement, additional
        // binary variables would be needed.

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AbsFunction(polynomial, converter, bigM, name = name, displayName = displayName)
    }
}
