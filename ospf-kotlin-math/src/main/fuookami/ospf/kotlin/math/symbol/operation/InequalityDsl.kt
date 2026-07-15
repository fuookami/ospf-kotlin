@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 泛型不等式 DSL
 * Inequality DSL
 *
 * 提供基于 Flt64ValueConverter 的泛型比较运算符重载，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic comparison operator overloads based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
 *
 * 通过 Flt64ValueConverter 提供泛型比较运算符重载。
 * Provides generic comparison operator overloads via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 * @property converter Flt64 到 V 的转换器 / Flt64 to V converter
*/
class InequalityDsl<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {

    /** 将符号转换为系数为 1 的线性多项式 / Convert a symbol to a linear polynomial with coefficient one
     * @return 系数为 1 的线性多项式 / Linear polynomial with coefficient one */
    private fun Symbol.asLinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this)), converter.zero)

    /** 将 Flt64 值转换为常数线性多项式 / Convert a Flt64 value to a constant linear polynomial
     * @return 常数线性多项式 / Constant linear polynomial */
    private fun Flt64.asLinearPolynomial(): LinearPolynomial<V> =
        LinearPolynomial(emptyList(), converter.intoValue(this))

    /** 将布尔值转换为 Flt64，true 为 1，false 为 0 / Convert a Boolean to Flt64: true becomes 1, false becomes 0
     * @return 对应的 Flt64 值 / Corresponding Flt64 value */
    private fun Boolean.asFlt64(): Flt64 = if (this) Flt64.one else Flt64.zero

    // ========== Symbol vs Flt64 ==========
    // 符号与 Flt64 比较 / Symbol comparison with Flt64

    /** 符号 < Flt64 / Symbol less than Flt64 */ infix fun Symbol.lt(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    /** 符号 <= Flt64 / Symbol less than or equal to Flt64 */ infix fun Symbol.le(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    /** 符号 == Flt64 / Symbol equal to Flt64 */ infix fun Symbol.eq(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    /** 符号 != Flt64 / Symbol not equal to Flt64 */ infix fun Symbol.ne(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    /** 符号 >= Flt64 / Symbol greater than or equal to Flt64 */ infix fun Symbol.ge(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    /** 符号 > Flt64 / Symbol greater than Flt64 */ infix fun Symbol.gt(rhs: Flt64): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Flt64 vs Symbol ==========
    // Flt64 与符号比较 / Flt64 comparison with Symbol

    /** Flt64 < 符号 / Flt64 less than symbol */ infix fun Flt64.lt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    /** Flt64 <= 符号 / Flt64 less than or equal to symbol */ infix fun Flt64.le(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    /** Flt64 == 符号 / Flt64 equal to symbol */ infix fun Flt64.eq(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    /** Flt64 != 符号 / Flt64 not equal to symbol */ infix fun Flt64.ne(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    /** Flt64 >= 符号 / Flt64 greater than or equal to symbol */ infix fun Flt64.ge(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    /** Flt64 > 符号 / Flt64 greater than symbol */ infix fun Flt64.gt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Symbol vs Symbol ==========
    // 符号与符号比较 / Symbol comparison with Symbol

    /** 符号 < 符号 / Symbol less than symbol */ infix fun Symbol.lt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
    /** 符号 <= 符号 / Symbol less than or equal to symbol */ infix fun Symbol.le(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
    /** 符号 == 符号 / Symbol equal to symbol */ infix fun Symbol.eq(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
    /** 符号 != 符号 / Symbol not equal to symbol */ infix fun Symbol.ne(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
    /** 符号 >= 符号 / Symbol greater than or equal to symbol */ infix fun Symbol.ge(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
    /** 符号 > 符号 / Symbol greater than symbol */ infix fun Symbol.gt(rhs: Symbol): LinearInequality<V> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

    // ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========
    // 别名（leq/geq/neq/ls/gr）匹配核心约定 / Alias names matching core convention

    /** 符号 <= Flt64（别名） / Symbol less than or equal to Flt64 (alias) */ infix fun Symbol.leq(rhs: Flt64): LinearInequality<V> = this le rhs
    /** 符号 >= Flt64（别名） / Symbol greater than or equal to Flt64 (alias) */ infix fun Symbol.geq(rhs: Flt64): LinearInequality<V> = this ge rhs
    /** 符号 != Flt64（别名） / Symbol not equal to Flt64 (alias) */ infix fun Symbol.neq(rhs: Flt64): LinearInequality<V> = this ne rhs
    /** 符号 < Flt64（别名） / Symbol less than Flt64 (alias) */ infix fun Symbol.ls(rhs: Flt64): LinearInequality<V> = this lt rhs
    /** 符号 > Flt64（别名） / Symbol greater than Flt64 (alias) */ infix fun Symbol.gr(rhs: Flt64): LinearInequality<V> = this gt rhs

    /** Flt64 <= 符号（别名） / Flt64 less than or equal to symbol (alias) */ infix fun Flt64.leq(rhs: Symbol): LinearInequality<V> = this le rhs
    /** Flt64 >= 符号（别名） / Flt64 greater than or equal to symbol (alias) */ infix fun Flt64.geq(rhs: Symbol): LinearInequality<V> = this ge rhs
    /** Flt64 != 符号（别名） / Flt64 not equal to symbol (alias) */ infix fun Flt64.neq(rhs: Symbol): LinearInequality<V> = this ne rhs
    /** Flt64 < 符号（别名） / Flt64 less than symbol (alias) */ infix fun Flt64.ls(rhs: Symbol): LinearInequality<V> = this lt rhs
    /** Flt64 > 符号（别名） / Flt64 greater than symbol (alias) */ infix fun Flt64.gr(rhs: Symbol): LinearInequality<V> = this gt rhs

    /** 符号 <= 符号（别名） / Symbol less than or equal to symbol (alias) */ infix fun Symbol.leq(rhs: Symbol): LinearInequality<V> = this le rhs
    /** 符号 >= 符号（别名） / Symbol greater than or equal to symbol (alias) */ infix fun Symbol.geq(rhs: Symbol): LinearInequality<V> = this ge rhs
    /** 符号 != 符号（别名） / Symbol not equal to symbol (alias) */ infix fun Symbol.neq(rhs: Symbol): LinearInequality<V> = this ne rhs
    /** 符号 < 符号（别名） / Symbol less than symbol (alias) */ infix fun Symbol.ls(rhs: Symbol): LinearInequality<V> = this lt rhs
    /** 符号 > 符号（别名） / Symbol greater than symbol (alias) */ infix fun Symbol.gr(rhs: Symbol): LinearInequality<V> = this gt rhs

    // ========== Symbol vs Int ==========
    // 符号与 Int 比较 / Symbol comparison with Int

    /** 符号 < Int / Symbol less than Int */ infix fun Symbol.lt(rhs: Int): LinearInequality<V> = this lt Flt64(rhs)
    /** 符号 <= Int / Symbol less than or equal to Int */ infix fun Symbol.le(rhs: Int): LinearInequality<V> = this le Flt64(rhs)
    /** 符号 == Int / Symbol equal to Int */ infix fun Symbol.eq(rhs: Int): LinearInequality<V> = this eq Flt64(rhs)
    /** 符号 != Int / Symbol not equal to Int */ infix fun Symbol.ne(rhs: Int): LinearInequality<V> = this ne Flt64(rhs)
    /** 符号 >= Int / Symbol greater than or equal to Int */ infix fun Symbol.ge(rhs: Int): LinearInequality<V> = this ge Flt64(rhs)
    /** 符号 > Int / Symbol greater than Int */ infix fun Symbol.gt(rhs: Int): LinearInequality<V> = this gt Flt64(rhs)
    /** 符号 <= Int（别名） / Symbol less than or equal to Int (alias) */ infix fun Symbol.leq(rhs: Int): LinearInequality<V> = this le Flt64(rhs)
    /** 符号 >= Int（别名） / Symbol greater than or equal to Int (alias) */ infix fun Symbol.geq(rhs: Int): LinearInequality<V> = this ge Flt64(rhs)
    /** 符号 != Int（别名） / Symbol not equal to Int (alias) */ infix fun Symbol.neq(rhs: Int): LinearInequality<V> = this ne Flt64(rhs)
    /** 符号 < Int（别名） / Symbol less than Int (alias) */ infix fun Symbol.ls(rhs: Int): LinearInequality<V> = this lt Flt64(rhs)
    /** 符号 > Int（别名） / Symbol greater than Int (alias) */ infix fun Symbol.gr(rhs: Int): LinearInequality<V> = this gt Flt64(rhs)

    // ========== Symbol vs Double ==========
    // 符号与 Double 比较 / Symbol comparison with Double

    /** 符号 < Double / Symbol less than Double */ infix fun Symbol.lt(rhs: Double): LinearInequality<V> = this lt Flt64(rhs)
    /** 符号 <= Double / Symbol less than or equal to Double */ infix fun Symbol.le(rhs: Double): LinearInequality<V> = this le Flt64(rhs)
    /** 符号 == Double / Symbol equal to Double */ infix fun Symbol.eq(rhs: Double): LinearInequality<V> = this eq Flt64(rhs)
    /** 符号 != Double / Symbol not equal to Double */ infix fun Symbol.ne(rhs: Double): LinearInequality<V> = this ne Flt64(rhs)
    /** 符号 >= Double / Symbol greater than or equal to Double */ infix fun Symbol.ge(rhs: Double): LinearInequality<V> = this ge Flt64(rhs)
    /** 符号 > Double / Symbol greater than Double */ infix fun Symbol.gt(rhs: Double): LinearInequality<V> = this gt Flt64(rhs)
    /** 符号 <= Double（别名） / Symbol less than or equal to Double (alias) */ infix fun Symbol.leq(rhs: Double): LinearInequality<V> = this le Flt64(rhs)
    /** 符号 >= Double（别名） / Symbol greater than or equal to Double (alias) */ infix fun Symbol.geq(rhs: Double): LinearInequality<V> = this ge Flt64(rhs)
    /** 符号 != Double（别名） / Symbol not equal to Double (alias) */ infix fun Symbol.neq(rhs: Double): LinearInequality<V> = this ne Flt64(rhs)
    /** 符号 < Double（别名） / Symbol less than Double (alias) */ infix fun Symbol.ls(rhs: Double): LinearInequality<V> = this lt Flt64(rhs)
    /** 符号 > Double（别名） / Symbol greater than Double (alias) */ infix fun Symbol.gr(rhs: Double): LinearInequality<V> = this gt Flt64(rhs)

    // ========== Symbol vs Boolean ==========
    // 符号与 Boolean 比较 / Symbol comparison with Boolean

    /** 符号 < Boolean / Symbol less than Boolean */ infix fun Symbol.lt(rhs: Boolean): LinearInequality<V> = this lt rhs.asFlt64()
    /** 符号 <= Boolean / Symbol less than or equal to Boolean */ infix fun Symbol.le(rhs: Boolean): LinearInequality<V> = this le rhs.asFlt64()
    /** 符号 == Boolean / Symbol equal to Boolean */ infix fun Symbol.eq(rhs: Boolean): LinearInequality<V> = this eq rhs.asFlt64()
    /** 符号 != Boolean / Symbol not equal to Boolean */ infix fun Symbol.ne(rhs: Boolean): LinearInequality<V> = this ne rhs.asFlt64()
    /** 符号 >= Boolean / Symbol greater than or equal to Boolean */ infix fun Symbol.ge(rhs: Boolean): LinearInequality<V> = this ge rhs.asFlt64()
    /** 符号 > Boolean / Symbol greater than Boolean */ infix fun Symbol.gt(rhs: Boolean): LinearInequality<V> = this gt rhs.asFlt64()
    /** 符号 <= Boolean（别名） / Symbol less than or equal to Boolean (alias) */ infix fun Symbol.leq(rhs: Boolean): LinearInequality<V> = this le rhs.asFlt64()
    /** 符号 >= Boolean（别名） / Symbol greater than or equal to Boolean (alias) */ infix fun Symbol.geq(rhs: Boolean): LinearInequality<V> = this ge rhs.asFlt64()
    /** 符号 != Boolean（别名） / Symbol not equal to Boolean (alias) */ infix fun Symbol.neq(rhs: Boolean): LinearInequality<V> = this ne rhs.asFlt64()
    /** 符号 < Boolean（别名） / Symbol less than Boolean (alias) */ infix fun Symbol.ls(rhs: Boolean): LinearInequality<V> = this lt rhs.asFlt64()
    /** 符号 > Boolean（别名） / Symbol greater than Boolean (alias) */ infix fun Symbol.gr(rhs: Boolean): LinearInequality<V> = this gt rhs.asFlt64()

    // ========== Symbol vs UInt64 ==========
    // 符号与 UInt64 比较 / Symbol comparison with UInt64

    /** 符号 <= UInt64 / Symbol less than or equal to UInt64 */ infix fun Symbol.leq(rhs: UInt64): LinearInequality<V> = this le rhs.toFlt64()
    /** 符号 >= UInt64 / Symbol greater than or equal to UInt64 */ infix fun Symbol.geq(rhs: UInt64): LinearInequality<V> = this ge rhs.toFlt64()
    /** 符号 == UInt64 / Symbol equal to UInt64 */ infix fun Symbol.eq(rhs: UInt64): LinearInequality<V> = this eq rhs.toFlt64()

    /** UInt64 <= 符号 / UInt64 less than or equal to symbol */ infix fun UInt64.leq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) le rhs
    /** UInt64 >= 符号 / UInt64 greater than or equal to symbol */ infix fun UInt64.geq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) ge rhs
    /** UInt64 == 符号 / UInt64 equal to symbol */ infix fun UInt64.eq(rhs: Symbol): LinearInequality<V> = Flt64(this.toFlt64().value) eq rhs
}
