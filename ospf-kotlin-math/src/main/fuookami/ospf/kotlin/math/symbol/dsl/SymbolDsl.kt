/**
 * 符号 DSL
 * Symbol DSL
 *
 * 提供用于构建符号表达式的领域特定语言（DSL）。
 * 支持使用中缀运算符构建线性、二次和规范多项式及不等式。
 * Provides a domain-specific language (DSL) for building symbolic expressions.
 * Supports building linear, quadratic, and canonical polynomials and inequalities
 * using infix operators.
 */
package fuookami.ospf.kotlin.math.symbol.dsl

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.parser.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.parser.ComparisonOperator
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.toLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.serde.toLinearPolynomialOrNull
import fuookami.ospf.kotlin.math.symbol.serde.toQuadraticInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.serde.toQuadraticPolynomialOrNull

class SymbolDslScope {
    fun num(value: Number): Expr.NumberLiteral {
        return Expr.NumberLiteral(value.toString())
    }

    fun symbol(name: String): Expr.Identifier {
        return Expr.Identifier(name)
    }

    fun call(
        name: String,
        vararg arguments: Expr
    ): Expr.FunctionCall {
        return Expr.FunctionCall(name = name, arguments = arguments.toList())
    }
}

fun symbolExpr(block: SymbolDslScope.() -> Expr): Expr {
    return SymbolDslScope().block()
}

operator fun Expr.plus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Add, rhs)
}

operator fun Expr.minus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Subtract, rhs)
}

operator fun Expr.times(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Multiply, rhs)
}

operator fun Expr.unaryMinus(): Expr {
    return Expr.UnaryMinus(this)
}

infix fun Expr.pow(exponent: Int): Expr {
    return Expr.Binary(
        left = this,
        operator = BinaryOperator.Power,
        right = Expr.NumberLiteral(exponent.toString())
    )
}

infix fun Expr.lt(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Less, rhs)
}

infix fun Expr.le(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.LessEqual, rhs)
}

infix fun Expr.eq(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Equal, rhs)
}

infix fun Expr.ne(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.NotEqual, rhs)
}

infix fun Expr.ge(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.GreaterEqual, rhs)
}

infix fun Expr.gt(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Greater, rhs)
}

// ============================================================================
// DSL 快捷转换入口 / DSL shortcut conversion entry points
// ============================================================================

/**
 * 从 DSL 块直接构建 LinearPolynomial
 * Build LinearPolynomial directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @return LinearPolynomial 或 null（如果表达式不是线性的）/ LinearPolynomial or null (if expression is not linear)
 */
fun linearPolynomial(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr
): LinearPolynomial<Flt64>? {
    return symbolExpr(block).toLinearPolynomialOrNull(symbolOf)
}

/**
 * 从 DSL 块直接构建 QuadraticPolynomial
 * Build QuadraticPolynomial directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @param symbolComparator 符号排序比较器（可选）/ Symbol ordering comparator (optional)
 * @return QuadraticPolynomial 或 null（如果表达式不是二次的）/ QuadraticPolynomial or null (if expression is not quadratic)
 */
fun quadraticPolynomial(
    symbolOf: (String) -> Symbol,
    symbolComparator: Comparator<Symbol>? = null,
    block: SymbolDslScope.() -> Expr
): QuadraticPolynomial<Flt64>? {
    return symbolExpr(block).toQuadraticPolynomialOrNull(symbolOf, symbolComparator)
}

/**
 * 从 DSL 块直接构建 CanonicalPolynomial
 * Build CanonicalPolynomial directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @return CanonicalPolynomial
 */
fun canonicalPolynomial(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr
): CanonicalPolynomial<Flt64> {
    return symbolExpr(block).toCanonicalPolynomial(symbolOf)
}

/**
 * 从 DSL 块直接构建 LinearInequality
 * Build LinearInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @return LinearInequality 或 null（如果表达式不是线性不等式）/ LinearInequality or null (if expression is not linear inequality)
 */
fun linearInequality(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr.Comparison
): LinearInequality? {
    return (symbolExpr(block) as Expr.Comparison).toLinearInequalityOrNull(symbolOf)
}

/**
 * 从 DSL 块直接构建 QuadraticInequality
 * Build QuadraticInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @param symbolComparator 符号排序比较器（可选）/ Symbol ordering comparator (optional)
 * @return QuadraticInequality 或 null（如果表达式不是二次不等式）/ QuadraticInequality or null (if expression is not quadratic inequality)
 */
fun quadraticInequality(
    symbolOf: (String) -> Symbol,
    symbolComparator: Comparator<Symbol>? = null,
    block: SymbolDslScope.() -> Expr.Comparison
): QuadraticInequality? {
    return (symbolExpr(block) as Expr.Comparison).toQuadraticInequalityOrNull(symbolOf, symbolComparator)
}

/**
 * 从 DSL 块直接构建 CanonicalInequality
 * Build CanonicalInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数 / Symbol lookup function
 * @return CanonicalInequality
 */
fun canonicalInequality(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr.Comparison
): CanonicalInequality {
    return (symbolExpr(block) as Expr.Comparison).toCanonicalInequality(symbolOf)
}

