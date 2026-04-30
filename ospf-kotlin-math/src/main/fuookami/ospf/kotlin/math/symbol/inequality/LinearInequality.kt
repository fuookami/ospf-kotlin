/**
 * 线性不等式
 * Linear Inequality
 *
 * 定义线性不等式，左右两边均为线性多项式。
 * 线性不等式在优化问题中广泛使用，特别是线性规划和混合整数规划。
 * Defines linear inequalities, where both sides are linear polynomials.
 * Linear inequalities are widely used in optimization problems,
 * especially in linear programming and mixed-integer programming.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.zeroOf

/**
 * 线性不等式
 * Linear Inequality
 *
 * 表示线性不等式，包含左侧线性多项式、右侧线性多项式和比较运算符。
 * 线性不等式是优化问题中最基本的约束形式，广泛用于线性规划和混合整数规划。
 * Represents a linear inequality, containing left-hand linear polynomial,
 * right-hand linear polynomial, and comparison operator.
 * Linear inequalities are the most basic constraint form in optimization problems,
 * widely used in linear programming and mixed-integer programming.
 *
 * @property lhs 左侧线性多项式 / Left-hand linear polynomial
 * @property rhs 右侧线性多项式 / Right-hand linear polynomial
 * @property comparison 比较运算符 / Comparison operator
 */
data class LinearInequality<T : Ring<T>>(
    val lhs: LinearPolynomial<T>,
    val rhs: LinearPolynomial<T>,
    val comparison: Comparison,
    val name: String = "",
    val displayName: String = ""
) {
    fun reverse(): LinearInequality<T> {
        return LinearInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse(),
            name = name,
            displayName = displayName
        )
    }
}

/** Flt64 兼容类型别名 / Flt64 compatibility typealias */
typealias Flt64LinearInequality = LinearInequality<Flt64>

// ========== Private helper functions ==========

private fun <T : Ring<T>> LinearMonomial<T>.asPolynomial(): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), zeroOf(coefficient))
}

private fun <T : Ring<T>> T.asLinearPolynomial(): LinearPolynomial<T> {
    return LinearPolynomial(emptyList(), this)
}

// ========== LinearPolynomial vs LinearPolynomial ==========

infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.LT)
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.LE)
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.EQ)
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.NE)
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.GE)
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.GT)

// ========== LinearMonomial vs LinearMonomial ==========

infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() lt rhs.asPolynomial()
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() le rhs.asPolynomial()
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() eq rhs.asPolynomial()
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() ne rhs.asPolynomial()
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() ge rhs.asPolynomial()
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() gt rhs.asPolynomial()

// ========== LinearMonomial vs LinearPolynomial ==========

infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() lt rhs
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() le rhs
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() eq rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() ne rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() ge rhs
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() gt rhs

// ========== LinearPolynomial vs LinearMonomial ==========

infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs.asPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs.asPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearMonomial<T>): LinearInequality<T> = this eq rhs.asPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs.asPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs.asPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs.asPolynomial()

// ========== LinearPolynomial vs scalar ==========

infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: T): LinearInequality<T> = this lt rhs.asLinearPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: T): LinearInequality<T> = this le rhs.asLinearPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: T): LinearInequality<T> = this eq rhs.asLinearPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: T): LinearInequality<T> = this ne rhs.asLinearPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: T): LinearInequality<T> = this ge rhs.asLinearPolynomial()
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: T): LinearInequality<T> = this gt rhs.asLinearPolynomial()

// ========== Scalar vs LinearPolynomial ==========

infix fun <T : Ring<T>> T.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() lt rhs
infix fun <T : Ring<T>> T.le(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() le rhs
infix fun <T : Ring<T>> T.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() eq rhs
infix fun <T : Ring<T>> T.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() ne rhs
infix fun <T : Ring<T>> T.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() ge rhs
infix fun <T : Ring<T>> T.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() gt rhs

// ========== Symbol-level DSL ==========

private fun Symbol.asLinearPolynomial(): LinearPolynomial<F64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)
}

private fun linearIneq(lhs: LinearPolynomial<F64>, rhs: LinearPolynomial<F64>, cmp: Comparison): Flt64LinearInequality =
    LinearInequality(lhs, rhs, cmp)

infix fun Symbol.lt(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Flt64): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Flt64.lt(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Flt64.le(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Flt64.eq(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Flt64.ne(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Flt64.ge(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Flt64.gt(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Symbol.lt(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Symbol): Flt64LinearInequality = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

// ========== Named inequality constructors ==========

fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.LT, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.LE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.EQ, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.NE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.GE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.GT, name, displayName)

fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.LT, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.LE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.EQ, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.NE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.GE, name, displayName)
fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.GT, name, displayName)

/**
 * 检查给定赋值是否满足此线性不等式
 * Check whether the given assignment satisfies this linear inequality
 *
 * 计算不等式两侧的线性多项式在给定符号赋值下的值，并比较它们是否满足
 * 不等式的比较关系。如果任何符号缺少赋值，返回 null。
 * Evaluates both sides of the linear inequality under the given symbol assignment
 * and checks whether the comparison relation is satisfied. Returns null if any
 * symbol is missing an assignment.
 *
 * @param values 符号到 Flt64 值的映射 / Map of symbols to Flt64 values
 * @return 是否满足不等式，或 null（赋值不完整时）
 *         Whether the inequality is satisfied, or null if assignment is incomplete
 */
fun Flt64LinearInequality.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val lhsValue = lhs.evaluate(values) ?: return null
    val rhsValue = rhs.evaluate(values) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 使用有序列表检查赋值是否满足此线性不等式
 * Check whether an ordered assignment satisfies this linear inequality
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
fun Flt64LinearInequality.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}
