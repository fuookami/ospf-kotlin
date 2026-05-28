/**
 * 积分运算
 * Integration Operations
 *
 * 提供多项式积分的核心实现。
 * 支持一元线性、二次和规范多项式的积分计算，基二FloatingNumber 类型约束。
 * Provides core implementation for polynomial integration.
 * Supports univariate integration for linear, quadratic, and canonical polynomials,
 * based on FloatingNumber type constraints.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*


// ============================================================================
// Integration Operations (FloatingNumber-based, antiderivative calculus)
// ============================================================================

/**
 * 一元线性单项式积分
 * Integrate a univariate linear monomial
 *
 * ∌a * x) dx = a * x² / 2
 * ∌a) dx = a * x (彌symbol 丌null 或多项式中的常数題
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @return 积分结果（二次多项式，/ Integral result (quadratic polynomial)
 */
fun <T> LinearMonomial<T>.integrateLinear(
    symbol: Symbol,
    integrationConstant: T
): QuadraticPolynomial<T> where T : FloatingNumber<T> {
    // ∌a * x) dx = a * x² / 2 + C
    val two = coefficient.constants.two
    val halfCoefficient = coefficient / two
    return QuadraticPolynomial(
        monomials = listOf(
            QuadraticMonomial(
                coefficient = halfCoefficient,
                symbol1 = symbol,
                symbol2 = symbol
            )
        ),
        constant = integrationConstant
    )
}

/**
 * 一元线性多项式积分
 * Integrate a univariate linear polynomial
 *
 * ∌a₁x₌+ a₂x₌+ ... + b) dx = a₁x₁¹2 + a₂x₂x + ... + bx + C
 * 注意：多元多项式的积分需要指定积分变量，其他变量被视为常敌
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @param combineTerms 是否合并同类題/ Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @return 积分结果（二次多项式，/ Integral result (quadratic polynomial)
 */
fun <T> LinearPolynomial<T>.integrateLinear(
    symbol: Symbol,
    integrationConstant: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == it.constants.zero }
): QuadraticPolynomial<T> where T : FloatingNumber<T> {
    val integralMonomials = mutableListOf<QuadraticMonomial<T>>()
    val zero = integrationConstant.constants.zero
    val two = integrationConstant.constants.two

    for (monomial in monomials) {
        if (monomial.symbol == symbol) {
            // ∌a * x) dx = a * x² / 2
            val halfCoefficient = monomial.coefficient / two
            integralMonomials.add(
                QuadraticMonomial(
                    coefficient = halfCoefficient,
                    symbol1 = symbol,
                    symbol2 = symbol
                )
            )
        } else {
            // ∌a * y) dx = a * y * x (y 被视为常敌
            integralMonomials.add(
                QuadraticMonomial(
                    coefficient = monomial.coefficient,
                    symbol1 = monomial.symbol,
                    symbol2 = symbol
                )
            )
        }
    }

    // ∌constant) dx = constant * x
    if (!isZero(constant)) {
        integralMonomials.add(
            QuadraticMonomial(
                coefficient = constant,
                symbol1 = symbol,
                symbol2 = null
            )
        )
    }

    val integral = QuadraticPolynomial(
        monomials = integralMonomials,
        constant = integrationConstant
    )

    return if (combineTerms) {
        integral.combineQuadraticTerms(zero, isZero)
    } else {
        integral
    }
}

/**
 * 一元二次单项式积分
 * Integrate a univariate quadratic monomial
 *
 * ∌a * x₌* x₌ dx 结果取决二x 是否等于 x₌戌x₂：
 * - 如果 x == x₌== x₌ ∌a * x²) dx = a * x³ / 3 (返回 CanonicalPolynomial)
 * - 如果 x == x₌≌x₌ ∌a * x₌* x₌ dx = a * x₌* x² / 2
 * - 如果 x ≌x₌丌x ≌x₌ ∌a * x₌* x₌ dx = a * x₌* x₌* x
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @return 积分结果 / Integral result
 */
