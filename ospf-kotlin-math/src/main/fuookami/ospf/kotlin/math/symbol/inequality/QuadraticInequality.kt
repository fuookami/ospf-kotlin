/**
 * 二次不等式
 * Quadratic Inequality
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

/**
 * 二次不等式
 * Quadratic Inequality
 *
 * 表示二次不等式，包含左侧二次多项式、右侧二次多项式和比较运算符。
 * 二次不等式在二次规划和非线性优化问题中广泛使用。
 * Represents a quadratic inequality, containing left-hand quadratic polynomial,
 * right-hand quadratic polynomial, and comparison operator.
 * Quadratic inequalities are widely used in quadratic programming and nonlinear optimization.
 *
 * @param T 系数类型（必须实现Ring）/ The coefficient type (must implement Ring)
 * @property lhs 左侧二次多项式 / Left-hand quadratic polynomial
 * @property rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @property comparison 比较运算符 / Comparison operator
 * @property name 不等式名称（可选）/ Inequality name (optional)
 * @property displayName 不等式显示名称（可选）/ Inequality display name (optional)
 */
data class QuadraticInequalityOf<T : Ring<T>>(
    val lhs: QuadraticPolynomial<T>,
    val rhs: QuadraticPolynomial<T>,
    val comparison: Comparison,
    val name: String = "",
    val displayName: String = ""
) {
    /**
     * 返回反转后的不等式（交换左右两侧并反转比较运算符）。
     * Returns the reversed inequality (swaps left and right sides and reverses the comparison operator).
     *
     * @return 反转后的二次不等式 / The reversed quadratic inequality
     */
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

private fun <T : Ring<T>> QuadraticMonomial<T>.asPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), coefficient - coefficient)
}

private fun <T : Ring<T>> LinearMonomial<T>.asPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        listOf(QuadraticMonomial.linear(coefficient, symbol)),
        coefficient - coefficient
    )
}

private fun <T : Ring<T>> T.asQuadraticPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(emptyList(), this)
}

/** 二次多项式 < 二次多项式 / quadratic polynomial < quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.LT)
/** 二次多项式 <= 二次多项式 / quadratic polynomial <= quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.LE)
/** 二次多项式 == 二次多项式 / quadratic polynomial == quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.EQ)
/** 二次多项式 != 二次多项式 / quadratic polynomial != quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.NE)
/** 二次多项式 >= 二次多项式 / quadratic polynomial >= quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.GE)
/** 二次多项式 > 二次多项式 / quadratic polynomial > quadratic polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.GT)

/** 二次单项式 < 二次单项式 / quadratic monomial < quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.lt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs.asPolynomial()
/** 二次单项式 <= 二次单项式 / quadratic monomial <= quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.le(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs.asPolynomial()
/** 二次单项式 == 二次单项式 / quadratic monomial == quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.eq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs.asPolynomial()
/** 二次单项式 != 二次单项式 / quadratic monomial != quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ne(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs.asPolynomial()
/** 二次单项式 >= 二次单项式 / quadratic monomial >= quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ge(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs.asPolynomial()
/** 二次单项式 > 二次单项式 / quadratic monomial > quadratic monomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.gt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs.asPolynomial()

/** 二次多项式 < 线性多项式 / quadratic polynomial < linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs.toQuadraticPolynomial()
/** 二次多项式 <= 线性多项式 / quadratic polynomial <= linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this le rhs.toQuadraticPolynomial()
/** 二次多项式 == 线性多项式 / quadratic polynomial == linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this eq rhs.toQuadraticPolynomial()
/** 二次多项式 != 线性多项式 / quadratic polynomial != linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs.toQuadraticPolynomial()
/** 二次多项式 >= 线性多项式 / quadratic polynomial >= linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs.toQuadraticPolynomial()
/** 二次多项式 > 线性多项式 / quadratic polynomial > linear polynomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs.toQuadraticPolynomial()

/** 线性多项式 < 二次多项式 / linear polynomial < quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() lt rhs
/** 线性多项式 <= 二次多项式 / linear polynomial <= quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() le rhs
/** 线性多项式 == 二次多项式 / linear polynomial == quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() eq rhs
/** 线性多项式 != 二次多项式 / linear polynomial != quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() ne rhs
/** 线性多项式 >= 二次多项式 / linear polynomial >= quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() ge rhs
/** 线性多项式 > 二次多项式 / linear polynomial > quadratic polynomial */
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() gt rhs

/** 二次多项式 < 二次单项式 / quadratic polynomial < quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs.asPolynomial()
/** 二次多项式 <= 二次单项式 / quadratic polynomial <= quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs.asPolynomial()
/** 二次多项式 == 二次单项式 / quadratic polynomial == quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this eq rhs.asPolynomial()
/** 二次多项式 != 二次单项式 / quadratic polynomial != quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs.asPolynomial()
/** 二次多项式 >= 二次单项式 / quadratic polynomial >= quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs.asPolynomial()
/** 二次多项式 > 二次单项式 / quadratic polynomial > quadratic monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs.asPolynomial()

/** 二次单项式 < 二次多项式 / quadratic monomial < quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs
/** 二次单项式 <= 二次多项式 / quadratic monomial <= quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs
/** 二次单项式 == 二次多项式 / quadratic monomial == quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs
/** 二次单项式 != 二次多项式 / quadratic monomial != quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs
/** 二次单项式 >= 二次多项式 / quadratic monomial >= quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs
/** 二次单项式 > 二次多项式 / quadratic monomial > quadratic polynomial */
infix fun <T : Ring<T>> QuadraticMonomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs

