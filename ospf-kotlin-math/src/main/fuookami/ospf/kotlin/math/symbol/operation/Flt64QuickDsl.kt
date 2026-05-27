/**
 * Flt64 快捷 DSL 构造函数与运算符
 * Flt64 Quick DSL Constructors and Operators
 *
 * 提供 Flt64 多项式的快捷构造函数（LinearPolynomial/QuadraticPolynomial 工厂函数）、
 * 聚合函数（sumVars/sum/qsumVars/qsum）以及符号和多项式的算术运算符重载。
 * Provides Flt64 polynomial quick constructors (LinearPolynomial/QuadraticPolynomial factory functions),
 * aggregation functions (sumVars/sum/qsumVars/qsum), and arithmetic operator overloads
 * for symbols and polynomials.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.jvm.JvmName
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial

// ========== LinearPolynomial quick constructors ==========
// 线性多项式快捷构造函数 / Linear polynomial quick constructors

/**
 * 创建空的 Flt64 线性多项式（零多项式）
 * Create an empty Flt64 linear polynomial (zero polynomial)
 *
 * @return 零线性多项式 / Zero linear polynomial
 */
@JvmName("quickLinearPolynomialZero")
fun LinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), Flt64.zero)
}

/**
 * 从常数创建 Flt64 线性多项式
 * Create a Flt64 linear polynomial from a constant
 *
 * @param constant 常数值 / Constant value
 * @return 线性多项式 / Linear polynomial
 */
@JvmName("quickLinearPolynomialFromConstant")
fun LinearPolynomial(constant: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), constant)
}

@JvmName("quickLinearPolynomialFromMonomial")
fun LinearPolynomial(monomial: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(monomial), Flt64.zero)
}

@JvmName("quickLinearPolynomialFromSymbol")
fun LinearPolynomial(symbol: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
}

@JvmName("quickMutableLinearPolynomialZero")
fun MutableLinearPolynomial(): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(emptyList(), Flt64.zero)
}

@JvmName("quickMutableLinearPolynomialFromMonomial")
fun MutableLinearPolynomial(monomial: LinearMonomial<Flt64>): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(listOf(monomial), Flt64.zero)
}

// ========== QuadraticPolynomial quick constructors ==========

@JvmName("quickQuadraticPolynomialZero")
fun QuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), Flt64.zero)
}

@JvmName("quickQuadraticPolynomialFromConstant")
fun QuadraticPolynomial(constant: Flt64): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), constant)
}

@JvmName("quickQuadraticPolynomialFromQuadraticMonomial")
fun QuadraticPolynomial(monomial: QuadraticMonomial<Flt64>): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(monomial), Flt64.zero)
}

@JvmName("quickQuadraticPolynomialFromLinearMonomial")
fun QuadraticPolynomial(monomial: LinearMonomial<Flt64>): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(monomial.coefficient, monomial.symbol)), Flt64.zero)
}

@JvmName("quickQuadraticPolynomialFromSymbol")
fun QuadraticPolynomial(symbol: Symbol): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(Flt64.one, symbol)), Flt64.zero)
}

@JvmName("quickMutableQuadraticPolynomialZero")
fun MutableQuadraticPolynomial(): MutableQuadraticPolynomial<Flt64> {
    return MutableQuadraticPolynomial(emptyList(), Flt64.zero)
}

// ========== Linear aggregation ==========
// 线性聚合函数 / Linear aggregation functions

/**
 * 对集合中每个元素的符号求和（线性多项式）
 * Sum symbols from each element in a collection (linear polynomial)
 *
 * @param items 元素集合 / Collection of elements
 * @param selector 从元素提取符号的函数 / Function to extract symbol from element
 * @return 线性多项式之和 / Sum as linear polynomial
 */
@JvmName("quickSumVars")
fun <E> sumVars(
    items: Iterable<E>,
    selector: (E) -> Symbol?
): LinearPolynomial<Flt64> {
    val monomials = items.mapNotNull(selector).map { LinearMonomial(Flt64.one, it) }
    return LinearPolynomial(monomials, Flt64.zero)
}

/**
 * 对符号集合求和（线性多项式）
 * Sum a collection of symbols (linear polynomial)
 *
 * @param symbols 符号集合 / Collection of symbols
 * @return 线性多项式之和 / Sum as linear polynomial
 */
