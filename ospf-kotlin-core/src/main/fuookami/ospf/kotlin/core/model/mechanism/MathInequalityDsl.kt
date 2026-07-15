/**
 * 数学不等式 DSL 扩展函数
 * Mathematical inequality DSL extension functions
*/
@file:Suppress("unused", "EXTENSION_SHADOWED_BY_MEMBER")
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 数学不等式 DSL
 * Mathematical inequality DSL
*/

/** 将令牌表不安全转换为目标数值类型 / Unchecked-cast a token table to the target numeric type */
@Suppress("UNCHECKED_CAST")
private fun <V, T> tokenTableAs(tokens: AbstractTokenTable<T>): AbstractTokenTable<V>
        where V : RealNumber<V>, V : NumberField<V>, T : RealNumber<T>, T : NumberField<T> {
    return tokens as AbstractTokenTable<V>
}

// ========== Flt64 convenience aliases ==========

// The math layer provides generic infix operators (eq/le/ge/lt/gt/ne)
// for LinearPolynomial<T>, QuadraticPolynomial<T>, Symbol, and Flt64.
// This file adds convenience aliases (leq/geq/neq/ls/gr) and
// core-specific cross-type operators (Int/Double/Boolean/UInt64).

// LinearPolynomial<Flt64> aliases -> delegate directly to constructor to avoid
// resolution ambiguity with math-layer generic operators.

