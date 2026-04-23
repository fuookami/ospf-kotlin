/**
 * 旧表达式桥接
 * Legacy Expression Bridge
 *
 * 提供旧 `symbol.parser.Expr` 与新 `ScalarExpression/BooleanExpression` 的双向转换。
 * Provides bidirectional conversion between legacy `symbol.parser.Expr` and
 * new `ScalarExpression/BooleanExpression`.
 *
 * 注意：仅转换公共子集，不支持新表达式的完整特性（如逻辑运算、In、PatternMatch）。
 * Note: Only converts common subset, does not support full features of new expressions
 * (e.g., logical operations, In, PatternMatch).
 */
package fuookami.ospf.kotlin.math.symbol.expression.adapter

import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parser.BinaryOperator as LegacyBinaryOperator
import fuookami.ospf.kotlin.math.symbol.parser.ComparisonOperator as LegacyComparisonOperator
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.stableIdOrNull

// ========== 旧 Expr -> 新 Expression 转换 / Legacy Expr -> New Expression Conversion ==========

/**
 * 将旧 Expr 转换为新的 ScalarExpression
 * Convert legacy Expr to new ScalarExpression
 *
 * @return 转换后的 ScalarExpression，如果无法转换则返回 null
 */
fun Expr.toScalarExpressionOrNull(): ScalarExpression<*>? = when (this) {
    is Expr.NumberLiteral -> {
        // 尝试解析数字 / Try to parse number
        val value: Any? = text.toDoubleOrNull()
            ?: text.toLongOrNull()
            ?: text.toIntOrNull()
            ?: text
        ScalarConstant(value)
    }

    is Expr.Identifier -> {
        val path = PropertyPath.parseOrNull(name)
        if (path != null) {
            ScalarReference<Any>(path)
        } else {
            ScalarSymbolReference(symbolOfSerializedIdentifier(name))
        }
    }

    is Expr.UnaryMinus -> {
        val operand = operand.toScalarExpressionOrNull()
        if (operand != null) {
            ScalarUnary(UnaryOperator.Negate, operand)
        } else {
            null
        }
    }

    is Expr.Binary -> {
        val left = left.toScalarExpressionOrNull()
        val right = right.toScalarExpressionOrNull()
        if (left != null && right != null) {
            ScalarBinary(operator.toNewBinaryOperator(), left, right)
        } else {
            null
        }
    }

    is Expr.FunctionCall -> {
        val args = arguments.mapNotNull { it.toScalarExpressionOrNull() }
        if (args.size == arguments.size) {
            ScalarFunction(name, args)
        } else {
            null
        }
    }

    is Expr.Comparison -> null  // Comparison 返回 BooleanExpression，不是 ScalarExpression
}

/**
 * 将旧 Expr 转换为新的 BooleanExpression
 * Convert legacy Expr to new BooleanExpression
 *
 * @return 转换后的 BooleanExpression，如果无法转换则返回 null
 */
fun Expr.toBooleanExpressionOrNull(): BooleanExpression? = when (this) {
    is Expr.Comparison -> {
        val left = left.toScalarExpressionOrNull()
        val right = right.toScalarExpressionOrNull()
        if (left != null && right != null) {
            Comparison(operator.toNewComparisonOperator(), left, right)
        } else {
            null
        }
    }

    // 其他 Expr 类型无法转换为 BooleanExpression
    // Other Expr types cannot be converted to BooleanExpression
    else -> null
}

/**
 * 将旧 BinaryOperator 转换为新的 BinaryOperator
 * Convert legacy BinaryOperator to new BinaryOperator
 */
private fun LegacyBinaryOperator.toNewBinaryOperator(): BinaryOperator = when (this) {
    LegacyBinaryOperator.Add -> BinaryOperator.Add
    LegacyBinaryOperator.Subtract -> BinaryOperator.Subtract
    LegacyBinaryOperator.Multiply -> BinaryOperator.Multiply
    LegacyBinaryOperator.Power -> BinaryOperator.Power
}

/**
 * 将旧 ComparisonOperator 转换为新的 ComparisonOperator
 * Convert legacy ComparisonOperator to new ComparisonOperator
 */
private fun LegacyComparisonOperator.toNewComparisonOperator(): ComparisonOperator = when (this) {
    LegacyComparisonOperator.Less -> ComparisonOperator.Lt
    LegacyComparisonOperator.LessEqual -> ComparisonOperator.Le
    LegacyComparisonOperator.Equal -> ComparisonOperator.Eq
    LegacyComparisonOperator.NotEqual -> ComparisonOperator.Ne
    LegacyComparisonOperator.GreaterEqual -> ComparisonOperator.Ge
    LegacyComparisonOperator.Greater -> ComparisonOperator.Gt
}

// ========== 新 Expression -> 旧 Expr 转换 / New Expression -> Legacy Expr Conversion ==========

/**
 * 将新 ScalarExpression 转换为旧的 Expr
 * Convert new ScalarExpression to legacy Expr
 *
 * @return 转换后的 Expr，如果不在公共子集内则返回 null
 */
