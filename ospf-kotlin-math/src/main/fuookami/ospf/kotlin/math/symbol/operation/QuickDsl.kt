@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 泛型快捷 DSL
 * Quick DSL
 *
 * 提供基于 Flt64ValueConverter 的泛型快捷 DSL，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic quick DSL based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 */

/**
 * 泛型快捷 DSL
 * Quick DSL
 *
 * 通过 Flt64ValueConverter 提供泛型多项式构造和聚合函数。
 * Provides generic polynomial construction and aggregation functions via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 * @property converter Flt64 到 V 的转换器 / Flt64 to V converter
 */
class QuickDsl<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {
    // ========== LinearPolynomial constructors ==========
    // 线性多项式构造函数 / Linear polynomial constructors

    /**
     * 创建空的线性多项式（零多项式） / Create empty linear polynomial (zero polynomial)
     *
     * @return 空的线性多项式 / Empty linear polynomial
     */
    fun LinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.zero)

    /**
     * 从 Flt64 常数创建线性多项式 / Create linear polynomial from Flt64 constant
     *
     * @param constant Flt64 常数 / Flt64 constant value
     * @return 线性多项式 / Linear polynomial
     */
    fun LinearPolynomial(constant: Flt64): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.intoValue(constant))

    /**
     * 从 V 常数创建线性多项式 / Create linear polynomial from V constant
     *
     * @param constant V 类型常数 / V-type constant value
     * @return 线性多项式 / Linear polynomial
     */
    fun LinearPolynomial(constant: V): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), constant)

    /**
     * 从单项式创建线性多项式 / Create linear polynomial from monomial
     *
     * @param monomial 线性单项式 / Linear monomial
     * @return 线性多项式 / Linear polynomial
     */
    fun LinearPolynomial(monomial: LinearMonomial<V>): LinearPolynomial<V> =
        LinearPolynomial(listOf(monomial), converter.zero)

    /**
     * 从符号创建线性多项式（系数为 1） / Create linear polynomial from symbol (coefficient = 1)
     *
     * @param symbol 符号 / Symbol
     * @return 线性多项式 / Linear polynomial
     */
    fun LinearPolynomial(symbol: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, symbol)), converter.zero)

    /**
     * 创建空的可变线性多项式 / Create empty mutable linear polynomial
     *
     * @return 空的可变线性多项式 / Empty mutable linear polynomial
     */
    fun MutableLinearPolynomial(): MutableLinearPolynomial<V> =
        MutableLinearPolynomial(emptyList(), converter.zero)

    // ========== QuadraticPolynomial constructors ==========
    // 二次多项式构造函数 / Quadratic polynomial constructors

    /**
     * 创建空的二次多项式（零多项式） / Create empty quadratic polynomial (zero polynomial)
     *
     * @return 空的二次多项式 / Empty quadratic polynomial
     */
    fun QuadraticPolynomial(): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), converter.zero)

    /**
     * 从 Flt64 常数创建二次多项式 / Create quadratic polynomial from Flt64 constant
     *
     * @param constant Flt64 常数 / Flt64 constant value
     * @return 二次多项式 / Quadratic polynomial
     */
    fun QuadraticPolynomial(constant: Flt64): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), converter.intoValue(constant))

    /**
     * 从 V 常数创建二次多项式 / Create quadratic polynomial from V constant
     *
     * @param constant V 类型常数 / V-type constant value
     * @return 二次多项式 / Quadratic polynomial
     */
    fun QuadraticPolynomial(constant: V): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), constant)

    /**
     * 从二次单项式创建二次多项式 / Create quadratic polynomial from quadratic monomial
     *
     * @param monomial 二次单项式 / Quadratic monomial
     * @return 二次多项式 / Quadratic polynomial
     */
    fun QuadraticPolynomial(monomial: QuadraticMonomial<V>): QuadraticPolynomial<V> =
        QuadraticPolynomial(listOf(monomial), converter.zero)

    /**
     * 从符号创建二次多项式（系数为 1 的线性项） / Create quadratic polynomial from symbol (linear term with coefficient = 1)
     *
     * @param symbol 符号 / Symbol
     * @return 二次多项式 / Quadratic polynomial
     */
    fun QuadraticPolynomial(symbol: Symbol): QuadraticPolynomial<V> =
        QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, symbol)), converter.zero)

    /**
     * 创建空的可变二次多项式 / Create empty mutable quadratic polynomial
     *
     * @return 空的可变二次多项式 / Empty mutable quadratic polynomial
     */
    fun MutableQuadraticPolynomial(): MutableQuadraticPolynomial<V> =
        MutableQuadraticPolynomial(emptyList(), converter.zero)

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
    fun <E> sumVars(items: Iterable<E>, selector: (E) -> Symbol?): LinearPolynomial<V> {
        val monomials = items.mapNotNull(selector).map { LinearMonomial(converter.one, it) }
        return LinearPolynomial(monomials, converter.zero)
    }

    /**
     * 对符号集合求和（线性多项式）
     * Sum a collection of symbols (linear polynomial)
     *
     * @param symbols 符号集合 / Collection of symbols
     * @return 线性多项式之和 / Sum as linear polynomial
     */
    fun sum(symbols: Iterable<Symbol>): LinearPolynomial<V> =
        LinearPolynomial(symbols.map { LinearMonomial(converter.one, it) }, converter.zero)

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
    fun <E> qsumVars(items: Iterable<E>, selector: (E) -> Symbol?): QuadraticPolynomial<V> {
        val monomials = items.mapNotNull(selector).map { QuadraticMonomial.linear(converter.one, it) }
        return QuadraticPolynomial(monomials, converter.zero)
    }

    /**
     * 对符号集合求和（二次多项式）
     * Sum a collection of symbols (quadratic polynomial)
     *
     * @param symbols 符号集合 / Collection of symbols
     * @return 二次多项式之和 / Sum as quadratic polynomial
     */
    fun qsum(symbols: Iterable<Symbol>): QuadraticPolynomial<V> =
        QuadraticPolynomial(symbols.map { QuadraticMonomial.linear(converter.one, it) }, converter.zero)
}
