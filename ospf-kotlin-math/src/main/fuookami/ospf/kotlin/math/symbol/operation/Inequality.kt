@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * Flt64 不等式 DSL 和求值
 * Flt64 Inequality DSL and Evaluation
 *
 * 提供 Flt64 不等式的符号级 DSL（lt/le/eq/ne/ge/gt）和满足性判断。
 * Supports symbol-level comparison DSL and satisfaction checking for Flt64 inequalities.
*/

// ========== Comparison.satisfiedBy ==========

/**
 * 判断 Flt64 值是否满足比较关系
 * Check if Flt64 values satisfy the comparison relation
 *
 * @param lhs 左侧操作数 / Left-hand operand
 * @param rhs 右侧操作数 / Right-hand operand
 * @return 是否满足比较关系 / Whether the comparison is satisfied
*/
fun Comparison.satisfiedBy(lhs: Flt64, rhs: Flt64): Boolean {
    return when (this) {
        Comparison.LT -> lhs < rhs
        Comparison.LE -> lhs <= rhs
        Comparison.EQ -> lhs == rhs
        Comparison.NE -> lhs != rhs
        Comparison.GE -> lhs >= rhs
        Comparison.GT -> lhs > rhs
    }
}

// ========== Symbol-level DSL ==========
// 符号级不等式 DSL 运算符重载 / Symbol-level inequality DSL operator overloads

/**
 * Converts a symbol to a linear polynomial with coefficient one.
 * 将符号转换为系数为 1 的线性多项式。
 *
 * @return the linear polynomial representation of this symbol / 此符号的线性多项式表示
*/
private fun Symbol.asLinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)
}

/**
 * Converts a Flt64 value to a constant linear polynomial.
 * 将 Flt64 值转换为常数线性多项式。
 *
 * @return the constant linear polynomial representation of this value / 此值的常数线性多项式表示
*/
private fun Flt64.asLinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), this)
}

/**
 * Creates a less-than linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的小于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this < rhs / 表示 this < rhs 的线性不等式
*/
infix fun Symbol.lt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)

/**
 * Creates a less-than-or-equal linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的小于等于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this <= rhs / 表示 this <= rhs 的线性不等式
*/
infix fun Symbol.le(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)

/**
 * Creates an equal linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的等于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this == rhs / 表示 this == rhs 的线性不等式
*/
infix fun Symbol.eq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)

/**
 * Creates a not-equal linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的不等于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this != rhs / 表示 this != rhs 的线性不等式
*/
infix fun Symbol.ne(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)

/**
 * Creates a greater-than-or-equal linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的大于等于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this >= rhs / 表示 this >= rhs 的线性不等式
*/
infix fun Symbol.ge(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)

/**
 * Creates a greater-than linear inequality between a symbol and a Flt64 value.
 * 创建符号与 Flt64 值之间的大于线性不等式。
 *
 * @param rhs the right-hand side Flt64 value / 右侧 Flt64 值
 * @return the linear inequality representing this > rhs / 表示 this > rhs 的线性不等式
*/
infix fun Symbol.gt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

/**
 * Creates a less-than linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的小于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this < rhs / 表示 this < rhs 的线性不等式
*/
infix fun Flt64.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)

/**
 * Creates a less-than-or-equal linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的小于等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this <= rhs / 表示 this <= rhs 的线性不等式
*/
infix fun Flt64.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)

/**
 * Creates an equal linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this == rhs / 表示 this == rhs 的线性不等式
*/
infix fun Flt64.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)

/**
 * Creates a not-equal linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的不等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this != rhs / 表示 this != rhs 的线性不等式
*/
infix fun Flt64.ne(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)

/**
 * Creates a greater-than-or-equal linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的大于等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this >= rhs / 表示 this >= rhs 的线性不等式
*/
infix fun Flt64.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)

/**
 * Creates a greater-than linear inequality between a Flt64 value and a symbol.
 * 创建 Flt64 值与符号之间的大于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this > rhs / 表示 this > rhs 的线性不等式
*/
infix fun Flt64.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

/**
 * Creates a less-than linear inequality between two symbols.
 * 创建两个符号之间的小于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this < rhs / 表示 this < rhs 的线性不等式
*/
infix fun Symbol.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)

/**
 * Creates a less-than-or-equal linear inequality between two symbols.
 * 创建两个符号之间的小于等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this <= rhs / 表示 this <= rhs 的线性不等式
*/
infix fun Symbol.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)

/**
 * Creates an equal linear inequality between two symbols.
 * 创建两个符号之间的等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this == rhs / 表示 this == rhs 的线性不等式
*/
infix fun Symbol.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)

/**
 * Creates a not-equal linear inequality between two symbols.
 * 创建两个符号之间的不等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this != rhs / 表示 this != rhs 的线性不等式
*/
infix fun Symbol.ne(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)

/**
 * Creates a greater-than-or-equal linear inequality between two symbols.
 * 创建两个符号之间的大于等于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this >= rhs / 表示 this >= rhs 的线性不等式
*/
infix fun Symbol.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)

/**
 * Creates a greater-than linear inequality between two symbols.
 * 创建两个符号之间的大于线性不等式。
 *
 * @param rhs the right-hand side symbol / 右侧符号
 * @return the linear inequality representing this > rhs / 表示 this > rhs 的线性不等式
*/
infix fun Symbol.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

// ========== isSatisfied ==========
// 不等式满足性判断 / Inequality satisfaction checking

/**
 * 判断 Flt64 线性不等式是否被给定值满足
 * Check if a Flt64 linear inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
*/
fun LinearInequality<Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 线性不等式是否被有序值满足
 * Check if a Flt64 linear inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
*/
fun LinearInequality<Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Ret<Boolean> {
    val lhsValue = when (val result = lhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val rhsValue = when (val result = rhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return Ok(comparison.satisfiedBy(lhsValue, rhsValue))
}

/**
 * 判断 Flt64 二次不等式是否被给定值满足
 * Check if a Flt64 quadratic inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
*/
fun QuadraticInequalityOf<Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 二次不等式是否被有序值满足
 * Check if a Flt64 quadratic inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
*/
fun QuadraticInequalityOf<Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Ret<Boolean> {
    val lhsValue = when (val result = lhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val rhsValue = when (val result = rhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return Ok(comparison.satisfiedBy(lhsValue, rhsValue))
}

/**
 * 判断 Flt64 规范不等式是否被给定值满足
 * Check if a Flt64 canonical inequality is satisfied by given values
 *
 * @param values 符号到值的映射 / Symbol-to-value mapping
 * @return 是否满足，若缺少值则返回 null / Whether satisfied, or null if values are missing
*/
fun CanonicalInequality<Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

/**
 * 判断 Flt64 规范不等式是否被有序值满足
 * Check if a Flt64 canonical inequality is satisfied by ordered values
 *
 * @param order 符号顺序 / Symbol order
 * @param values 对应值列表 / Corresponding value list
 * @return 是否满足 / Whether satisfied
*/
fun CanonicalInequality<Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<Flt64>): Ret<Boolean> {
    val lhsValue = when (val result = lhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val rhsValue = when (val result = rhs.evaluateOrdered(order, values)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return Ok(comparison.satisfiedBy(lhsValue, rhsValue))
}
