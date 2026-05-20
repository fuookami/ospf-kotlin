/**
 * 泛型快捷 DSL
 * Quick DSL
 *
 * 提供基于 Flt64ValueConverter 的泛型快捷 DSL，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic quick DSL based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial

/**
 * 泛型快捷 DSL
 * Quick DSL
 *
 * 通过 Flt64ValueConverter 提供泛型多项式构造和聚合函数。
 * Provides generic polynomial construction and aggregation functions via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 */
class QuickDsl<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {
    // ========== LinearPolynomial constructors ==========

    fun LinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.zero)

    fun LinearPolynomial(constant: Flt64): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.intoValue(constant))

    fun LinearPolynomial(constant: V): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), constant)

    fun LinearPolynomial(monomial: LinearMonomial<V>): LinearPolynomial<V> =
        LinearPolynomial(listOf(monomial), converter.zero)

    fun LinearPolynomial(symbol: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, symbol)), converter.zero)

    fun MutableLinearPolynomial(): MutableLinearPolynomial<V> =
        MutableLinearPolynomial(emptyList(), converter.zero)

    // ========== QuadraticPolynomial constructors ==========

    fun QuadraticPolynomial(): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), converter.zero)

    fun QuadraticPolynomial(constant: Flt64): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), converter.intoValue(constant))

    fun QuadraticPolynomial(constant: V): QuadraticPolynomial<V> =
        QuadraticPolynomial(emptyList(), constant)

    fun QuadraticPolynomial(monomial: QuadraticMonomial<V>): QuadraticPolynomial<V> =
        QuadraticPolynomial(listOf(monomial), converter.zero)

    fun QuadraticPolynomial(symbol: Symbol): QuadraticPolynomial<V> =
        QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, symbol)), converter.zero)

    fun MutableQuadraticPolynomial(): MutableQuadraticPolynomial<V> =
        MutableQuadraticPolynomial(emptyList(), converter.zero)

    // ========== Linear aggregation ==========

    fun <E> sumVars(items: Iterable<E>, selector: (E) -> Symbol?): LinearPolynomial<V> {
        val monomials = items.mapNotNull(selector).map { LinearMonomial(converter.one, it) }
        return LinearPolynomial(monomials, converter.zero)
    }

    fun sum(symbols: Iterable<Symbol>): LinearPolynomial<V> =
        LinearPolynomial(symbols.map { LinearMonomial(converter.one, it) }, converter.zero)

    // ========== Quadratic aggregation ==========

    fun <E> qsumVars(items: Iterable<E>, selector: (E) -> Symbol?): QuadraticPolynomial<V> {
        val monomials = items.mapNotNull(selector).map { QuadraticMonomial.linear(converter.one, it) }
        return QuadraticPolynomial(monomials, converter.zero)
    }

    fun qsum(symbols: Iterable<Symbol>): QuadraticPolynomial<V> =
        QuadraticPolynomial(symbols.map { QuadraticMonomial.linear(converter.one, it) }, converter.zero)
}
