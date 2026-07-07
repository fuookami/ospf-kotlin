@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import kotlin.jvm.JvmName
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

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

/**
 * 从单项式创建 Flt64 线性多项式
 * Create a Flt64 linear polynomial from a monomial
 *
 * @param monomial 线性单项式 / Linear monomial
 * @return 线性多项式 / Linear polynomial
 */
@JvmName("quickLinearPolynomialFromMonomial")
fun LinearPolynomial(monomial: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(monomial), Flt64.zero)
}

/**
 * 从符号创建 Flt64 线性多项式（系数为 1）
 * Create a Flt64 linear polynomial from a symbol (coefficient = 1)
 *
 * @param symbol 变量符号 / Variable symbol
 * @return 线性多项式 / Linear polynomial
 */
@JvmName("quickLinearPolynomialFromSymbol")
fun LinearPolynomial(symbol: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
}

/**
 * 创建空的 Flt64 可变线性多项式（零多项式）
 * Create an empty Flt64 mutable linear polynomial (zero polynomial)
 *
 * @return 零可变线性多项式 / Zero mutable linear polynomial
 */
@JvmName("quickMutableLinearPolynomialZero")
fun MutableLinearPolynomial(): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(emptyList(), Flt64.zero)
}

/**
 * 从单项式创建 Flt64 可变线性多项式
 * Create a Flt64 mutable linear polynomial from a monomial
 *
 * @param monomial 线性单项式 / Linear monomial
 * @return 可变线性多项式 / Mutable linear polynomial
 */
@JvmName("quickMutableLinearPolynomialFromMonomial")
fun MutableLinearPolynomial(monomial: LinearMonomial<Flt64>): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(listOf(monomial), Flt64.zero)
}

// ========== QuadraticPolynomial quick constructors ==========

/**
 * 创建空的 Flt64 二次多项式（零多项式）
 * Create an empty Flt64 quadratic polynomial (zero polynomial)
 *
 * @return 零二次多项式 / Zero quadratic polynomial
 */
@JvmName("quickQuadraticPolynomialZero")
fun QuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), Flt64.zero)
}

/**
 * 从常数创建 Flt64 二次多项式
 * Create a Flt64 quadratic polynomial from a constant
 *
 * @param constant 常数值 / Constant value
 * @return 二次多项式 / Quadratic polynomial
 */
@JvmName("quickQuadraticPolynomialFromConstant")
fun QuadraticPolynomial(constant: Flt64): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), constant)
}

/**
 * 从二次单项式创建 Flt64 二次多项式
 * Create a Flt64 quadratic polynomial from a quadratic monomial
 *
 * @param monomial 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
@JvmName("quickQuadraticPolynomialFromQuadraticMonomial")
fun QuadraticPolynomial(monomial: QuadraticMonomial<Flt64>): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(monomial), Flt64.zero)
}

/**
 * 从线性单项式创建 Flt64 二次多项式
 * Create a Flt64 quadratic polynomial from a linear monomial
 *
 * @param monomial 线性单项式 / Linear monomial
 * @return 二次多项式 / Quadratic polynomial
 */
@JvmName("quickQuadraticPolynomialFromLinearMonomial")
fun QuadraticPolynomial(monomial: LinearMonomial<Flt64>): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(monomial.coefficient, monomial.symbol)), Flt64.zero)
}

/**
 * 从符号创建 Flt64 二次多项式（系数为 1 的线性项）
 * Create a Flt64 quadratic polynomial from a symbol (linear term with coefficient = 1)
 *
 * @param symbol 变量符号 / Variable symbol
 * @return 二次多项式 / Quadratic polynomial
 */
@JvmName("quickQuadraticPolynomialFromSymbol")
fun QuadraticPolynomial(symbol: Symbol): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(Flt64.one, symbol)), Flt64.zero)
}

/**
 * 创建空的 Flt64 可变二次多项式（零多项式）
 * Create an empty Flt64 mutable quadratic polynomial (zero polynomial)
 *
 * @return 零可变二次多项式 / Zero mutable quadratic polynomial
 */
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
// 符号算术运算 / Symbol arithmetic operators

/**
 * Negate a symbol.
 * 中文取负。
 */
operator fun Symbol.unaryMinus(): LinearMonomial<Flt64> {
    return LinearMonomial(-Flt64.one, this)
}

/**
 * Add two symbols.
 * 中文两个符号相加。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

/**
 * Subtract two symbols.
 * 中文两个符号相减。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

/**
 * Add a symbol and a monomial.
 * 中文符号加单项式。
 *
 * @param rhs the right-hand linear monomial / 右侧线性单项式
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), rhs),
        Flt64.zero
    )
}

/**
 * Subtract a monomial from a symbol.
 * 中文符号减单项式。
 *
 * @param rhs the right-hand linear monomial / 右侧线性单项式
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-rhs.coefficient, rhs.symbol)),
        Flt64.zero
    )
}

/**
 * Add a monomial and a symbol.
 * 中文单项式加符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun LinearMonomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

/**
 * Subtract a symbol from a monomial.
 * 中文单项式减符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun LinearMonomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

/**
 * Add a symbol and a polynomial.
 * 中文符号加多项式。
 *
 * @param rhs the right-hand linear polynomial / 右侧线性多项式
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials,
        rhs.constant
    )
}

/**
 * Subtract a polynomial from a symbol.
 * 中文符号减多项式。
 *
 * @param rhs the right-hand linear polynomial / 右侧线性多项式
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -rhs.constant
    )
}

/**
 * Add a polynomial and a symbol.
 * 中文多项式加符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun LinearPolynomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(Flt64.one, rhs),
        constant
    )
}

/**
 * Subtract a symbol from a polynomial.
 * 中文多项式减符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun LinearPolynomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(-Flt64.one, rhs),
        constant
    )
}

// ========== Symbol vs Int/Double ==========
// 符号与 Int/Double 运算 / Symbol arithmetic with Int/Double

/**
 * Add a symbol and an Int.
 * 中文符号加 Int。
 *
 * @param rhs the right-hand integer / 右侧整数
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs.toDouble()))
}

/**
 * Subtract an Int from a symbol.
 * 中文符号减 Int。
 *
 * @param rhs the right-hand integer / 右侧整数
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(-rhs.toDouble()))
}

/**
 * Add a symbol and a Double.
 * 中文符号加 Double。
 *
 * @param rhs the right-hand floating-point value / 右侧浮点数
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(rhs))
}

/**
 * Subtract a Double from a symbol.
 * 中文符号减 Double。
 *
 * @param rhs the right-hand floating-point value / 右侧浮点数
 * @return the linear polynomial / 线性多项式
 */
