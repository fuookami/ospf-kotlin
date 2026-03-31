package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.operator.Abs

/**
 * 标准单项式 / Canonical monomial
 *
 * 形式：`c * S1^n1 * S2^n2 * ...`
 * Form: `c * S1^n1 * S2^n2 * ...`
 *
 * @param T 系数类型，通常为 Flt64、BigDecimal 等
 * @param E 指数类型，默认为 Int32，支持负指数
 */
data class CanonicalMonomial<T, E : Number>(
    val coefficient: T,
    val powers: Map<Symbol, E> = emptyMap()
) {
    companion object {
        /**
         * 创建常数项（只有系数，没有符号）
         * Create a constant term (coefficient only, no symbols)
         */
        fun <T, E : Number> constant(coefficient: T): CanonicalMonomial<T, E> {
            return CanonicalMonomial(coefficient, emptyMap())
        }

        /**
         * 创建线性单项式（单个符号，指数为1）
         * Create a linear monomial (single symbol with exponent 1)
         */
        fun <T, E : Number> linear(coefficient: T, symbol: Symbol, one: E): CanonicalMonomial<T, E> {
            return CanonicalMonomial(coefficient, mapOf(symbol to one))
        }
    }

    /**
     * 是否为常数项
     * Check if this is a constant term
     */
    val isConstant: Boolean
        get() = powers.isEmpty()

    /**
     * 获取符号数量
     * Get the number of symbols
     */
    val symbolCount: Int
        get() = powers.size

    /**
     * 获取符号的指数
     * Get the exponent of a symbol
     */
    fun exponent(symbol: Symbol): E? = powers[symbol]

    /**
     * 获取总次数（所有指数之和）
     * Get the total degree (sum of all exponents)
     * Note: For negative exponents, this may not be meaningful
     */
    val degree: Int
        get() = powers.values.sumOf { it.toInt() }

    val category: Category
        get() = when {
            powers.isEmpty() -> Linear  // 常数项视为线性
            powers.size == 1 && powers.values.first().toInt() == 1 -> Linear
            powers.size == 1 && powers.values.first().toInt() == 2 -> Quadratic
            powers.size == 2 && powers.values.all { it.toInt() == 1 } -> Quadratic
            else -> Nonlinear
        }
}

// ============================================================================
// Unary minus / 取负运算
// ============================================================================

operator fun <T : NumberField<T>, E : Number> CanonicalMonomial<T, E>.unaryMinus(): CanonicalMonomial<T, E> {
    return CanonicalMonomial(-coefficient, powers)
}

// ============================================================================
// Scalar multiplication / 标量乘法
// ============================================================================

operator fun <T : NumberField<T>, E : Number> CanonicalMonomial<T, E>.times(rhs: T): CanonicalMonomial<T, E> {
    return CanonicalMonomial(coefficient * rhs, powers)
}

// ============================================================================
// Scalar division / 标量除法
// ============================================================================

operator fun <T : NumberField<T>, E : Number> CanonicalMonomial<T, E>.div(rhs: T): CanonicalMonomial<T, E> {
    return CanonicalMonomial(coefficient / rhs, powers)
}

// ============================================================================
// Abs implementation / 绝对值实现
// ============================================================================

fun <T : NumberField<T>, E : Number> CanonicalMonomial<T, E>.abs(): CanonicalMonomial<T, E> {
    return CanonicalMonomial(coefficient.abs(), powers)
}

// ============================================================================
// Reciprocal implementation / 倒数实现
// CanonicalMonomial reciprocal -> CanonicalMonomial with negated exponents
// ============================================================================

fun <T : NumberField<T>, E : Number> CanonicalMonomial<T, E>.reciprocal(negate: (E) -> E): CanonicalMonomial<T, E> {
    val newPowers = powers.mapValues { negate(it.value) }
    return CanonicalMonomial(coefficient.reciprocal(), newPowers)
}

// 为 Int32 类型提供便捷的 reciprocal 函数
fun <T : NumberField<T>> CanonicalMonomial<T, Int32>.reciprocal(): CanonicalMonomial<T, Int32> {
    return reciprocal { -it }
}

// 为 Int64 类型提供便捷的 reciprocal 函数
fun <T : NumberField<T>> CanonicalMonomial<T, Int64>.reciprocal(): CanonicalMonomial<T, Int64> {
    return reciprocal { -it }
}

// ============================================================================
// Scalar * CanonicalMonomial (reverse multiplication)
// 标量 * CanonicalMonomial（反向乘法）
// ============================================================================

operator fun <T : NumberField<T>, E : Number> T.times(rhs: CanonicalMonomial<T, E>): CanonicalMonomial<T, E> {
    return CanonicalMonomial(this * rhs.coefficient, rhs.powers)
}