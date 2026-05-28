/**
 * 泛型不等式 DSL
 * Inequality DSL
 *
 * 提供基于 Flt64ValueConverter 的泛型比较运算符重载，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic comparison operator overloads based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*


/**
 * 泛型不等式 DSL
 * Inequality DSL
 *
 * 通过 Flt64ValueConverter 提供泛型比较运算符重载。
 * Provides generic comparison operator overloads via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 */
class InequalityDsl<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {
    private fun Symbol.asLinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this)), converter.zero)

    private fun Flt64.asLinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.intoValue(this))

    private fun Boolean.asFlt64(): Flt64 = if (this) Flt64.one else Flt64.zero

    // ========== Symbol vs Flt64 ==========

    infix fun Symbol.lt(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    infix fun Symbol.le(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    infix fun Symbol.eq(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    infix fun Symbol.ne(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    infix fun Symbol.ge(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    infix fun Symbol.gt(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Flt64 vs Symbol ==========

    infix fun Flt64.lt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    infix fun Flt64.le(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    infix fun Flt64.eq(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    infix fun Flt64.ne(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    infix fun Flt64.ge(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    infix fun Flt64.gt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Symbol vs Symbol ==========

    infix fun Symbol.lt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    infix fun Symbol.le(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    infix fun Symbol.eq(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    infix fun Symbol.ne(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    infix fun Symbol.ge(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    infix fun Symbol.gt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========

    infix fun Symbol.leq(rhs: Flt64): LinearInequality<V> = this le rhs
    infix fun Symbol.geq(rhs: Flt64): LinearInequality<V> = this ge rhs
    infix fun Symbol.neq(rhs: Flt64): LinearInequality<V> = this ne rhs
    infix fun Symbol.ls(rhs: Flt64): LinearInequality<V> = this lt rhs
    infix fun Symbol.gr(rhs: Flt64): LinearInequality<V> = this gt rhs

    infix fun Flt64.leq(rhs: Symbol): LinearInequality<V> = this le rhs
    infix fun Flt64.geq(rhs: Symbol): LinearInequality<V> = this ge rhs
    infix fun Flt64.neq(rhs: Symbol): LinearInequality<V> = this ne rhs
    infix fun Flt64.ls(rhs: Symbol): LinearInequality<V> = this lt rhs
    infix fun Flt64.gr(rhs: Symbol): LinearInequality<V> = this gt rhs

    infix fun Symbol.leq(rhs: Symbol): LinearInequality<V> = this le rhs
    infix fun Symbol.geq(rhs: Symbol): LinearInequality<V> = this ge rhs
    infix fun Symbol.neq(rhs: Symbol): LinearInequality<V> = this ne rhs
    infix fun Symbol.ls(rhs: Symbol): LinearInequality<V> = this lt rhs
    infix fun Symbol.gr(rhs: Symbol): LinearInequality<V> = this gt rhs

    // ========== Symbol vs Int ==========

    infix fun Symbol.lt(rhs: Int): LinearInequality<V> = this lt Flt64(rhs)
    infix fun Symbol.le(rhs: Int): LinearInequality<V> = this le Flt64(rhs)
    infix fun Symbol.eq(rhs: Int): LinearInequality<V> = this eq Flt64(rhs)
    infix fun Symbol.ne(rhs: Int): LinearInequality<V> = this ne Flt64(rhs)
    infix fun Symbol.ge(rhs: Int): LinearInequality<V> = this ge Flt64(rhs)
    infix fun Symbol.gt(rhs: Int): LinearInequality<V> = this gt Flt64(rhs)
    infix fun Symbol.leq(rhs: Int): LinearInequality<V> = this le Flt64(rhs)
    infix fun Symbol.geq(rhs: Int): LinearInequality<V> = this ge Flt64(rhs)
    infix fun Symbol.neq(rhs: Int): LinearInequality<V> = this ne Flt64(rhs)
    infix fun Symbol.ls(rhs: Int): LinearInequality<V> = this lt Flt64(rhs)
    infix fun Symbol.gr(rhs: Int): LinearInequality<V> = this gt Flt64(rhs)

    // ========== Symbol vs Double ==========

    infix fun Symbol.lt(rhs: Double): LinearInequality<V> = this lt Flt64(rhs)
    infix fun Symbol.le(rhs: Double): LinearInequality<V> = this le Flt64(rhs)
    infix fun Symbol.eq(rhs: Double): LinearInequality<V> = this eq Flt64(rhs)
    infix fun Symbol.ne(rhs: Double): LinearInequality<V> = this ne Flt64(rhs)
    infix fun Symbol.ge(rhs: Double): LinearInequality<V> = this ge Flt64(rhs)
    infix fun Symbol.gt(rhs: Double): LinearInequality<V> = this gt Flt64(rhs)
    infix fun Symbol.leq(rhs: Double): LinearInequality<V> = this le Flt64(rhs)
    infix fun Symbol.geq(rhs: Double): LinearInequality<V> = this ge Flt64(rhs)
    infix fun Symbol.neq(rhs: Double): LinearInequality<V> = this ne Flt64(rhs)
    infix fun Symbol.ls(rhs: Double): LinearInequality<V> = this lt Flt64(rhs)
    infix fun Symbol.gr(rhs: Double): LinearInequality<V> = this gt Flt64(rhs)

    // ========== Symbol vs Boolean ==========

    infix fun Symbol.lt(rhs: Boolean): LinearInequality<V> = this lt rhs.asFlt64()
    infix fun Symbol.le(rhs: Boolean): LinearInequality<V> = this le rhs.asFlt64()
    infix fun Symbol.eq(rhs: Boolean): LinearInequality<V> = this eq rhs.asFlt64()
    infix fun Symbol.ne(rhs: Boolean): LinearInequality<V> = this ne rhs.asFlt64()
    infix fun Symbol.ge(rhs: Boolean): LinearInequality<V> = this ge rhs.asFlt64()
    infix fun Symbol.gt(rhs: Boolean): LinearInequality<V> = this gt rhs.asFlt64()
    infix fun Symbol.leq(rhs: Boolean): LinearInequality<V> = this le rhs.asFlt64()
    infix fun Symbol.geq(rhs: Boolean): LinearInequality<V> = this ge rhs.asFlt64()
    infix fun Symbol.neq(rhs: Boolean): LinearInequality<V> = this ne rhs.asFlt64()
    infix fun Symbol.ls(rhs: Boolean): LinearInequality<V> = this lt rhs.asFlt64()
    infix fun Symbol.gr(rhs: Boolean): LinearInequality<V> = this gt rhs.asFlt64()

    // ========== Symbol vs UInt64 ==========

    infix fun Symbol.leq(rhs: UInt64): LinearInequality<V> = this le rhs.toFlt64()
    infix fun Symbol.geq(rhs: UInt64): LinearInequality<V> = this ge rhs.toFlt64()
    infix fun Symbol.eq(rhs: UInt64): LinearInequality<V> = this eq rhs.toFlt64()

    infix fun UInt64.leq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) le rhs
    infix fun UInt64.geq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) ge rhs
    infix fun UInt64.eq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) eq rhs
}
