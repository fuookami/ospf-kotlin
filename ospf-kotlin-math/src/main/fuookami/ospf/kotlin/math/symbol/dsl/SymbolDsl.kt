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
import fuookami.ospf.kotlin.math.symbol.serde.legacyToCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.serde.legacyToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.legacyToLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.serde.legacyToLinearPolynomialOrNull
import fuookami.ospf.kotlin.math.symbol.serde.legacyToQuadraticInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.serde.legacyToQuadraticPolynomialOrNull

/**
 * 符号 DSL 作用域
 * Symbol DSL Scope
 *
 * 提供 DSL 构建表达式的作用域环境，包含创建数字、符号和函数调用的辅助方法。
 * Provides the scope environment for building expressions with DSL,
 * containing helper methods for creating numbers, symbols, and function calls.
 */
class SymbolDslScope {
    /**
     * 创建数字字面量表达式
     * Creates a number literal expression
     *
     * @param value 数值 / The numeric value
     * @return 数字字面量表达式 / Number literal expression
     */
    fun num(value: Number): Expr.NumberLiteral {
        return Expr.NumberLiteral(value.toString())
    }

    /**
     * 创建符号标识符表达式
     * Creates a symbol identifier expression
     *
     * @param name 符号名称 / The symbol name
     * @return 标识符表达式 / Identifier expression
     */
    fun symbol(name: String): Expr.Identifier {
        return Expr.Identifier(name)
    }

    /**
     * 创建函数调用表达式
     * Creates a function call expression
     *
     * @param name 函数名称 / The function name
     * @param arguments 函数参数列表 / The function arguments
     * @return 函数调用表达式 / Function call expression
     */
    fun call(
        name: String,
        vararg arguments: Expr
    ): Expr.FunctionCall {
        return Expr.FunctionCall(name = name, arguments = arguments.toList())
    }
}

/**
 * 使用 DSL 作用域构建符号表达式
 * Builds a symbolic expression using DSL scope
 *
 * @param block DSL 构建块 / The DSL building block
 * @return 构建的符号表达式 / The built symbolic expression
 */
fun legacySymbolExpr(block: SymbolDslScope.() -> Expr): Expr {
    return SymbolDslScope().block()
}

@Deprecated(
    message = "symbolExpr is the legacy Expr-based DSL entry. Prefer legacySymbolExpr for explicit legacy usage or symbol.expression.dsl for the new expression stack.",
    replaceWith = ReplaceWith("legacySymbolExpr(block)")
)
fun symbolExpr(block: SymbolDslScope.() -> Expr): Expr {
    return legacySymbolExpr(block)
}

/**
 * 表达式加法运算符
 * Expression addition operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 加法表达式 / Addition expression
 */
operator fun Expr.plus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Add, rhs)
}

/**
 * 表达式减法运算符
 * Expression subtraction operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 减法表达式 / Subtraction expression
 */
operator fun Expr.minus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Subtract, rhs)
}

/**
 * 表达式乘法运算符
 * Expression multiplication operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 乘法表达式 / Multiplication expression
 */
operator fun Expr.times(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Multiply, rhs)
}

/**
 * 表达式一元负运算符
 * Expression unary negation operator
 *
 * @receiver 表达式 / The expression
 * @return 负数表达式 / Negated expression
 */
operator fun Expr.unaryMinus(): Expr {
    return Expr.UnaryMinus(this)
}

/**
 * 表达式幂运算符
 * Expression power operator
 *
 * @receiver 基数表达式 / Base expression
 * @param exponent 指数 / The exponent
 * @return 幂表达式 / Power expression
 */
infix fun Expr.pow(exponent: Int): Expr {
    return Expr.Binary(
        left = this,
        operator = BinaryOperator.Power,
        right = Expr.NumberLiteral(exponent.toString())
    )
}

