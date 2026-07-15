@file:Suppress("unused")

/** 平衡三值化函数符号 / Balanced ternaryzation function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 平衡三值化函数符号 / Balance ternaryzation function symbol
 *
 * 提供 [BalanceTernaryzationFunction]，将输入映射到 {-1, 0, 1}。
 *
 * Provides [BalanceTernaryzationFunction] for mapping inputs to {-1, 0, 1}.
*/

/**
 * 平衡三值化函数：将 x 映射为 sign(x) 取值 {-1, 0, 1}。
 * Balance Ternaryzation function: maps x to sign(x) in {-1, 0, 1}.
 *
 * 输出：
 * - y = 1  当 x > epsilon
 * - y = 0  当 -epsilon <= x <= epsilon
 * - y = -1 当 x < -epsilon
 *
 * Output:
 * - y = 1  when x > epsilon
 * - y = 0  when -epsilon <= x <= epsilon
 * - y = -1 when x < -epsilon
 *
 * 使用 [UnivariateLinearPiecewiseFunction] 的分段线性近似，断点位于符号转换边界。
 * Uses piecewise linear approximation via [UnivariateLinearPiecewiseFunction]
 * with breakpoints at the sign transition boundaries.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property epsilon 零阈值（默认 1e-6）/ zero threshold (default 1e-6)
 * @property extract 保留参数，当前未使用（始终使用分段线性）/ reserved parameter; currently unused (piecewise is always used)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 * @property fallbackLower 输入无有限下界时的分段回退下界 / fallback lower breakpoint when input has no finite lower bound
 * @property fallbackUpper 输入无有限上界时的分段回退上界 / fallback upper breakpoint when input has no finite upper bound
*/
class BalanceTernaryzationFunction<V>(
    val x: LinearPolynomial<V>,
    val epsilon: Flt64 = Flt64(1e-6),
    val extract: Boolean = true,
    private val converter: IntoValue<V>,
    override var name: String = "bter",
    override var displayName: String? = null,
    val fallbackLower: Flt64 = Flt64(-1e6),
    val fallbackUpper: Flt64 = Flt64(1e6)
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(fallbackLower ls fallbackUpper) {
            "BalanceTernaryzation fallbackLower must be less than fallbackUpper"
        }
    }

    private val impl: UnivariateLinearPiecewiseFunction<V> by lazy {
        val eps = epsilon
        val epsValue = converter.intoValue(eps)
        val negEpsValue = converter.intoValue(-eps)
        val precision = Flt64(1e-10)
        val precisionValue = converter.intoValue(precision)
        val xBounds = x.finiteBounds(converter)
        val inferredLower = xBounds?.lower ?: converter.intoValue(fallbackLower)
        val inferredUpper = xBounds?.upper ?: converter.intoValue(fallbackUpper)
        val xLower = if (inferredLower ls negEpsValue) inferredLower else negEpsValue - precisionValue
        val xUpper = if (inferredUpper gr epsValue) inferredUpper else epsValue + precisionValue
        val inversePrecision = Flt64.one / precision
        val breakpoints = listOf(
            xLower,
            negEpsValue,
            negEpsValue + precisionValue,
            epsValue - precisionValue,
            epsValue,
            xUpper
        )
        val slopes = listOf(
            Flt64.zero,   // segment 0: constant -1
            inversePrecision, // segment 1: rising from -1 to 0
            Flt64.zero,   // segment 2: constant 0
            inversePrecision, // segment 3: rising from 0 to 1
            Flt64.zero    // segment 4: constant 1
        ).map { converter.intoValue(it) }
        val intercepts = listOf(
            Flt64(-1.0),
            -Flt64.one + eps / precision,
            Flt64.zero,
            (precision - eps) / precision,
            Flt64.one
        ).map { converter.intoValue(it) }
        UnivariateLinearPiecewiseFunction(
            x = x,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            converter = converter,
            name = "${name}_impl",
            displayName = displayName
        )
    }

    val result: LinearPolynomial<V> by lazy { impl.result }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = impl.helperVariables

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val epsilonValue = converter.intoValue(epsilon)
        val minusOne = converter.intoValue(Flt64(-1.0))
        return when {
            xValue gr epsilonValue -> converter.one
            xValue ls -epsilonValue -> minusOne
            else -> converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return impl.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return impl.registerConstraints(model)
    }
    companion object {
        /**
         * 创建平衡三值化函数实例 / Create a balance ternaryzation function instance
         * @param x 输入线性多项式 / input linear polynomial
         * @param epsilon 零阈值 / zero threshold
         * @param extract 保留参数，当前未使用 / reserved parameter, currently unused
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param fallbackLower 输入无有限下界时的分段回退下界 / fallback lower breakpoint when input has no finite lower bound
         * @param fallbackUpper 输入无有限上界时的分段回退上界 / fallback upper breakpoint when input has no finite upper bound
         * @return [BalanceTernaryzationFunction] 实例 / [BalanceTernaryzationFunction] instance
        */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            epsilon: Flt64 = Flt64(1e-6),
            extract: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null,
            fallbackLower: Flt64 = Flt64(-1e6),
            fallbackUpper: Flt64 = Flt64(1e6)
        ): BalanceTernaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BalanceTernaryzationFunction(
                x = x,
                epsilon = epsilon,
                extract = extract,
                converter = converter,
                name = name,
                displayName = displayName,
                fallbackLower = fallbackLower,
                fallbackUpper = fallbackUpper
            )
    }
}