/** 线性多项式小于等于比较 / Linear polynomial less-than-or-equal comparison *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.LE)

/** 线性多项式大于等于比较 / Linear polynomial greater-than-or-equal comparison *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.GE)

/** 线性多项式不等于比较 / Linear polynomial not-equal comparison *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.NE)

/** 线性多项式等于常量 / Linear polynomial equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.eq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.EQ)

/** 线性多项式小于等于常量 / Linear polynomial less-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.LE)

/** 线性多项式大于等于常量 / Linear polynomial greater-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.GE)

/** 线性多项式不等于常量 / Linear polynomial not-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.NE)

/** 常量等于线性多项式 / Constant equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.EQ)

/** 常量小于等于线性多项式 / Constant less-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.LE)

/** 常量大于等于线性多项式 / Constant greater-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.GE)

/** 常量不等于线性多项式 / Constant not-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.NE)

// QuadraticPolynomial<Flt64> aliases
/** 二次多项式小于等于比较 / Quadratic polynomial less-than-or-equal comparison *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs, Comparison.LE)

/** 二次多项式大于等于比较 / Quadratic polynomial greater-than-or-equal comparison *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs, Comparison.GE)

/** 二次多项式不等于比较 / Quadratic polynomial not-equal comparison *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs, Comparison.NE)

/** 二次多项式小于等于常量 / Quadratic polynomial less-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.leq(rhs: Flt64): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.LE)

/** 二次多项式大于等于常量 / Quadratic polynomial greater-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.geq(rhs: Flt64): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.GE)

/** 二次多项式不等于常量 / Quadratic polynomial not-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.neq(rhs: Flt64): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.NE)

// LinearPolynomial<Flt64> vs QuadraticPolynomial<Flt64> aliases
/** 线性多项式小于等于二次多项式 / Linear polynomial less-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun LinearPolynomial<Flt64>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.LE)

/** 线性多项式大于等于二次多项式 / Linear polynomial greater-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun LinearPolynomial<Flt64>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.GE)

/** 线性多项式不等于二次多项式 / Linear polynomial not-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun LinearPolynomial<Flt64>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.NE)

/** 二次多项式小于等于线性多项式 / Quadratic polynomial less-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.leq(rhs: LinearPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.LE)

/** 二次多项式大于等于线性多项式 / Quadratic polynomial greater-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.geq(rhs: LinearPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.GE)

/** 二次多项式不等于线性多项式 / Quadratic polynomial not-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.neq(rhs: LinearPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.NE)

/** 二次多项式严格小于线性多项式 / Quadratic polynomial strictly-less-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.ls(rhs: LinearPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.LT)

/** 二次多项式严格大于线性多项式 / Quadratic polynomial strictly-greater-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticPolynomial<Flt64>.gr(rhs: LinearPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.GT)

// ========== Symbol convenience aliases ==========

// Symbol vs Flt64 aliases
/** 符号小于等于常量 / Symbol less-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)

/** 符号大于等于常量 / Symbol greater-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)

/** 符号不等于常量 / Symbol not-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)

/** 符号严格小于常量 / Symbol strictly-less-than constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)

/** 符号严格大于常量 / Symbol strictly-greater-than constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)

// Flt64 vs Symbol aliases
/** 常量小于等于符号 / Constant less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LE)

/** 常量大于等于符号 / Constant greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GE)

/** 常量不等于符号 / Constant not-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.NE)

/** 常量严格小于符号 / Constant strictly-less-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.ls(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LT)

/** 常量严格大于符号 / Constant strictly-greater-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Flt64.gr(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GT)

// Symbol vs Symbol aliases
/** 符号小于等于符号 / Symbol less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LE)

/** 符号大于等于符号 / Symbol greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GE)

/** 符号不等于符号 / Symbol not-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.NE)

/** 符号严格小于符号 / Symbol strictly-less-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LT)

/** 符号严格大于符号 / Symbol strictly-greater-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GT)

// ========== Symbol vs Int/Double ==========

/** 符号等于整数 / Symbol equal-to integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.eq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.EQ)

/** 符号小于等于整数 / Symbol less-than-or-equal-to integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.le(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)

/** 符号大于等于整数 / Symbol greater-than-or-equal-to integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ge(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)

/** 符号严格小于整数 / Symbol strictly-less-than integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.lt(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)

/** 符号严格大于整数 / Symbol strictly-greater-than integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gt(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

/** 符号不等于整数 / Symbol not-equal-to integer *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ne(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

/** 符号等于双精度浮点数 / Symbol equal-to double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.eq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.EQ)

/** 符号小于等于双精度浮点数 / Symbol less-than-or-equal-to double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.le(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)

/** 符号大于等于双精度浮点数 / Symbol greater-than-or-equal-to double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ge(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)

/** 符号严格小于双精度浮点数 / Symbol strictly-less-than double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.lt(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)

/** 符号严格大于双精度浮点数 / Symbol strictly-greater-than double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gt(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

/** 符号不等于双精度浮点数 / Symbol not-equal-to double *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ne(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

// Int/Double vs Symbol
/** 整数等于符号 / Integer equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Int.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.EQ)

/** 整数小于等于符号 / Integer less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Int.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LE)

/** 整数大于等于符号 / Integer greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Int.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GE)

/** 整数严格小于符号 / Integer strictly-less-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Int.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LT)

/** 整数严格大于符号 / Integer strictly-greater-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Int.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GT)

/** 双精度浮点数等于符号 / Double equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Double.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.EQ)

/** 双精度浮点数小于等于符号 / Double less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Double.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LE)

/** 双精度浮点数大于等于符号 / Double greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Double.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GE)

/** 双精度浮点数严格小于符号 / Double strictly-less-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Double.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LT)

/** 双精度浮点数严格大于符号 / Double strictly-greater-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun Double.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GT)

// Int/Double aliases
/** 符号小于等于整数别名 / Symbol less-than-or-equal-to integer alias *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)

/** 符号大于等于整数别名 / Symbol greater-than-or-equal-to integer alias *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)

/** 符号不等于整数别名 / Symbol not-equal-to integer alias *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

/** 符号严格小于整数别名 / Symbol strictly-less-than integer alias *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)

/** 符号严格大于整数别名 / Symbol strictly-greater-than integer alias *//**
 * @param rhs 右侧整数值 / right-hand side integer value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

/** 符号小于等于双精度浮点数别名 / Symbol less-than-or-equal-to double alias *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)

/** 符号大于等于双精度浮点数别名 / Symbol greater-than-or-equal-to double alias *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)

/** 符号不等于双精度浮点数别名 / Symbol not-equal-to double alias *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

/** 符号严格小于双精度浮点数别名 / Symbol strictly-less-than double alias *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)

/** 符号严格大于双精度浮点数别名 / Symbol strictly-greater-than double alias *//**
 * @param rhs 右侧双精度浮点数值 / right-hand side double value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

// ========== Symbol vs LinearPolynomial/QuadraticPolynomial ==========

// Symbol vs LinearPolynomial<Flt64>
/**
 * 将 Symbol 转换为单项线性多项式
 * Convert a Symbol to a single-term linear polynomial
 *
 * @return 线性多项式 / linear polynomial
*/
private fun Symbol.asLinearPoly(): LinearPolynomial<Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)

