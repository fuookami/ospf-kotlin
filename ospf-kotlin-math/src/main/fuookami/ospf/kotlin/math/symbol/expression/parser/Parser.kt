/**
 * 布尔表达式解析器
 * Boolean Expression Parser
 *
 * 将布尔表达式字符串解析为 BooleanExpression AST�?
 * Parses boolean expression strings into BooleanExpression AST.
 *
 * 支持的语法：
 * Supported syntax:
 * - 逻辑操作: and, or, not / Logical operations: and, or, not
 * - 比较操作: =, <>, !=, <, <=, >, >= / Comparison operations
 * - 集合成员: in (...), not in (...) / Set membership
 * - 空值检�? is null, is not null / Null checks
 * - 分组: ( ... ) / Grouping
 *
 * 优先级（从高到低）：
 * Precedence (high to low):
 * 1. not, 括号 / not, parentheses
 * 2. and
 * 3. or
 */
package fuookami.ospf.kotlin.math.symbol.expression.parser

import fuookami.ospf.kotlin.math.symbol.expression.*

/**
 * 解析异常
 * Parse Exception
 */
class ParseException(message: String, val position: Int = -1) : Exception(message)

/**
 * 布尔表达式解析器
 * Boolean Expression Parser
 */
class Parser(private val tokens: List<Token>) {
    private var position = 0

