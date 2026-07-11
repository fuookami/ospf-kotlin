@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * Flt64 乘除运算符重载
 * Flt64 Multiplication and Division Operator Overloads
 *
 * 提供 Flt64/Int/Double 与符号和线性单项式之间的乘除运算符重载。
 * Provides multiplication and division operator overloads between
 * Flt64/Int/Double and symbols/linear monomials.
*/

// ========== Flt64 arithmetic ==========
// Flt64 算术运算 / Flt64 arithmetic operators

/**
 * Flt64 乘符号 / Multiply Flt64 and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性单项式 / Linear monomial
*/
operator fun Flt64.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this, rhs)
}

/**
 * 符号乘 Flt64 / Multiply symbol and Flt64
 *
 * @param rhs 右侧 Flt64 / Right-hand Flt64
 * @return 线性单项式 / Linear monomial
*/
operator fun Symbol.times(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(rhs, this)
}

/**
 * Flt64 乘单项式 / Multiply Flt64 and monomial
 *
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 线性单项式 / Linear monomial
*/
operator fun Flt64.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(this * rhs.coefficient, rhs.symbol)
}

/**
 * 单项式乘 Flt64 / Multiply monomial and Flt64
 *
 * @param rhs 右侧 Flt64 / Right-hand Flt64
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.times(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * rhs, symbol)
}

/**
 * 单项式乘符号，产生二次单项式 / Multiply monomial and symbol, producing quadratic monomial
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 二次单项式 / Quadratic monomial
*/
operator fun LinearMonomial<Flt64>.times(rhs: Symbol): QuadraticMonomial<Flt64> {
    return QuadraticMonomial(coefficient, symbol, rhs)
}

/**
 * 符号乘单项式，产生二次单项式 / Multiply symbol and monomial, producing quadratic monomial
 *
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 二次单项式 / Quadratic monomial
*/
operator fun Symbol.times(rhs: LinearMonomial<Flt64>): QuadraticMonomial<Flt64> {
    return QuadraticMonomial(rhs.coefficient, this, rhs.symbol)
}

/**
 * 单项式除 Flt64 / Divide monomial by Flt64
 *
 * @param rhs 右侧 Flt64 除数 / Right-hand Flt64 divisor
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.div(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / rhs, symbol)
}

// ========== Int arithmetic ==========
// Int 算术运算 / Int arithmetic operators

/**
 * Int 乘符号 / Multiply Int and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性单项式 / Linear monomial
*/
operator fun Int.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), rhs)
}

/**
 * 符号乘 Int / Multiply symbol and Int
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 线性单项式 / Linear monomial
*/
operator fun Symbol.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs.toDouble()), this)
}

/**
 * Int 乘单项式 / Multiply Int and monomial
 *
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 线性单项式 / Linear monomial
*/
operator fun Int.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()) * rhs.coefficient, rhs.symbol)
}

/**
 * 单项式乘 Int / Multiply monomial and Int
 *
 * @param rhs 右侧整数 / Right-hand integer
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * Flt64(rhs.toDouble()), symbol)
}

/**
 * 单项式除 Int / Divide monomial by Int
 *
 * @param rhs 右侧整数除数 / Right-hand integer divisor
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.div(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / Flt64(rhs.toDouble()), symbol)
}

// ========== Double arithmetic ==========
// Double 算术运算 / Double arithmetic operators

/**
 * Double 乘符号 / Multiply Double and symbol
 *
 * @param rhs 右侧符号 / Right-hand symbol
 * @return 线性单项式 / Linear monomial
*/
operator fun Double.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

/**
 * 符号乘 Double / Multiply symbol and Double
 *
 * @param rhs 右侧浮点数 / Right-hand floating-point value
 * @return 线性单项式 / Linear monomial
*/
operator fun Symbol.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

/**
 * Double 乘单项式 / Multiply Double and monomial
 *
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 线性单项式 / Linear monomial
*/
operator fun Double.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

/**
 * 单项式乘 Double / Multiply monomial and Double
 *
 * @param rhs 右侧浮点数 / Right-hand floating-point value
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * Flt64(rhs), symbol)
}

/**
 * 单项式除 Double / Divide monomial by Double
 *
 * @param rhs 右侧浮点数除数 / Right-hand floating-point divisor
 * @return 线性单项式 / Linear monomial
*/
operator fun LinearMonomial<Flt64>.div(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / Flt64(rhs), symbol)
}
