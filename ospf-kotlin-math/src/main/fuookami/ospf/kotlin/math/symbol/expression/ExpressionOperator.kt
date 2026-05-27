/**
 * 表达式操作符
 * Expression Operator
 *
 * 定义表达式系统中使用的操作符类型，包括标量操作符、比较操作符和模式匹配模式。
 * Defines operator types used in the expression system,
 * including scalar operators, comparison operators, and pattern match modes.
 */
package fuookami.ospf.kotlin.math.symbol.expression

/**
 * 标量一元操作符
 * Scalar Unary Operator
 */
enum class UnaryOperator {
    /**
     * 负号 / Negation
     */
    Negate,

    /**
     * 正号 / Positive (identity)
     */
    Positive,

    /**
     * 绝对倌/ Absolute value
     */
    Abs
}

/**
 * 标量二元操作笌
 * Scalar Binary Operator
 */
enum class BinaryOperator {
    /**
     * 加法 / Addition
     */
    Add,

    /**
     * 减法 / Subtraction
     */
    Subtract,

    /**
     * 乘法 / Multiplication
     */
    Multiply,

    /**
     * 除法 / Division
     */
    Divide,

    /**
     * 取模 / Modulo
     */
    Modulo,

    /**
     * 幂运箌/ Power
     */
    Power
}

/**
 * 比较操作笌
 * Comparison Operator
 */
enum class ComparisonOperator {
    /**
     * 等于 / Equal
     */
    Eq,

    /**
     * 不等二/ Not Equal
     */
    Ne,

    /**
     * 小于 / Less Than
     */
    Lt,

    /**
     * 小于等于 / Less Than or Equal
     */
    Le,

    /**
     * 大于 / Greater Than
     */
    Gt,

    /**
     * 大于等于 / Greater Than or Equal
     */
    Ge
}

/**
 * 模式匹配模式
 * Pattern Match Mode
 *
 * 定义通用的模式匹配语义，不携希SQL 方言细节。
 * Defines generic pattern matching semantics without SQL dialect details.
 */
enum class PatternMatchMode {
    /**
     * 精确匹配 / Exact match
     *
     * 整个字符串必须完全匹配模式。
     * The entire string must exactly match the pattern.
     */
    Exact,

    /**
     * 前缀匹配 / Prefix Match
     *
     * 字符串以模式开头。
     * String starts with the pattern.
     */
    Prefix,

    /**
     * 后缀匹配 / Suffix Match
     *
     * 字符串以模式结尾。
     * String ends with the pattern.
     */
    Suffix,

    /**
     * 包含匹配 / Contains Match
     *
     * 字符串包含模式。
     * String contains the pattern.
     */
    Contains,

    /**
     * 通配符匹酌/ Wildcard Match
     *
     * 支持 SQL LIKE 风格的通配符（% 和_）。
     * Supports SQL LIKE-style wildcards (% and _).
     *
     * 注意：这是通用语义，具佌SQL 方言映射甌framework 层处理。
     * Note: This is generic semantics, specific SQL dialect mapping is handled by framework layer.
     */
    Like,

    /**
     * 正则匹配 / Regex Match
     *
     * 支持正则表达式。
     * Supports regular expressions.
     */
    Regex
}

/**
 * 布尔操作笌
 * Boolean Operator
 */
enum class BooleanOperator {
    /**
     * 逻辑丌/ Logical AND
     */
    And,

    /**
     * 逻辑戌/ Logical OR
     */
    Or,

    /**
     * 逻辑靌/ Logical NOT
     */
    Not
}

/**
 * 空值检查类垌
 * Null Check Type
 */
enum class NullCheckType {
    /**
     * 是空倌/ Is Null
     */
    IsNull,

    /**
     * 非空倌/ Is Not Null
     */
    IsNotNull
}

/**
 * 操作符符号映尌
 * Operator Symbol Mapping
 *
 * 提供操作符到字符串符号的映射。
 * Provides mapping from operators to string symbols.
 */
object OperatorSymbols {
    /**
     * 获取一元操作符的符号 / Get symbol for unary operator
     *
     * @param op 一元操作符 / Unary operator
     * @return 操作符符号字符串 / Operator symbol string
     */
    fun unary(op: UnaryOperator): String = when (op) {
        UnaryOperator.Negate -> "-"
        UnaryOperator.Positive -> "+"
        UnaryOperator.Abs -> "abs"
    }

    /**
     * 获取二元操作符的符号 / Get symbol for binary operator
     *
     * @param op 二元操作符 / Binary operator
     * @return 操作符符号字符串 / Operator symbol string
     */
    fun binary(op: BinaryOperator): String = when (op) {
        BinaryOperator.Add -> "+"
        BinaryOperator.Subtract -> "-"
        BinaryOperator.Multiply -> "*"
        BinaryOperator.Divide -> "/"
        BinaryOperator.Modulo -> "%"
        BinaryOperator.Power -> "^"
    }

    /**
     * 获取比较操作符的符号 / Get symbol for comparison operator
     *
     * @param op 比较操作符 / Comparison operator
     * @return 操作符符号字符串 / Operator symbol string
     */
    fun comparison(op: ComparisonOperator): String = when (op) {
        ComparisonOperator.Eq -> "="
        ComparisonOperator.Ne -> "<>"
        ComparisonOperator.Lt -> "<"
        ComparisonOperator.Le -> "<="
        ComparisonOperator.Gt -> ">"
        ComparisonOperator.Ge -> ">="
    }

    /**
     * 获取布尔操作符的符号 / Get symbol for boolean operator
     *
     * @param op 布尔操作符 / Boolean operator
     * @return 操作符符号字符串 / Operator symbol string
     */
    fun boolean(op: BooleanOperator): String = when (op) {
        BooleanOperator.And -> "and"
        BooleanOperator.Or -> "or"
        BooleanOperator.Not -> "not"
    }

    /**
     * 获取空值检查类型的符号 / Get symbol for null check type
     *
     * @param type 空值检查类型 / Null check type
     * @return 操作符符号字符串 / Operator symbol string
     */
    fun nullCheck(type: NullCheckType): String = when (type) {
        NullCheckType.IsNull -> "is null"
        NullCheckType.IsNotNull -> "is not null"
    }
}

/**
 * 比较操作符反轌
 * Comparison Operator Inversion
 *
 * 返回比较操作符的反向操作符（妌< 变为 >）。
 * Returns the inverse of a comparison operator (e.g., < becomes >).
 *
 * @return 反向比较操作符 / Inverse comparison operator
 */
fun ComparisonOperator.inverse(): ComparisonOperator = when (this) {
    ComparisonOperator.Eq -> ComparisonOperator.Ne
    ComparisonOperator.Ne -> ComparisonOperator.Eq
    ComparisonOperator.Lt -> ComparisonOperator.Gt
    ComparisonOperator.Le -> ComparisonOperator.Ge
    ComparisonOperator.Gt -> ComparisonOperator.Lt
    ComparisonOperator.Ge -> ComparisonOperator.Le
}
