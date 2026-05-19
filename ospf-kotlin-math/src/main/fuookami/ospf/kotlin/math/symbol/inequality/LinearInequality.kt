/**
 * 线性不等式
 * Linear Inequality
 *
 * 定义线性不等式，左右两边均为线性多项式。 * 线性不等式在优化问题中广泛使用，特别是线性规划和混合整数规划。 * Defines linear inequalities, where both sides are linear polynomials.
 * Linear inequalities are widely used in optimization problems,
 * especially in linear programming and mixed-integer programming.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.zeroOf

/**
 * 线性不等式
 * Linear Inequality
 *
 * 表示线性不等式，包含左侧线性多项式、右侧线性多项式和比较运算符。 * 线性不等式是优化问题中最基本的约束形式，广泛用于线性规划和混合整数规划。 * Represents a linear inequality, containing left-hand linear polynomial,
 * right-hand linear polynomial, and comparison operator.
 * Linear inequalities are the most basic constraint form in optimization problems,
 * widely used in linear programming and mixed-integer programming.
 *
 * @property lhs 左侧线性多项式 / Left-hand linear polynomial
 * @property rhs 右侧线性多项式 / Right-hand linear polynomial
 * @property comparison 比较运算笌/ Comparison operator
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

// ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========

infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs

infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs

infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: T): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: T): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: T): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: T): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: T): LinearInequality<T> = this gt rhs

infix fun <T : Ring<T>> T.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
infix fun <T : Ring<T>> T.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
infix fun <T : Ring<T>> T.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
infix fun <T : Ring<T>> T.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
infix fun <T : Ring<T>> T.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

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