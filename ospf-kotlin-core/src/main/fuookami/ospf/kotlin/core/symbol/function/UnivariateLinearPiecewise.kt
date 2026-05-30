@file:Suppress("unused")

/** 单变量线性分段函数符号 / Univariate linear piecewise function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 单变量线性分段函数符号 / Univariate linear piecewise function symbol
 *
 * 提供 [UnivariateLinearPiecewiseFunction]，使用断点和斜率定义分段线性函数。
 *
 * Provides [UnivariateLinearPiecewiseFunction] for defining piecewise linear functions using breakpoints and slopes.
 */

/**
 * 单变量线性分段函数：由断点和斜率定义的 y = f(x)。
 * Univariate linear piecewise function: y = f(x) defined by breakpoints and slopes.
 *
 * 使用二值选择变量选择激活的线段。
 * Uses binary selector variables to choose the active segment.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property breakpoints 断点值列表 / list of breakpoint values
 * @property slopes 各段斜率 / slope of each segment
 * @property intercepts 各段截距 / intercept of each segment
 * @param m Big-M 常量（默认 1e6）/ Big-M constant (default 1e6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
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
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
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
    override val resultPolynomial: LinearPolynomial<V> get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        for (i in 0 until numSegments) {
            val bpLow = breakpoints[i]
            val bpHigh = breakpoints[i + 1]
            val inLower = xValue gr bpLow || xValue eq bpLow
            val inUpper = xValue ls bpHigh || xValue eq bpHigh
            if (inLower && inUpper) {
                return slopes[i] * xValue + intercepts[i]
            }
        }
        return null
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val bigMValue = m
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Exactly one segment must be active: sum(s[i]) = 1 / 恰好一个线段激活：sum(s[i]) = 1
        val sumMonos = selectorVars.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(sumMonos, zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_select_one")

        for (i in 0 until numSegments) {
            val sVar = selectorVars[i]
            val bpLow = breakpoints[i]
            val bpHigh = breakpoints[i + 1]
            val slope = slopes[i]
            val intercept = intercepts[i]

            // Lower bound: x >= bpLow - M*(1 - s[i]) => x + M*s[i] >= bpLow - M... => x + M - M*s >= bpLow
            // 下界：x >= bpLow - M*(1 - s[i])，即 x + M - M*s >= bpLow
            allConstraints += LinearInequality(
                LinearPolynomial(x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    LinearMonomial(-bigMValue, sVar), x.constant + bigMValue),
                LinearPolynomial(emptyList(), bpLow), Comparison.GE, "${name}_seg_${i}_lb")

            // Upper bound: x <= bpHigh + M*(1 - s[i]) => x + M*s[i] <= bpHigh + M
            // 上界：x <= bpHigh + M*(1 - s[i])，即 x + M*s[i] <= bpHigh + M
            allConstraints += LinearInequality(
                LinearPolynomial(x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
                    LinearMonomial(bigMValue, sVar), x.constant),
                LinearPolynomial(emptyList(), bpHigh + bigMValue), Comparison.LE, "${name}_seg_${i}_ub")

            // y = slope*x + intercept when s[i]=1
            // s[i]=1 时 y = slope*x + intercept
            // y - slope*x - intercept <= M*(1 - s[i]) => y - slope*x - intercept + M*s[i] <= M
            // y - slope*x - intercept <= M*(1 - s[i])，即 y - slope*x - intercept + M*s[i] <= M
            val negSlopeXMonos = x.monomials.map { LinearMonomial(-it.coefficient * slope, it.symbol) }
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, resultVar)) +
                    negSlopeXMonos + LinearMonomial(bigMValue, sVar), -intercept),
                LinearPolynomial(emptyList(), bigMValue), Comparison.LE, "${name}_seg_${i}_eq_ub")

            // y - slope*x - intercept >= -M*(1 - s[i]) => y - slope*x - intercept - M*s[i] >= -M
            // y - slope*x - intercept >= -M*(1 - s[i])，即 y - slope*x - intercept - M*s[i] >= -M
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, resultVar)) +
                    negSlopeXMonos + LinearMonomial(-bigMValue, sVar), -intercept),
                LinearPolynomial(emptyList(), -bigMValue), Comparison.GE, "${name}_seg_${i}_eq_lb")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建分段线性函数实例。
         * Create a piecewise linear function instance.
         *
         * @param x 输入线性多项式 / the input linear polynomial
         * @param breakpoints 断点值列表 / list of breakpoint values
         * @param slopes 各段斜率 / slope of each segment
         * @param intercepts 各段截距 / intercept of each segment
         * @param m Big-M 常量（默认 1e6）/ Big-M constant (default 1e6)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 分段线性函数实例 / piecewise linear function instance
         */
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

        /**
         * 从采样点创建分段线性函数。
         * Create a piecewise linear function from sampling points.
         *
         * @param x 输入线性多项式 / the input linear polynomial
         * @param points 采样点列表 / list of sampling points
         * @param m Big-M 常量（默认 1e6）/ Big-M constant (default 1e6)
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 分段线性函数实例 / piecewise linear function instance
         */
        @JvmStatic
        @JvmName("fromPoints")
        fun <V> fromPoints(
            x: LinearPolynomial<V>,
            points: List<Point<Dim2, V>>,
            m: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<V> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
            require(points.size >= 2) { "Need at least 2 points" }
            val breakpoints = points.map { it[0] }
            val slopes = (0 until points.size - 1).map { i ->
                (points[i + 1][1] - points[i][1]) / (points[i + 1][0] - points[i][0])
            }
            val intercepts = (0 until points.size - 1).map { i ->
                points[i][1] - slopes[i] * points[i][0]
            }
            return UnivariateLinearPiecewiseFunction(x, breakpoints, slopes, intercepts, m, converter, name, displayName)
        }
    }
}
