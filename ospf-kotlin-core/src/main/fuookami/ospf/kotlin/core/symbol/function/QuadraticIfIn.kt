package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次输入的闭区间指示器 / Closed-interval indicator of a quadratic input.
 *
 * @param input 有界二次输入 / bounded quadratic input
 * @param lower 闭区间下界 / inclusive lower endpoint
 * @param upper 闭区间上界 / inclusive upper endpoint
 * @param strictBoundary 真、假分支间隔 / gap between true and false branches
 * @param delta 离散步长；默认使用边界间隔 / discrete step, defaulting to the boundary gap
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticIfInFunction<V>(
    input: QuadraticPolynomial<V>,
    private val lower: V,
    private val upper: V,
    private val strictBoundary: V? = null,
    private val delta: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_ifin",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(input),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = IfInFunction(
        x = inputs[0],
        lower = lower,
        upper = upper,
        strictBoundary = strictBoundary,
        delta = delta,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
