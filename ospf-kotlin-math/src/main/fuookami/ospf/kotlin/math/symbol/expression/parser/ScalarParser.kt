/**
 * 标量表达式解析器
 * Scalar Expression Parser
 *
 * 将标量表达式字符串解析为 ScalarExpression AST。
 * Parses scalar expression strings into ScalarExpression AST.
 *
 * 支持的语法：
 * Supported syntax:
 * - 算术操作: +, -, *, /, %, ^, ** / Arithmetic operations
 * - 比较操作: >, <, >=, <=, ==, != / Comparison operations
 * - 逻辑操作: &&, ||, !, and, or, not / Logical operations
 * - 三元条件: ? : / Ternary conditional
 * - if/then/else/fi 条件 / if/then/else/fi conditional
 * - 函数调用: name(args) / Function calls
 * - math.* 函数与常量 / math.* functions and constants
 *
 * 优先级（从高到低）：
 * Precedence (high to low):
 * 1. 原子（数字、标识符、括号）
 * 2. 幂运算 ^, **（右结合）
 * 3. 一元正号 +
 * 4. 乘除模 *, /, %
 * 5. 加减 +, -（一元负号在此层处理）
 * 6. 比较 >, <, >=, <=, ==, !=
 * 7. 逻辑与 &&, and
 * 8. 逻辑或 ||, or
 * 9. 三元条件 ? :, if/then/else/fi
*/
package fuookami.ospf.kotlin.math.symbol.expression.parser

import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssue
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 标量表达式解析器
 * Scalar Expression Parser
 *
 * @property tokens 词法单元列表 / List of tokens
 * @property input 原始输入字符串（可选，用于错误报告）/ Original input string (optional, for error reporting)
*/
class ScalarParser(private val tokens: List<Token>, private val input: String? = null) {

    /**
     * Current position in the token list during parsing.
     * 解析过程中在词法单元列表中的当前位置。
    */
    private var position = 0

