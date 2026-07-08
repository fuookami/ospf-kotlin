/**
 * 布尔表达式求倌
 * Boolean Expression Evaluation
 *
 * 提供布尔表达式的本地求值能力，支持比较、逻辑、空值检查、集合成员判断、模式匹配。
 * Provides local evaluation capability for boolean expressions,
 * supporting comparison, logic, null check, set membership, and pattern matching.
 */
package fuookami.ospf.kotlin.math.symbol.expression.operation

import java.math.*
import kotlin.math.abs
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 求值上下文
 * Evaluation Context
 *
 * 提供属性路径到值的映射。
 * Provides mapping from property paths to values.
 */
interface EvaluationContext {
    /**
     * 获取指定路径的倌
     * Get value at specified path
     *
     * @param path 属性路後/ Property path
     * @return 路径对应的值，如果不存在则返回 null / Value at path, null if not exists
     */
    operator fun get(path: PropertyPath): Any?

    /**
     * 检查指定路径是否存在
     * Check if specified path exists
     *
     * @param path 属性路径 / Property path
     * @return 是否存在 / Whether exists
     */
    fun contains(path: PropertyPath): Boolean
}

/**
 * 基于 Map 的求值上下文
 * Map-based evaluation context
 */
class MapEvaluationContext private constructor(
    private val values: Map<PropertyPath, Any?>
) : EvaluationContext {
    companion object {
        /**
         * 从 PropertyPath 映射创建上下文 / Create context from PropertyPath map
         *
         * @param values 属性路径到值的映射 / Property path to value mapping
         * @return 求值上下文 / Evaluation context
         */
        fun fromPathMap(values: Map<PropertyPath, Any?>): MapEvaluationContext = MapEvaluationContext(values)

        /**
         * 从字符串映射创建上下文 / Create context from string map
         *
         * @param values 字符串路径到值的映射 / String path to value mapping
         * @return 求值上下文 / Evaluation context
         */
        fun fromStringMap(values: Map<String, Any?>): MapEvaluationContext = MapEvaluationContext(
            values.mapKeys { PropertyPath.parse(it.key) }
        )
    }

    override fun get(path: PropertyPath): Any? = values[path]

    override fun contains(path: PropertyPath): Boolean = values.containsKey(path)
}

/**
 * 空求值上下文
 * Empty evaluation context
 */
object EmptyEvaluationContext : EvaluationContext {
    override fun get(path: PropertyPath): Any? = null

    override fun contains(path: PropertyPath): Boolean = false
}

/**
 * 求值结枌
 * Evaluation Result
 *
 * 返回 Trivalent 以支持三值逻辑。
 * Returns Trivalent to support three-valued logic.
 */
typealias EvaluationResult = Trivalent

/**
 * 求值布尔表达式
 * Evaluate boolean expression
 *
 * @param expr 要求值的表达弌/ Expression to evaluate
 * @param context 求值上下文 / Evaluation context
 * @return 求值结果（三值逻辑， Evaluation result (three-valued logic)
 */
fun evaluateBoolean(expr: BooleanExpression, context: EvaluationContext): EvaluationResult {
    return when (expr) {
        is BooleanConstant -> expr.value

        is Comparison<*> -> evaluateComparison(expr, context)

        is InExpression<*> -> evaluateIn(expr, context)

        is PatternMatch<*> -> evaluatePatternMatch(expr, context)

        is NullCheck -> evaluateNullCheck(expr, context)

        is AndExpression -> evaluateAnd(expr, context)

        is OrExpression -> evaluateOr(expr, context)

        is NotExpression -> evaluateNot(expr, context)

        is BooleanCustom -> Trivalent.Unknown
    }
}

/**
 * 求值布尔表达式（返回可空布尔）
 * Evaluate boolean expression (returning nullable boolean)
 *
 * @param expr 要求值的表达式 / Expression to evaluate
 * @param context 求值上下文 / Evaluation context
 * @return 求值结果（true/false/null 表示未知） / Evaluation result (true/false/null for unknown)
 */
