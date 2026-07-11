@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 泛型快捷运算
 * Quick Ops
 *
 * 提供基于 Flt64ValueConverter 的泛型算术运算符重载，支持 Flt64/FltX/Rtn64/RtnX 四种数值类型。
 * Provides generic arithmetic operator overloads based on Flt64ValueConverter, supporting Flt64/FltX/Rtn64/RtnX numeric types.
*/

/**
 * 泛型快捷运算
 * Quick Ops
 *
 * 通过 Flt64ValueConverter 提供泛型算术运算符重载。
 * Provides generic arithmetic operator overloads via Flt64ValueConverter.
 *
 * @param V 数值类型，同时满足 NumberField 和 RealNumber 约束 / Numeric type satisfying both NumberField and RealNumber constraints
 * @param converter Flt64 到 V 的转换器 / Flt64 to V converter
 * @property converter Flt64 to V converter / Flt64 到 V 的转换器
*/
class QuickOps<V>(private val converter: Flt64ValueConverter<V>) where V : NumberField<V>, V : RealNumber<V> {
    // ========== Flt64 arithmetic ==========
    // Flt64 算术运算 / Flt64 arithmetic operators

    /**
     * Flt64 乘符号 / Multiply Flt64 and symbol
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性单项式 / Linear monomial
    */
    operator fun Flt64.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(this), rhs)

    /**
     * 符号乘 Flt64 / Multiply symbol and Flt64
     *
     * @param rhs 右侧 Flt64 值 / Right-hand side Flt64 value
     * @return 线性单项式 / Linear monomial
    */
    operator fun Symbol.times(rhs: Flt64): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(rhs), this)

    // ========== Int arithmetic ==========
    // Int 算术运算 / Int arithmetic operators

    /**
     * Int 乘符号 / Multiply Int and symbol
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性单项式 / Linear monomial
    */
    operator fun Int.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(this.toDouble())), rhs)

    /**
     * 符号乘 Int / Multiply symbol and Int
     *
     * @param rhs 右侧 Int 值 / Right-hand side Int value
     * @return 线性单项式 / Linear monomial
    */
    operator fun Symbol.times(rhs: Int): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(rhs.toDouble())), this)

    /**
     * Int 减符号 / Subtract symbol from Int
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Int.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-converter.one, rhs)), converter.intoValue(Flt64(this.toDouble())))

    /**
     * Int 加符号 / Add Int and symbol
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Int.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, rhs)), converter.intoValue(Flt64(this.toDouble())))

    // ========== Double arithmetic ==========
    // Double 算术运算 / Double arithmetic operators

    /**
     * Double 乘符号 / Multiply Double and symbol
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性单项式 / Linear monomial
    */
    operator fun Double.times(rhs: Symbol): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(this)), rhs)

    /**
     * 符号乘 Double / Multiply symbol and Double
     *
     * @param rhs 右侧 Double 值 / Right-hand side Double value
     * @return 线性单项式 / Linear monomial
    */
    operator fun Symbol.times(rhs: Double): LinearMonomial<V> =
        LinearMonomial(converter.intoValue(Flt64(rhs)), this)

    /**
     * Double 减符号 / Subtract symbol from Double
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Double.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(-converter.one, rhs)), converter.intoValue(Flt64(this)))

    /**
     * Double 加符号 / Add Double and symbol
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Double.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, rhs)), converter.intoValue(Flt64(this)))

    // ========== Symbol arithmetic ==========
    // 符号算术运算 / Symbol arithmetic operators

    /** 取负 / Negate */
    operator fun Symbol.unaryMinus(): LinearMonomial<V> =
        LinearMonomial(-converter.one, this)

    /**
     * 两个符号相加 / Add two symbols
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this), LinearMonomial(converter.one, rhs)), converter.zero)

    /**
     * 两个符号相减 / Subtract two symbols
     *
     * @param rhs 右侧符号 / Right-hand side symbol
     * @return 线性多项式 / Linear polynomial
    */
    operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<V> =
        LinearPolynomial(listOf(LinearMonomial(converter.one, this), LinearMonomial(-converter.one, rhs)), converter.zero)
}
