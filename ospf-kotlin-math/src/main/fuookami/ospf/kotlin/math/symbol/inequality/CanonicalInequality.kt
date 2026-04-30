/**
 * 规范不等式
 * Canonical Inequality
 *
 * 定义规范形式的不等式，左右两边均为规范多项式。
 * 规范不等式支持任意次数的多项式，是最通用的不等式表示形式。
 * Defines inequalities in canonical form, where both sides are canonical polynomials.
 * Canonical inequalities support polynomials of any degree,
 * representing the most general form of inequalities.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

/**
 * 规范不等式
 * Canonical Inequality
 *
 * 表示规范形式的不等式，包含左侧多项式、右侧多项式和比较运算符。
 * 规范不等式可以表示任意次数的多项式不等式，是最通用的不等式表示形式。
 * Represents an inequality in canonical form, containing left-hand polynomial,
 * right-hand polynomial, and comparison operator.
 * Canonical inequalities can represent polynomial inequalities of any degree,
 * being the most general form of inequality representation.
 *
 * @property lhs 左侧规范多项式 / Left-hand canonical polynomial
 * @property rhs 右侧规范多项式 / Right-hand canonical polynomial
 * @property comparison 比较运算符 / Comparison operator
 */
data class CanonicalInequality(
    val lhs: CanonicalPolynomial<F64>,
    val rhs: CanonicalPolynomial<F64>,
    val comparison: Comparison
) {
    /**
     * 反转不等式
     * Reverses the inequality
     *
     * 交换左右两侧并反转比较运算符，生成等价的不等式。
     * 例如：a < b 反转为 b > a。
     * Swaps left and right sides and reverses the comparison operator,
     * generating an equivalent inequality.
     * For example: a < b reverses to b > a.
     *
     * @return 反转后的不等式 / The reversed inequality
     */
    fun reverse(): CanonicalInequality {
        return CanonicalInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    }
}

/**
 * 检查给定赋值是否满足此规范不等式
 * Check whether the given assignment satisfies this canonical inequality
 *
 * 计算不等式两侧的规范多项式在给定符号赋值下的值，并比较它们是否满足
 * 不等式的比较关系。如果任何符号缺少赋值，返回 null。
 * Evaluates both sides of the canonical inequality under the given symbol assignment
 * and checks whether the comparison relation is satisfied. Returns null if any
 * symbol is missing an assignment.
 *
 * @param values 符号到 Flt64 值的映射 / Map of symbols to Flt64 values
 * @return 是否满足不等式，或 null（赋值不完整时）
 *         Whether the inequality is satisfied, or null if assignment is incomplete
 */
fun CanonicalInequality.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val lhsValue = lhs.evaluate(values) ?: return null
    val rhsValue = rhs.evaluate(values) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 使用有序列表检查赋值是否满足此规范不等式
 * Check whether an ordered assignment satisfies this canonical inequality
 *
 * 与 [isSatisfied] 不同，此函数接受符号顺序列表和对应的值列表，避免
 * 创建 Map 的开销。适用于求解器返回有序结果的场景。
 * Unlike [isSatisfied], this function accepts an ordered list of symbols and
 * a corresponding list of values, avoiding the overhead of creating a Map.
 * Suitable for scenarios where solvers return ordered results.
 *
 * @param order 符号的顺序列表 / Ordered list of symbols
 * @param values 与 order 一一对应的值列表 / List of values corresponding to order
 * @return 是否满足不等式 / Whether the inequality is satisfied
 */
fun CanonicalInequality.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}
