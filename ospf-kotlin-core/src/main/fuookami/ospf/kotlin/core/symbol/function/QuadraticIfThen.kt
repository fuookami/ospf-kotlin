package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次条件控制二次结果，假分支为零 / Quadratic condition with quadratic consequent and zero false branch.
 *
 * @param condition 有界二次条件表达式 / bounded quadratic condition
 * @param thenPoly 条件为真时的有界二次值 / bounded quadratic value when the condition is true
 * @param relation 条件比较关系 / condition comparison relation
 * @param strictBoundary 真、假分支间隔 / gap between true and false branches
 * @param delta 离散步长；默认使用边界间隔 / discrete step, defaulting to the boundary gap
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticIfThenFunction<V>(
    condition: QuadraticPolynomial<V>,
    thenPoly: QuadraticPolynomial<V>,
    private val relation: Comparison = Comparison.GT,
    private val strictBoundary: V? = null,
    private val delta: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_ifthen",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(condition, thenPoly),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = IfThenFunction(
        condition = inputs[0],
        thenPoly = inputs[1],
        relation = relation,
        strictBoundary = strictBoundary,
        delta = delta,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
