package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.operator.Abs

data class LinearMonomial<T>(
    val coefficient: T,
    val symbol: Symbol
) {
    val category: Category
        get() = Linear
}

// ============================================================================
// Unary minus / 取负运算
// ============================================================================

operator fun <T : NumberField<T>> LinearMonomial<T>.unaryMinus(): LinearMonomial<T> {
    return LinearMonomial(-coefficient, symbol)
}

// ============================================================================
// Scalar multiplication / 标量乘法
// ============================================================================

operator fun <T : NumberField<T>> LinearMonomial<T>.times(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient * rhs, symbol)
}

// ============================================================================
// Scalar division / 标量除法
// ============================================================================

operator fun <T : NumberField<T>> LinearMonomial<T>.div(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient / rhs, symbol)
}

// ============================================================================
// LinearMonomial multiplication / 单项式乘法
// LinearMonomial * LinearMonomial -> QuadraticMonomial
// ============================================================================

operator fun <T : NumberField<T>> LinearMonomial<T>.times(rhs: LinearMonomial<T>): QuadraticMonomial<T> {
    return QuadraticMonomial(coefficient * rhs.coefficient, symbol, rhs.symbol)
}

// ============================================================================
// Addition / 加法运算
// ============================================================================

// LinearMonomial + LinearMonomial -> LinearPolynomial
operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this, rhs), T.constants.zero)
}

// LinearMonomial + LinearPolynomial -> LinearPolynomial
operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

// LinearPolynomial + LinearMonomial -> LinearPolynomial
operator fun <T : NumberField<T>> LinearPolynomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs, constant)
}

// ============================================================================
// Abs implementation / 绝对值实现
// ============================================================================

fun <T : NumberField<T>> LinearMonomial<T>.abs(): LinearMonomial<T> {
    return LinearMonomial(coefficient.abs(), symbol)
}

// ============================================================================
// Reciprocal implementation / 倒数实现
// LinearMonomial reciprocal -> CanonicalMonomial with exponent -1
// ============================================================================

fun <T : NumberField<T>> LinearMonomial<T>.reciprocal(): CanonicalMonomial<T, Int32> {
    val powers = mapOf(symbol to Int32(-1))
    return CanonicalMonomial(coefficient.reciprocal(), powers)
}

// ============================================================================
// Scalar * LinearMonomial (reverse multiplication)
// 标量 * LinearMonomial（反向乘法）
// ============================================================================

operator fun <T : NumberField<T>> T.times(rhs: LinearMonomial<T>): LinearMonomial<T> {
    return LinearMonomial(this * rhs.coefficient, rhs.symbol)
}