operator fun Symbol.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64(-rhs))
}

// ========== Int/Double vs Symbol ==========
// Int/Double 与符号运算 / Int/Double arithmetic with Symbol

/**
 * Add an Int and a symbol.
 * 中文Int 加符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun Int.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this.toDouble()))
}

/**
 * Subtract a symbol from an Int.
 * 中文Int 减符号。
 *
 * @param rhs the right-hand symbol / 右侧符号
 * @return the linear polynomial / 线性多项式
 */
operator fun Int.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        Flt64(this.toDouble())
    )
}

/**
 * Double 加符号 / Add Double and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun Double.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, rhs)), Flt64(this))
}

/**
 * Double 减符号 / Subtract symbol from Double
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun Double.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        Flt64(this)
    )
}

// ========== Int/Double vs LinearPolynomial ==========
// Int/Double 与线性多项式运算 / Int/Double arithmetic with LinearPolynomial

/**
 * Int 加多项式 / Add Int and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Int.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this.toDouble()) + rhs.constant)
}

/**
 * Int 减多项式 / Subtract polynomial from Int
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Int.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        Flt64(this.toDouble()) - rhs.constant
    )
}

/**
 * Int 乘多项式 / Multiply Int and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Int.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = Flt64(this.toDouble())
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

/**
 * Double 加多项式 / Add Double and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Double.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, Flt64(this) + rhs.constant)
}

/**
 * Double 减多项式 / Subtract polynomial from Double
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Double.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        Flt64(this) - rhs.constant
    )
}

/**
 * Double 乘多项式 / Multiply Double and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun Double.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = Flt64(this)
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

// ========== LinearPolynomial vs Int/Double ==========
// 线性多项式与 Int/Double 运算 / LinearPolynomial arithmetic with Int/Double

/**
 * 多项式加 Int / Add polynomial and Int
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.plus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant + Flt64(rhs.toDouble()))
}

/**
 * 多项式减 Int / Subtract Int from polynomial
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.minus(rhs: Int): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant - Flt64(rhs.toDouble()))
}

/**
 * 多项式乘 Int / Multiply polynomial and Int
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.times(rhs: Int): LinearPolynomial<Flt64> {
    val scalar = Flt64(rhs.toDouble())
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        constant * scalar
    )
}

/**
 * 多项式加 Double / Add polynomial and Double
 *
 * @param rhs 右侧浮点数 / Right-hand floating-point value
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.plus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant + Flt64(rhs))
}

/**
 * 多项式减 Double / Subtract Double from polynomial
 *
 * @param rhs 右侧浮点数 / Right-hand floating-point value
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.minus(rhs: Double): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials, constant - Flt64(rhs))
}

/**
 * 多项式乘 Double / Multiply polynomial and Double
 *
 * @param rhs 右侧浮点数 / Right-hand floating-point value
 * @return 线性多项式 / Linear polynomial
 */
operator fun LinearPolynomial<Flt64>.times(rhs: Double): LinearPolynomial<Flt64> {
    val scalar = Flt64(rhs)
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        constant * scalar
    )
}

// ========== UInt64 arithmetic ==========
// UInt64 算术运算 / UInt64 arithmetic operators

/**
 * UInt64 减符号 / Subtract symbol from UInt64
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun UInt64.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        this.toFlt64()
    )
}

/**
 * UInt64 加符号 / Add UInt64 and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性多项式 / Linear polynomial
 */
operator fun UInt64.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, rhs)),
        this.toFlt64()
    )
}

/**
 * UInt64 乘符号 / Multiply UInt64 and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性单项式 / Linear monomial
 */
operator fun UInt64.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this.toFlt64(), rhs)
}

/**
 * 符号乘 UInt64 / Multiply symbol and UInt64
 *
 * @param rhs 右侧 UInt64 / Right-hand UInt64
 * @return 线性单项式 / Linear monomial
 */
operator fun Symbol.times(rhs: UInt64): LinearMonomial<Flt64> {
    return LinearMonomial(rhs.toFlt64(), this)
}

/**
 * UInt64 乘多项式 / Multiply UInt64 and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun UInt64.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

/**
 * UInt64 减多项式 / Subtract polynomial from UInt64
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun UInt64.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        scalar - rhs.constant
    )
}

/**
 * UInt64 加多项式 / Add UInt64 and polynomial
 *
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun UInt64.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(rhs.monomials, this.toFlt64() + rhs.constant)
}
