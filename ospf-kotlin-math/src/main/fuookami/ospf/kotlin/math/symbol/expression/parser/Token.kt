/**
 * 词法单元
 * Token
 *
 * 定义布尔表达式解析器使用的词法单元类型。
 * Defines token types used in the boolean expression parser.
*/
package fuookami.ospf.kotlin.math.symbol.expression.parser

import fuookami.ospf.kotlin.math.symbol.expression.*

/**
 * 词法单元类型
 * Token Type
*/
enum class TokenType {
    // 字面里/ Literals
    /** 布尔常量 true / Boolean constant true */
    TRUE,
    /** 布尔常量 false / Boolean constant false */
    FALSE,
    /** 空倌/ Null value */
    NULL,
    /** 字符串字面量 / String literal */
    STRING,
    /** 数字字面里/ Number literal */
    NUMBER,
    /** 标识符（包括路径妌a.b.c， Identifier (including paths like a.b.c) */
    IDENTIFIER,

    // 关键孌/ Keywords
    /** 逻辑丌/ Logical AND */
    AND,
    /** 逻辑戌/ Logical OR */
    OR,
    /** 逻辑靌/ Logical NOT */
    NOT,
    /** 集合成员判断 / Set membership (IN) */
    IN,
    /** 是否 / Is */
    IS,
    /** LIKE 模式匹配 / LIKE pattern match */
    LIKE,
    /** 包含匹配 / Contains match */
    CONTAINS,
    /** 前缀匹配 / Prefix match */
    PREFIX,
    /** 后缀匹配 / Suffix match */
    SUFFIX,
    /** 正则匹配 / Regex match */
    REGEX,
    /** 精确匹配 / Exact match */
    EXACT,

    // 比较操作笌/ Comparison Operators
    /** 等于 / Equal */
    EQ,          // =
    /** 不等二/ Not Equal */
    NE,          // <> 戌!=
    /** 小于 / Less Than */
    LT,          // <
    /** 小于等于 / Less Than or Equal */
    LE,          // <=
    /** 大于 / Greater Than */
    GT,          // >

    /** 大于等于 / Greater Than or Equal */
    GE,          // >=

    // 条件关键字 / Conditional Keywords
    /** if 关键字 / if keyword */
    IF,

    /** then 关键字 / then keyword */
    THEN,

    /** else 关键字 / else keyword */
    ELSE,

    /** fi 关键字 / fi keyword */
    FI,

    // 算术操作符 / Arithmetic Operators
    /** 加号 / Plus */
    PLUS,        // +

    /** 减号 / Minus */
    MINUS,       // -

    /** 乘号 / Star */
    STAR,        // *

    /** 除号 / Slash */
    SLASH,       // /

    /** 取模 / Percent */
    PERCENT,     // %

    /** 幂运算符 ^ / Caret (power) */
    CARET,       // ^

    /** 幂运算符 ** / Double star (power) */
    DOUBLE_STAR, // **

    // 条件操作符 / Conditional Operators
    /** 问号（三元条件） / Question mark (ternary) */
    QUESTION,    // ?

    /** 冒号（三元条件） / Colon (ternary) */
    COLON,       // :

    // 逻辑操作符（符号形式） / Logical Operators (symbolic form)
    /** 逻辑与 && / Logical AND */
    AMPERSAND_AMPERSAND, // &&

    /** 逻辑或 || / Logical OR */
    PIPE_PIPE,           // ||

    /** 逻辑非 ! / Logical NOT */
    BANG,                // !

    // 其他符号 / Other Symbols
    /** 左括号 / Left parenthesis */
    LPAREN,      // (

    /** 右括号 / Right parenthesis */
    RPAREN,      // )

    /** 逗号 / Comma */
    COMMA,       // ,

    // 特殊 / Special
    /** 文件结束 / End of file */
    EOF,

    /** 未知/错误 / Unknown/Error */
    UNKNOWN
}

/**
 * 词法单元
 * Token
 *
 * 表示词法分析器输出的单个词法单元。
 * Represents a single token output by the lexer.
 *
 * @property type 词法单元类型 / Token type
 * @property value 词法单元的字符串倌/ String value of the token
 * @property position 词法单元在输入中的位置（起始索引， Position in input (start index)
*/
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int = 0
) {
    override fun toString(): String = when (type) {
        TokenType.EOF -> "EOF"
        TokenType.UNKNOWN -> "UNKNOWN($value)"
        else -> "${type.name}($value)"
    }

    companion object {
        /**
         * 创建 EOF 词法单元
         * Create EOF token
         *
         * @param position 位置索引 / Position index
         * @return EOF 词法单元 / EOF token
        */
        fun eof(position: Int = 0): Token = Token(TokenType.EOF, "", position)

        /**
         * 创建未知词法单元
         * Create unknown token
         *
         * @param value 未知值 / Unknown value
         * @param position 位置索引 / Position index
         * @return 未知词法单元 / Unknown token
        */
        fun unknown(value: String, position: Int = 0): Token = Token(TokenType.UNKNOWN, value, position)
    }
}

/**
 * 判断词法单元是否是比较操作符
 * Check if token is a comparison operator
 *
 * @return 是否是比较操作符 / Whether it is a comparison operator
*/
fun Token.isComparisonOperator(): Boolean = type in listOf(
    TokenType.EQ, TokenType.NE, TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE
)

/**
 * 判断词法单元是否是模式匹配操作符
 * Check if token is a pattern match operator
 *
 * @return 是否是模式匹配操作符 / Whether it is a pattern match operator
*/
fun Token.isPatternOperator(): Boolean = type in listOf(
    TokenType.LIKE, TokenType.CONTAINS, TokenType.PREFIX, TokenType.SUFFIX, TokenType.REGEX, TokenType.EXACT
)

/**
 * 将词法单元类型转换为比较操作符
 * Convert token type to comparison operator
 *
 * @return 比较操作符，不支持时返回 null / Comparison operator, null if not supported
*/
fun TokenType.toComparisonOperator(): ComparisonOperator? = when (this) {
    TokenType.EQ -> ComparisonOperator.Eq
    TokenType.NE -> ComparisonOperator.Ne
    TokenType.LT -> ComparisonOperator.Lt
    TokenType.LE -> ComparisonOperator.Le
    TokenType.GT -> ComparisonOperator.Gt
    TokenType.GE -> ComparisonOperator.Ge
    else -> null
}

/**
 * 将词法单元类型转换为模式匹配模式
 * Convert token type to pattern match mode
 *
 * @return 模式匹配模式，不支持时返回 null / Pattern match mode, null if not supported
*/
fun TokenType.toPatternMatchMode(): PatternMatchMode? = when (this) {
    TokenType.LIKE -> PatternMatchMode.Like
    TokenType.CONTAINS -> PatternMatchMode.Contains
    TokenType.PREFIX -> PatternMatchMode.Prefix
    TokenType.SUFFIX -> PatternMatchMode.Suffix
    TokenType.REGEX -> PatternMatchMode.Regex
    TokenType.EXACT -> PatternMatchMode.Exact
    else -> null
}