/** 二次多项式 < 线性单项式 / quadratic polynomial < linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this lt rhs.asPolynomial()
/** 二次多项式 <= 线性单项式 / quadratic polynomial <= linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this le rhs.asPolynomial()
/** 二次多项式 == 线性单项式 / quadratic polynomial == linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this eq rhs.asPolynomial()
/** 二次多项式 != 线性单项式 / quadratic polynomial != linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ne rhs.asPolynomial()
/** 二次多项式 >= 线性单项式 / quadratic polynomial >= linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ge rhs.asPolynomial()
/** 二次多项式 > 线性单项式 / quadratic polynomial > linear monomial */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this gt rhs.asPolynomial()

/** 线性单项式 < 二次多项式 / linear monomial < quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs
/** 线性单项式 <= 二次多项式 / linear monomial <= quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs
/** 线性单项式 == 二次多项式 / linear monomial == quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs
/** 线性单项式 != 二次多项式 / linear monomial != quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs
/** 线性单项式 >= 二次多项式 / linear monomial >= quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs
/** 线性单项式 > 二次多项式 / linear monomial > quadratic polynomial */
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs

/** 二次多项式 < 标量 / quadratic polynomial < scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: T): QuadraticInequalityOf<T> = this lt rhs.asQuadraticPolynomial()
/** 二次多项式 <= 标量 / quadratic polynomial <= scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: T): QuadraticInequalityOf<T> = this le rhs.asQuadraticPolynomial()
/** 二次多项式 == 标量 / quadratic polynomial == scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: T): QuadraticInequalityOf<T> = this eq rhs.asQuadraticPolynomial()
/** 二次多项式 != 标量 / quadratic polynomial != scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: T): QuadraticInequalityOf<T> = this ne rhs.asQuadraticPolynomial()
/** 二次多项式 >= 标量 / quadratic polynomial >= scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: T): QuadraticInequalityOf<T> = this ge rhs.asQuadraticPolynomial()
/** 二次多项式 > 标量 / quadratic polynomial > scalar */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: T): QuadraticInequalityOf<T> = this gt rhs.asQuadraticPolynomial()

/** 标量 < 二次多项式 / scalar < quadratic polynomial */
infix fun <T : Ring<T>> T.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() lt rhs
/** 标量 <= 二次多项式 / scalar <= quadratic polynomial */
infix fun <T : Ring<T>> T.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() le rhs
/** 标量 == 二次多项式 / scalar == quadratic polynomial */
infix fun <T : Ring<T>> T.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() eq rhs
/** 标量 != 二次多项式 / scalar != quadratic polynomial */
infix fun <T : Ring<T>> T.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() ne rhs
/** 标量 >= 二次多项式 / scalar >= quadratic polynomial */
infix fun <T : Ring<T>> T.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() ge rhs
/** 标量 > 二次多项式 / scalar > quadratic polynomial */
infix fun <T : Ring<T>> T.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() gt rhs

// ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========

