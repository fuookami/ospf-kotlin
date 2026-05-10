/**
 * 表达�?DSL
 * Expression DSL
 *
 * 提供便捷的布尔表达式构�?DSL�?
 * Provides convenient DSL for constructing boolean expressions.
 *
 * 使用示例 / Usage Examples:
 * ```kotlin
 * // 使用 DSL 构造表达式
 * // Build expression using DSL
 * val expr = booleanExpression {
 *     path("age") gt 18 and (path("status") eq "active")
 * }
 *
 * // 等价于解析字符串
 * // Equivalent to parsing string
 * val expr2 = parseBooleanExpression("age > 18 and status = 'active'")
 * ```
 */
package fuookami.ospf.kotlin.math.symbol.expression.dsl

import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*

/**
 * 布尔表达式构建器
 * Boolean Expression Builder
 */
class BooleanExpressionBuilder {
    internal var expression: BooleanExpression? = null

    /**
     * 构建最终的布尔表达�?
     * Build the final boolean expression
     */
    fun build(): BooleanExpression {
        return expression ?: throw IllegalStateException("No expression built")
    }

    /**
     * 添加表达式（用于内部组合�?
     * Add expression (for internal composition)
     */
    internal fun add(expr: BooleanExpression) {
        expression = expr
    }
}

/**
 * 标量表达式构建器
 * Scalar Expression Builder
 */
class ScalarExpressionBuilder<T> {
    internal var expression: ScalarExpression<T>? = null

    fun build(): ScalarExpression<T> {
        return expression ?: throw IllegalStateException("No expression built")
    }
}

/**
 * 路径引用构建�?
 * Path Reference Builder
 */