    /**
     * 解析布尔表达�?
     * Parse boolean expression
     */
    fun parse(): BooleanExpression {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens[0].type == TokenType.EOF)) {
            throw ParseException("Empty expression")
        }

        val expr = parseOrExpression()

        if (currentToken().type != TokenType.EOF) {
            throw ParseException("Unexpected token: ${currentToken().value}", currentToken().position)
        }

        return expr
    }

    // ========== 解析方法 / Parse Methods ==========

    /**
     * 解析 or 表达式（最低优先级�?
     * Parse or expression (lowest precedence)
     */
    private fun parseOrExpression(): BooleanExpression {
        var left = parseAndExpression()

        while (currentToken().type == TokenType.OR) {
            advance()
            val right = parseAndExpression()
            left = mergeOr(left, right)
        }

        return left
    }

    /**
     * 解析 and 表达�?
     * Parse and expression
     */
    private fun parseAndExpression(): BooleanExpression {
        var left = parseNotExpression()

        while (currentToken().type == TokenType.AND) {
            advance()
            val right = parseNotExpression()
            left = mergeAnd(left, right)
        }

        return left
    }

    /**
     * 解析 not 表达�?
     * Parse not expression
     */
    private fun parseNotExpression(): BooleanExpression {
        if (currentToken().type == TokenType.NOT) {
            advance()
            val operand = parseNotExpression()
            return NotExpression(operand)
        }

        return parsePrimaryExpression()
    }

    /**
     * 解析基本表达�?
     * Parse primary expression
     */
    private fun parsePrimaryExpression(): BooleanExpression {
        return when (currentToken().type) {
            // 括号分组 / Parentheses grouping
            TokenType.LPAREN -> {
                advance()
                val expr = parseOrExpression()
                expect(TokenType.RPAREN, "Expected ')'")
                expr
            }

            // 布尔常量 / Boolean constants
            TokenType.TRUE -> {
                advance()
                BooleanConstant(fuookami.ospf.kotlin.math.Trivalent.True)
            }
            TokenType.FALSE -> {
                advance()
                BooleanConstant(fuookami.ospf.kotlin.math.Trivalent.False)
            }

            // 标识符或路径（可能是比较、in、is null 等）
            // Identifier or path (may be comparison, in, is null, etc.)
            TokenType.IDENTIFIER -> parsePathExpression()

            else -> throw ParseException(
                "Unexpected token: ${currentToken().value}",
                currentToken().position
            )
        }
    }

    /**
     * 解析路径表达式（标识符开头的表达式）
     * Parse path expression (expression starting with identifier)
     */
    private fun parsePathExpression(): BooleanExpression {
        val path = parsePath()

        // 检查是否是 is null / is not null
        // Check if it's is null / is not null
        if (currentToken().type == TokenType.IS) {
            return parseNullCheck(path)
        }

        // 检查是否是 not in
        // Check if it's not in
        if (currentToken().type == TokenType.NOT) {
            advance()
            if (currentToken().type == TokenType.IN) {
                advance()
                return parseInExpression(path, negated = true)
            }
            if (currentToken().isPatternOperator()) {
                val mode = currentToken().type.toPatternMatchMode()
                    ?: throw ParseException("Expected pattern operator after 'not'", currentToken().position)
                advance()
                return parsePatternMatch(path, mode, negated = true)
            }
            throw ParseException(
                "Expected 'in' or pattern operator after 'not'",
                currentToken().position
            )
        }

        // 检查是否是 in
        // Check if it's in
        if (currentToken().type == TokenType.IN) {
            advance()
            return parseInExpression(path, negated = false)
        }

        // 检查是否是 pattern match
        // Check if it's pattern match
        if (currentToken().isPatternOperator()) {
            val mode = currentToken().type.toPatternMatchMode()
                ?: throw ParseException("Expected pattern operator", currentToken().position)
            advance()
            return parsePatternMatch(path, mode, negated = false)
        }

        // 检查是否是比较操作
        // Check if it's a comparison operation
        if (currentToken().isComparisonOperator()) {
            return parseComparison(path)
        }

        // 如果没有操作符，将路径作为布尔值引�?
        // If no operator, treat path as boolean reference
        throw ParseException(
            "Expected comparison operator, 'in', 'is' after '$path'",
            currentToken().position
        )
    }

    /**
     * 解析空值检�?
     * Parse null check
     */
    private fun parseNullCheck(path: PropertyPath): NullCheck {
        advance() // 跳过 'is' / Skip 'is'

        val notNull = if (currentToken().type == TokenType.NOT) {
            advance()
            true
        } else {
            false
        }

        expect(TokenType.NULL, "Expected 'null' after 'is'")

        return NullCheck(
            path,
            if (notNull) NullCheckType.IsNotNull else NullCheckType.IsNull
        )
    }

    /**
     * 解析集合成员判断
     * Parse set membership (in) expression
     */
    private fun parseInExpression(path: PropertyPath, negated: Boolean): InExpression<Any?> {
        expect(TokenType.LPAREN, "Expected '(' after 'in'")

        val candidates = mutableListOf<ScalarExpression<Any?>>()

        // 解析候选值列�?/ Parse candidate value list
        while (true) {
            val candidate = parseScalarValue()
            candidates.add(candidate)
            if (currentToken().type != TokenType.COMMA) {
                break
            }
            advance()
        }

        expect(TokenType.RPAREN, "Expected ')' after 'in' list")

        return InExpression(ScalarReference(path), candidates, negated)
    }

    /**
     * 解析模式匹配表达�?
     * Parse pattern match expression
     */
    private fun parsePatternMatch(
        path: PropertyPath,
        mode: PatternMatchMode,
        negated: Boolean
    ): PatternMatch<Any?> {
        val pattern = parseScalarValue()
        return PatternMatch(ScalarReference(path), pattern, mode, negated)
    }

    /**
     * 解析比较表达�?
     * Parse comparison expression
     */
    private fun parseComparison(leftPath: PropertyPath): Comparison<Any?> {
        val operator = currentToken().type.toComparisonOperator()
            ?: throw ParseException("Expected comparison operator", currentToken().position)

        advance()

        val right = parseScalarValue()

        return Comparison(operator, ScalarReference(leftPath), right)
    }

    /**
     * 解析标量值（常量或路径引用）
     * Parse scalar value (constant or path reference)
     */
    private fun parseScalarValue(): ScalarExpression<Any?> {
        return when (currentToken().type) {
            TokenType.STRING -> {
                val value = currentToken().value
                advance()
                ScalarConstant(value)
            }
            TokenType.NUMBER -> {
                val value = currentToken().value
                advance()
                // 尝试解析为数�?
                // Try to parse as number
                value.toDoubleOrNull()?.let { ScalarConstant(it) }
                    ?: ScalarConstant(value)
            }
            TokenType.TRUE, TokenType.FALSE -> {
                val isTrue = currentToken().type == TokenType.TRUE
                advance()
                ScalarConstant(isTrue)
            }
            TokenType.NULL -> {
                advance()
                ScalarConstant<Nothing?>(null)
            }
            TokenType.IDENTIFIER -> {
                val path = parsePath()
                ScalarReference(path)
            }
            else -> throw ParseException(
                "Expected scalar value, got: ${currentToken().value}",
                currentToken().position
            )
        }
    }

    /**
     * 解析属性路�?
     * Parse property path
     */
    private fun parsePath(): PropertyPath {
        if (currentToken().type != TokenType.IDENTIFIER) {
            throw ParseException("Expected identifier", currentToken().position)
        }

        val path = PropertyPath.parse(currentToken().value)
        advance()

        return path
    }

    // ========== 辅助方法 / Helper Methods ==========

    /**
     * 获取当前词法单元
     * Get current token
     */
    private fun currentToken(): Token {
        return tokens.getOrNull(position) ?: Token.eof(position)
    }

    /**
     * 向前移动并返回之前的词法单元
     * Advance and return previous token
     */
    private fun advance(): Token {
        val token = currentToken()
        position++
        return token
    }

    /**
     * 期望指定类型的词法单�?
     * Expect a token of specified type
     */
    private fun expect(type: TokenType, message: String): Token {
        if (currentToken().type != type) {
            throw ParseException(message, currentToken().position)
        }
        return advance()
    }

    /**
     * 合并 or 表达式（扁平化）
     * Merge or expressions (flatten)
     */
    private fun mergeOr(left: BooleanExpression, right: BooleanExpression): OrExpression {
        val operands = mutableListOf<BooleanExpression>()

        if (left is OrExpression) {
            operands.addAll(left.operands)
        } else {
            operands.add(left)
        }

        if (right is OrExpression) {
            operands.addAll(right.operands)
        } else {
            operands.add(right)
        }

        return OrExpression(operands)
    }

    /**
     * 合并 and 表达式（扁平化）
     * Merge and expressions (flatten)
     */
    private fun mergeAnd(left: BooleanExpression, right: BooleanExpression): AndExpression {
        val operands = mutableListOf<BooleanExpression>()

        if (left is AndExpression) {
            operands.addAll(left.operands)
        } else {
            operands.add(left)
        }

        if (right is AndExpression) {
            operands.addAll(right.operands)
        } else {
            operands.add(right)
        }

        return AndExpression(operands)
    }
}

/**
 * 解析布尔表达式字符串
 * Parse boolean expression string
 */
fun parseBooleanExpression(input: String): BooleanExpression {
    val lexer = Lexer(input)
    val tokens = lexer.tokenize()
    val parser = Parser(tokens)
    return parser.parse()
}

/**
 * 尝试解析布尔表达式字符串
 * Try to parse boolean expression string
 */
fun parseBooleanExpressionOrNull(input: String): BooleanExpression? {
    return try {
        parseBooleanExpression(input)
    } catch (e: Exception) {
        null
    }
}
