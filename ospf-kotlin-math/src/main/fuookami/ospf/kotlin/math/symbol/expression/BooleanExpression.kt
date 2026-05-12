/**
 * 布尔表达弌
 * Boolean Expression
 *
 * 定义布尔值的表达弌AST，包括布尔常量、比较、集合成员判断、模式匹配、空值检查、逻辑组合和自定义表达式。
 * Defines the expression AST for boolean values,
 * including boolean constant, comparison, set membership, pattern match, null check,
 * logical combination, and custom expressions.
 */
package fuookami.ospf.kotlin.math.symbol.expression

import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.*

/**
 * 布尔表达弌
 * Boolean Expression
 *
 * 表示布尔值的表达式，支持比较操作和完整的逻辑组合（and/or/not）。
 * Represents an expression for boolean values,
 * supporting comparison operations and full logical combination (and/or/not).
 */
sealed interface BooleanExpression {

    /**
     * 表达式类型名秌
     * Expression type name
     */
    val typeName: String

    /**
     * 子表达式列表
     * Child expressions list
     */
    val children: List<BooleanExpression>

    /**
     * 判断表达式是否是常量
     * Check if expression is constant
     */
    fun isConstant(): Boolean = when (this) {
        is BooleanConstant -> true
        is Comparison<*> -> left.isConstant() && right.isConstant()
        is InExpression<*> -> value.isConstant()
        is PatternMatch<*> -> value.isConstant()
        is NullCheck -> false
        is AndExpression -> operands.all { it.isConstant() }
        is OrExpression -> operands.all { it.isConstant() }
        is NotExpression -> operand.isConstant()
        is BooleanCustom -> false
    }

    /**
     * 判断表达式是否是纯逻辑表达式（不含比较等）
     * Check if expression is pure logical (no comparisons etc.)
     */
    fun isPureLogical(): Boolean = when (this) {
        is BooleanConstant -> true
        is Comparison<*>, is InExpression<*>, is PatternMatch<*>, is NullCheck -> false
        is AndExpression -> operands.all { it.isPureLogical() }
        is OrExpression -> operands.all { it.isPureLogical() }
        is NotExpression -> operand.isPureLogical()
        is BooleanCustom -> false
    }

    /**
     * 获取表达式中的所有引用路後
     * Get all reference paths in the expression
     */
    fun collectReferences(): Set<PropertyPath> {
        val refs = mutableSetOf<PropertyPath>()
        collectReferencesInto(refs)
        return refs
    }

    /**
     * 将引用路径收集到指定集合
     * Collect reference paths into specified collection
     */
    fun collectReferencesInto(refs: MutableSet<PropertyPath>) {
        when (this) {
            is Comparison<*> -> {
                (left as? ScalarReference<*>)?.let { refs.add(it.path) }
                (right as? ScalarReference<*>)?.let { refs.add(it.path) }
                left.collectReferencesInto(refs)
                right.collectReferencesInto(refs)
            }
            is InExpression<*> -> {
                value.collectReferencesInto(refs)
                candidates.forEach { it.collectReferencesInto(refs) }
            }
            is PatternMatch<*> -> {
                value.collectReferencesInto(refs)
            }
            is NullCheck -> {
                refs.add(path)
            }
            is AndExpression -> {
                operands.forEach { it.collectReferencesInto(refs) }
            }
            is OrExpression -> {
                operands.forEach { it.collectReferencesInto(refs) }
            }
            is NotExpression -> {
                operand.collectReferencesInto(refs)
            }
            is BooleanConstant, is BooleanCustom -> { }
        }
    }

    /**
     * 获取逻辑操作符数里
     * Get number of logical operators
     */
    fun logicalOperatorCount(): Int = when (this) {
        is BooleanConstant, is Comparison<*>, is InExpression<*>, is PatternMatch<*>, is NullCheck -> 0
        is AndExpression -> operands.size + operands.sumOf { it.logicalOperatorCount() }
        is OrExpression -> operands.size + operands.sumOf { it.logicalOperatorCount() }
        is NotExpression -> 1 + operand.logicalOperatorCount()
        is BooleanCustom -> 0
    }

    /**
     * 获取表达式深庌
     * Get expression depth
     */
    fun depth(): Int = when (this) {
        is BooleanConstant, is Comparison<*>, is InExpression<*>, is PatternMatch<*>, is NullCheck -> 1
        is AndExpression -> 1 + (operands.maxOfOrNull { it.depth() } ?: 0)
        is OrExpression -> 1 + (operands.maxOfOrNull { it.depth() } ?: 0)
        is NotExpression -> 1 + operand.depth()
        is BooleanCustom -> 1
    }
}

/**
 * 布尔常量
 * Boolean Constant
 *
 * 表示布尔常量值，支持三值逻辑（True/False/Unknown）。
 * Represents a boolean constant value, supporting three-valued logic (True/False/Unknown).
 */
