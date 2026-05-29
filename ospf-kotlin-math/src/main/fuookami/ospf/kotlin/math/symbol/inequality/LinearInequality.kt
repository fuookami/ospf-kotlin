/**
 * 线性不等式
 * Linear Inequality
 *
 * 定义线性不等式，左右两边均为线性多项式。 * 线性不等式在优化问题中广泛使用，特别是线性规划和混合整数规划。 * Defines linear inequalities, where both sides are linear polynomials.
 * Linear inequalities are widely used in optimization problems,
 * especially in linear programming and mixed-integer programming.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.*

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
 * @property comparison 比较运算符 / Comparison operator
 * @property name 不等式名称（可选）/ Inequality name (optional)
 * @property displayName 不等式显示名称（可选）/ Inequality display name (optional)
 */
data class LinearInequality<T : Ring<T>>(
    val lhs: LinearPolynomial<T>,
    val rhs: LinearPolynomial<T>,
    val comparison: Comparison,
    val name: String = "",
    val displayName: String = ""
) {
    /**
     * 返回反转后的不等式（交换左右两侧并反转比较运算符）。
     * Returns the reversed inequality (swaps left and right sides and reverses the comparison operator).
     *
     * @return 反转后的线性不等式 / The reversed linear inequality
     */
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

/** 将线性单项式转换为只含该单项式的线性多项式 / Convert a linear monomial to a linear polynomial containing only that monomial */
private fun <T : Ring<T>> LinearMonomial<T>.asPolynomial(): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), zeroOf(coefficient))
}

/** 将 Ring 值转换为常数线性多项式 / Convert a Ring value to a constant linear polynomial */
private fun <T : Ring<T>> T.asLinearPolynomial(): LinearPolynomial<T> {
    return LinearPolynomial(emptyList(), this)
}

// ========== LinearPolynomial vs LinearPolynomial ==========

/**
 * 多项式 < 多项式
 * polynomial < polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.LT)
/**
 * 多项式 <= 多项式
 * polynomial <= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.LE)
/**
 * 多项式 == 多项式
 * polynomial == polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.EQ)
/**
 * 多项式 != 多项式
 * polynomial != polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.NE)
/**
 * 多项式 >= 多项式
 * polynomial >= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.GE)
/**
 * 多项式 > 多项式
 * polynomial > polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = LinearInequality(this, rhs, Comparison.GT)

// ========== LinearMonomial vs LinearMonomial ==========

/**
 * 单项式 < 单项式
 * monomial < monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() lt rhs.asPolynomial()
/**
 * 单项式 <= 单项式
 * monomial <= monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() le rhs.asPolynomial()
/**
 * 单项式 == 单项式
 * monomial == monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() eq rhs.asPolynomial()
/**
 * 单项式 != 单项式
 * monomial != monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() ne rhs.asPolynomial()
/**
 * 单项式 >= 单项式
 * monomial >= monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() ge rhs.asPolynomial()
/**
 * 单项式 > 单项式
 * monomial > monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: LinearMonomial<T>): LinearInequality<T> = asPolynomial() gt rhs.asPolynomial()

// ========== LinearMonomial vs LinearPolynomial ==========

/**
 * 单项式 < 多项式
 * monomial < polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() lt rhs
/**
 * 单项式 <= 多项式
 * monomial <= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() le rhs
/**
 * 单项式 == 多项式
 * monomial == polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() eq rhs
/**
 * 单项式 != 多项式
 * monomial != polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() ne rhs
/**
 * 单项式 >= 多项式
 * monomial >= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() ge rhs
/**
 * 单项式 > 多项式
 * monomial > polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = asPolynomial() gt rhs

// ========== LinearPolynomial vs LinearMonomial ==========

/**
 * 多项式 < 单项式
 * polynomial < monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs.asPolynomial()
/**
 * 多项式 <= 单项式
 * polynomial <= monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs.asPolynomial()
/**
 * 多项式 == 单项式
 * polynomial == monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearMonomial<T>): LinearInequality<T> = this eq rhs.asPolynomial()
/**
 * 多项式 != 单项式
 * polynomial != monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs.asPolynomial()
/**
 * 多项式 >= 单项式
 * polynomial >= monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs.asPolynomial()
/**
 * 多项式 > 单项式
 * polynomial > monomial
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs.asPolynomial()

// ========== LinearPolynomial vs scalar ==========

/**
 * 多项式 < 标量
 * polynomial < scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: T): LinearInequality<T> = this lt rhs.asLinearPolynomial()
/**
 * 多项式 <= 标量
 * polynomial <= scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: T): LinearInequality<T> = this le rhs.asLinearPolynomial()
/**
 * 多项式 == 标量
 * polynomial == scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: T): LinearInequality<T> = this eq rhs.asLinearPolynomial()
/**
 * 多项式 != 标量
 * polynomial != scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: T): LinearInequality<T> = this ne rhs.asLinearPolynomial()
/**
 * 多项式 >= 标量
 * polynomial >= scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: T): LinearInequality<T> = this ge rhs.asLinearPolynomial()
/**
 * 多项式 > 标量
 * polynomial > scalar
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: T): LinearInequality<T> = this gt rhs.asLinearPolynomial()

// ========== Scalar vs LinearPolynomial ==========

/**
 * 标量 < 多项式
 * scalar < polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.lt(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() lt rhs
/**
 * 标量 <= 多项式
 * scalar <= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.le(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() le rhs
/**
 * 标量 == 多项式
 * scalar == polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.eq(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() eq rhs
/**
 * 标量 != 多项式
 * scalar != polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.ne(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() ne rhs
/**
 * 标量 >= 多项式
 * scalar >= polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.ge(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() ge rhs
/**
 * 标量 > 多项式
 * scalar > polynomial
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.gt(rhs: LinearPolynomial<T>): LinearInequality<T> = asLinearPolynomial() gt rhs

// ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========

/**
 * 多项式 <= 多项式（别名）
 * polynomial <= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
/**
 * 多项式 >= 多项式（别名）
 * polynomial >= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
/**
 * 多项式 != 多项式（别名）
 * polynomial != polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
/**
 * 多项式 < 多项式（别名）
 * polynomial < polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
/**
 * 多项式 > 多项式（别名）
 * polynomial > polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

/**
 * 单项式 <= 单项式（别名）
 * monomial <= monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs
/**
 * 单项式 >= 单项式（别名）
 * monomial >= monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs
/**
 * 单项式 != 单项式（别名）
 * monomial != monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs
/**
 * 单项式 < 单项式（别名）
 * monomial < monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs
/**
 * 单项式 > 单项式（别名）
 * monomial > monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs

/**
 * 单项式 <= 多项式（别名）
 * monomial <= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
/**
 * 单项式 >= 多项式（别名）
 * monomial >= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
/**
 * 单项式 != 多项式（别名）
 * monomial != polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
/**
 * 单项式 < 多项式（别名）
 * monomial < polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
/**
 * 单项式 > 多项式（别名）
 * monomial > polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

/**
 * 多项式 <= 单项式（别名）
 * polynomial <= monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: LinearMonomial<T>): LinearInequality<T> = this le rhs
/**
 * 多项式 >= 单项式（别名）
 * polynomial >= monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: LinearMonomial<T>): LinearInequality<T> = this ge rhs
/**
 * 多项式 != 单项式（别名）
 * polynomial != monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: LinearMonomial<T>): LinearInequality<T> = this ne rhs
/**
 * 多项式 < 单项式（别名）
 * polynomial < monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: LinearMonomial<T>): LinearInequality<T> = this lt rhs
/**
 * 多项式 > 单项式（别名）
 * polynomial > monomial (alias)
 *
 * @param rhs 右侧单项式 / Right-hand monomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: LinearMonomial<T>): LinearInequality<T> = this gt rhs

/**
 * 多项式 <= 标量（别名）
 * polynomial <= scalar (alias)
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: T): LinearInequality<T> = this le rhs
/**
 * 多项式 >= 标量（别名）
 * polynomial >= scalar (alias)
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: T): LinearInequality<T> = this ge rhs
/**
 * 多项式 != 标量（别名）
 * polynomial != scalar (alias)
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: T): LinearInequality<T> = this ne rhs
/**
 * 多项式 < 标量（别名）
 * polynomial < scalar (alias)
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: T): LinearInequality<T> = this lt rhs
/**
 * 多项式 > 标量（别名）
 * polynomial > scalar (alias)
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: T): LinearInequality<T> = this gt rhs

/**
 * 标量 <= 多项式（别名）
 * scalar <= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.leq(rhs: LinearPolynomial<T>): LinearInequality<T> = this le rhs
/**
 * 标量 >= 多项式（别名）
 * scalar >= polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.geq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ge rhs
/**
 * 标量 != 多项式（别名）
 * scalar != polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.neq(rhs: LinearPolynomial<T>): LinearInequality<T> = this ne rhs
/**
 * 标量 < 多项式（别名）
 * scalar < polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.ls(rhs: LinearPolynomial<T>): LinearInequality<T> = this lt rhs
/**
 * 标量 > 多项式（别名）
 * scalar > polynomial (alias)
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @return 线性不等式 / Linear inequality
 */