/** 符号等于线性多项式 / Symbol equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.EQ)

/** 符号小于等于线性多项式 / Symbol less-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.le(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LE)

/** 符号大于等于线性多项式 / Symbol greater-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ge(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GE)

/** 符号严格小于线性多项式 / Symbol strictly-less-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.lt(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LT)

/** 符号严格大于线性多项式 / Symbol strictly-greater-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gt(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GT)

/** 符号不等于线性多项式 / Symbol not-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ne(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.NE)

/** 符号小于等于线性多项式别名 / Symbol less-than-or-equal-to linear polynomial alias *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LE)

/** 符号大于等于线性多项式别名 / Symbol greater-than-or-equal-to linear polynomial alias *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GE)

/** 符号不等于线性多项式别名 / Symbol not-equal-to linear polynomial alias *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.NE)

/** 符号严格小于线性多项式别名 / Symbol strictly-less-than linear polynomial alias *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LT)

/** 符号严格大于线性多项式别名 / Symbol strictly-greater-than linear polynomial alias *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GT)

// LinearPolynomial<Flt64> vs Symbol
/** 线性多项式等于符号 / Linear polynomial equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.EQ)

/** 线性多项式小于等于符号 / Linear polynomial less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LE)

/** 线性多项式大于等于符号 / Linear polynomial greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GE)

/** 线性多项式严格小于符号 / Linear polynomial strictly-less-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LT)

/** 线性多项式严格大于符号 / Linear polynomial strictly-greater-than symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GT)

/** 线性多项式不等于符号 / Linear polynomial not-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.ne(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

/** 线性多项式小于等于符号别名 / Linear polynomial less-than-or-equal-to symbol alias *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LE)

/** 线性多项式大于等于符号别名 / Linear polynomial greater-than-or-equal-to symbol alias *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GE)

/** 线性多项式不等于符号别名 / Linear polynomial not-equal-to symbol alias *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun LinearPolynomial<Flt64>.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

// Symbol vs QuadraticPolynomial<Flt64>
/** 符号等于二次多项式 / Symbol equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)

/** 符号小于等于二次多项式 / Symbol less-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)

/** 符号大于等于二次多项式 / Symbol greater-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

/** 符号不等于二次多项式 / Symbol not-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

/** 符号小于等于二次多项式别名 / Symbol less-than-or-equal-to quadratic polynomial alias *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)

/** 符号大于等于二次多项式别名 / Symbol greater-than-or-equal-to quadratic polynomial alias *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

/** 符号不等于二次多项式别名 / Symbol not-equal-to quadratic polynomial alias *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun Symbol.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

// ========== Symbol vs Boolean ==========

/**
 * 将 Boolean 转换为 Flt64（true=1, false=0）
 * Convert Boolean to Flt64 (true=1, false=0)
 *
 * @return 对应的 Flt64 值 / the corresponding Flt64 value
*/
private fun Boolean.asFlt64(): Flt64 = if (this) Flt64.one else Flt64.zero

