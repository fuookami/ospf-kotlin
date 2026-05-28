/**
 * 泛型快捷运算
 * Quick Ops
 *
 * 提供基于 Flt64ValueConverter 的泛型算术运算符重载，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic arithmetic operator overloads based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*


/**
 * 泛型快捷运算
 * Quick Ops
 *
 * 通过 Flt64ValueConverter 提供泛型算术运算符重载。
 * Provides generic arithmetic operator overloads via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 */
class QuickOps<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {
    // ========== Flt64 arithmetic ==========

    operator fun Flt64.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(this), rhs)

    operator fun Symbol.times(rhs: Flt64): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(rhs), this)

    // ========== Int arithmetic ==========

    operator fun Int.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(this.toDouble())), rhs)

    operator fun Symbol.times(rhs: Int): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(rhs.toDouble())), this)

    operator fun Int.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-converter.one, rhs)), converter.intoValue(Flt64(this.toDouble())))

    operator fun Int.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, rhs)), converter.intoValue(Flt64(this.toDouble())))

    // ========== Double arithmetic ==========

    operator fun Double.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(this)), rhs)

    operator fun Symbol.times(rhs: Double): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(rhs)), this)

    operator fun Double.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-converter.one, rhs)), converter.intoValue(Flt64(this)))

    operator fun Double.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, rhs)), converter.intoValue(Flt64(this)))

    // ========== Symbol arithmetic ==========

    operator fun Symbol.unaryMinus(): LinearMonomial<V> =
        LinearMonomial(-converter.one, this)

    operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this), LinearMonomial(converter.one, rhs)), converter.zero)

    operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this), LinearMonomial(-converter.one, rhs)), converter.zero)
}