fun evaluateBooleanOrNull(expr: BooleanExpression, context: EvaluationContext): Boolean? {
    return when (evaluateBoolean(expr, context)) {
        Trivalent.True -> true
        Trivalent.False -> false
        Trivalent.Unknown -> null
    }
}

// ========== 内部求值函敌/ Internal Evaluation Functions ==========

/**
 * 求值比较表达式
 * Evaluate comparison expression
 *
 * @param expr the comparison expression to evaluate / 要求值的比较表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateComparison(expr: Comparison<*>, context: EvaluationContext): Trivalent {
    val leftValue = evaluateScalarInternal(expr.left, context) ?: return Trivalent.Unknown
    val rightValue = evaluateScalarInternal(expr.right, context) ?: return Trivalent.Unknown

    val result = compareValues(leftValue, rightValue, expr.operator) ?: return Trivalent.Unknown
    return Trivalent(result)
}

/**
 * 求值 In 表达式
 * Evaluate In expression
 *
 * @param expr the in expression to evaluate / 要求值的 In 表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateIn(expr: InExpression<*>, context: EvaluationContext): Trivalent {
    val value = evaluateScalarInternal(expr.value, context) ?: return Trivalent.Unknown

    var isIn = false
    for (candidateExpr in expr.candidates) {
        val candidate = evaluateScalarInternal(candidateExpr, context) ?: continue
        if (valuesEqual(value, candidate)) {
            isIn = true
            break
        }
    }

    return Trivalent(if (expr.negated) !isIn else isIn)
}

/**
 * 求值模式匹配表达式
 * Evaluate pattern match expression
 *
 * @param expr the pattern match expression to evaluate / 要求值的模式匹配表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluatePatternMatch(expr: PatternMatch<*>, context: EvaluationContext): Trivalent {
    val value = evaluateScalarInternal(expr.value, context)?.toString() ?: return Trivalent.Unknown
    val pattern = evaluateScalarInternal(expr.pattern, context)?.toString() ?: return Trivalent.Unknown

    val matches = when (expr.mode) {
        PatternMatchMode.Exact -> value == pattern
        PatternMatchMode.Prefix -> value.startsWith(pattern)
        PatternMatchMode.Suffix -> value.endsWith(pattern)
        PatternMatchMode.Contains -> value.contains(pattern)
        PatternMatchMode.Like -> matchLike(value, pattern)
        PatternMatchMode.Regex -> {
            try {
                value.matches(Regex(pattern))
            } catch (e: Exception) {
                return Trivalent.Unknown
            }
        }
    }

    return Trivalent(if (expr.negated) !matches else matches)
}

/**
 * 求值空值检查
 * Evaluate null check
 *
 * @param expr the null check expression to evaluate / 要求值的空值检查表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateNullCheck(expr: NullCheck, context: EvaluationContext): Trivalent {
    if (!context.contains(expr.path)) {
        return Trivalent.Unknown
    }
    val value = context[expr.path]

    val isNull = value == null

    return Trivalent(
        if (expr.isNull) isNull else !isNull
    )
}

/**
 * 求值 And 表达式
 * Evaluate And expression
 *
 * @param expr the and expression to evaluate / 要求值的 And 表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateAnd(expr: AndExpression, context: EvaluationContext): Trivalent {
    var hasUnknown = false
    for (operand in expr.operands) {
        val result = evaluateBoolean(operand, context)
        if (result == Trivalent.False) {
            return Trivalent.False
        }
        if (result == Trivalent.Unknown) {
            hasUnknown = true
        }
    }
    return if (hasUnknown) Trivalent.Unknown else Trivalent.True
}

/**
 * 求值 Or 表达式
 * Evaluate Or expression
 *
 * @param expr the or expression to evaluate / 要求值的 Or 表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateOr(expr: OrExpression, context: EvaluationContext): Trivalent {
    var hasUnknown = false
    for (operand in expr.operands) {
        val result = evaluateBoolean(operand, context)
        if (result == Trivalent.True) {
            return Trivalent.True
        }
        if (result == Trivalent.Unknown) {
            hasUnknown = true
        }
    }
    return if (hasUnknown) Trivalent.Unknown else Trivalent.False
}

/**
 * 求值 Not 表达式
 * Evaluate Not expression
 *
 * @param expr the not expression to evaluate / 要求值的 Not 表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluation result in three-valued logic / 三值逻辑的求值结果
 */
