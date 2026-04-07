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
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

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
data class LinearInequality(
    val lhs: LinearPolynomial<Flt64>,
    val rhs: LinearPolynomial<Flt64>,
    val comparison: Comparison
)

/**
 * 将线性单项式转换为线性多项式
 * Converts a linear monomial to a linear polynomial
 *
 * @receiver 线性单项式 / The linear monomial
 * @return 线性多项式 / Linear polynomial
 */
private fun LinearMonomial<Flt64>.asPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), Flt64.zero)
}

/**
 * 将浮点数转换为线性多项式（常数项）
 * Converts a floating-point number to a linear polynomial (constant term)
 *
 * @receiver 浮点数 / The floating-point number
 * @return 常数线性多项式 / Constant linear polynomial
 */
private fun Flt64.asLinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), this)
}

/**
 * 线性多项式小于比较
 * Linear polynomial less than comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.LT)

/**
 * 线性多项式小于等于比较
 * Linear polynomial less than or equal comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.LE)

/**
 * 线性多项式等于比较
 * Linear polynomial equal comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.EQ)

/**
 * 线性多项式不等于比较
 * Linear polynomial not equal comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.NE)

/**
 * 线性多项式大于等于比较
 * Linear polynomial greater than or equal comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.GE)

/**
 * 线性多项式大于比较
 * Linear polynomial greater than comparison
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun LinearPolynomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.GT)

// 线性单项式之间的比较运算符 / Comparison operators between linear monomials

/**
 * 线性单项式小于比较
 * Linear monomial less than comparison
 */
infix fun LinearMonomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() lt rhs.asPolynomial()

/**
 * 线性单项式小于等于比较
 * Linear monomial less than or equal comparison
 */
infix fun LinearMonomial<Flt64>.le(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() le rhs.asPolynomial()

/**
 * 线性单项式等于比较
 * Linear monomial equal comparison
 */
infix fun LinearMonomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() eq rhs.asPolynomial()

/**
 * 线性单项式不等于比较
 * Linear monomial not equal comparison
 */
infix fun LinearMonomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() ne rhs.asPolynomial()

/**
 * 线性单项式大于等于比较
 * Linear monomial greater than or equal comparison
 */
infix fun LinearMonomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() ge rhs.asPolynomial()

/**
 * 线性单项式大于比较
 * Linear monomial greater than comparison
 */
infix fun LinearMonomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() gt rhs.asPolynomial()

// 线性单项式与线性多项式的比较运算符 / Comparison operators between linear monomial and linear polynomial

/**
 * 线性单项式小于线性多项式
 * Linear monomial less than linear polynomial
 */
infix fun LinearMonomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() lt rhs

/**
 * 线性单项式小于等于线性多项式
 * Linear monomial less than or equal linear polynomial
 */
infix fun LinearMonomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() le rhs

/**
 * 线性单项式等于线性多项式
 * Linear monomial equal linear polynomial
 */
infix fun LinearMonomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() eq rhs

/**
 * 线性单项式不等于线性多项式
 * Linear monomial not equal linear polynomial
 */
infix fun LinearMonomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() ne rhs

/**
 * 线性单项式大于等于线性多项式
 * Linear monomial greater than or equal linear polynomial
 */
infix fun LinearMonomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() ge rhs

/**
 * 线性单项式大于线性多项式
 * Linear monomial greater than linear polynomial
 */
infix fun LinearMonomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() gt rhs

// 线性多项式与线性单项式的比较运算符 / Comparison operators between linear polynomial and linear monomial

/**
 * 线性多项式小于线性单项式
 * Linear polynomial less than linear monomial
 */
infix fun LinearPolynomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): LinearInequality = this lt rhs.asPolynomial()

/**
 * 线性多项式小于等于线性单项式
 * Linear polynomial less than or equal linear monomial
 */
infix fun LinearPolynomial<Flt64>.le(rhs: LinearMonomial<Flt64>): LinearInequality = this le rhs.asPolynomial()

/**
 * 线性多项式等于线性单项式
 * Linear polynomial equal linear monomial
 */
infix fun LinearPolynomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): LinearInequality = this eq rhs.asPolynomial()

/**
 * 线性多项式不等于线性单项式
 * Linear polynomial not equal linear monomial
 */
infix fun LinearPolynomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): LinearInequality = this ne rhs.asPolynomial()

/**
 * 纯性多项式大于等于线性单项式
 * Linear polynomial greater than or equal linear monomial
 */
infix fun LinearPolynomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): LinearInequality = this ge rhs.asPolynomial()

/**
 * 线性多项式大于线性单项式
 * Linear polynomial greater than linear monomial
 */
infix fun LinearPolynomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): LinearInequality = this gt rhs.asPolynomial()

// 线性多项式与浮点数的比较运算符 / Comparison operators between linear polynomial and floating-point number

/**
 * 线性多项式小于浮点数
 * Linear polynomial less than floating-point number
 */
infix fun LinearPolynomial<Flt64>.lt(rhs: Flt64): LinearInequality = this lt rhs.asLinearPolynomial()

/**
 * 线性多项式小于等于浮点数
 * Linear polynomial less than or equal floating-point number
 */
infix fun LinearPolynomial<Flt64>.le(rhs: Flt64): LinearInequality = this le rhs.asLinearPolynomial()

/**
 * 线性多项式等于浮点数
 * Linear polynomial equal floating-point number
 */
infix fun LinearPolynomial<Flt64>.eq(rhs: Flt64): LinearInequality = this eq rhs.asLinearPolynomial()

/**
 * 线性多项式不等于浮点数
 * Linear polynomial not equal floating-point number
 */
infix fun LinearPolynomial<Flt64>.ne(rhs: Flt64): LinearInequality = this ne rhs.asLinearPolynomial()

/**
 * 线性多项式大于等于浮点数
 * Linear polynomial greater than or equal floating-point number
 */
infix fun LinearPolynomial<Flt64>.ge(rhs: Flt64): LinearInequality = this ge rhs.asLinearPolynomial()

/**
 * 线性多项式大于浮点数
 * Linear polynomial greater than floating-point number
 */
infix fun LinearPolynomial<Flt64>.gt(rhs: Flt64): LinearInequality = this gt rhs.asLinearPolynomial()

// 浮点数与线性多项式的比较运算符 / Comparison operators between floating-point number and linear polynomial

/**
 * 浮点数小于线性多项式
 * Floating-point number less than linear polynomial
 */
infix fun Flt64.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() lt rhs

/**
 * 浮点数小于等于线性多项式
 * Floating-point number less than or equal linear polynomial
 */
infix fun Flt64.le(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() le rhs

/**
 * 浮点数等于线性多项式
 * Floating-point number equal linear polynomial
 */
infix fun Flt64.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() eq rhs

/**
 * 浮点数不等于线性多项式
 * Floating-point number not equal linear polynomial
 */
infix fun Flt64.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() ne rhs

/**
 * 浮点数大于等于线性多项式
 * Floating-point number greater than or equal linear polynomial
 */
infix fun Flt64.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() ge rhs

/**
 * 浮点数大于线性多项式
 * Floating-point number greater than linear polynomial
 */
infix fun Flt64.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() gt rhs

