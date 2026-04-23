/**
 * 规范单项式
 * Canonical Monomial
 *
 * 定义规范单项式的数据结构和运算。规范单项式形如 c*x₁^n₁*x₂^n₂*...，
 * 其中 c 为系数，xᵢ 为符号变量，nᵢ 为对应的幂次。
 * 是构建规范多项式的基本单元，支持任意次数的多项式。
 * Defines data structures and operations for canonical monomials.
 * A canonical monomial has the form c*x₁^n₁*x₂^n₂*..., where c is the coefficient,
 * xᵢ are symbol variables, and nᵢ are the corresponding powers.
 * It is the basic building block for canonical polynomials,
 * supporting polynomials of any degree.
 */
package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.ToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

/**
 * 规范单项式
 * Canonical Monomial
 *
 * 表示规范单项式，形如 c*x₁^n₁*x₂^n₂*...，其中 c 为系数，xᵢ 为符号变量，nᵢ 为对应的幂次。
 * 规范单项式是构建规范多项式的基本单元，支持任意次数的多项式表达式。
 * Represents a canonical monomial of the form c*x₁^n₁*x₂^n₂*..., where c is the coefficient,
 * xᵢ are symbol variables, and nᵢ are the corresponding powers.
 * Canonical monomials are the basic building blocks for canonical polynomials,
 * supporting polynomial expressions of any degree.
 *
 * @property coefficient 系数 / The coefficient
 * @property powers 符号到幂次的映射 / Mapping from symbols to their powers
 */
data class CanonicalMonomial<T : Ring<T>>(
    val coefficient: T,
    val powers: Map<Symbol, Int32> = emptyMap()
) : ToCanonicalPolynomial<T> {
    /**
     * 使用符号列表创建规范单项式
     * Creates a canonical monomial using a list of symbols
     *
     * 从符号列表创建规范单项式，每个符号的幂次为列表中出现的次数。
     * Creates a canonical monomial from a list of symbols, where each symbol's power
     * is its count in the list.
     *
     * @param coefficient 系数 / The coefficient
     * @param factors 符号列表 / List of symbols
     */
    constructor(
        coefficient: T,
        factors: List<Symbol>
    ) : this(
        coefficient = coefficient,
        powers = factors.groupingBy { it }.eachCount().mapValues { Int32(it.value) }
    )

    /**
     * 符号因子列表
     * List of symbol factors
     *
     * 返回展开后的符号列表，每个符号按其幂次重复出现。
     * Returns the expanded list of symbols, where each symbol appears according to its power.
     */
    val factors: List<Symbol>
        get() = powers.entries
            .flatMap { (symbol, exp) -> List(exp.toInt()) { symbol } }

    /**
     * 单项式的总次数
     * Total degree of the monomial
     *
     * 返回所有幂次的和，即单项式的总次数。
     * Returns the sum of all powers, which is the total degree of the monomial.
     */
    val degree: Int
        get() = powers.values.sum().toInt()

    /**
     * 表达式类型分类
     * Expression type category
     *
     * 根据总次数返回对应的分类：
     * - 0 或 1：Linear
     * - 2：Quadratic
     * - 大于 2：Nonlinear
     * Returns the corresponding category based on total degree:
     * - 0 or 1: Linear
     * - 2: Quadratic
     * - Greater than 2: Nonlinear
     */
    val category: Category
        get() = when (degree) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }

    override fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return CanonicalPolynomial(listOf(this), coefficient - coefficient)
    }
}

/**
 * 规范单项式的负运算符
 * Negation operator for canonical monomial
 *
 * @receiver 规范单项式 / Canonical monomial
 * @return 系数取负后的规范单项式 / Canonical monomial with negated coefficient
 */
operator fun <T : Ring<T>> CanonicalMonomial<T>.unaryMinus(): CanonicalMonomial<T> {
    return copy(coefficient = -coefficient)
}

/**
 * 规范单项式与标量的乘法运算符
 * Multiplication operator for canonical monomial and scalar
 *
 * @receiver 规范单项式 / Canonical monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数乘以标量后的规范单项式 / Canonical monomial with coefficient multiplied by scalar
 */
operator fun <T : Ring<T>> CanonicalMonomial<T>.times(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient * rhs)
}

/**
 * 规范单项式与标量的除法运算符
 * Division operator for canonical monomial and scalar
 *
 * @receiver 规范单项式 / Canonical monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数除以标量后的规范单项式 / Canonical monomial with coefficient divided by scalar
 */
operator fun <T : Field<T>> CanonicalMonomial<T>.div(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient / rhs)
}