data class BooleanConstant(
    val value: Trivalent
) : BooleanExpression {
    override val typeName: String = "BooleanConstant"
    override val children: List<BooleanExpression> = emptyList()

    /**
     * 判断是否为真
     * Check if value is true
     */
    val isTrue: Boolean get() = value == Trivalent.True

    /**
     * 判断是否为假
     * Check if value is false
     */
    val isFalse: Boolean get() = value == Trivalent.False

    /**
     * 判断是否为未知（null，
     * Check if value is unknown (null)
     */
    val isUnknown: Boolean get() = value == Trivalent.Unknown

    override fun toString(): String = when (value) {
        Trivalent.True -> "true"
        Trivalent.False -> "false"
        Trivalent.Unknown -> "unknown"
    }

    companion object {
        fun true_(): BooleanConstant = BooleanConstant(Trivalent.True)
        fun false_(): BooleanConstant = BooleanConstant(Trivalent.False)
        fun unknown(): BooleanConstant = BooleanConstant(Trivalent.Unknown)
    }
}

/**
 * 比较表达弌
 * Comparison Expression
 *
 * 表示两个标量值之间的比较操作。
 * Represents a comparison operation between two scalar values.
 */
data class Comparison<T>(
    val operator: ComparisonOperator,
    val left: ScalarExpression<T>,
    val right: ScalarExpression<T>
) : BooleanExpression {
    override val typeName: String = "Comparison"
    override val children: List<BooleanExpression> = emptyList()

    override fun toString(): String =
        "$left ${OperatorSymbols.comparison(operator)} $right"
}

/**
 * 集合成员判断表达式（In，
 * Set Membership Expression (In)
 *
 * 判断值是否在候选集合中。
 * Checks if a value is in a candidate set.
 */
data class InExpression<T>(
    val value: ScalarExpression<T>,
    val candidates: List<ScalarExpression<T>>,
    val negated: Boolean = false
) : BooleanExpression {
    override val typeName: String = if (negated) "NotIn" else "In"
    override val children: List<BooleanExpression> = emptyList()

    /**
     * 判断是否是否定形式（not in，
     * Check if this is negated form (not in)
     */
    val isNegated: Boolean get() = negated

    override fun toString(): String = if (negated) {
        "$value not in (${candidates.joinToString(", ")})"
    } else {
        "$value in (${candidates.joinToString(", ")})"
    }
}

/**
 * 模式匹配表达弌
 * Pattern Match Expression
 *
 * 判断值是否匹配指定模式。
 * Checks if a value matches a specified pattern.
 */
data class PatternMatch<T>(
    val value: ScalarExpression<T>,
    val pattern: ScalarExpression<T>,
    val mode: PatternMatchMode,
    val negated: Boolean = false
) : BooleanExpression {
    override val typeName: String = if (negated) "NotPatternMatch" else "PatternMatch"
    override val children: List<BooleanExpression> = emptyList()

    override fun toString(): String = if (negated) {
        "$value not ${mode.name.lowercase()} $pattern"
    } else {
        "$value ${mode.name.lowercase()} $pattern"
    }
}

/**
 * 空值检查表达式
 * Null Check Expression
 *
 * 判断属性路径是否为空值或非空值。
 * Checks if a property path is null or not null.
 */
data class NullCheck(
    val path: PropertyPath,
    val type: NullCheckType,
    val symbol: PathSymbol = path.toPathSymbol()
) : BooleanExpression {
    override val typeName: String = "NullCheck"
    override val children: List<BooleanExpression> = emptyList()

    /**
     * 判断是否检查空倌
     * Check if this checks for null
     */
    val isNull: Boolean get() = type == NullCheckType.IsNull

    /**
     * 判断是否检查非穌
     * Check if this checks for not null
     */
    val isNotNull: Boolean get() = type == NullCheckType.IsNotNull

    override fun toString(): String = "${path.value} ${OperatorSymbols.nullCheck(type)}"
}

/**
 * 逻辑与表达式
 * Logical AND Expression
 *
 * 表示多个布尔表达式的逻辑与组合。
 * Represents logical AND combination of multiple boolean expressions.
 */
data class AndExpression(
    val operands: List<BooleanExpression>
) : BooleanExpression {
    override val typeName: String = "And"
    override val children: List<BooleanExpression> = operands

    init {
        require(operands.isNotEmpty()) { "And expression requires at least one operand" }
    }

    override fun toString(): String = operands.joinToString(" and ") { "($it)" }
}

/**
 * 逻辑或表达式
 * Logical OR Expression
 *
 * 表示多个布尔表达式的逻辑或组合。
 * Represents logical OR combination of multiple boolean expressions.
 */
data class OrExpression(
    val operands: List<BooleanExpression>
) : BooleanExpression {
    override val typeName: String = "Or"
    override val children: List<BooleanExpression> = operands

    init {
        require(operands.isNotEmpty()) { "Or expression requires at least one operand" }
    }

    override fun toString(): String = operands.joinToString(" or ") { "($it)" }
}

/**
 * 逻辑非表达式
 * Logical NOT Expression
 *
 * 表示布尔表达式的逻辑非。
 * Represents logical NOT of a boolean expression.
 */
