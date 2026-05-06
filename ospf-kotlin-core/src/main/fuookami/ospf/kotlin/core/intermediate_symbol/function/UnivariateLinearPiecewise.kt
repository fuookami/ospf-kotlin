@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
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
 * Univariate linear piecewise function: y = f(x) defined by breakpoints and slopes.
 *
 * Uses binary selector variables to choose the active segment.
 */
class UnivariateLinearPiecewiseFunction<V>(
    val x: LinearPolynomial<V>,
    val breakpoints: List<V>,
    val slopes: List<V>,
    val intercepts: List<V>,
    m: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "piecewise",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val m: V = m ?: converter.intoValue(Flt64(1e6))

    init {
        require(breakpoints.size >= 2) { "Need at least 2 breakpoints" }
        require(slopes.size == breakpoints.size - 1) { "slopes size must be breakpoints.size - 1" }
        require(intercepts.size == breakpoints.size - 1) { "intercepts size must be breakpoints.size - 1" }
    }

    private val numSegments = breakpoints.size - 1
    val selectorVars: List<AbstractVariableItem<*, *>> = (0 until numSegments).map { BinVar("${name}_s${it}") }
    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_y")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val xDouble = converter.fromValue(xValue).toDouble()
        for (i in 0 until numSegments) {
            val bpLow = converter.fromValue(breakpoints[i]).toDouble()
            val bpHigh = converter.fromValue(breakpoints[i + 1]).toDouble()
            if (xDouble >= bpLow && xDouble <= bpHigh) {
                return converter.intoValue(Flt64(converter.fromValue(slopes[i]).toDouble() * xDouble + converter.fromValue(intercepts[i]).toDouble()))
            }
        }
        return null
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mF = converter.fromValue(m)
        val xF = x.asFlt64Poly(converter)
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // Exactly one segment must be active: sum(s[i]) = 1
        val sumMonos = selectorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(sumMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_select_one")

        for (i in 0 until numSegments) {
            val sVar = selectorVars[i]
            val bpLowF = converter.fromValue(breakpoints[i])
            val bpHighF = converter.fromValue(breakpoints[i + 1])
            val slopeF = converter.fromValue(slopes[i])
            val interceptF = converter.fromValue(intercepts[i])

            // Lower bound: x >= bpLow - M*(1 - s[i]) => x + M*s[i] >= bpLow - M... => x + M - M*s >= bpLow
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    LinearMonomial(-mF, sVar), xF.constant + mF),
                LinearPolynomial(emptyList(), bpLowF), Comparison.GE, "${name}_seg_${i}_lb")

            // Upper bound: x <= bpHigh + M*(1 - s[i]) => x + M*s[i] <= bpHigh + M
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(xF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    LinearMonomial(mF, sVar), xF.constant),
                LinearPolynomial(emptyList(), bpHighF + mF), Comparison.LE, "${name}_seg_${i}_ub")

            // y = slope*x + intercept when s[i]=1
            // y - slope*x - intercept <= M*(1 - s[i]) => y - slope*x - intercept + M*s[i] <= M
            val negSlopeXMonos = xF.monomials.map { LinearMonomial(-it.coefficient * slopeF, it.symbol) }
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)) +
                    negSlopeXMonos + LinearMonomial(mF, sVar), -interceptF),
                LinearPolynomial(emptyList(), mF), Comparison.LE, "${name}_seg_${i}_eq_ub")

            // y - slope*x - intercept >= -M*(1 - s[i]) => y - slope*x - intercept - M*s[i] >= -M
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)) +
                    negSlopeXMonos + LinearMonomial(-mF, sVar), -interceptF),
                LinearPolynomial(emptyList(), -mF), Comparison.GE, "${name}_seg_${i}_eq_lb")
        }

        addConstraints(model, allConstraints, converter)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            breakpoints: List<V>,
            slopes: List<V>,
            intercepts: List<V>,
            m: V? = null,
            converter: IntoValue<V>,
            name: String = "piecewise",
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            UnivariateLinearPiecewiseFunction(
                x = x, breakpoints = breakpoints, slopes = slopes, intercepts = intercepts, m = m,
                converter = converter, name = name, displayName = displayName
            )

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            breakpoints: List<Flt64>,
            slopes: List<Flt64>,
            intercepts: List<Flt64>,
            m: Flt64 = Flt64(1e6),
            name: String = "piecewise",
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<Flt64> = UnivariateLinearPiecewiseFunction(
            x = x, breakpoints = breakpoints, slopes = slopes, intercepts = intercepts, m = m,
            converter = flt64Converter, name = name, displayName = displayName)

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            breakpoints: List<Flt64>,
            slopes: List<Flt64>,
            intercepts: List<Flt64>,
            m: Flt64 = Flt64(1e6),
            name: String = "piecewise",
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<Flt64> = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            breakpoints = breakpoints, slopes = slopes, intercepts = intercepts, m = m,
            converter = flt64Converter, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromPoints")
        fun fromPoints(
            x: LinearPolynomial<Flt64>,
            points: List<Point2>,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<Flt64> {
            require(points.size >= 2) { "Need at least 2 points" }
            val breakpoints = points.map { it[0] }
            val slopes = (0 until points.size - 1).map { i ->
                (points[i + 1][1] - points[i][1]) / (points[i + 1][0] - points[i][0])
            }
            val intercepts = (0 until points.size - 1).map { i ->
                points[i][1] - slopes[i] * points[i][0]
            }
            return UnivariateLinearPiecewiseFunction(x, breakpoints, slopes, intercepts, m, flt64Converter, name, displayName)
        }
    }
}