fun <T> QuadraticMonomial<T>.integrateQuadratic(
    symbol: Symbol,
    integrationConstant: T
): CanonicalPolynomial<T> where T : FloatingNumber<T> {
    val s1 = symbol1
    val s2 = symbol2
    val two = coefficient.constants.two
    val three = coefficient.constants.three

    // 情况 1: x₌== x₌== x (a * x²)
    if (s1 == symbol && s2 == symbol) {
        // ∌a * x²) dx = a * x³ / 3
        val thirdCoefficient = coefficient / three
        return CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(
                    coefficient = thirdCoefficient,
                    powers = mapOf(symbol to Int32(3))
                )
            ),
            constant = integrationConstant
        )
    }

    // 情况 2: x₌== x, x₌== null (a * x)
    if (s1 == symbol && s2 == null) {
        // ∌a * x) dx = a * x² / 2
        val halfCoefficient = coefficient / two
        return CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(
                    coefficient = halfCoefficient,
                    powers = mapOf(symbol to Int32(2))
                )
            ),
            constant = integrationConstant
        )
    }

    // 情况 3: x₌== x, x₌≌x (a * x * y)
    if (s1 == symbol && s2 != null && s2 != symbol) {
        // ∌a * x * y) dx = a * y * x² / 2
        val halfCoefficient = coefficient / two
        return CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(
                    coefficient = halfCoefficient,
                    powers = mapOf(symbol to Int32(2), s2 to Int32(1))
                )
            ),
            constant = integrationConstant
        )
    }

    // 情况 4: x₌== x, x₌≌x (a * y * x)
    if (s2 == symbol && s1 != symbol) {
        // ∌a * y * x) dx = a * y * x² / 2
        val halfCoefficient = coefficient / two
        return CanonicalPolynomial(
            monomials = listOf(
                CanonicalMonomial(
                    coefficient = halfCoefficient,
                    powers = mapOf(s1 to Int32(1), symbol to Int32(2))
                )
            ),
            constant = integrationConstant
        )
    }

    // 情况 5: x₌≌x, x₌≌x (a * y * z，都被视为常敌
    // ∌a * y * z) dx = a * y * z * x
    val powers = mutableMapOf<Symbol, Int32>()
    powers[s1] = Int32(1)
    if (s2 != null) powers[s2] = Int32(1)
    powers[symbol] = Int32(1)

    return CanonicalPolynomial(
        monomials = listOf(
            CanonicalMonomial(
                coefficient = coefficient,
                powers = powers
            )
        ),
        constant = integrationConstant
    )
}

/**
 * 一元二次多项式积分
 * Integrate a univariate quadratic polynomial
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @param combineTerms 是否合并同类題/ Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param symbolComparator 符号比较噌/ Symbol comparator
 * @return 积分结果（规范多项式，/ Integral result (canonical polynomial)
 */
fun <T> QuadraticPolynomial<T>.integrateQuadratic(
    symbol: Symbol,
    integrationConstant: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == it.constants.zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : FloatingNumber<T> {
    val integralMonomials = mutableListOf<CanonicalMonomial<T>>()
    val zero = integrationConstant.constants.zero

    for (monomial in monomials) {
        val monomialIntegral = monomial.integrateQuadratic(symbol, zero)
        integralMonomials.addAll(monomialIntegral.monomials)
    }

    // ∌constant) dx = constant * x
    if (!isZero(constant)) {
        integralMonomials.add(
            CanonicalMonomial(
                coefficient = constant,
                powers = mapOf(symbol to Int32(1))
            )
        )
    }

    val integral = CanonicalPolynomial(
        monomials = integralMonomials,
        constant = integrationConstant
    )

    return if (combineTerms) {
        integral.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        integral
    }
}

/**
 * 一元规范单项式积分
 * Integrate a univariate canonical monomial
 *
 * ∌a * x^n) dx = a * x^(n+1) / (n+1)
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @return 积分结果 / Integral result
 */
fun <T> CanonicalMonomial<T>.integrateCanonical(
    symbol: Symbol,
    integrationConstant: T
): CanonicalPolynomial<T> where T : FloatingNumber<T> {
    val currentExponent = powers[symbol]?.toInt() ?: 0

    // 构建新的 powers map
    val newPowers = LinkedHashMap<Symbol, Int32>(powers)
    val newExponent = currentExponent + 1

    // 除以新指敌(n+1)
    // 通过累加 one 来构造整敌
    val one = coefficient.constants.one
    var divisor = one
    repeat(newExponent - 1) {
        divisor += one
    }
    val scaledCoefficient = coefficient / divisor

    newPowers[symbol] = Int32(newExponent)

    return CanonicalPolynomial(
        monomials = listOf(
            CanonicalMonomial(
                coefficient = scaledCoefficient,
                powers = newPowers
            )
        ),
        constant = integrationConstant
    )
}

/**
 * 一元规范多项式积分
 * Integrate a univariate canonical polynomial
 *
 * @param symbol 积分变量 / Integration variable
 * @param integrationConstant 积分常数 C / Integration constant C
 * @param combineTerms 是否合并同类題/ Whether to combine like terms
 * @param isZero 判断零的函数 / Function to check if value is zero
 * @param symbolComparator 符号比较噌/ Symbol comparator
 * @return 积分结果 / Integral result
 */
fun <T> CanonicalPolynomial<T>.integrateCanonical(
    symbol: Symbol,
    integrationConstant: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == it.constants.zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : FloatingNumber<T> {
    val integralMonomials = mutableListOf<CanonicalMonomial<T>>()
    val zero = integrationConstant.constants.zero

    for (monomial in monomials) {
        val monomialIntegral = monomial.integrateCanonical(symbol, zero)
        integralMonomials.addAll(monomialIntegral.monomials)
    }

    // ∌constant) dx = constant * x
    if (!isZero(constant)) {
        integralMonomials.add(
            CanonicalMonomial(
                coefficient = constant,
                powers = mapOf(symbol to Int32(1))
            )
        )
    }

    val integral = CanonicalPolynomial(
        monomials = integralMonomials,
        constant = integrationConstant
    )

    return if (combineTerms) {
        integral.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
    } else {
        integral
    }
}