    /**
     * 解析标量表达式
     * Parse scalar expression
     *
     * @return 解析后的标量表达式或失败原因 / Parsed scalar expression or failure reason
    */
    fun parse(): Ret<ScalarExpression<Double>> {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens[0].type == TokenType.EOF)) {
            return parseFailed("Empty expression")
        }

        val result = parseTernary()
        if (result is Failed) return Failed(result.error)
        if (result is Fatal) return Fatal(result.errors)

        val expr = (result as Ok).value

        if (currentToken().type != TokenType.EOF) {
            return parseFailed(
                message = "Unexpected token: ${currentToken().value}",
                position = currentToken().position
            )
        }

        return Ok(expr)
    }

    // ========== 解析方法 / Parse Methods ==========

    /**
     * Creates a parse failure result with the given message and position.
     * 使用给定的消息和位置创建解析失败结果。
     *
     * @param T the expected result type / 期望的结果类型
     * @param message the error message / 错误消息
     * @param position the token position where the error occurred / 发生错误的词法单元位置
     * @return a failed parse result / 解析失败结果
    */
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

    // ========== 优先级 9: 三元条件 / Priority 9: Ternary ==========

    /**
     * Parses a ternary conditional expression (? : or if/then/else/fi).
     * 解析三元条件表达式（? : 或 if/then/else/fi）。
     *
     * @return the parsed ternary expression or failure / 解析后的三元表达式或失败
    */
    private fun parseTernary(): Ret<ScalarExpression<Double>> {
        // 检查 if/then/else/fi 形式 / Check if/then/else/fi form
        if (currentToken().type == TokenType.IF) {
            return parseIfThenElse()
        }

        // 普通表达式（可能后接 ? : 三元）
        val expr = when (val r = parseLogicalOr()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        if (currentToken().type == TokenType.QUESTION) {
            val questionPos = currentToken().position
            advance() // 跳过 ? / skip ?

            val thenBranch = when (val r = parseTernary()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }

            when (val r = expect(TokenType.COLON, "Expected ':' in ternary expression")) {
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
                is Ok -> {}
            }

            val elseBranch = when (val r = parseTernary()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }

            val condition = extractBooleanCondition(expr, questionPos) ?: return parseFailed(
                "Ternary condition must be a boolean expression",
                questionPos
            )

            return Ok(ScalarConditional(condition, thenBranch, elseBranch))
        }

        return Ok(expr)
    }

    /**
     * Parses if/then/else/fi conditional expression.
     * 解析 if/then/else/fi 条件表达式。
     *
     * @return the parsed conditional expression or failure / 解析后的条件表达式或失败
    */
    private fun parseIfThenElse(): Ret<ScalarExpression<Double>> {
        val ifPos = currentToken().position
        advance() // 跳过 if / skip if

        // 条件解析到逻辑或层（允许 && 和 ||）
        // Condition parsed at logical OR level (allows && and ||)
        val conditionResult = parseLogicalOr()
        val conditionExpr = when (conditionResult) {
            is Ok -> conditionResult.value
            is Failed -> return Failed(conditionResult.error)
            is Fatal -> return Fatal(conditionResult.errors)
        }

        val condition = extractBooleanCondition(conditionExpr, ifPos) ?: return parseFailed(
            "if condition must be a boolean expression",
            ifPos
        )

        when (val r = expect(TokenType.THEN, "Expected 'then' after if condition")) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            is Ok -> {}
        }

        val thenBranch = when (val r = parseTernary()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        when (val r = expect(TokenType.ELSE, "Expected 'else' in if expression")) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            is Ok -> {}
        }

        val elseBranch = when (val r = parseTernary()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        when (val r = expect(TokenType.FI, "Expected 'fi' to close if expression")) {
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
            is Ok -> {}
        }

        return Ok(ScalarConditional(condition, thenBranch, elseBranch))
    }

    // ========== 优先级 8: 逻辑或 / Priority 8: Logical OR ==========

    /**
     * Parses a logical OR expression (|| or `or`), left-associative.
     * 解析逻辑或表达式（|| 或 `or`），左结合。
     *
     * @return the parsed logical OR expression or failure / 解析后的逻辑或表达式或失败
    */
    private fun parseLogicalOr(): Ret<ScalarExpression<Double>> {
        var left = when (val r = parseLogicalAnd()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        while (currentToken().type == TokenType.OR || currentToken().type == TokenType.PIPE_PIPE) {
            advance()
            val right = when (val r = parseLogicalAnd()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            val leftBool = unwrapBoolean(left) ?: return parseFailed("Expected boolean expression before 'or'", currentToken().position)
            val rightBool = unwrapBoolean(right) ?: return parseFailed("Expected boolean expression after 'or'", currentToken().position)
            left = ScalarBoolean<Double>(mergeOr(leftBool, rightBool))
        }

        return Ok(left)
    }

    // ========== 优先级 7: 逻辑与 / Priority 7: Logical AND ==========

    /**
     * Parses a logical AND expression (&& or `and`), left-associative.
     * 解析逻辑与表达式（&& 或 `and`），左结合。
     *
     * @return the parsed logical AND expression or failure / 解析后的逻辑与表达式或失败
    */
    private fun parseLogicalAnd(): Ret<ScalarExpression<Double>> {
        var left = when (val r = parseComparison()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        while (currentToken().type == TokenType.AND || currentToken().type == TokenType.AMPERSAND_AMPERSAND) {
            advance()
            val right = when (val r = parseComparison()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            val leftBool = unwrapBoolean(left) ?: return parseFailed("Expected boolean expression before 'and'", currentToken().position)
            val rightBool = unwrapBoolean(right) ?: return parseFailed("Expected boolean expression after 'and'", currentToken().position)
            left = ScalarBoolean<Double>(mergeAnd(leftBool, rightBool))
        }

        return Ok(left)
    }

    // ========== 优先级 6: 比较 / Priority 6: Comparison ==========

    /**
     * Parses a comparison expression (>, <, >=, <=, ==, !=).
     * 解析比较表达式（>, <, >=, <=, ==, !=）。
     *
     * @return the parsed comparison expression or failure / 解析后的比较表达式或失败
    */
    private fun parseComparison(): Ret<ScalarExpression<Double>> {
        var left = when (val r = parseAdditive()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        if (currentToken().isComparisonOperator()) {
            val operator = currentToken().type.toComparisonOperator()
                ?: return parseFailed("Expected comparison operator", currentToken().position)
            advance()

            val right = when (val r = parseAdditive()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }

            left = ScalarBoolean<Double>(Comparison(operator, left, right))
        }

        return Ok(left)
    }

    // ========== 优先级 5: 加减 / Priority 5: Additive ==========

    /**
     * Parses an additive expression (+, -), left-associative.
     * 解析加减表达式（+, -），左结合。
     *
     * @return the parsed additive expression or failure / 解析后的加减表达式或失败
    */
    private fun parseAdditive(): Ret<ScalarExpression<Double>> {
        // 左操作数经 parseUnaryMinus 处理前导负号（支持 -x^2 = -(x^2) 和 --x）
        // Left operand goes through parseUnaryMinus for leading minus
        // (supports -x^2 = -(x^2) and --x)
        var left = when (val r = parseUnaryMinus()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        while (currentToken().type == TokenType.PLUS || currentToken().type == TokenType.MINUS) {
            val operator = if (currentToken().type == TokenType.PLUS) BinaryOperator.Add else BinaryOperator.Subtract
            advance()
            val right = when (val r = parseMultiplicative()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            left = ScalarBinary(operator, left, right)
        }

        return Ok(left)
    }

    /**
     * 解析一元负号
     * Parse unary minus
     *
     * 在加减层处理一元负号，使 -x^2 解析为 -(x^2)（而非 (-x)^2）。
     * Handles unary minus at additive level so -x^2 parses as -(x^2) (not (-x)^2).
     *
     * 递归调用自身以支持连续负号（--x、- -x）。
     * Recursively calls itself to support consecutive minuses (--x, - -x).
     *
     * 递归目标是 parseMultiplicative 而非 parseAdditive，避免把后续 +/- 吞入操作数
     * （修复 A4：-x^2+1 应为 -(x^2)+1，-2-3 应为 (-2)-3）。
     * Recursion target is parseMultiplicative, not parseAdditive, to avoid
     * swallowing subsequent +/- into the operand (fixes A4:
     * -x^2+1 should be -(x^2)+1, -2-3 should be (-2)-3).
     *
     * @return parsed unary negation expression or the next-level result / 解析后的一元取负表达式或下一层级结果
    */
    private fun parseUnaryMinus(): Ret<ScalarExpression<Double>> {
        if (currentToken().type == TokenType.MINUS) {
            advance()
            val operand = when (val r = parseUnaryMinus()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            return Ok(ScalarUnary(UnaryOperator.Negate, operand))
        }
        return parseMultiplicative()
    }

    // ========== 优先级 4: 乘除模 / Priority 4: Multiplicative ==========

    /**
     * Parses a multiplicative expression (*, /, %), left-associative.
     * 解析乘除模表达式（*, /, %），左结合。
     *
     * @return the parsed multiplicative expression or failure / 解析后的乘除模表达式或失败
    */
    private fun parseMultiplicative(): Ret<ScalarExpression<Double>> {
        var left = when (val r = parsePower()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        while (currentToken().type == TokenType.STAR || currentToken().type == TokenType.SLASH || currentToken().type == TokenType.PERCENT) {
            val operator = when (currentToken().type) {
                TokenType.STAR -> BinaryOperator.Multiply
                TokenType.SLASH -> BinaryOperator.Divide
                TokenType.PERCENT -> BinaryOperator.Modulo
                else -> break
            }
            advance()
            val right = when (val r = parsePower()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            left = ScalarBinary(operator, left, right)
        }

        return Ok(left)
    }

    // ========== 优先级 2: 幂 / Priority 2: Power ==========

    /**
     * Parses a power expression (^, **), right-associative.
     * 解析幂运算表达式（^, **），右结合。
     *
     * @return the parsed power expression or failure / 解析后的幂运算表达式或失败
    */
    private fun parsePower(): Ret<ScalarExpression<Double>> {
        var base = when (val r = parseUnaryPlus()) {
            is Ok -> r.value
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        if (currentToken().type == TokenType.CARET || currentToken().type == TokenType.DOUBLE_STAR) {
            advance()
            // 右结合：递归调用自身 / Right-associative: recursive call
            val exponent = when (val r = parsePower()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            base = ScalarBinary(BinaryOperator.Power, base, exponent)
        }

        return Ok(base)
    }

    // ========== 优先级 3: 一元正号 / Priority 3: Unary plus ==========

    /**
     * Parses a unary plus expression, recursively handling consecutive plus signs.
     * 解析一元正号表达式，递归处理连续正号。
     *
     * @return the parsed unary plus expression or primary / 解析后的一元正号表达式或原子
    */
    private fun parseUnaryPlus(): Ret<ScalarExpression<Double>> {
        if (currentToken().type == TokenType.PLUS) {
            advance()
            val operand = when (val r = parseUnaryPlus()) {
                is Ok -> r.value
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
            return Ok(ScalarUnary(UnaryOperator.Positive, operand))
        }
        return parsePrimary()
    }

    // ========== 优先级 1: 原子 / Priority 1: Primary ==========

    /**
     * Parses a primary (atomic) expression: number, identifier, function call, parenthesized expression, boolean, null, string, or logical NOT.
     * 解析原子表达式：数字、标识符、函数调用、括号表达式、布尔值、null、字符串或逻辑非。
     *
     * @return the parsed primary expression or failure / 解析后的原子表达式或失败
    */
    private fun parsePrimary(): Ret<ScalarExpression<Double>> {
        return when (currentToken().type) {
            TokenType.NUMBER -> {
                val value = currentToken().value.toDoubleOrNull()
                    ?: return parseFailed("Invalid number: ${currentToken().value}", currentToken().position)
                advance()
                Ok(ScalarConstant(value))
            }

            TokenType.IDENTIFIER -> parseIdentifierOrFunction()

            TokenType.LPAREN -> {
                advance()
                val expr = when (val r = parseTernary()) {
                    is Ok -> r.value
                    is Failed -> return Failed(r.error)
                    is Fatal -> return Fatal(r.errors)
                }
                when (val r = expect(TokenType.RPAREN, "Expected ')'")) {
                    is Failed -> return Failed(r.error)
                    is Fatal -> return Fatal(r.errors)
                    is Ok -> {}
                }
                Ok(expr)
            }

            TokenType.TRUE -> {
                advance()
                Ok(ScalarBoolean<Double>(BooleanConstant(Trivalent.True)))
            }

            TokenType.FALSE -> {
                advance()
                Ok(ScalarBoolean<Double>(BooleanConstant(Trivalent.False)))
            }

            TokenType.NULL -> {
                advance()
                @Suppress("UNCHECKED_CAST")
                Ok(ScalarConstant(null) as ScalarExpression<Double>)
            }

            TokenType.NOT, TokenType.BANG -> {
                // 逻辑非作为前缀 / Logical NOT as prefix
                advance()
                val operand = when (val r = parseComparison()) {
                    is Ok -> r.value
                    is Failed -> return Failed(r.error)
                    is Fatal -> return Fatal(r.errors)
                }
                val boolExpr = extractBooleanCondition(operand)
                    ?: return parseFailed("! operator requires boolean operand", currentToken().position)
                Ok(ScalarBoolean<Double>(NotExpression(boolExpr)))
            }

            TokenType.STRING -> {
                val value = currentToken().value
                advance()
                @Suppress("UNCHECKED_CAST")
                Ok(ScalarConstant(value) as ScalarExpression<Double>)
            }

            else -> parseFailed(
                message = "Unexpected token: ${currentToken().value}",
                position = currentToken().position
            )
        }
    }

    /**
     * Parses an identifier or function call expression, handling math.* constants and function name normalization.
     * 解析标识符或函数调用，处理 math.* 常量和函数名归一化。
     *
     * @return the parsed identifier reference, function call, or constant / 解析后的标识符引用、函数调用或常量
    */
    private fun parseIdentifierOrFunction(): Ret<ScalarExpression<Double>> {
        val identifier = currentToken().value
        val startPos = currentToken().position
        advance()

        // 检查 math.PI 和 math.E 常量 / Check math.PI and math.E constants
        if (identifier == "math.PI") {
            return Ok(ScalarConstant(kotlin.math.PI))
        }
        if (identifier == "math.E") {
            return Ok(ScalarConstant(kotlin.math.E))
        }

        // 检查函数调用 / Check function call
        if (currentToken().type == TokenType.LPAREN) {
            advance() // 跳过 ( / skip (

            // 函数名归一化：剥离 math. 前缀 / Function name normalization: strip math. prefix
            val functionName = if (identifier.startsWith("math.")) {
                identifier.removePrefix("math.")
            } else {
                identifier
            }

            val args = mutableListOf<ScalarExpression<Double>>()
            if (currentToken().type != TokenType.RPAREN) {
                val firstArg = when (val r = parseTernary()) {
                    is Ok -> r.value
                    is Failed -> return Failed(r.error)
                    is Fatal -> return Fatal(r.errors)
                }
                args.add(firstArg)

                while (currentToken().type == TokenType.COMMA) {
                    advance()
                    val arg = when (val r = parseTernary()) {
                        is Ok -> r.value
                        is Failed -> return Failed(r.error)
                        is Fatal -> return Fatal(r.errors)
                    }
                    args.add(arg)
                }
            }

            when (val r = expect(TokenType.RPAREN, "Expected ')' after function arguments")) {
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
                is Ok -> {}
            }

            return Ok(ScalarFunction(functionName, args))
        }

        // 普通引用 / Simple reference
        return Ok(ScalarReference(PropertyPath.parse(identifier)))
    }

    // ========== 辅助方法 / Helper Methods ==========

    /**
     * Returns the current token at the parser position, or an EOF token if past the end.
     * 返回解析器当前位置的词法单元，如果超出末尾则返回 EOF 词法单元。
     *
     * @return the current token / 当前词法单元
    */
    private fun currentToken(): Token {
        return tokens.getOrNull(position) ?: Token.eof(position)
    }

    /**
     * Advances the parser position by one and returns the consumed token.
     * 将解析器位置前进一位并返回已消费的词法单元。
     *
     * @return the token that was consumed / 已消费的词法单元
    */
    private fun advance(): Token {
        val token = currentToken()
        position++
        return token
    }

    /**
     * Expects a token of the given type at the current position; advances and returns it, or fails with the given message.
     * 期望当前位置有给定类型的词法单元；前进并返回它，否则以给定消息失败。
     *
     * @param type the expected token type / 期望的词法单元类型
     * @param message the error message if the token does not match / 词法单元不匹配时的错误消息
     * @return the expected token or failure / 期望的词法单元或失败
    */
    private fun expect(type: TokenType, message: String): Ret<Token> {
        if (currentToken().type != type) {
            return parseFailed(message, currentToken().position)
        }
        return Ok(advance())
    }

    /**
     * Extracts a BooleanExpression from a ScalarExpression if it wraps a ScalarBoolean.
     * 从 ScalarExpression 中提取 BooleanExpression（如果它是一个 ScalarBoolean 包装）。
     *
     * @param expr the scalar expression to extract from / 要提取的标量表达式
     * @param errorPos optional position for error reporting / 可选的错误报告位置
     * @return the extracted BooleanExpression, or null if not a boolean expression / 提取的 BooleanExpression，如果不是布尔表达式则返回 null
    */
    private fun extractBooleanCondition(expr: ScalarExpression<Double>, errorPos: Int? = null): BooleanExpression? {
        return when (expr) {
            is ScalarBoolean<*> -> expr.expr
            else -> null
        }
    }

    /**
     * Unwraps a BooleanExpression from a ScalarExpression for use in logical operators and conditionals.
     * 从 ScalarExpression 中解包 BooleanExpression（用于逻辑操作符和条件）。
     *
     * @param expr the scalar expression to unwrap / 要解包的标量表达式
     * @return the unwrapped BooleanExpression, or null if not a boolean expression / 解包的 BooleanExpression，如果不是布尔表达式则返回 null
    */
    private fun unwrapBoolean(expr: ScalarExpression<Double>): BooleanExpression? {
        return when (expr) {
            is ScalarBoolean<*> -> expr.expr
            else -> null
        }
    }

    /**
     * Merges two boolean expressions with OR, flattening nested OrExpressions.
     * 合并 or 表达式（扁平化）。
     *
     * @param left the left boolean expression / 左布尔表达式
     * @param right the right boolean expression / 右布尔表达式
     * @return the merged OrExpression / 合并后的 OrExpression
    */
    private fun mergeOr(left: BooleanExpression, right: BooleanExpression): OrExpression {
        val operands = mutableListOf<BooleanExpression>()
        if (left is OrExpression) operands.addAll(left.operands) else operands.add(left)
        if (right is OrExpression) operands.addAll(right.operands) else operands.add(right)
        return OrExpression(operands)
    }

    /**
     * Merges two boolean expressions with AND, flattening nested AndExpressions.
     * 合并 and 表达式（扁平化）。
     *
     * @param left the left boolean expression / 左布尔表达式
     * @param right the right boolean expression / 右布尔表达式
     * @return the merged AndExpression / 合并后的 AndExpression
    */
    private fun mergeAnd(left: BooleanExpression, right: BooleanExpression): AndExpression {
        val operands = mutableListOf<BooleanExpression>()
        if (left is AndExpression) operands.addAll(left.operands) else operands.add(left)
        if (right is AndExpression) operands.addAll(right.operands) else operands.add(right)
        return AndExpression(operands)
    }
}

// ========== 公共入口函数 / Public Entry Points ==========

/**
 * 解析标量表达式字符串
 * Parse scalar expression string
 *
 * @param input 标量表达式字符串 / Scalar expression string
 * @return 解析后的标量表达式或失败原因 / Parsed scalar expression or failure reason
*/
fun parseScalarExpression(input: String): Ret<ScalarExpression<Double>> {
    val lexer = Lexer(input, LexMode.Scalar)
    val tokens = lexer.tokenize()
    val parser = ScalarParser(tokens, input)
    return parser.parse()
}

/**
 * 尝试解析标量表达式字符串
 * Try to parse scalar expression string
 *
 * @param input 标量表达式字符串 / Scalar expression string
 * @return 解析后的标量表达式，失败时返回 null / Parsed scalar expression, null on failure
*/
fun parseScalarExpressionOrNull(input: String): ScalarExpression<Double>? {
    return when (val result = parseScalarExpression(input)) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}