/** 二次多项式 <= 二次多项式（别名）/ quadratic polynomial <= quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次多项式 >= 二次多项式（别名）/ quadratic polynomial >= quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次多项式 != 二次多项式（别名）/ quadratic polynomial != quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次多项式 < 二次多项式（别名）/ quadratic polynomial < quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次多项式 > 二次多项式（别名）/ quadratic polynomial > quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次单项式 <= 二次单项式（别名）/ quadratic monomial <= quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.leq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次单项式 >= 二次单项式（别名）/ quadratic monomial >= quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.geq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次单项式 != 二次单项式（别名）/ quadratic monomial != quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.neq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次单项式 < 二次单项式（别名）/ quadratic monomial < quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ls(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次单项式 > 二次单项式（别名）/ quadratic monomial > quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.gr(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次多项式 <= 线性多项式（别名）/ quadratic polynomial <= linear polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次多项式 >= 线性多项式（别名）/ quadratic polynomial >= linear polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次多项式 != 线性多项式（别名）/ quadratic polynomial != linear polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次多项式 < 线性多项式（别名）/ quadratic polynomial < linear polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次多项式 > 线性多项式（别名）/ quadratic polynomial > linear polynomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 线性多项式 <= 二次多项式（别名）/ linear polynomial <= quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 线性多项式 >= 二次多项式（别名）/ linear polynomial >= quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 线性多项式 != 二次多项式（别名）/ linear polynomial != quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 线性多项式 < 二次多项式（别名）/ linear polynomial < quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 线性多项式 > 二次多项式（别名）/ linear polynomial > quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次多项式 <= 二次单项式（别名）/ quadratic polynomial <= quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次多项式 >= 二次单项式（别名）/ quadratic polynomial >= quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次多项式 != 二次单项式（别名）/ quadratic polynomial != quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次多项式 < 二次单项式（别名）/ quadratic polynomial < quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次多项式 > 二次单项式（别名）/ quadratic polynomial > quadratic monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次单项式 <= 二次多项式（别名）/ quadratic monomial <= quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次单项式 >= 二次多项式（别名）/ quadratic monomial >= quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次单项式 != 二次多项式（别名）/ quadratic monomial != quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次单项式 < 二次多项式（别名）/ quadratic monomial < quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次单项式 > 二次多项式（别名）/ quadratic monomial > quadratic polynomial (alias) */
infix fun <T : Ring<T>> QuadraticMonomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次多项式 <= 线性单项式（别名）/ quadratic polynomial <= linear monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 二次多项式 >= 线性单项式（别名）/ quadratic polynomial >= linear monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 二次多项式 != 线性单项式（别名）/ quadratic polynomial != linear monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 二次多项式 < 线性单项式（别名）/ quadratic polynomial < linear monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 二次多项式 > 线性单项式（别名）/ quadratic polynomial > linear monomial (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 线性单项式 <= 二次多项式（别名）/ linear monomial <= quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 线性单项式 >= 二次多项式（别名）/ linear monomial >= quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 线性单项式 != 二次多项式（别名）/ linear monomial != quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 线性单项式 < 二次多项式（别名）/ linear monomial < quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 线性单项式 > 二次多项式（别名）/ linear monomial > quadratic polynomial (alias) */
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/** 二次多项式 <= 标量（别名）/ quadratic polynomial <= scalar (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: T): QuadraticInequalityOf<T> = this le rhs
/** 二次多项式 >= 标量（别名）/ quadratic polynomial >= scalar (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: T): QuadraticInequalityOf<T> = this ge rhs
/** 二次多项式 != 标量（别名）/ quadratic polynomial != scalar (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: T): QuadraticInequalityOf<T> = this ne rhs
/** 二次多项式 < 标量（别名）/ quadratic polynomial < scalar (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: T): QuadraticInequalityOf<T> = this lt rhs
/** 二次多项式 > 标量（别名）/ quadratic polynomial > scalar (alias) */
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: T): QuadraticInequalityOf<T> = this gt rhs

/** 标量 <= 二次多项式（别名）/ scalar <= quadratic polynomial (alias) */
infix fun <T : Ring<T>> T.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
/** 标量 >= 二次多项式（别名）/ scalar >= quadratic polynomial (alias) */
infix fun <T : Ring<T>> T.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
/** 标量 != 二次多项式（别名）/ scalar != quadratic polynomial (alias) */
infix fun <T : Ring<T>> T.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
/** 标量 < 二次多项式（别名）/ scalar < quadratic polynomial (alias) */
infix fun <T : Ring<T>> T.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
/** 标量 > 二次多项式（别名）/ scalar > quadratic polynomial (alias) */
infix fun <T : Ring<T>> T.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

/**
 * 创建命名的二次多项式 < 二次多项式不等式
 * Creates a named quadratic polynomial < quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.LT, name, displayName)
/**
 * 创建命名的二次多项式 <= 二次多项式不等式
 * Creates a named quadratic polynomial <= quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.LE, name, displayName)
/**
 * 创建命名的二次多项式 == 二次多项式不等式
 * Creates a named quadratic polynomial == quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.EQ, name, displayName)
/**
 * 创建命名的二次多项式 != 二次多项式不等式
 * Creates a named quadratic polynomial != quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.NE, name, displayName)
/**
 * 创建命名的二次多项式 >= 二次多项式不等式
 * Creates a named quadratic polynomial >= quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.GE, name, displayName)
/**
 * 创建命名的二次多项式 > 二次多项式不等式
 * Creates a named quadratic polynomial > quadratic polynomial inequality
 *
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.GT, name, displayName)

/**
 * 创建命名的二次多项式 < 标量不等式
 * Creates a named quadratic polynomial < scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.LT, name, displayName)
/**
 * 创建命名的二次多项式 <= 标量不等式
 * Creates a named quadratic polynomial <= scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.LE, name, displayName)
/**
 * 创建命名的二次多项式 == 标量不等式
 * Creates a named quadratic polynomial == scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.EQ, name, displayName)
/**
 * 创建命名的二次多项式 != 标量不等式
 * Creates a named quadratic polynomial != scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.NE, name, displayName)
/**
 * 创建命名的二次多项式 >= 标量不等式
 * Creates a named quadratic polynomial >= scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.GE, name, displayName)
/**
 * 创建命名的二次多项式 > 标量不等式
 * Creates a named quadratic polynomial > scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的二次不等式 / Named quadratic inequality
 */
fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.GT, name, displayName)