/**
 * 小于比较运算符
 * Less than comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
infix fun Expr.lt(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Less, rhs)
}

/**
 * 小于等于比较运算符
 * Less than or equal comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
infix fun Expr.le(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.LessEqual, rhs)
}

/**
 * 等于比较运算符
 * Equal comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
infix fun Expr.eq(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Equal, rhs)
}

/**
 * 不等于比较运算符
 * Not equal comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
infix fun Expr.ne(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.NotEqual, rhs)
}

/**
 * 大于等于比较运算符
 * Greater than or equal comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
infix fun Expr.ge(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.GreaterEqual, rhs)
}

/**
 * 大于比较运算符
 * Greater than comparison operator
 *
 * @receiver 左侧表达式 / Left expression
 * @param rhs 右侧表达式 / Right expression
 * @return 比较表达式 / Comparison expression
 */
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
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param block DSL 构建块 / The DSL building block
 * @return LinearPolynomial 或 null（如果表达式不是线性的）/ LinearPolynomial or null (if expression is not linear)
 */
fun linearPolynomial(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr
): LinearPolynomial<Flt64>? {
    return legacySymbolExpr(block).legacyToLinearPolynomialOrNull(symbolOf)
}

/**
 * 从 DSL 块直接构建 QuadraticPolynomial
 * Build QuadraticPolynomial directly from DSL block
 *
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param symbolComparator 符号排序比较器（可选）/ Symbol ordering comparator (optional)
 * @param block DSL 构建块 / The DSL building block
 * @return QuadraticPolynomial 或 null（如果表达式不是二次的）/ QuadraticPolynomial or null (if expression is not quadratic)
 */
fun quadraticPolynomial(
    symbolOf: (String) -> Symbol,
    symbolComparator: Comparator<Symbol>? = null,
    block: SymbolDslScope.() -> Expr
): QuadraticPolynomial<Flt64>? {
    return legacySymbolExpr(block).legacyToQuadraticPolynomialOrNull(symbolOf, symbolComparator)
}

/**
 * 从 DSL 块直接构建 CanonicalPolynomial
 * Build CanonicalPolynomial directly from DSL block
 *
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param block DSL 构建块 / The DSL building block
 * @return CanonicalPolynomial 实例 / CanonicalPolynomial instance
 */
fun canonicalPolynomial(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr
): CanonicalPolynomial<Flt64> {
    return legacySymbolExpr(block).legacyToCanonicalPolynomial(symbolOf)
}

/**
 * 从 DSL 块直接构建 LinearInequality
 * Build LinearInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param block DSL 构建块，必须返回比较表达式 / The DSL building block that must return a comparison expression
 * @return LinearInequality 或 null（如果表达式不是线性不等式）/ LinearInequality or null (if expression is not linear inequality)
 */
fun linearInequality(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr.Comparison
): LinearInequality<Flt64>? {
    return (legacySymbolExpr(block) as Expr.Comparison).legacyToLinearInequalityOrNull(symbolOf)
}

/**
 * 从 DSL 块直接构建 QuadraticInequality
 * Build QuadraticInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param symbolComparator 符号排序比较器（可选）/ Symbol ordering comparator (optional)
 * @param block DSL 构建块，必须返回比较表达式 / The DSL building block that must return a comparison expression
 * @return QuadraticInequality 或 null（如果表达式不是二次不等式）/ QuadraticInequality or null (if expression is not quadratic inequality)
 */
fun quadraticInequality(
    symbolOf: (String) -> Symbol,
    symbolComparator: Comparator<Symbol>? = null,
    block: SymbolDslScope.() -> Expr.Comparison
): QuadraticInequality? {
    return (legacySymbolExpr(block) as Expr.Comparison).legacyToQuadraticInequalityOrNull(symbolOf, symbolComparator)
}

/**
 * 从 DSL 块直接构建 CanonicalInequality
 * Build CanonicalInequality directly from DSL block
 *
 * @param symbolOf 符号查找函数，将符号名称映射到符号对象 / Symbol lookup function that maps symbol names to symbol objects
 * @param block DSL 构建块，必须返回比较表达式 / The DSL building block that must return a comparison expression
 * @return CanonicalInequality 实例 / CanonicalInequality instance
 */
fun canonicalInequality(
    symbolOf: (String) -> Symbol,
    block: SymbolDslScope.() -> Expr.Comparison
): CanonicalInequality {
    return (legacySymbolExpr(block) as Expr.Comparison).legacyToCanonicalInequality(symbolOf)
}