/** 符号等于布尔值 / Symbol equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.eq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)

/** 符号小于等于布尔值 / Symbol less-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.le(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)

/** 符号大于等于布尔值 / Symbol greater-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ge(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)

/** 符号严格小于布尔值 / Symbol strictly-less-than boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.lt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)

/** 符号严格大于布尔值 / Symbol strictly-greater-than boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

/** 符号不等于布尔值 / Symbol not-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ne(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

/** 符号小于等于布尔值别名 / Symbol less-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)

/** 符号大于等于布尔值别名 / Symbol greater-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)

/** 符号不等于布尔值别名 / Symbol not-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.neq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

/** 符号严格小于布尔值别名 / Symbol strictly-less-than boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.ls(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)

/** 符号严格大于布尔值别名 / Symbol strictly-greater-than boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.gr(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== AbstractVariableItem DSL ==========

/**
 * 将变量项转换为单项线性多项式
 * Convert a variable item to a single-term linear polynomial
 *
 * @return 线性多项式 / linear polynomial
*/
private fun AbstractVariableItem<*, *>.asSymbolPoly(): LinearPolynomial<Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this as Symbol)), Flt64.zero)

/** 变量项小于等于常量 / Variable item less-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)

/** 变量项大于等于常量 / Variable item greater-than-or-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)

/** 变量项等于常量 / Variable item equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.eq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.EQ)

/** 变量项不等于常量 / Variable item not-equal-to constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)

/** 变量项严格小于常量 / Variable item strictly-less-than constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ls(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)

/** 变量项严格大于常量 / Variable item strictly-greater-than constant *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gr(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)

/** 变量项小于等于常量（别名） / Variable item less-than-or-equal-to constant (alias) *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.le(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)

/** 变量项大于等于常量（别名） / Variable item greater-than-or-equal-to constant (alias) *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ge(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)

/** 变量项严格小于常量（别名） / Variable item strictly-less-than constant (alias) *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.lt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)

/** 变量项严格大于常量（别名） / Variable item strictly-greater-than constant (alias) *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)

/** 变量项不等于常量（别名） / Variable item not-equal-to constant (alias) *//**
 * @param rhs 右侧常量 / right-hand side constant
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ne(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)

/** 变量项小于等于变量项 / Variable item less-than-or-equal-to variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LE)

/** 变量项大于等于变量项 / Variable item greater-than-or-equal-to variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GE)

/** 变量项等于变量项 / Variable item equal-to variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.EQ)

/** 变量项不等于变量项 / Variable item not-equal-to variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.NE)

/** 变量项严格小于变量项 / Variable item strictly-less-than variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LT)

/** 变量项严格大于变量项 / Variable item strictly-greater-than variable item *//**
 * @param rhs 右侧变量项 / right-hand side variable item
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GT)

// AbstractVariableItem vs LinearPolynomial<Flt64>
/** 变量项小于等于线性多项式 / Variable item less-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.LE)

/** 变量项大于等于线性多项式 / Variable item greater-than-or-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.GE)

/** 变量项等于线性多项式 / Variable item equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.EQ)

/** 变量项不等于线性多项式 / Variable item not-equal-to linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.NE)

/** 变量项严格小于线性多项式 / Variable item strictly-less-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ls(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.LT)

/** 变量项严格大于线性多项式 / Variable item strictly-greater-than linear polynomial *//**
 * @param rhs 右侧线性多项式 / right-hand side linear polynomial
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gr(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.GT)

// AbstractVariableItem vs QuadraticPolynomial<Flt64>
/** 变量项小于等于二次多项式 / Variable item less-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)

/** 变量项大于等于二次多项式 / Variable item greater-than-or-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

/** 变量项等于二次多项式 / Variable item equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)

/** 变量项不等于二次多项式 / Variable item not-equal-to quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

/** 变量项严格小于二次多项式 / Variable item strictly-less-than quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> =
    QuadraticInequalityOf<Flt64>(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.LT)

/** 变量项严格大于二次多项式 / Variable item strictly-greater-than quadratic polynomial *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> =
    QuadraticInequalityOf<Flt64>(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.GT)

/** 变量项小于等于二次多项式（别名） / Variable item less-than-or-equal-to quadratic polynomial (alias) *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)

/** 变量项大于等于二次多项式（别名） / Variable item greater-than-or-equal-to quadratic polynomial (alias) *//**
 * @param rhs 右侧二次多项式 / right-hand side quadratic polynomial
 * @return 二次不等式 / quadratic inequality
*/
infix fun AbstractVariableItem<*, *>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequalityOf<Flt64> = QuadraticInequalityOf<Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