data class NotExpression(
    val operand: BooleanExpression
) : BooleanExpression {
    override val typeName: String = "Not"
    override val children: List<BooleanExpression> = listOf(operand)

    override fun toString(): String = "not ($operand)"
}

/**
 * 布尔自定义表达式
 * Boolean Custom Expression
 *
 * 表示自定义布尔表达式，用于扩展。
 * Represents a custom boolean expression for extension.
 */
data class BooleanCustom(
    val value: Any,
    val description: String? = null
) : BooleanExpression {
    override val typeName: String = "Custom"
    override val children: List<BooleanExpression> = emptyList()

    override fun toString(): String = description ?: "Custom($value)"
}

/**
 * 布尔表达式工厌
 * Boolean Expression Factory
 *
 * 提供便捷的布尔表达式构造方法。
 * Provides convenient boolean expression construction methods.
 */
object BooleanExpressionFactory {
    /**
     * 创建布尔常量
     * Create boolean constant
     */
    fun constant(value: Trivalent): BooleanConstant = BooleanConstant(value)

    /**
     * 创建布尔常量（true，
     * Create boolean constant (true)
     */
    fun trueConstant(): BooleanConstant = BooleanConstant.true_()

    /**
     * 创建布尔常量（false，
     * Create boolean constant (false)
     */
    fun falseConstant(): BooleanConstant = BooleanConstant.false_()

    /**
     * 创建布尔常量（unknown/null，
     * Create boolean constant (unknown/null)
     */
    fun unknownConstant(): BooleanConstant = BooleanConstant.unknown()

    /**
     * 创建比较表达弌
     * Create comparison expression
     */
    fun <T> comparison(
        operator: ComparisonOperator,
        left: ScalarExpression<T>,
        right: ScalarExpression<T>
    ): Comparison<T> = Comparison(operator, left, right)

    /**
     * 创建等于表达弌
     * Create equals expression
     */
    fun <T> eq(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Eq, left, right)

    /**
     * 创建不等于表达式
     * Create not equals expression
     */
    fun <T> ne(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Ne, left, right)

    /**
     * 创建小于表达弌
     * Create less than expression
     */
    fun <T> lt(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Lt, left, right)

    /**
     * 创建小于等于表达弌
     * Create less than or equal expression
     */
    fun <T> le(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Le, left, right)

    /**
     * 创建大于表达弌
     * Create greater than expression
     */
    fun <T> gt(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Gt, left, right)

    /**
     * 创建大于等于表达弌
     * Create greater than or equal expression
     */
    fun <T> ge(left: ScalarExpression<T>, right: ScalarExpression<T>): Comparison<T> =
        comparison(ComparisonOperator.Ge, left, right)

    /**
     * 创建 In 表达弌
     * Create In expression
     */
    fun <T> inExpr(
        value: ScalarExpression<T>,
        candidates: List<ScalarExpression<T>>,
        negated: Boolean = false
    ): InExpression<T> = InExpression(value, candidates, negated)

    /**
     * 创建 Not In 表达弌
     * Create Not In expression
     */
    fun <T> notIn(
        value: ScalarExpression<T>,
        candidates: List<ScalarExpression<T>>
    ): InExpression<T> = InExpression(value, candidates, negated = true)

    /**
     * 创建模式匹配表达弌
     * Create pattern match expression
     */
    fun <T> patternMatch(
        value: ScalarExpression<T>,
        pattern: ScalarExpression<T>,
        mode: PatternMatchMode,
        negated: Boolean = false
    ): PatternMatch<T> = PatternMatch(value, pattern, mode, negated)

    /**
     * 创建空值检查表达式
     * Create null check expression
     */
    fun isNull(path: PropertyPath): NullCheck = NullCheck(path, NullCheckType.IsNull)

    /**
     * 创建非空检查表达式
     * Create not null check expression
     */
    fun isNotNull(path: PropertyPath): NullCheck = NullCheck(path, NullCheckType.IsNotNull)

    /**
     * 创建逻辑与表达式
     * Create logical AND expression
     */
    fun and(operands: List<BooleanExpression>): AndExpression = AndExpression(operands)

    /**
     * 创建逻辑与表达式（可变参数）
     * Create logical AND expression (vararg)
     */
    fun and(first: BooleanExpression, second: BooleanExpression, vararg rest: BooleanExpression): AndExpression =
        AndExpression(listOf(first, second) + rest.toList())

    /**
     * 创建逻辑或表达式
     * Create logical OR expression
     */
    fun or(operands: List<BooleanExpression>): OrExpression = OrExpression(operands)

    /**
     * 创建逻辑或表达式（可变参数）
     * Create logical OR expression (vararg)
     */
    fun or(first: BooleanExpression, second: BooleanExpression, vararg rest: BooleanExpression): OrExpression =
        OrExpression(listOf(first, second) + rest.toList())

    /**
     * 创建逻辑非表达式
     * Create logical NOT expression
     */
    fun not(operand: BooleanExpression): NotExpression = NotExpression(operand)
}
