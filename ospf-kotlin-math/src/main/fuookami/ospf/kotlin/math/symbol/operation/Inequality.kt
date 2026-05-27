/**
 * Flt64 不等式 DSL 和求值
 * Flt64 Inequality DSL and Evaluation
 *
 * 提供 Flt64 不等式的符号级 DSL（lt/le/eq/ne/ge/gt）和满足性判断。
 * Supports symbol-level comparison DSL and satisfaction checking for Flt64 inequalities.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

// ========== Comparison.satisfiedBy ==========

/**
 * 判断 Flt64 值是否满足比较关系
 * Check if Flt64 values satisfy the comparison relation
 *
 * @param lhs 左侧操作数 / Left-hand operand
 * @param rhs 右侧操作数 / Right-hand operand
 * @return 是否满足比较关系 / Whether the comparison is satisfied
 */
fun Comparison.satisfiedBy(lhs: Flt64, rhs: Flt64): Boolean {
    return when (this) {
        Comparison.LT -> lhs < rhs
        Comparison.LE -> lhs <= rhs
        Comparison.EQ -> lhs == rhs
        Comparison.NE -> lhs != rhs
        Comparison.GE -> lhs >= rhs
        Comparison.GT -> lhs > rhs
    }
}

// ========== Symbol-level DSL ==========
// 符号级不等式 DSL 运算符重载 / Symbol-level inequality DSL operator overloads

private fun Symbol.asLinearPolynomial(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)
}

private fun Flt64.asLinearPolynomial(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearPolynomial(emptyList(), this)
}

infix fun Symbol.lt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Flt64.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Flt64.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Flt64.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Flt64.ne(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Flt64.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Flt64.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Symbol.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

// ========== isSatisfied ==========
// 不等式满足性判断 / Inequality satisfaction checking

/**
 * 判断 Flt64 线性不等式是否被给定值满足
 * Check if a Flt64 linear inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
 */
fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 线性不等式是否被有序值满足
 * Check if a Flt64 linear inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
 */
fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}

/**
 * 判断 Flt64 二次不等式是否被给定值满足
 * Check if a Flt64 quadratic inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
 */
fun QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 二次不等式是否被有序值满足
 * Check if a Flt64 quadratic inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
 */
fun QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}

/**
 * 判断 Flt64 规范不等式是否被给定值满足
 * Check if a Flt64 canonical inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
 */
fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 规范不等式是否被有序值满足
 * Check if a Flt64 canonical inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
 */
fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}
