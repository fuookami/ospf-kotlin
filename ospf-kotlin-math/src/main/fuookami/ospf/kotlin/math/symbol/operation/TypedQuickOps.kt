/**
 * 泛型快捷运算
 * Typed Quick Ops
 *
 * 提供基于 Flt64Bridge 的泛型算术运算符重载，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic arithmetic operator overloads based on Flt64Bridge, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 泛型快捷运算
 * Typed Quick Ops
 *
 * 通过 Flt64Bridge 提供泛型算术运算符重载。
 * Provides generic arithmetic operator overloads via Flt64Bridge.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param bridge Flt64 到 V 的桥接转换器 / Flt64 to V bridge converter
 */
class TypedQuickOps<V>(private val bridge: Flt64Bridge<V>) where V : NumberField<V>, V : RealNumber<V> {
    // ========== Flt64 arithmetic ==========

    operator fun Flt64.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(this), rhs)

    operator fun Symbol.times(rhs: Flt64): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(rhs), this)

    // ========== Int arithmetic ==========

    operator fun Int.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(Flt64(this.toDouble())), rhs)

    operator fun Symbol.times(rhs: Int): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(Flt64(rhs.toDouble())), this)

    operator fun Int.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-bridge.one, rhs)), bridge.intoValue(Flt64(this.toDouble())))

    operator fun Int.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(bridge.one, rhs)), bridge.intoValue(Flt64(this.toDouble())))

    // ========== Double arithmetic ==========

    operator fun Double.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(Flt64(this)), rhs)

    operator fun Symbol.times(rhs: Double): LinearMonomial<V> =
        LinearMonomial(bridge.intoValue(Flt64(rhs)), this)

    operator fun Double.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-bridge.one, rhs)), bridge.intoValue(Flt64(this)))

    operator fun Double.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(bridge.one, rhs)), bridge.intoValue(Flt64(this)))

    // ========== Symbol arithmetic ==========

    operator fun Symbol.unaryMinus(): LinearMonomial<V> =
        LinearMonomial(-bridge.one, this)

    operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(bridge.one, this), LinearMonomial(bridge.one, rhs)), bridge.zero)

    operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(bridge.one, this), LinearMonomial(-bridge.one, rhs)), bridge.zero)
}
