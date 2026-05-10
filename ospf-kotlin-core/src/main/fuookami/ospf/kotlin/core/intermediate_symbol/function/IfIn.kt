@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * If-In function: `y = 1 if a <= x <= b, else y = 0`.
 *
 * Uses two binary indicators for the lower and upper bound checks,
 * combined via an AND-like constraint.
 *
 * @param x the input linear polynomial
 * @param lower the lower bound (a)
 * @param upper the upper bound (b)
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class IfInFunction<V>(
    val x: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ifin",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_ifin")
    val geVar: AbstractVariableItem<*, *> = BinVar("${name}_ge")
    val geSideVar: AbstractVariableItem<*, *> = BinVar("${name}_ge_side")
    val leVar: AbstractVariableItem<*, *> = BinVar("${name}_le")
    val leSideVar: AbstractVariableItem<*, *> = BinVar("${name}_le_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, geVar, geSideVar, leVar, leSideVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val xDouble = converter.fromValue(xValue).toDouble()
        val lo = converter.fromValue(lower).toDouble()
        val hi = converter.fromValue(upper).toDouble()
        return if (xDouble >= lo && xDouble <= hi) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mVal = bigM
        val allConstraints = mutableListOf<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

        // x - lower >= 0 indicator (x >= lower)
        val xMinusLower = LinearPolynomial(x.monomials, x.constant - lower)
        allConstraints += nonzeroIndicatorConstraints(xMinusLower, geVar, geSideVar, mVal, tolerance, strictBoundary, converter, "${name}_ge")

        // upper - x >= 0 indicator (x <= upper)
        val upperMinusX = LinearPolynomial(x.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, -x.constant + upper)
        allConstraints += nonzeroIndicatorConstraints(upperMinusX, leVar, leSideVar, mVal, tolerance, strictBoundary, converter, "${name}_le")

        // result = ge AND le: result <= ge, result <= le, result >= ge + le - 1
        allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, geVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE, "${name}_link_ge"
        )

        allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, leVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE, "${name}_link_le"
        )

        allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
            LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, resultVar), LinearMonomial(-Flt64.one, geVar), LinearMonomial(-Flt64.one, leVar)),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), -Flt64.one),
            Comparison.GE, "${name}_link_lb"
        )

        addConstraints(model, allConstraints, converter)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lower: V,
            upper: V,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            IfInFunction(x, lower, upper, converter, bigM, name = name, displayName = displayName)

        operator fun invoke(
            x: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            lower: Flt64,
            upper: Flt64,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = IfInFunction(x, lower, upper, flt64Converter, bigM, name = name, displayName = displayName)

        operator fun invoke(
            x: LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            lower: Flt64,
            upper: Flt64,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = IfInFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            lower = lower,
            upper = upper,
            bigM = bigM,
            name = name,
            displayName = displayName
        )
    }
}
