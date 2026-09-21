package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次条件指示器，继承线性版本的边界间隔语义 / Quadratic condition indicator preserving boundary-gap semantics.
 *
 * @param condition 有界二次条件表达式 / bounded quadratic condition
 * @param relation 条件比较关系 / condition comparison relation
 * @param strictBoundary 真、假分支间隔 / gap between true and false branches
 * @param delta 离散步长；默认使用边界间隔 / discrete step, defaulting to the boundary gap
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticIfFunction<V>(
    condition: QuadraticPolynomial<V>,
    private val relation: Comparison = Comparison.GT,
    private val strictBoundary: V? = null,
    private val delta: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_if",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(condition),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = IfFunction(
        condition = inputs[0],
        relation = relation,
        strictBoundary = strictBoundary,
        delta = delta,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