@JvmName("quickSumSymbols")
fun sum(symbols: Iterable<Symbol>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        symbols.map { LinearMonomial(Flt64.one, it) },
        Flt64.zero
    )
}

// ========== Quadratic aggregation ==========
// 二次聚合函数 / Quadratic aggregation functions

/**
 * 对集合中每个元素的符号求和（二次多项式）
 * Sum symbols from each element in a collection (quadratic polynomial)
 *
 * @param items 元素集合 / Collection of elements
 * @param selector 从元素提取符号的函数 / Function to extract symbol from element
 * @return 二次多项式之和 / Sum as quadratic polynomial
 */
@JvmName("quickQSumVars")
fun <E> qsumVars(
    items: Iterable<E>,
    selector: (E) -> Symbol?
): QuadraticPolynomial<Flt64> {
    val monomials = items.mapNotNull(selector).map { QuadraticMonomial.linear(Flt64.one, it) }
    return QuadraticPolynomial(monomials, Flt64.zero)
}

/**
 * 对符号集合求和（二次多项式）
 * Sum a collection of symbols (quadratic polynomial)
 *
 * @param symbols 符号集合 / Collection of symbols
 * @return 二次多项式之和 / Sum as quadratic polynomial
 */
@JvmName("quickQSumSymbols")
fun qsum(symbols: Iterable<Symbol>): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(
        symbols.map { QuadraticMonomial.linear(Flt64.one, it) },
        Flt64.zero
    )
}

// ========== Symbol arithmetic ==========

operator fun Symbol.unaryMinus(): LinearMonomial<Flt64> {
    return LinearMonomial(-Flt64.one, this)
}

operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), rhs),
        Flt64.zero
    )
}

operator fun Symbol.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-rhs.coefficient, rhs.symbol)),
        Flt64.zero
    )
}

operator fun LinearMonomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun LinearMonomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials,
        rhs.constant
    )
}

operator fun Symbol.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -rhs.constant
    )
}

operator fun LinearPolynomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(Flt64.one, rhs),
        constant
    )
}

operator fun LinearPolynomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(-Flt64.one, rhs),
        constant
    )
}

// ========== Symbol vs Int/Double ==========

operator fun Symbol.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs.toDouble()))
}

operator fun Symbol.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(-rhs.toDouble()))
}

operator fun Symbol.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

operator fun Symbol.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(-rhs))
}

// ========== Int/Double vs Symbol ==========

operator fun Int.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this.toDouble()))
}

operator fun Int.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        Flt64(this.toDouble())
    )
}

operator fun Double.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this))
}

operator fun Double.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        Flt64(this)
    )
}

// ========== Int/Double vs LinearPolynomial ==========

operator fun Int.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this.toDouble()) + rhs.constant)
}

operator fun Int.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        Flt64(this.toDouble()) - rhs.constant
    )
}

operator fun Int.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = Flt64(this.toDouble())
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

operator fun Double.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this) + rhs.constant)
}

operator fun Double.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        Flt64(this) - rhs.constant
    )
}

operator fun Double.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = Flt64(this)
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

// ========== LinearPolynomial vs Int/Double ==========

operator fun LinearPolynomial<Flt64>.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant + Flt64(rhs.toDouble()))
}

operator fun LinearPolynomial<Flt64>.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant - Flt64(rhs.toDouble()))
}

operator fun LinearPolynomial<Flt64>.times(rhs: Int): LinearPolynomial<Flt64> {
    val scalar = Flt64(rhs.toDouble())
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        constant * scalar
    )
}

operator fun LinearPolynomial<Flt64>.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant + Flt64(rhs))
}

operator fun LinearPolynomial<Flt64>.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant - Flt64(rhs))
}

operator fun LinearPolynomial<Flt64>.times(rhs: Double): LinearPolynomial<Flt64> {
    val scalar = Flt64(rhs)
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        constant * scalar
    )
}

// ========== UInt64 arithmetic ==========

operator fun UInt64.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        this.toFlt64()
    )
}

operator fun UInt64.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, rhs)),
        this.toFlt64()
    )
}

operator fun UInt64.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), rhs)
}

operator fun Symbol.times(rhs: UInt64): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64(), this)
}

operator fun UInt64.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

operator fun UInt64.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        scalar - rhs.constant
    )
}

operator fun UInt64.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, this.toFlt64() + rhs.constant)
}