private fun evaluateNot(expr: NotExpression, context: EvaluationContext): Trivalent {
    return when (evaluateBoolean(expr.operand, context)) {
        Trivalent.True -> Trivalent.False
        Trivalent.False -> Trivalent.True
        Trivalent.Unknown -> Trivalent.Unknown
    }
}

/**
 * 求值标量表达式
 * Evaluate scalar expression
 *
 * @param expr the scalar expression to evaluate / 要求值的标量表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluated value, or null if evaluation is not possible / 求值结果，无法求值时返回 null
 */
@Suppress("FunctionName")
private fun evaluateScalarInternal(expr: ScalarExpression<*>, context: EvaluationContext): Any? {
    val result = evaluateScalar(expr, context, DefaultScalarFunctionEvaluator)
    return when (result) {
        is Ok<*, *, *> -> result.value
        is Failed<*, *, *> -> null
        is Fatal<*, *, *> -> null
    }
}

/**
 * 默认标量函数求值器
 * Default scalar function evaluator
 */
object DefaultScalarFunctionEvaluator : ScalarFunctionEvaluator {
    override fun evaluate(name: String, arguments: List<Any?>): Any? {
        return when (name.lowercase()) {
            ScalarFunctionNames.Abs -> evaluateAbs(arguments)
            ScalarFunctionNames.Lower -> evaluateStringUnary(name, arguments) { it.lowercase() }
            ScalarFunctionNames.Upper -> evaluateStringUnary(name, arguments) { it.uppercase() }
            ScalarFunctionNames.Trim -> evaluateStringUnary(name, arguments) { it.trim() }
            ScalarFunctionNames.Length -> evaluateStringUnary(name, arguments) { it.length }
            ScalarFunctionNames.Coalesce -> evaluateCoalesce(arguments)
            else -> null
        }
    }

    /**
     * 计算绝对值，支持多种数值类型
     * Compute absolute value, supporting multiple numeric types
     *
     * @param arguments the argument list (must contain exactly one numeric value) / 参数列表（必须恰好包含一个数值）
     * @return the absolute value, or null if arguments are invalid / 绝对值，参数无效时返回 null
     */
    private fun evaluateAbs(arguments: List<Any?>): Any? {
        if (arguments.size != 1) {
            return null
        }
        val value = arguments[0] ?: return null
        if (value !is Number) {
            return null
        }
        return when (value) {
            is Byte -> abs(value.toInt())
            is Short -> abs(value.toInt())
            is Int -> abs(value)
            is Long -> abs(value)
            is Float -> abs(value)
            is Double -> abs(value)
            is BigDecimal -> value.abs()
            is BigInteger -> value.abs()
            else -> null
        }
    }

    /**
     * 对字符串参数执行一元运算
     * Apply a unary operation to a string argument
     *
     * @param name 函数名称 / Function name
     * @param arguments 参数列表 / Argument list
     * @param operation 字符串变换运算 / String transformation operation
     * @return 运算结果，参数为 null 时返回 null / Operation result, or null if the argument is null
     */
    private fun evaluateStringUnary(
        name: String,
        arguments: List<Any?>,
        operation: (String) -> Any
    ): Any? {
        if (arguments.size != 1) {
            return null
        }
        val value = arguments[0] ?: return null
        if (value !is String) {
            return null
        }
        return operation(value)
    }

