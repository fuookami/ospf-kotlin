/**
 * 二次不等式
 * Quadratic Inequality
 *
 * 定义二次不等式，左右两边均为二次多项式。
 * 二次不等式在二次规划和凸优化问题中广泛使用。
 * Defines quadratic inequalities, where both sides are quadratic polynomials.
 * Quadratic inequalities are widely used in quadratic programming
 * and convex optimization problems.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.operation.evaluateOrdered
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

/**
 * 二次不等式
 * Quadratic Inequality
 *
 * 表示二次不等式，包含左侧二次多项式、右侧二次多项式和比较运算符。
 * 二次不等式在二次规划和凸优化问题中广泛使用。
 * Represents a quadratic inequality, containing left-hand quadratic polynomial,
 * right-hand quadratic polynomial, and comparison operator.
 * Quadratic inequalities are widely used in quadratic programming and convex optimization.
 *
 * @property lhs 左侧二次多项式 / Left-hand quadratic polynomial
 * @property rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @property comparison 比较运算符 / Comparison operator
 */
data class QuadraticInequalityOf<T : Ring<T>>(
    val lhs: QuadraticPolynomial<T>,
    val rhs: QuadraticPolynomial<T>,
    val comparison: Comparison,
    val name: String = "",
    val displayName: String = ""
) {
    fun reverse(): QuadraticInequalityOf<T> {
        return QuadraticInequalityOf(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse(),
            name = name,
            displayName = displayName
        )
    }
}

// Backward compatibility typealias (Phase 1)
typealias QuadraticInequality = QuadraticInequalityOf<Flt64>

/**
 * 将二次单项式转换为二次多项式
 * Converts a quadratic monomial to a quadratic polynomial
 *
 * @receiver 二次单项式 / The quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
private fun QuadraticMonomial<Flt64>.asPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(this), Flt64.zero)
}

/**
 * 将线性单项式转换为二次多项式
 * Converts a linear monomial to a quadratic polynomial
 *
 * @receiver 线性单项式 / The linear monomial
 * @return 二次多项式（线性项）/ Quadratic polynomial (linear term)
 */
private fun LinearMonomial<Flt64>.asPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)), Flt64.zero)
}

/**
 * 将浮点数转换为二次多项式（常数项）
 * Converts a floating-point number to a quadratic polynomial (constant term)
 *
 * @receiver 浮点数 / The floating-point number
 * @return 常数二次多项式 / Constant quadratic polynomial
 */
private fun Flt64.asQuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), this)
}

// 二次多项式之间的比较运算符 / Comparison operators between quadratic polynomials

/**
 * 二次多项式小于比较
 * Quadratic polynomial less than comparison
 */
infix fun QuadraticPolynomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequalityOf(this, rhs, Comparison.LT)

/**
 * 二次多项式小于等于比较
 * Quadratic polynomial less than or equal comparison
 */
infix fun QuadraticPolynomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.LE)

/**
 * 二次多项式等于比较
 * Quadratic polynomial equal comparison
 */
infix fun QuadraticPolynomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.EQ)

/**
 * 二次多项式不等于比较
 * Quadratic polynomial not equal comparison
 */
infix fun QuadraticPolynomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.NE)

/**
 * 二次多项式大于等于比较
 * Quadratic polynomial greater than or equal comparison
 */
infix fun QuadraticPolynomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.GE)

/**
 * 二次多项式大于比较
 * Quadratic polynomial greater than comparison
 */
infix fun QuadraticPolynomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.GT)

// 二次单项式之间的比较运算符 / Comparison operators between quadratic monomials

/**
 * 二次单项式小于比较
 * Quadratic monomial less than comparison
 */
infix fun QuadraticMonomial<Flt64>.lt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs.asPolynomial()

/**
 * 二次单项式小于等于比较
 * Quadratic monomial less than or equal comparison
 */
