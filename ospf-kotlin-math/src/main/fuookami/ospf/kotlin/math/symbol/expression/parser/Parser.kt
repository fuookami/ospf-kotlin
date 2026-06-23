/**
 * 布尔表达式解析器
 * Boolean Expression Parser
 *
 * 将布尔表达式字符串解析为 BooleanExpression AST。
 * Parses boolean expression strings into BooleanExpression AST.
 *
 * 支持的语法：
 * Supported syntax:
 * - 逻辑操作: and, or, not / Logical operations: and, or, not
 * - 比较操作: =, <>, !=, <, <=, >, >= / Comparison operations
 * - 集合成员: in (...), not in (...) / Set membership
 * - 空值检查: is null, is not null / Null checks
 * - 分组: ( ... ) / Grouping
 *
 * 优先级（从高到低）：
 * Precedence (high to low):
 * 1. not, 括号 / not, parentheses
 * 2. and
 * 3. or
 */
package fuookami.ospf.kotlin.math.symbol.expression.parser

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssue
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType

/**
 * 布尔表达式解析器
 * Boolean Expression Parser
 *
 * @property tokens 词法单元列表 / List of tokens
 * @property input 原始输入字符串（可选，用于错误报告）/ Original input string (optional, for error reporting)
 */
class Parser(private val tokens: List<Token>, private val input: String? = null) {
    private var position = 0

