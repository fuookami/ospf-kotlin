/**
 * 区间条件函数符号 / If-In function symbol
 *
 * 提供 [IfInFunction]，实现 y = (a <= x <= b ? 1 : 0) 的线性化建模。
 *
 * Provides [IfInFunction] for linearized modeling of y = 1 if a <= x <= b, else y = 0.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar

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
        return if (!(xValue ls lower) && !(xValue gr upper)) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mVal = bigM
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // x - lower >= 0 indicator (x >= lower)
        val xMinusLower = LinearPolynomial(x.monomials, x.constant - lower)
        allConstraints += nonzeroIndicatorConstraints(xMinusLower, geVar, geSideVar, mVal, tolerance, strictBoundary, "${name}_ge")

        // upper - x >= 0 indicator (x <= upper)
        val upperMinusX = LinearPolynomial(x.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, -x.constant + upper)
        allConstraints += nonzeroIndicatorConstraints(upperMinusX, leVar, leSideVar, mVal, tolerance, strictBoundary, "${name}_le")

        // result = ge AND le: result <= ge, result <= le, result >= ge + le - 1
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, geVar)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_link_ge"
        )

        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, leVar)), zero),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_link_le"
        )

        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, geVar), LinearMonomial(-one, leVar)),
                zero
            ),
            LinearPolynomial(emptyList(), -one),
            Comparison.GE, "${name}_link_lb"
        )

        addConstraints(model, allConstraints)?.let { return it }
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
    }
}