infix fun QuadraticMonomial<Flt64>.le(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() le rhs.asPolynomial()

/**
 * 二次单项式等于比较
 * Quadratic monomial equal comparison
 */
infix fun QuadraticMonomial<Flt64>.eq(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs.asPolynomial()

/**
 * 二次单项式不等于比较
 * Quadratic monomial not equal comparison
 */
infix fun QuadraticMonomial<Flt64>.ne(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs.asPolynomial()

/**
 * 二次单项式大于等于比较
 * Quadratic monomial greater than or equal comparison
 */
infix fun QuadraticMonomial<Flt64>.ge(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs.asPolynomial()

/**
 * 二次单项式大于比较
 * Quadratic monomial greater than comparison
 */
infix fun QuadraticMonomial<Flt64>.gt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs.asPolynomial()

// 二次多项式与线性多项式的比较运算符 / Comparison operators between quadratic polynomial and linear polynomial

/**
 * 二次多项式小于线性多项式
 * Quadratic polynomial less than linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this lt rhs.toQuadraticPolynomial()

/**
 * 二次多项式小于等于线性多项式
 * Quadratic polynomial less than or equal linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this le rhs.toQuadraticPolynomial()

/**
 * 二次多项式等于线性多项式
 * Quadratic polynomial equal linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this eq rhs.toQuadraticPolynomial()

/**
 * 二次多项式不等于线性多项式
 * Quadratic polynomial not equal linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this ne rhs.toQuadraticPolynomial()

/**
 * 二次多项式大于等于线性多项式
 * Quadratic polynomial greater than or equal linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this ge rhs.toQuadraticPolynomial()

/**
 * 二次多项式大于线性多项式
 * Quadratic polynomial greater than linear polynomial
 */
infix fun QuadraticPolynomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this gt rhs.toQuadraticPolynomial()

// 线性多项式与二次多项式的比较运算符 / Comparison operators between linear polynomial and quadratic polynomial

/**
 * 线性多项式小于二次多项式
 * Linear polynomial less than quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() lt rhs

/**
 * 线性多项式小于等于二次多项式
 * Linear polynomial less than or equal quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() le rhs

/**
 * 线性多项式等于二次多项式
 * Linear polynomial equal quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() eq rhs

/**
 * 线性多项式不等于二次多项式
 * Linear polynomial not equal quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() ne rhs

/**
 * 线性多项式大于等于二次多项式
 * Linear polynomial greater than or equal quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() ge rhs

/**
 * 线性多项式大于二次多项式
 * Linear polynomial greater than quadratic polynomial
 */
infix fun LinearPolynomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() gt rhs

// 二次多项式与二次单项式的比较运算符 / Comparison operators between quadratic polynomial and quadratic monomial

/**
 * 二次多项式小于二次单项式
 * Quadratic polynomial less than quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.lt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this lt rhs.asPolynomial()

/**
 * 二次多项式小于等于二次单项式
 * Quadratic polynomial less than or equal quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.le(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this le rhs.asPolynomial()

/**
 * 二次多项式等于二次单项式
 * Quadratic polynomial equal quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.eq(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this eq rhs.asPolynomial()

/**
 * 二次多项式不等于二次单项式
 * Quadratic polynomial not equal quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.ne(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this ne rhs.asPolynomial()

/**
 * 二次多项式大于等于二次单项式
 * Quadratic polynomial greater than or equal quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.ge(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this ge rhs.asPolynomial()

/**
 * 二次多项式大于二次单项式
 * Quadratic polynomial greater than quadratic monomial
 */
infix fun QuadraticPolynomial<Flt64>.gt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this gt rhs.asPolynomial()

// 二次单项式与二次多项式的比较运算符 / Comparison operators between quadratic monomial and quadratic polynomial

/**
 * 二次单项式小于二次多项式
 * Quadratic monomial less than quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs

/**
 * 二次单项式小于等于二次多项式
 * Quadratic monomial less than or equal quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() le rhs

/**
 * 二次单项式等于二次多项式
 * Quadratic monomial equal quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs

/**
 * 二次单项式不等于二次多项式
 * Quadratic monomial not equal quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs

/**
 * 二次单项式大于等于二次多项式
 * Quadratic monomial greater than or equal quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs

/**
 * 二次单项式大于二次多项式
 * Quadratic monomial greater than quadratic polynomial
 */
infix fun QuadraticMonomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs

// 二次多项式与线性单项式的比较运算符 / Comparison operators between quadratic polynomial and linear monomial

/**
 * 二次多项式小于线性单项式
 * Quadratic polynomial less than linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): QuadraticInequality = this lt rhs.asPolynomial()

/**
 * 二次多项式小于等于线性单项式
 * Quadratic polynomial less than or equal linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.le(rhs: LinearMonomial<Flt64>): QuadraticInequality = this le rhs.asPolynomial()

/**
 * 二次多项式等于线性单项式
 * Quadratic polynomial equal linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): QuadraticInequality = this eq rhs.asPolynomial()

/**
 * 二次多项式不等于线性单项式
 * Quadratic polynomial not equal linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): QuadraticInequality = this ne rhs.asPolynomial()

/**
 * 二次多项式大于等于线性单项式
 * Quadratic polynomial greater than or equal linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): QuadraticInequality = this ge rhs.asPolynomial()

/**
 * 二次多项式大于线性单项式
 * Quadratic polynomial greater than linear monomial
 */
infix fun QuadraticPolynomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): QuadraticInequality = this gt rhs.asPolynomial()

// 线性单项式与二次多项式的比较运算符 / Comparison operators between linear monomial and quadratic polynomial

/**
 * 线性单项式小于二次多项式
 * Linear monomial less than quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs

/**
 * 线性单项式小于等于二次多项式
 * Linear monomial less than or equal quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() le rhs

/**
 * 线性单项式等于二次多项式
 * Linear monomial equal quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs

/**
 * 线性单项式不等于二次多项式
 * Linear monomial not equal quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs

/**
 * 线性单项式大于等于二次多项式
 * Linear monomial greater than or equal quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs

/**
 * 线性单项式大于二次多项式
 * Linear monomial greater than quadratic polynomial
 */
infix fun LinearMonomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs

// 二次多项式与浮点数的比较运算符 / Comparison operators between quadratic polynomial and floating-point number

/**
 * 二次多项式小于浮点数
 * Quadratic polynomial less than floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.lt(rhs: Flt64): QuadraticInequality = this lt rhs.asQuadraticPolynomial()

/**
 * 二次多项式小于等于浮点数
 * Quadratic polynomial less than or equal floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.le(rhs: Flt64): QuadraticInequality = this le rhs.asQuadraticPolynomial()

/**
 * 二次多项式等于浮点数
 * Quadratic polynomial equal floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.eq(rhs: Flt64): QuadraticInequality = this eq rhs.asQuadraticPolynomial()

/**
 * 二次多项式不等于浮点数
 * Quadratic polynomial not equal floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.ne(rhs: Flt64): QuadraticInequality = this ne rhs.asQuadraticPolynomial()

/**
 * 二次多项式大于等于浮点数
 * Quadratic polynomial greater than or equal floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.ge(rhs: Flt64): QuadraticInequality = this ge rhs.asQuadraticPolynomial()

/**
 * 二次多项式大于浮点数
 * Quadratic polynomial greater than floating-point number
 */
infix fun QuadraticPolynomial<Flt64>.gt(rhs: Flt64): QuadraticInequality = this gt rhs.asQuadraticPolynomial()

// 浮点数与二次多项式的比较运算符 / Comparison operators between floating-point number and quadratic polynomial

/**
 * 浮点数小于二次多项式
 * Floating-point number less than quadratic polynomial
 */
infix fun Flt64.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() lt rhs

/**
 * 浮点数小于等于二次多项式
 * Floating-point number less than or equal quadratic polynomial
 */
infix fun Flt64.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() le rhs

/**
 * 浮点数等于二次多项式
 * Floating-point number equal quadratic polynomial
 */
infix fun Flt64.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() eq rhs

/**
 * 浮点数不等于二次多项式
 * Floating-point number not equal quadratic polynomial
 */
infix fun Flt64.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() ne rhs

/**
 * 浮点数大于等于二次多项式
 * Floating-point number greater than or equal quadratic polynomial
 */
infix fun Flt64.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() ge rhs

/**
 * 浮点数大于二次多项式
 * Floating-point number greater than quadratic polynomial
 */
infix fun Flt64.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() gt rhs

// ========== Named inequality constructors ==========

fun QuadraticPolynomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.LT, name, displayName)
fun QuadraticPolynomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.LE, name, displayName)
fun QuadraticPolynomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.EQ, name, displayName)
fun QuadraticPolynomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.NE, name, displayName)
fun QuadraticPolynomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.GE, name, displayName)
fun QuadraticPolynomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs, Comparison.GT, name, displayName)

