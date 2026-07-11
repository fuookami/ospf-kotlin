/**
 * 词法分析器
 * Lexer
 *
 * 将表达式字符串解析为词法单元序列。
 * Parses expression strings into token sequences.
*/
package fuookami.ospf.kotlin.math.symbol.expression.parser

/**
 * 词法分析模式
 * Lexing Mode
 *
 * 控制负号的处理方式：布尔模式下负号作为数字的一部分，标量模式下负号始终作为独立 token。
 * Controls how the minus sign is handled: in boolean mode it's part of a number,
 * in scalar mode it's always a separate token.
*/
enum class LexMode {
    /** 布尔表达式模式（默认），-3 合并为 NUMBER / Boolean mode (default), -3 merged into NUMBER */
    Boolean,
    /** 标量表达式模式，-3 拆为 MINUS + NUMBER / Scalar mode, -3 split into MINUS + NUMBER */
    Scalar
}

/**
 * 词法分析器
 * Lexer
 *
 * 支持的关键字：and, or, not, in, is, null, true, false, if, then, else, fi
 * Supported keywords: and, or, not, in, is, null, true, false, if, then, else, fi
 *
 * 支持的标识符：单个标识符或点分隔路径（如 a.b.c）
 * Supported identifiers: single identifiers or dot-separated paths (e.g., a.b.c)
 *
 * @property input the input string to tokenize / 待词法分析的输入字符串
 * @property lexMode the lexing mode / 词法分析模式
*/
class Lexer(private val input: String, private val lexMode: LexMode = LexMode.Boolean) {
    private var position = 0
    private var currentChar: Char? = input.getOrNull(position)

    /**
     * 分析整个输入，返回词法单元列表
     * Tokenize the entire input, returning a list of tokens
     *
     * @return 词法单元列表（包含 EOF） / List of tokens (including EOF)
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
     * 获取下一个词法单元
     * Get the next token
     *
     * @return 下一个词法单元 / Next token
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

        // 处理数字字面量 / Handle number literals
        if (lexMode == LexMode.Boolean) {
            // 布尔模式：负号可作为数字前缀 / Boolean mode: minus can be number prefix
            if (currentChar?.isDigit() == true || (currentChar == '-' && peekChar(1)?.isDigit() == true)) {
                return readNumber(startPos)
            }
        } else {
            // 标量模式：仅数字开头 / Scalar mode: only digit starts a number
            if (currentChar?.isDigit() == true) {
                return readNumber(startPos)
            }
        }

        // 处理标识符和关键字 / Handle identifiers and keywords
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
            '+' -> {
                advance()
                Token(TokenType.PLUS, "+", startPos)
            }
            '-' -> {
                advance()
                Token(TokenType.MINUS, "-", startPos)
            }
            '*' -> {
                advance()
                if (currentChar == '*') {
                    advance()
                    Token(TokenType.DOUBLE_STAR, "**", startPos)
                } else {
                    Token(TokenType.STAR, "*", startPos)
                }
            }
            '/' -> {
                advance()
                Token(TokenType.SLASH, "/", startPos)
            }
            '%' -> {
                advance()
                Token(TokenType.PERCENT, "%", startPos)
            }
            '^' -> {
                advance()
                Token(TokenType.CARET, "^", startPos)
            }
            '?' -> {
                advance()
                Token(TokenType.QUESTION, "?", startPos)
            }
            ':' -> {
                advance()
                Token(TokenType.COLON, ":", startPos)
            }
            '&' -> {
                advance()
                if (currentChar == '&') {
                    advance()
                    Token(TokenType.AMPERSAND_AMPERSAND, "&&", startPos)
                } else {
                    Token.unknown("&", startPos)
                }
            }
            '|' -> {
                advance()
                if (currentChar == '|') {
                    advance()
                    Token(TokenType.PIPE_PIPE, "||", startPos)
                } else {
                    Token.unknown("|", startPos)
                }
            }
            '!' -> {
                advance()
                if (currentChar == '=') {
                    advance()
                    Token(TokenType.NE, "!=", startPos)
                } else {
                    Token(TokenType.BANG, "!", startPos)
                }
            }
            '=' -> {
                advance()
                if (currentChar == '=') {
                    advance()
                    Token(TokenType.EQ, "==", startPos)
                } else {
                    Token(TokenType.EQ, "=", startPos)
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
     * 向前移动一个字符
     * Advance one character
    */
    private fun advance() {
        position++
        currentChar = input.getOrNull(position)
    }

    /**
     * 查看前方第n个字符，不移动位置
     * Peek at the nth character ahead, without moving position
     *
     * @param n the number of characters to look ahead / 向前查看的字符数
     * @return the character at the specified offset, or null if out of bounds / 指定偏移量处的字符，越界则返回 null
    */
    private fun peekChar(n: Int = 1): Char? {
        return input.getOrNull(position + n)
    }

    /**
     * 读取字符串字面量
     * Read string literal
     *
     * @param startPos the starting position of the string literal / 字符串字面量的起始位置
     * @return the string token / 字符串词法单元
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
     * 读取数字字面量
     * Read number literal
     *
     * @param startPos the starting position of the number literal / 数字字面量的起始位置
     * @return the number token / 数字词法单元
    */
    private fun readNumber(startPos: Int): Token {
        val sb = StringBuilder()

        // 处理负号（仅布尔模式）/ Handle negative sign (boolean mode only)
        if (lexMode == LexMode.Boolean && currentChar == '-') {
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
     * 读取标识符或关键字
     * Read identifier or keyword
     *
     * @param startPos the starting position of the identifier or keyword / 标识符或关键字的起始位置
     * @return the identifier or keyword token / 标识符或关键字词法单元
    */
    private fun readIdentifierOrKeyword(startPos: Int): Token {
        val sb = StringBuilder()

        // 读取第一个标识符 / Read first identifier
        while (currentChar?.isLetterOrDigit() == true || currentChar == '_') {
            sb.append(currentChar)
            advance()
        }

        // 检查是否是路径（点分隔）/ Check if it's a path (dot-separated)
        while (currentChar == '.' && peekChar(1)?.let { it.isLetter() || it == '_' } == true) {
            sb.append(currentChar)
            advance() // 跳过点 / Skip dot

            while (currentChar?.isLetterOrDigit() == true || currentChar == '_') {
                sb.append(currentChar)
                advance()
            }
        }

        val value = sb.toString()

        // 检查是否是关键字 / Check if it's a keyword
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
            "if" -> Token(TokenType.IF, value, startPos)
            "then" -> Token(TokenType.THEN, value, startPos)
            "else" -> Token(TokenType.ELSE, value, startPos)
            "fi" -> Token(TokenType.FI, value, startPos)
            else -> Token(TokenType.IDENTIFIER, value, startPos)
        }
    }
}

/**
 * 扩展函数：字符串转词法单元列表
 * Extension function: String to token list
 *
 * @return 词法单元列表 / List of tokens
*/
fun String.tokenize(): List<Token> = Lexer(this).tokenize()

/**
 * 扩展函数：字符串转词法单元列表（标量模式）
 * Extension function: String to token list (scalar mode)
 *
 * @return 词法单元列表 / List of tokens
*/
fun String.tokenizeAsScalar(): List<Token> = Lexer(this, LexMode.Scalar).tokenize()
