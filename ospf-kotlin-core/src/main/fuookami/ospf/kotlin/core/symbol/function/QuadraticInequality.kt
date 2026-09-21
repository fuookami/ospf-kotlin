package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次不等式满足指示器 / Quadratic inequality satisfaction indicator.
 *
 * @param lhs 有界二次左侧表达式 / bounded quadratic left-hand expression
 * @param rhs 右侧常数 / right-hand constant
 * @param sign 比较关系 / comparison relation
 * @param bigM 可选显式 Big-M；默认按范围推导 / optional explicit Big-M, inferred from bounds by default
 * @param tolerance 非严格比较间隔或相等关系零容差 / gap for non-strict comparisons or zero tolerance for equality
 * @param strictBoundary 真、假分支间隔 / gap between true and false branches
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticInequalityFunction<V>(
    lhs: QuadraticPolynomial<V>,
    private val rhs: V,
    private val sign: Comparison,
    private val bigM: V? = null,
    private val tolerance: V? = null,
    private val strictBoundary: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_inequality",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(lhs),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = InequalityFunction(
        lhs = inputs[0],
        rhs = rhs,
        sign = sign,
        bigM = bigM,
        tolerance = tolerance,
        strictBoundary = strictBoundary,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
