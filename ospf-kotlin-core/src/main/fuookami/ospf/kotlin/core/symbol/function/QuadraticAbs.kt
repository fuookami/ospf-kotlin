package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次输入的精确绝对值 / Exact absolute value of a quadratic input.
 *
 * @param polynomial 有界二次输入 / bounded quadratic input
 * @param bigM 可选显式 Big-M；默认按范围推导 / optional explicit Big-M, inferred from bounds by default
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticAbsFunction<V>(
    polynomial: QuadraticPolynomial<V>,
    private val bigM: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_abs",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(polynomial),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = AbsFunction(
        polynomial = inputs[0],
        bigM = bigM,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