class PathBuilder(private val path: PropertyPath) {
    /**
     * 等于比较
     * Equal comparison
     */
    infix fun eq(value: String): Comparison<String> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    infix fun eq(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    infix fun eq(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    infix fun eq(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    infix fun eq(value: Boolean): Comparison<Boolean> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 不等于比�?
     * Not equal comparison
     */
    infix fun ne(value: String): Comparison<String> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    infix fun ne(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    infix fun ne(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    infix fun ne(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于比较
     * Less than comparison
     */
    infix fun lt(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    infix fun lt(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    infix fun lt(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于等于比较
     * Less than or equal comparison
     */
    infix fun le(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    infix fun le(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    infix fun le(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于比较
     * Greater than comparison
     */
    infix fun gt(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    infix fun gt(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    infix fun gt(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于等于比较
     * Greater than or equal comparison
     */
    infix fun ge(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    infix fun ge(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    infix fun ge(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    /**
     * 集合成员判断（in�?
     * Set membership (in)
     */
    fun inValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    fun inValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    fun inValues(vararg values: Long): InExpression<Long> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    /**
     * 非集合成员判断（not in�?
     * Negated set membership (not in)
     */
    fun notInValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    fun notInValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    /**
     * 空值检查（is null�?
     * Null check (is null)
     */
    fun isNull(): NullCheck = NullCheck(path, NullCheckType.IsNull)

    /**
     * 非空检查（is not null�?
     * Not null check (is not null)
     */
    fun isNotNull(): NullCheck = NullCheck(path, NullCheckType.IsNotNull)

    /**
     * 模式匹配
     * Pattern match
     */
    infix fun like(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Like)

    infix fun likeExact(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Exact)

    infix fun likePrefix(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Prefix)

    infix fun likeSuffix(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Suffix)

    infix fun likeContains(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Contains)

    /**
     * 非模式匹�?
     * Negated pattern match
     */
    fun notLike(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Like, negated = true)
}

// ========== DSL 入口函数 / DSL Entry Functions ==========

/**
 * 创建路径引用
 * Create path reference
 */
fun path(name: String): PathBuilder = PathBuilder(PropertyPath.parse(name))

/**
 * 创建布尔常量
 * Create boolean constant
 */
fun bool(value: Boolean): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 创建布尔常量（三值逻辑�?
 * Create boolean constant (three-valued logic)
 */
fun trivalent(value: Boolean?): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 逻辑与操�?
 * Logical AND operation
 */
infix fun BooleanExpression.and(other: BooleanExpression): AndExpression {
    val operands = mutableListOf<BooleanExpression>()
    if (this is AndExpression) operands.addAll(this.operands) else operands.add(this)
    if (other is AndExpression) operands.addAll(other.operands) else operands.add(other)
    return AndExpression(operands)
}

/**
 * 逻辑或操�?
 * Logical OR operation
 */
infix fun BooleanExpression.or(other: BooleanExpression): OrExpression {
    val operands = mutableListOf<BooleanExpression>()
    if (this is OrExpression) operands.addAll(this.operands) else operands.add(this)
    if (other is OrExpression) operands.addAll(other.operands) else operands.add(other)
    return OrExpression(operands)
}

/**
 * 逻辑非操�?
 * Logical NOT operation
 */
operator fun BooleanExpression.not(): NotExpression = NotExpression(this)

/**
 * 布尔表达�?DSL 入口
 * Boolean expression DSL entry
 */
fun booleanExpression(block: BooleanExpressionBuilder.() -> BooleanExpression): BooleanExpression {
    val builder = BooleanExpressionBuilder()
    return builder.apply { add(block()) }.build()
}

// ========== 快捷构造函�?/ Convenience Constructors ==========

/**
 * 快速创建比较表达式
 * Quick create comparison expression
 */
fun <T> compare(path: String, op: ComparisonOperator, value: T): Comparison<T> {
    val ref: ScalarExpression<T> = ScalarReference(PropertyPath.parse(path))
    val const: ScalarExpression<T> = ScalarConstant(value)
    return Comparison(op, ref, const)
}

/**
 * 快速创建等于表达式
 * Quick create equals expression
 */
fun <T> eq(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Eq, value)

/**
 * 快速创建不等于表达�?
 * Quick create not equals expression
 */
fun <T> ne(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Ne, value)

/**
 * 快速创建小于表达式
 * Quick create less than expression
 */
fun <T> lt(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Lt, value)

/**
 * 快速创建小于等于表达式
 * Quick create less than or equal expression
 */
fun <T> le(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Le, value)

/**
 * 快速创建大于表达式
 * Quick create greater than expression
 */
fun <T> gt(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Gt, value)

/**
 * 快速创建大于等于表达式
 * Quick create greater than or equal expression
 */
fun <T> ge(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Ge, value)

/**
 * 快速创�?in 表达�?
 * Quick create in expression
 */
fun <T> inExpr(path: String, values: List<T>): InExpression<T> {
    return InExpression(
        ScalarReference(PropertyPath.parse(path)),
        values.map { ScalarConstant(it) }
    )
}

/**
 * 快速创�?not in 表达�?
 * Quick create not in expression
 */
fun <T> notInExpr(path: String, values: List<T>): InExpression<T> {
    return InExpression(
        ScalarReference(PropertyPath.parse(path)),
        values.map { ScalarConstant(it) },
        negated = true
    )
}

/**
 * 快速创�?is null 表达�?
 * Quick create is null expression
 */
fun isNull(path: String): NullCheck = NullCheck(PropertyPath.parse(path), NullCheckType.IsNull)

/**
 * 快速创�?is not null 表达�?
 * Quick create is not null expression
 */
fun isNotNull(path: String): NullCheck = NullCheck(PropertyPath.parse(path), NullCheckType.IsNotNull)

/**
 * 快速创建逻辑与表达式
 * Quick create AND expression
 */
fun and(vararg expressions: BooleanExpression): AndExpression = AndExpression(expressions.toList())

/**
 * 快速创建逻辑或表达式
 * Quick create OR expression
 */
fun or(vararg expressions: BooleanExpression): OrExpression = OrExpression(expressions.toList())

/**
 * 快速创建逻辑非表达式
 * Quick create NOT expression
 */
fun notExpr(expression: BooleanExpression): NotExpression = NotExpression(expression)