infix fun <T : Ring<T>> T.gr(rhs: LinearPolynomial<T>): LinearInequality<T> = this gt rhs

// ========== Named inequality constructors ==========

/**
 * 创建命名的多项式 < 多项式不等式
 * Creates a named polynomial < polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.LT, name, displayName)
/**
 * 创建命名的多项式 <= 多项式不等式
 * Creates a named polynomial <= polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.LE, name, displayName)
/**
 * 创建命名的多项式 == 多项式不等式
 * Creates a named polynomial == polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.EQ, name, displayName)
/**
 * 创建命名的多项式 != 多项式不等式
 * Creates a named polynomial != polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.NE, name, displayName)
/**
 * 创建命名的多项式 >= 多项式不等式
 * Creates a named polynomial >= polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.GE, name, displayName)
/**
 * 创建命名的多项式 > 多项式不等式
 * Creates a named polynomial > polynomial inequality
 *
 * @param rhs 右侧多项式 / Right-hand polynomial
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: LinearPolynomial<T>, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs, Comparison.GT, name, displayName)

/**
 * 创建命名的多项式 < 标量不等式
 * Creates a named polynomial < scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.LT, name, displayName)
/**
 * 创建命名的多项式 <= 标量不等式
 * Creates a named polynomial <= scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.LE, name, displayName)
/**
 * 创建命名的多项式 == 标量不等式
 * Creates a named polynomial == scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.EQ, name, displayName)
/**
 * 创建命名的多项式 != 标量不等式
 * Creates a named polynomial != scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.NE, name, displayName)
/**
 * 创建命名的多项式 >= 标量不等式
 * Creates a named polynomial >= scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.GE, name, displayName)
/**
 * 创建命名的多项式 > 标量不等式
 * Creates a named polynomial > scalar inequality
 *
 * @param rhs 右侧标量 / Right-hand scalar
 * @param name 不等式名称 / Inequality name
 * @param displayName 显示名称 / Display name
 * @return 命名的线性不等式 / Named linear inequality
 */
fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: T, name: String, displayName: String = ""): LinearInequality<T> =
    LinearInequality(this, rhs.asLinearPolynomial(), Comparison.GT, name, displayName)