    /**
     * 返回参数列表中第一个非空值
     * Return the first non-null value from the argument list
     *
     * @param arguments the argument list / 参数列表
     * @return the first non-null value, or null if all values are null / 第一个非空值，全部为 null 时返回 null
     */
    private fun evaluateCoalesce(arguments: List<Any?>): Any? {
        if (arguments.isEmpty()) {
            return null
        }
        return arguments.firstOrNull { it != null }
    }
}

/**
 * 求值一元操作
 * Evaluate unary operation
 *
 * @param expr the unary expression to evaluate / 要求值的一元表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluated value, or null if evaluation is not possible / 求值结果，无法求值时返回 null
 */
private fun evaluateUnary(expr: UnaryExpression<*>, context: EvaluationContext): Any? {
    val operand = evaluateScalarInternal(expr.operand, context) ?: return null
    return NumericDispatcher.evaluateUnary(expr.operator, operand)
}

// 使用类型别名避免与表达式类冲窌
// Use type alias to avoid conflict with expression class
private typealias UnaryExpression<T> = ScalarUnary<T>
private typealias BinaryExpression<T> = ScalarBinary<T>

/**
 * 求值二元操作
 * Evaluate binary operation
 *
 * @param expr the binary expression to evaluate / 要求值的二元表达式
 * @param context the evaluation context / 求值上下文
 * @return the evaluated value, or null if evaluation is not possible / 求值结果，无法求值时返回 null
 */
private fun evaluateBinary(expr: BinaryExpression<*>, context: EvaluationContext): Any? {
    val left = evaluateScalarInternal(expr.left, context) ?: return null
    val right = evaluateScalarInternal(expr.right, context) ?: return null
    return NumericDispatcher.evaluateBinary(expr.operator, left, right)
}

// ========== 辅助函数 / Helper Functions ==========

/**
 * 比较两个值
 * Compare two values
 *
 * @param left the left operand / 左操作数
 * @param right the right operand / 右操作数
 * @param operator the comparison operator / 比较运算符
 * @return the comparison result, or null if either value is null / 比较结果，任一值为 null 时返回 null
 */
private fun compareValues(left: Any?, right: Any?, operator: ComparisonOperator): Boolean? {
    if (left == null || right == null) return null

    return when (operator) {
        ComparisonOperator.Eq -> valuesEqual(left, right)
        ComparisonOperator.Ne -> !valuesEqual(left, right)
        ComparisonOperator.Lt -> compareOrder(left, right)?.let { it < 0 }
        ComparisonOperator.Le -> compareOrder(left, right)?.let { it <= 0 }
        ComparisonOperator.Gt -> compareOrder(left, right)?.let { it > 0 }
        ComparisonOperator.Ge -> compareOrder(left, right)?.let { it >= 0 }
    }
}

/**
 * 比较两个值的大小
 * Compare magnitude of two values
 *
 * @param left the left operand / 左操作数
 * @param right the right operand / 右操作数
 * @return comparison result (negative if less, zero if equal, positive if greater), or null if types are incompatible / 比较结果（负值表示小于，零表示相等，正值表示大于），类型不兼容时返回 null
 */
private fun compareOrder(left: Any, right: Any): Int? {
    return when {
        left is Number && right is Number -> compareNumbers(left, right)
        left is Comparable<*> && right::class == left::class -> {
            compareSameTypeComparable(left, right)
        }
        left is String && right is String -> left.compareTo(right)
        else -> null
    }
}

/**
 * 比较两个相同运行时类型的 Comparable 值
 * Compare two Comparable values of the same runtime type
 *
 * @param left 左操作数 / Left operand
 * @param right 右操作数 / Right operand
 * @return 比较结果，负值表示小于，零表示相等，正值表示大于 / Comparison result: negative if less, zero if equal, positive if greater
 */
@Suppress("UNCHECKED_CAST")
private fun compareSameTypeComparable(left: Any, right: Any): Int {
    // 安全不变量：调用点先保证 right::class == left::class，compareTo 的参数类型与 left 运行时类型一致。
    // Safety invariant: call site guarantees right::class == left::class, so compareTo receives the runtime-compatible type of left.
    return (left as Comparable<Any>).compareTo(right)
}

