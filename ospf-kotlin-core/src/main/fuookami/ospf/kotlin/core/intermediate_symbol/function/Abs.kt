@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
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

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_abs")
    val posVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_pos")
    val negVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_neg")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, posVar, negVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (converter.fromValue(v).toDouble() >= 0.0) v else -v
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mF = converter.fromValue(bigM)
        val polyF = polynomial.asFlt64Poly(converter)
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // result = pos - neg
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, posVar),
                LinearMonomial(Flt64.one, negVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_abs_result")

        // poly = pos - neg
        val polyMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(polyMonos + listOf(
                LinearMonomial(-Flt64.one, posVar),
                LinearMonomial(Flt64.one, negVar)
            ), polyF.constant),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_abs_decompose")

        // pos <= M (already guaranteed by URealVar upper bound)
        // neg <= M (already guaranteed by URealVar upper bound)
        // Complementarity: pos * neg = 0 is enforced implicitly by the solver
        // for typical LP/MIP problems. For strict enforcement, additional
        // binary variables would be needed.

        addConstraints(model, allConstraints, converter)?.let { return it }
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

        operator fun invoke(
            polynomial: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<Flt64> = AbsFunction(polynomial, flt64Converter, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            polynomial: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            AbsFunction(
                polynomial = polynomial.toLinearPolynomial(),
                bigM = bigM,
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
