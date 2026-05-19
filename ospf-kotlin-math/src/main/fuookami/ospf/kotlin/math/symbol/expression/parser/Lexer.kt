/**
 * 词法分析噌
 * Lexer
 *
 * 将布尔表达式字符串解析为词法单元序列。
 * Parses boolean expression strings into token sequences.
 */
package fuookami.ospf.kotlin.math.symbol.expression.parser

/**
 * 词法分析噌
 * Lexer
 *
 * 支持的关键字：and, or, not, in, is, null, true, false
 * Supported keywords: and, or, not, in, is, null, true, false
 *
 * 支持的标识符：单个标识符或点分隔路径（如 a.b.c，
 * Supported identifiers: single identifiers or dot-separated paths (e.g., a.b.c)
 */
class Lexer(private val input: String) {
    private var position = 0
    private var currentChar: Char? = input.getOrNull(position)

    /**
     * 分析整个输入，返回词法单元列行
     * Tokenize the entire input, returning a list of tokens
     */
    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        var token = nextToken()
        while (token.type != TokenType.EOF) {
            tokens.add(token)
            token = nextToken()
        }
        tokens.add(token) // 添加 EOF
        return tokens
    }

    /**
     * 获取下一个词法单兌
     * Get the next token
     */
    fun nextToken(): Token {
        skipWhitespace()

        if (currentChar == null) {
            return Token.eof(position)
        }

        val startPos = position

        // 处理字符串字面量 / Handle string literals
        if (currentChar == '\'' || currentChar == '"') {
            return readString(startPos)
        }

        // 处理数字字面里/ Handle number literals
        if (currentChar?.isDigit() == true || (currentChar == '-' && peekChar(1)?.isDigit() == true)) {
            return readNumber(startPos)
        }

        // 处理标识符和关键孌/ Handle identifiers and keywords
        if (currentChar?.isLetter() == true || currentChar == '_') {
            return readIdentifierOrKeyword(startPos)
        }

        // 处理操作符和符号 / Handle operators and symbols
        return when (currentChar) {
            '(' -> {
                advance()
                Token(TokenType.LPAREN, "(", startPos)
            }
            ')' -> {
                advance()
                Token(TokenType.RPAREN, ")", startPos)
            }
            ',' -> {
                advance()
                Token(TokenType.COMMA, ",", startPos)
            }
            '=' -> {
                advance()
                Token(TokenType.EQ, "=", startPos)
            }
            '!' -> {
                advance()
                if (currentChar == '=') {
                    advance()
                    Token(TokenType.NE, "!=", startPos)
                } else {
                    Token.unknown("!", startPos)
                }
            }
            '<' -> {
                advance()
                if (currentChar == '=') {
                    advance()
                    Token(TokenType.LE, "<=", startPos)
                } else if (currentChar == '>') {
                    advance()
                    Token(TokenType.NE, "<>", startPos)
                } else {
                    Token(TokenType.LT, "<", startPos)
                }
            }
            '>' -> {
                advance()
                if (currentChar == '=') {
                    advance()
                    Token(TokenType.GE, ">=", startPos)
                } else {
                    Token(TokenType.GT, ">", startPos)
                }
            }
            else -> {
                val char = currentChar ?: ""
                advance()
                Token.unknown(char.toString(), startPos)
            }
        }
    }

    /**
     * 跳过空白字符
     * Skip whitespace characters
     */
    private fun skipWhitespace() {
        while (currentChar?.isWhitespace() == true) {
            advance()
        }
    }

    /**
     * 向前移动一个字笌
     * Advance one character
     */
    private fun advance() {
        position++
        currentChar = input.getOrNull(position)
    }

    /**
     * 查看前方笌n 个字符，不移动位罌
     * Peek at the nth character ahead, without moving position
     */
    private fun peekChar(n: Int = 1): Char? {
        return input.getOrNull(position + n)
    }

    /**
     * 读取字符串字面量
     * Read string literal
     */
    private fun readString(startPos: Int): Token {
        val quote = currentChar!!
        advance() // 跳过起始引号 / Skip opening quote

        val sb = StringBuilder()
        while (currentChar != null && currentChar != quote) {
            if (currentChar == '\\') {
                advance()
                when (currentChar) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\', '\'', '"' -> sb.append(currentChar)
                    else -> {
                        sb.append('\\')
                        if (currentChar != null) sb.append(currentChar)
                    }
                }
            } else {
                sb.append(currentChar)
            }
            advance()
        }

        if (currentChar == quote) {
            advance() // 跳过结束引号 / Skip closing quote
        }

        return Token(TokenType.STRING, sb.toString(), startPos)
    }

    /**
     * 读取数字字面里
     * Read number literal
     */
    private fun readNumber(startPos: Int): Token {
        val sb = StringBuilder()

        // 处理负号 / Handle negative sign
        if (currentChar == '-') {
            sb.append(currentChar)
            advance()
        }

        // 读取整数部分 / Read integer part
        while (currentChar?.isDigit() == true) {
            sb.append(currentChar)
            advance()
        }

        // 读取小数部分 / Read decimal part
        if (currentChar == '.' && peekChar(1)?.isDigit() == true) {
            sb.append(currentChar)
            advance()
            while (currentChar?.isDigit() == true) {
                sb.append(currentChar)
                advance()
            }
        }

        // 读取指数部分 / Read exponent part
        if (currentChar == 'e' || currentChar == 'E') {
            sb.append(currentChar)
            advance()
            if (currentChar == '+' || currentChar == '-') {
                sb.append(currentChar)
                advance()
            }
            while (currentChar?.isDigit() == true) {
                sb.append(currentChar)
                advance()
            }
        }

        return Token(TokenType.NUMBER, sb.toString(), startPos)
    }

    /**
     * 读取标识符或关键孌
     * Read identifier or keyword
     */
    private fun readIdentifierOrKeyword(startPos: Int): Token {
        val sb = StringBuilder()

        // 读取第一个标识符 / Read first identifier
        while (currentChar?.isLetterOrDigit() == true || currentChar == '_') {
            sb.append(currentChar)
            advance()
        }

        // 检查是否是路径（点分隔， Check if it's a path (dot-separated)
        while (currentChar == '.' && peekChar(1)?.let { it.isLetter() || it == '_' } == true) {
            sb.append(currentChar)
            advance() // 跳过炌/ Skip dot

            while (currentChar?.isLetterOrDigit() == true || currentChar == '_') {
                sb.append(currentChar)
                advance()
            }
        }

        val value = sb.toString()

        // 检查是否是关键孌/ Check if it's a keyword
        return when (value.lowercase()) {
            "and" -> Token(TokenType.AND, value, startPos)
            "or" -> Token(TokenType.OR, value, startPos)
            "not" -> Token(TokenType.NOT, value, startPos)
            "in" -> Token(TokenType.IN, value, startPos)
            "is" -> Token(TokenType.IS, value, startPos)
            "like" -> Token(TokenType.LIKE, value, startPos)
            "contains" -> Token(TokenType.CONTAINS, value, startPos)
            "prefix" -> Token(TokenType.PREFIX, value, startPos)
            "suffix" -> Token(TokenType.SUFFIX, value, startPos)
            "regex", "matches" -> Token(TokenType.REGEX, value, startPos)
            "exact" -> Token(TokenType.EXACT, value, startPos)
            "null" -> Token(TokenType.NULL, value, startPos)
            "true" -> Token(TokenType.TRUE, value, startPos)
            "false" -> Token(TokenType.FALSE, value, startPos)
            else -> Token(TokenType.IDENTIFIER, value, startPos)
        }
    }
}

/**
 * 扩展函数：字符串转词法单元列行
 * Extension function: String to token list
 */
fun String.tokenize(): List<Token> = Lexer(this).tokenize()