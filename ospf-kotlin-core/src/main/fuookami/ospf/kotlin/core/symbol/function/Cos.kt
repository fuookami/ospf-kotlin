@file:Suppress("unused")

/** 余弦函数符号 / Cosine function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 余弦函数符号 / Cosine function symbol
 *
 * 提供 [CosFunction]，使用分段线性插值近似余弦函数。
 *
 * Provides [CosFunction] for piecewise linear approximation of the cosine function.
*/

/**
 * 通过分段线性插值近似的余弦函数。
 * Cosine function approximated by piecewise linear interpolation.
 *
 * 这是 [UnivariateLinearPiecewiseFunction] 的薄包装。
 * This is a thin wrapper around [UnivariateLinearPiecewiseFunction].
 * 余弦函数 cos(v) 在关键点处采样用于 MIP 编码。
 * The cosine function cos(v) is sampled at strategic points for MIP encoding.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property samplingPoints 预计算的 (x, cos(x)) 断点 / pre-computed (x, cos(x)) break points
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class CosFunction<V>(
    val x: LinearPolynomial<V>,
    val samplingPoints: List<Point<Dim2, Flt64>>,
    private val converter: IntoValue<V>,
    override var name: String = "cos",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val impl: UnivariateLinearPiecewiseFunction<V> by lazy {
        val breakpoints = samplingPoints.map { converter.intoValue(it[0]) }
        val slopes = mutableListOf<V>()
        val intercepts = mutableListOf<V>()
        for (i in 0 until samplingPoints.size - 1) {
            val x0 = samplingPoints[i][0]
            val y0 = samplingPoints[i][1]
            val x1 = samplingPoints[i + 1][0]
            val y1 = samplingPoints[i + 1][1]
            val slopeVal = (y1 - y0) / (x1 - x0)
            val slope = converter.intoValue(slopeVal)
            val intercept = converter.intoValue(y0 - slopeVal * x0)
            slopes.add(slope)
            intercepts.add(intercept)
        }
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
        return impl.evaluate(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return impl.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return impl.registerConstraints(model)
    }
    companion object {
        /**
         * 创建余弦函数实例 / Create a cosine function instance
         * @param x 输入线性多项式 / input linear polynomial
         * @param samplingPoints 预计算的 (x, cos(x)) 断点 / pre-computed (x, cos(x)) break points
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [CosFunction] 实例 / [CosFunction] instance
        */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            samplingPoints: List<Point<Dim2, Flt64>> = defaultPoints(),
            converter: IntoValue<V>,
            name: String = "cos",
            displayName: String? = null
        ): CosFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            CosFunction(x = x, samplingPoints = samplingPoints, converter = converter, name = name, displayName = displayName)

        /**
         * 生成默认余弦采样点（-pi 到 pi 区间内 5 个关键点）。
         * Generate default cosine sampling points (5 key points in the range [-pi, pi]).
         *
         * @return 默认采样点列表 / default sampling point list
        */
        private fun defaultPoints(): List<Point<Dim2, Flt64>> {
            val pi = Flt64(kotlin.math.PI)
            val pi2 = pi / Flt64(2.0)
            return listOf(
                point2(-pi, Flt64(-1.0)),
                point2(-pi2, Flt64.zero),
                point2(Flt64.zero, Flt64.one),
                point2(pi2, Flt64.zero),
                point2(pi, Flt64(-1.0))
            )
        }
    }
}