fun ScalarExpression<*>.toLegacyExprOrNull(): Expr? = when (this) {
    is ScalarConstant<*> -> {
        val text = when (val v = value) {
            is Byte, is Short, is Int, is Long, is Float, is Double -> v.toString()
            else -> return null
        }
        Expr.NumberLiteral(text)
    }

    is ScalarReference<*> -> Expr.Identifier(path.value)

    is ScalarSymbolReference<*> -> Expr.Identifier(symbol.stableIdOrNull()?.value ?: symbol.name)

    is ScalarUnary<*> -> {
        if (operator == UnaryOperator.Negate) {
            val operand = operand.toLegacyExprOrNull()
            if (operand != null) {
                Expr.UnaryMinus(operand)
            } else {
                null
            }
        } else {
            null  // 旧 Expr 只支持 UnaryMinus / Legacy Expr only supports UnaryMinus
        }
    }

    is ScalarBinary<*> -> {
        val left = left.toLegacyExprOrNull()
        val right = right.toLegacyExprOrNull()
        val legacyOp = operator.toLegacyBinaryOperatorOrNull()
        if (left != null && right != null && legacyOp != null) {
            Expr.Binary(left, legacyOp, right)
        } else {
            null
        }
    }

    is ScalarFunction<*> -> {
        val args = arguments.mapNotNull { it.toLegacyExprOrNull() }
        if (args.size == arguments.size) {
            Expr.FunctionCall(name, args)
        } else {
            null
        }
    }

    is ScalarCustom<*> -> null
}

/**
 * 将新 BooleanExpression 转换为旧的 Expr
 * Convert new BooleanExpression to legacy Expr
 *
 * @return 转换后的 Expr，如果不在公共子集内则返回 null
 */
fun BooleanExpression.toLegacyExprOrNull(): Expr? = when (this) {
    is Comparison<*> -> {
        val left = left.toLegacyExprOrNull()
        val right = right.toLegacyExprOrNull()
        val legacyOp = operator.toLegacyComparisonOperator()
        if (left != null && right != null) {
            Expr.Comparison(left, legacyOp, right)
        } else {
            null
        }
    }

    // 新 BooleanExpression 的其他类型不在旧 Expr 公共子集内
    // Other BooleanExpression types are not in legacy Expr common subset
    else -> null
}

/**
 * 将新 BinaryOperator 转换为旧的 BinaryOperator
 * Convert new BinaryOperator to legacy BinaryOperator
 */
private fun BinaryOperator.toLegacyBinaryOperatorOrNull(): LegacyBinaryOperator? = when (this) {
    BinaryOperator.Add -> LegacyBinaryOperator.Add
    BinaryOperator.Subtract -> LegacyBinaryOperator.Subtract
    BinaryOperator.Multiply -> LegacyBinaryOperator.Multiply
    BinaryOperator.Power -> LegacyBinaryOperator.Power
    else -> null  // Divide, Modulo 不在旧 Expr 子集内 / Divide, Modulo not in legacy subset
}

/**
 * 将新 ComparisonOperator 转换为旧的 ComparisonOperator
 * Convert new ComparisonOperator to legacy ComparisonOperator
 */
private fun ComparisonOperator.toLegacyComparisonOperator(): LegacyComparisonOperator = when (this) {
    ComparisonOperator.Lt -> LegacyComparisonOperator.Less
    ComparisonOperator.Le -> LegacyComparisonOperator.LessEqual
    ComparisonOperator.Eq -> LegacyComparisonOperator.Equal
    ComparisonOperator.Ne -> LegacyComparisonOperator.NotEqual
    ComparisonOperator.Ge -> LegacyComparisonOperator.GreaterEqual
    ComparisonOperator.Gt -> LegacyComparisonOperator.Greater
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 将旧 Expr 转换为新的 ScalarExpression（抛出异常如果失败）
 * Convert legacy Expr to new ScalarExpression (throws if fails)
 */
fun Expr.toScalarExpression(): ScalarExpression<*> =
    toScalarExpressionOrNull() ?: throw IllegalArgumentException("Cannot convert Expr to ScalarExpression: $this")

/**
 * 将旧 Expr 转换为新的 BooleanExpression（抛出异常如果失败）
 * Convert legacy Expr to new BooleanExpression (throws if fails)
 */
fun Expr.toBooleanExpression(): BooleanExpression =
    toBooleanExpressionOrNull() ?: throw IllegalArgumentException("Cannot convert Expr to BooleanExpression: $this")

/**
 * 将新 ScalarExpression 转换为旧的 Expr（抛出异常如果失败）
 * Convert new ScalarExpression to legacy Expr (throws if fails)
 */
fun ScalarExpression<*>.toLegacyExpr(): Expr =
    toLegacyExprOrNull() ?: throw IllegalArgumentException("Cannot convert ScalarExpression to Expr: $this")

/**
 * 将新 BooleanExpression 转换为旧的 Expr（抛出异常如果失败）
 * Convert new BooleanExpression to legacy Expr (throws if fails)
 */
fun BooleanExpression.toLegacyExpr(): Expr =
    toLegacyExprOrNull() ?: throw IllegalArgumentException("Cannot convert BooleanExpression to Expr: $this")
