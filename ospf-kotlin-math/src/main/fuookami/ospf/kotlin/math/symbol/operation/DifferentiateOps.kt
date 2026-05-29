/**
 * 微分运算
 * Differentiation Operations
 *
 * 提供多项式微分的核心实现。
 * 支持线性、二次和规范多项式的导数和梯度计算，基于 Ring 类型约束。
 * Provides core implementation for polynomial differentiation.
 * Supports derivative and gradient computation for linear, quadratic,
 * and canonical polynomials, based on Ring type constraints.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.math.abs
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

// ============================================================================
// Differentiation Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * 按带符号整数缩放值，支持负倍数
 * Scale a value by a signed integer, supporting negative amounts
 *
 * @param value 要缩放的值 / Value to scale
 * @param amount 缩放倍数（可为负） / Scale amount (may be negative)
 * @param zero 类型零值 / Zero value for the type
 * @return 缩放后的值 / Scaled result
 */
private fun <T> scaleByIntWithSign(
    value: T,
    amount: Int,
    zero: T
): T where T : Ring<T> {
    if (amount == 0) {
        return zero
    }
    var result = zero
    repeat(abs(amount)) {
        result += value
    }
    return if (amount < 0) {
        -result
    } else {
        result
    }
}

/**
 * 线性单项式对指定符号求导
 * Derivative of a linear monomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @return 导数值 / The derivative value
 */
fun <T> LinearMonomial<T>.derivativeLinear(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    return if (this.symbol == symbol) {
        coefficient
    } else {
        zero
    }
}

/**
 * 线性多项式对指定符号求导
 * Derivative of a linear polynomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @return 导数值 / The derivative value
 */
fun <T> LinearPolynomial<T>.derivativeLinear(
    symbol: Symbol,
    zero: T
): T where T : Ring<T> {
    var derivative = zero
    for (monomial in monomials) {
        derivative += monomial.derivativeLinear(symbol, zero)
    }
    return derivative
}

/**
 * 线性多项式的梯度
 * Gradient of a linear polynomial.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @return 各符号偏导数组成的列表 / List of partial derivatives for each symbol
 */
fun <T> LinearPolynomial<T>.gradientLinear(
    order: List<Symbol>,
    zero: T
): List<T> where T : Ring<T> {
    return order.map { derivativeLinear(it, zero) }
}

/**
 * 二次单项式对指定符号求导
 * Derivative of a quadratic monomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 导数（线性多项式） / The derivative as a linear polynomial
 */
fun <T> QuadraticMonomial<T>.derivativeQuadratic(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    if (symbol2 == null) {
        return if (symbol1 == symbol) {
            LinearPolynomial(constant = coefficient)
        } else {
            LinearPolynomial(constant = zero)
        }
    }

    val derivativeMonomials = ArrayList<LinearMonomial<T>>()
    if (symbol1 == symbol) {
        derivativeMonomials.add(
            LinearMonomial(
                coefficient = coefficient,
                symbol = symbol2
            )
        )
    }
    if (symbol2 == symbol) {
        derivativeMonomials.add(
            LinearMonomial(
                coefficient = coefficient,
                symbol = symbol1
            )
        )
    }
    val derivative = LinearPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineLinearTerms(zero, isZero)
    } else {
        derivative
    }
}

/**
 * 二次多项式对指定符号求导
 * Derivative of a quadratic polynomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 导数（线性多项式） / The derivative as a linear polynomial
 */
fun <T> QuadraticPolynomial<T>.derivativeQuadratic(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<LinearMonomial<T>>()
    var derivativeConstant = zero
    for (monomial in monomials) {
        val monomialDerivative = monomial.derivativeQuadratic(
            symbol = symbol,
            zero = zero,
            combineTerms = false,
            isZero = isZero
        )
        derivativeMonomials.addAll(monomialDerivative.monomials)
        derivativeConstant += monomialDerivative.constant
    }
    val derivative = LinearPolynomial(
        monomials = derivativeMonomials,
        constant = derivativeConstant
    )
    return if (combineTerms) {
        derivative.combineLinearTerms(zero, isZero)
    } else {
        derivative
    }
}

/**
 * 二次多项式的梯度
 * Gradient of a quadratic polynomial.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 各符号偏导数组成的列表 / List of partial derivatives for each symbol
 */
fun <T> QuadraticPolynomial<T>.gradientQuadratic(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): List<LinearPolynomial<T>> where T : Ring<T> {
    return order.map { derivativeQuadratic(it, zero, combineTerms, isZero) }
}

/**
 * 规范单项式对指定符号求导
 * Derivative of a canonical monomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 导数（规范多项式） / The derivative as a canonical polynomial
 */
fun <T> CanonicalMonomial<T>.derivativeCanonical(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val exponent = powers[symbol]?.toInt() ?: 0
    if (exponent == 0) {
        return CanonicalPolynomial(constant = zero)
    }

    val remainedPowers = LinkedHashMap(powers)
    if (exponent == 1) {
        remainedPowers.remove(symbol)
    } else {
        remainedPowers[symbol] = Int32(exponent - 1)
    }

    val scaledCoefficient = scaleByIntWithSign(coefficient, exponent, zero)
    val derivative = CanonicalPolynomial(
        monomials = listOf(
            CanonicalMonomial(
                coefficient = scaledCoefficient,
                powers = remainedPowers
            )
        ),
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

/**
 * 规范多项式对指定符号求导
 * Derivative of a canonical polynomial.
 *
 * @param symbol 求导变量 / The variable to differentiate with respect to
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 导数（规范多项式） / The derivative as a canonical polynomial
 */
fun <T> CanonicalPolynomial<T>.derivativeCanonical(
    symbol: Symbol,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val derivativeMonomials = ArrayList<CanonicalMonomial<T>>()
    for (monomial in monomials) {
        derivativeMonomials.addAll(
            monomial.derivativeCanonical(
                symbol = symbol,
                zero = zero,
                combineTerms = false,
                isZero = isZero,
                symbolComparator = symbolComparator
            ).monomials
        )
    }
    val derivative = CanonicalPolynomial(
        monomials = derivativeMonomials,
        constant = zero
    )
    return if (combineTerms) {
        derivative.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        derivative
    }
}

/**
 * 规范多项式的梯度
 * Gradient of a canonical polynomial.
 *
 * @param order 符号顺序列表 / Ordered list of symbols
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 各符号偏导数组成的列表 / List of partial derivatives for each symbol
 */
fun <T> CanonicalPolynomial<T>.gradientCanonical(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalPolynomial<T>> where T : Ring<T> {
    return order.map {
        derivativeCanonical(
            symbol = it,
            zero = zero,
            combineTerms = combineTerms,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
    }
}