    /**
     * 解析布尔表达式
     * Parse boolean expression.
     *
     * @return 解析后的布尔表达式 AST 或失败原因 / Parsed boolean expression AST or failure reason
     */
    fun parse(): Ret<BooleanExpression> {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens[0].type == TokenType.EOF)) {
            return parseFailed("Empty expression")
        }

        val expr = when (val result = parseOrExpression()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (currentToken().type != TokenType.EOF) {
            return parseFailed(
                message = "Unexpected token: ${currentToken().value}",
                position = currentToken().position
            )
        }

        return Ok(expr)
    }

    // ========== 解析方法 / Parse Methods ==========

    /** 构建解析失败结果 / Build a parse failure result */
    private fun <T> parseFailed(message: String, position: Int = 0): Ret<T> {
        val issue = ParseIssue(
            type = ParseIssueType.Syntax,
            message = message,
            input = input,
            position = position
        )
        return Failed(
            code = ErrorCode.IllegalArgument,
            message = "$message at position $position",
            value = issue
        )
    }

    /**
     * 解析 or 表达式（最低优先级）
     * Parse or expression (lowest precedence)
     */
    private fun parseOrExpression(): Ret<BooleanExpression> {
        var left = when (val result = parseAndExpression()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        while (currentToken().type == TokenType.OR) {
            advance()
            val right = when (val result = parseAndExpression()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            left = mergeOr(left, right)
        }

        return Ok(left)
    }

    /**
     * 解析 and 表达式
     * Parse and expression
     */
    private fun parseAndExpression(): Ret<BooleanExpression> {
        var left = when (val result = parseNotExpression()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        while (currentToken().type == TokenType.AND) {
            advance()
            val right = when (val result = parseNotExpression()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            left = mergeAnd(left, right)
        }

        return Ok(left)
    }

    /**
     * 解析 not 表达式
     * Parse not expression
     */
    private fun parseNotExpression(): Ret<BooleanExpression> {
        if (currentToken().type == TokenType.NOT) {
            advance()
            return when (val operand = parseNotExpression()) {
                is Ok -> Ok(NotExpression(operand.value))
                is Failed -> Failed(operand.error)
                is Fatal -> Fatal(operand.errors)
            }
        }

        return parsePrimaryExpression()
    }

    /**
     * 解析基本表达式
     * Parse primary expression
     */
    private fun parsePrimaryExpression(): Ret<BooleanExpression> {
        return when (currentToken().type) {
            TokenType.LPAREN -> {
                advance()
                val expr = when (val result = parseOrExpression()) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                when (val result = expect(TokenType.RPAREN, "Expected ')'")) {
                    is Ok -> Ok(expr)
                    is Failed -> Failed(result.error)
                    is Fatal -> Fatal(result.errors)
                }
            }

            TokenType.TRUE -> {
                advance()
                Ok(BooleanConstant(Trivalent.True))
            }

            TokenType.FALSE -> {
                advance()
                Ok(BooleanConstant(Trivalent.False))
            }

            TokenType.IDENTIFIER -> parsePathExpression()

            else -> parseFailed(
                message = "Unexpected token: ${currentToken().value}",
                position = currentToken().position
            )
        }
    }

    /**
     * 解析路径表达式（标识符开头的表达式）
     * Parse path expression (expression starting with identifier)
     */
    private fun parsePathExpression(): Ret<BooleanExpression> {
        val path = when (val result = parsePath()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (currentToken().type == TokenType.IS) {
            return parseNullCheck(path)
        }

        if (currentToken().type == TokenType.NOT) {
            advance()
            if (currentToken().type == TokenType.IN) {
                advance()
                return parseInExpression(path, negated = true)
            }
            if (currentToken().isPatternOperator()) {
                val mode = currentToken().type.toPatternMatchMode()
                    ?: return parseFailed(
                        message = "Expected pattern operator after 'not'",
                        position = currentToken().position
                    )
                advance()
                return parsePatternMatch(path, mode, negated = true)
            }
            return parseFailed(
                message = "Expected 'in' or pattern operator after 'not'",
                position = currentToken().position
            )
        }

        if (currentToken().type == TokenType.IN) {
            advance()
            return parseInExpression(path, negated = false)
        }

        if (currentToken().isPatternOperator()) {
            val mode = currentToken().type.toPatternMatchMode()
                ?: return parseFailed(
                    message = "Expected pattern operator",
                    position = currentToken().position
                )
            advance()
            return parsePatternMatch(path, mode, negated = false)
        }

        if (currentToken().isComparisonOperator()) {
            return parseComparison(path)
        }

        return parseFailed(
            message = "Expected comparison operator, 'in', 'is' after '$path'",
            position = currentToken().position
        )
    }

    /**
     * 解析空值检查
     * Parse null check
     */
    private fun parseNullCheck(path: PropertyPath): Ret<BooleanExpression> {
        advance()

        val notNull = if (currentToken().type == TokenType.NOT) {
            advance()
            true
        } else {
            false
        }

        when (val result = expect(TokenType.NULL, "Expected 'null' after 'is'")) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return Ok(NullCheck(
            path = path,
            type = if (notNull) NullCheckType.IsNotNull else NullCheckType.IsNull
        ))
    }

    /**
     * 解析集合成员判断
     * Parse set membership (in) expression
     */
    private fun parseInExpression(path: PropertyPath, negated: Boolean): Ret<BooleanExpression> {
        when (val result = expect(TokenType.LPAREN, "Expected '(' after 'in'")) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val candidates = mutableListOf<ScalarExpression<Any?>>()

        while (true) {
            val candidate = when (val result = parseScalarValue()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            candidates.add(candidate)
            if (currentToken().type != TokenType.COMMA) {
                break
            }
            advance()
        }

        when (val result = expect(TokenType.RPAREN, "Expected ')' after 'in' list")) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return Ok(InExpression(ScalarReference<Any?>(path), candidates, negated))
    }

    /**
     * 解析模式匹配表达式
     * Parse pattern match expression
     */
    private fun parsePatternMatch(
        path: PropertyPath,
        mode: PatternMatchMode,
        negated: Boolean
    ): Ret<BooleanExpression> {
        val pattern = when (val result = parseScalarValue()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(PatternMatch(ScalarReference<Any?>(path), pattern, mode, negated))
    }

    /**
     * 解析比较表达式
     * Parse comparison expression
     */
    private fun parseComparison(leftPath: PropertyPath): Ret<BooleanExpression> {
        val operator = currentToken().type.toComparisonOperator()
            ?: return parseFailed(
                message = "Expected comparison operator",
                position = currentToken().position
            )

        advance()

        val right = when (val result = parseScalarValue()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return Ok(Comparison(operator, ScalarReference<Any?>(leftPath), right))
    }

    /**
     * 解析标量值（常量或路径引用）
     * Parse scalar value (constant or path reference)
     */
    private fun parseScalarValue(): Ret<ScalarExpression<Any?>> {
        return when (currentToken().type) {
            TokenType.STRING -> {
                val value = currentToken().value
                advance()
                Ok(ScalarConstant(value))
            }

            TokenType.NUMBER -> {
                val value = currentToken().value
                advance()
                Ok(value.toDoubleOrNull()?.let { ScalarConstant(it) } ?: ScalarConstant(value))
            }

            TokenType.TRUE, TokenType.FALSE -> {
                val isTrue = currentToken().type == TokenType.TRUE
                advance()
                Ok(ScalarConstant(isTrue))
            }

            TokenType.NULL -> {
                advance()
                Ok(ScalarConstant<Nothing?>(null))
            }

            TokenType.IDENTIFIER -> {
                when (val path = parsePath()) {
                    is Ok -> Ok(ScalarReference(path.value))
                    is Failed -> Failed(path.error)
                    is Fatal -> Fatal(path.errors)
                }
            }

            else -> parseFailed(
                message = "Expected scalar value, got: ${currentToken().value}",
                position = currentToken().position
            )
        }
    }

    /**
     * 解析属性路径
     * Parse property path
     */
    private fun parsePath(): Ret<PropertyPath> {
        if (currentToken().type != TokenType.IDENTIFIER) {
            return parseFailed(
                message = "Expected identifier",
                position = currentToken().position
            )
        }

        val path = PropertyPath.parse(currentToken().value)
        advance()
        return Ok(path)
    }

    // ========== 辅助方法 / Helper Methods ==========

    /** 获取当前词法单元 / Get current token */
    private fun currentToken(): Token {
        return tokens.getOrNull(position) ?: Token.eof(position)
    }

    /** 向前移动并返回之前的词法单元 / Advance and return previous token */
    private fun advance(): Token {
        val token = currentToken()
        position++
        return token
    }

    /** 期望指定类型的词法单元 / Expect a token of specified type */
    private fun expect(type: TokenType, message: String): Ret<Token> {
        if (currentToken().type != type) {
            return parseFailed(message, currentToken().position)
        }
        return Ok(advance())
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
 *
 * @param input 布尔表达式字符串 / Boolean expression string
 * @return 解析后的布尔表达式或失败原因 / Parsed boolean expression or failure reason
 */
fun parseBooleanExpression(input: String): Ret<BooleanExpression> {
    val lexer = Lexer(input)
    val tokens = lexer.tokenize()
    val parser = Parser(tokens, input)
    return parser.parse()
}

/**
 * 尝试解析布尔表达式字符串
 * Try to parse boolean expression string
 *
 * @param input 布尔表达式字符串 / Boolean expression string
 * @return 解析后的布尔表达式，失败时返回 null / Parsed boolean expression, null on failure
 */
fun parseBooleanExpressionOrNull(input: String): BooleanExpression? {
    return when (val result = parseBooleanExpression(input)) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}