/**
 * 判断两个值是否相等
 * Check if two values are equal
 *
 * @param left the left operand / 左操作数
 * @param right the right operand / 右操作数
 * @return whether the two values are equal / 两个值是否相等
 */
private fun valuesEqual(left: Any?, right: Any?): Boolean {
    if (left == null && right == null) return true
    if (left == null || right == null) return false

    return when {
        left is Number && right is Number -> compareNumbers(left, right) == 0
        else -> left == right
    }
}

/**
 * 通过 BigDecimal 比较两个数值
 * Compare two numbers via BigDecimal conversion
 *
 * @param left 左操作数 / Left operand
 * @param right 右操作数 / Right operand
 * @return 比较结果，无法转换时返回 null / Comparison result, or null if conversion fails
 */
private fun compareNumbers(left: Number, right: Number): Int? {
    val leftDecimal = left.toBigDecimalOrNull() ?: return null
    val rightDecimal = right.toBigDecimalOrNull() ?: return null
    return leftDecimal.compareTo(rightDecimal)
}

/**
 * 将数值安全转换为 BigDecimal，非有限浮点数返回 null
 * Safely convert a Number to BigDecimal, returning null for non-finite floats
 *
 * @return the BigDecimal representation, or null for non-finite floats / BigDecimal 表示，非有限浮点数返回 null
 */
private fun Number.toBigDecimalOrNull(): BigDecimal? {
    return when (this) {
        is BigDecimal -> this
        is BigInteger -> BigDecimal(this)
        is Byte, is Short, is Int, is Long -> BigDecimal.valueOf(this.toLong())
        is Float -> if (this.isFinite()) BigDecimal(this.toString()) else null
        is Double -> if (this.isFinite()) BigDecimal(this.toString()) else null
        else -> this.toString().toBigDecimalOrNull()
    }
}

/**
 * LIKE 模式匹配
 * LIKE pattern matching
 *
 * 支持 %（任意字符）和_（单个字符）通配符。
 * Supports % (any characters) and _ (single character) wildcards.
 *
 * @param value the string value to match / 要匹配的字符串值
 * @param pattern the LIKE pattern / LIKE 模式
 * @return whether the value matches the pattern / 值是否匹配模式
 */
private fun matchLike(value: String, pattern: String): Boolean {
    // 尌SQL LIKE 模式转换为正则表达式
    // Convert SQL LIKE pattern to regex
    val regexPattern = StringBuilder()
    regexPattern.append("^")

    for (char in pattern) {
        when (char) {
            '%' -> regexPattern.append(".*")
            '_' -> regexPattern.append(".")
            else -> {
                if (char.isLetterOrDigit()) {
                    regexPattern.append(char)
                } else {
                    regexPattern.append("\\").append(char)
                }
            }
        }
    }

    regexPattern.append("$")

    return try {
        value.matches(Regex(regexPattern.toString()))
    } catch (e: Exception) {
        false
    }
}

// ========== 便捷扩展函数 / Convenience Extension Functions ==========

/**
 * 使用 Map 上下文求值布尔表达式
 * Evaluate boolean expression with Map context
 *
 * @param values 字符串路径到值的映射 / String path to value mapping
 * @return 求值结果（三值逻辑） / Evaluation result (three-valued logic)
 */
fun BooleanExpression.evaluateWith(values: Map<String, Any?>): Trivalent {
    return evaluateBoolean(this, MapEvaluationContext.fromStringMap(values))
}

/**
 * 使用 Map 上下文求值布尔表达式（返回可空布尔）
 * Evaluate boolean expression with Map context (returning nullable boolean)
 *
 * @param values 字符串路径到值的映射 / String path to value mapping
 * @return 求值结果（true/false/null 表示未知） / Evaluation result (true/false/null for unknown)
 */
fun BooleanExpression.evaluateWithOrNull(values: Map<String, Any?>): Boolean? {
    return evaluateBooleanOrNull(this, MapEvaluationContext.fromStringMap(values))
}