// AbstractVariableItem vs Boolean
/** 变量项等于布尔值 / Variable item equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.eq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)

/** 变量项小于等于布尔值 / Variable item less-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.le(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)

/** 变量项大于等于布尔值 / Variable item greater-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ge(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)

/** 变量项严格小于布尔值 / Variable item strictly-less-than boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.lt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)

/** 变量项严格大于布尔值 / Variable item strictly-greater-than boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

/** 变量项不等于布尔值 / Variable item not-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ne(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

/** 变量项小于等于布尔值别名 / Variable item less-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.leq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)

/** 变量项大于等于布尔值别名 / Variable item greater-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.geq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)

/** 变量项不等于布尔值别名 / Variable item not-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.neq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

/** 变量项严格小于布尔值别名 / Variable item strictly-less-than boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.ls(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)

/** 变量项严格大于布尔值别名 / Variable item strictly-greater-than boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 线性不等式 / linear inequality
*/
infix fun AbstractVariableItem<*, *>.gr(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== QuadraticIntermediateSymbol<Flt64> vs Boolean ==========

/** 二次中间符号等于布尔值 / Quadratic intermediate symbol equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticIntermediateSymbol<Flt64>.eq(rhs: Boolean): QuadraticInequalityOf<Flt64> =
    QuadraticInequalityOf<Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)

/** 二次中间符号小于等于布尔值 / Quadratic intermediate symbol less-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticIntermediateSymbol<Flt64>.le(rhs: Boolean): QuadraticInequalityOf<Flt64> =
    QuadraticInequalityOf<Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)

/** 二次中间符号大于等于布尔值 / Quadratic intermediate symbol greater-than-or-equal-to boolean *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticIntermediateSymbol<Flt64>.ge(rhs: Boolean): QuadraticInequalityOf<Flt64> =
    QuadraticInequalityOf<Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)

/** 二次中间符号小于等于布尔值别名 / Quadratic intermediate symbol less-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticIntermediateSymbol<Flt64>.leq(rhs: Boolean): QuadraticInequalityOf<Flt64> = this le rhs

/** 二次中间符号大于等于布尔值别名 / Quadratic intermediate symbol greater-than-or-equal-to boolean alias *//**
 * @param rhs 右侧布尔值 / right-hand side boolean value
 * @return 二次不等式 / quadratic inequality
*/
infix fun QuadraticIntermediateSymbol<Flt64>.geq(rhs: Boolean): QuadraticInequalityOf<Flt64> = this ge rhs

// ========== UInt comparison helpers ==========

/** 8位无符号整数大于等于比较 / UInt8 greater-than-or-equal comparison *//**
 * @param rhs 右侧8位无符号整数 / right-hand side UInt8
 * @return 比较结果 / comparison result
*/
infix fun UInt8.geq(rhs: UInt8): Boolean = this >= rhs

/** 64位无符号整数大于等于比较 / UInt64 greater-than-or-equal comparison *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun UInt64.geq(rhs: UInt64): Boolean = this >= rhs

/** 8位无符号整数等于比较 / UInt8 equal comparison *//**
 * @param rhs 右侧8位无符号整数 / right-hand side UInt8
 * @return 比较结果 / comparison result
*/
infix fun UInt8.eq(rhs: UInt8): Boolean = this == rhs

/** 64位无符号整数等于比较 / UInt64 equal comparison *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun UInt64.eq(rhs: UInt64): Boolean = this == rhs

/** 8位无符号整数不等于64位无符号整数 / UInt8 not-equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun UInt8.neq(rhs: UInt64): Boolean = this.toUInt64().toLong() != rhs.toLong()

/** 64位无符号整数不等于比较 / UInt64 not-equal comparison *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun UInt64.neq(rhs: UInt64): Boolean = this != rhs

/** 64位无符号整数等于浮点数 / UInt64 equal-to Flt64 *//**
 * @param rhs 右侧浮点数 / right-hand side Flt64
 * @return 比较结果 / comparison result
*/
infix fun UInt64.eq(rhs: Flt64): Boolean = this.toFlt64() == rhs

/** 64位无符号整数不等于浮点数 / UInt64 not-equal-to Flt64 *//**
 * @param rhs 右侧浮点数 / right-hand side Flt64
 * @return 比较结果 / comparison result
*/
infix fun UInt64.neq(rhs: Flt64): Boolean = this.toFlt64() != rhs

/** 浮点数等于64位无符号整数 / Flt64 equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun Flt64.eq(rhs: UInt64): Boolean = this == rhs.toFlt64()

/** 浮点数不等于64位无符号整数 / Flt64 not-equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 比较结果 / comparison result
*/
infix fun Flt64.neq(rhs: UInt64): Boolean = this != rhs.toFlt64()

// Symbol vs UInt64
/** 符号小于等于64位无符号整数 / Symbol less-than-or-equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.leq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.LE)

/** 符号大于等于64位无符号整数 / Symbol greater-than-or-equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.geq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.GE)

/** 符号等于64位无符号整数 / Symbol equal-to UInt64 *//**
 * @param rhs 右侧64位无符号整数 / right-hand side UInt64
 * @return 线性不等式 / linear inequality
*/
infix fun Symbol.eq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.EQ)

