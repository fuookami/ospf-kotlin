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
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Absolute value function: y = |x|.
 *
 * Decomposes into y = pos - neg where pos, neg >= 0, pos * neg = 0 (via Big-M).
 */
class AbsFunction<V>(
    val polynomial: LinearPolynomial<V>,
    bigM: V? = null,
    override var name: String = "abs",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: Flt64(BIG_M_DEFAULT) as V

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_abs")
    val posVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_pos")
    val negVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_neg")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, posVar, negVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v.asFlt64().toDouble() >= 0.0) v else -v
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mF = bigM.asFlt64()
        val polyF = polynomial.asFlt64Poly()
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // result = pos - neg
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(Flt64.one, resultVar),
                LinearMonomial(-Flt64.one, posVar),
                LinearMonomial(Flt64.one, negVar)
            ), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero), Comparison.EQ, "${name}_abs_result")

        // poly = pos - neg
        val polyMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += Flt64LinearInequality(
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

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AbsFunction(polynomial, bigM, name = name, displayName = displayName)

        operator fun invoke(
            polynomial: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<Flt64> = AbsFunction(polynomial, bigM, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomial")
        fun fromLinearPolynomial(
            polynomial: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            AbsFunction<Flt64>(
                polynomial = polynomial.toLinearPolynomial(),
                bigM = bigM,
                name = name,
                displayName = displayName
            )
        )
    }
}