fun QuadraticPolynomial<Flt64>.lt(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.LT, name, displayName)
fun QuadraticPolynomial<Flt64>.le(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.LE, name, displayName)
fun QuadraticPolynomial<Flt64>.eq(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.EQ, name, displayName)
fun QuadraticPolynomial<Flt64>.ne(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.NE, name, displayName)
fun QuadraticPolynomial<Flt64>.ge(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.GE, name, displayName)
fun QuadraticPolynomial<Flt64>.gt(rhs: Flt64, name: String, displayName: String = ""): QuadraticInequality =
    QuadraticInequality(this, rhs.asQuadraticPolynomial(), Comparison.GT, name, displayName)

/**
 * 检查给定赋值是否满足此二次不等式
 * Check whether the given assignment satisfies this quadratic inequality
 *
 * 计算不等式两侧的二次多项式在给定符号赋值下的值，并比较它们是否满足
 * 不等式的比较关系。如果任何符号缺少赋值，返回 null。
 * Evaluates both sides of the quadratic inequality under the given symbol assignment
 * and checks whether the comparison relation is satisfied. Returns null if any
 * symbol is missing an assignment.
 *
 * @param values 符号到 Flt64 值的映射 / Map of symbols to Flt64 values
 * @return 是否满足不等式，或 null（赋值不完整时）
 *         Whether the inequality is satisfied, or null if assignment is incomplete
 */
fun QuadraticInequality.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val lhsValue = lhs.evaluate(values) ?: return null
    val rhsValue = rhs.evaluate(values) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 使用有序列表检查赋值是否满足此二次不等式
 * Check whether an ordered assignment satisfies this quadratic inequality
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
fun QuadraticInequality.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}