/** 64位无符号整数等于符号 / UInt64 equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun UInt64.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.EQ)

/** 64位无符号整数小于等于符号 / UInt64 less-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun UInt64.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.LE)

/** 64位无符号整数大于等于符号 / UInt64 greater-than-or-equal-to symbol *//**
 * @param rhs 右侧符号 / right-hand side symbol
 * @return 线性不等式 / linear inequality
*/
infix fun UInt64.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.GE)

// ========== LinearInequality to Constraint<Flt64, Quadratic> direct conversion ==========

/** 将 Flt64 线性不等式直接转换为二次约束实现 / Convert an Flt64 linear inequality directly to a quadratic constraint implementation */
internal fun <T> LinearInequality<Flt64>.toQuadraticConstraint(
    tokens: AbstractTokenTable<T>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): Ret<QuadraticConstraintImpl<Flt64>> where T : RealNumber<T>, T : NumberField<T> {
    return QuadraticConstraintImpl(
        relation = toQuadraticInequality().let { QuadraticRelationImpl(it.flattenData, it.comparison) },
        tokens = tokenTableAs<Flt64, T>(tokens),
        converter = IntoValue.Identity,
        lazy = lazy,
        name = name,
        origin = origin,
        from = from
    )
}

// ========== Relation-based constraint creation ==========

/** 从线性关系创建线性约束实现 / Create a linear constraint implementation from a linear relation */
internal fun <V> LinearRelation<V>.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): Ret<LinearConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
    return LinearConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

/** 从二次关系创建二次约束实现 / Create a quadratic constraint implementation from a quadratic relation */
internal fun <V> QuadraticRelation<V>.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): Ret<QuadraticConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

/** 将线性关系提升后创建二次约束实现 / Create a quadratic constraint implementation by promoting a linear relation */
internal fun <V> LinearRelation<V>.toQuadraticConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): Ret<QuadraticConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
    val normalized = normalize()
    val qMonomials = normalized.flattenData.monomials.map {
        QuadraticMonomial(it.coefficient, it.symbol, null)
    }
    val qFlattenData = QuadraticFlattenData<V>(qMonomials, normalized.flattenData.constant)
    val qRelation = QuadraticRelationImpl(qFlattenData, normalized.sign, normalized.name, normalized.displayName)
    return QuadraticConstraintImpl(qRelation, tokens, converter, lazy, name, origin, from)
}
