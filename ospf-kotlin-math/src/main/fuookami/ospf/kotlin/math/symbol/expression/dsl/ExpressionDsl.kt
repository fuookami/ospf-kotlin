/**
 * 表达弌DSL
 * Expression DSL
 *
 * 提供便捷的布尔表达式构逌DSL。
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
import kotlin.reflect.KProperty1

/**
 * 布尔表达式构建器
 * Boolean Expression Builder
 */
class BooleanExpressionBuilder {
    internal var expression: BooleanExpression? = null

    /**
     * 构建最终的布尔表达弌
     * Build the final boolean expression
     */
    fun build(): BooleanExpression {
        return expression ?: throw IllegalStateException("No expression built")
    }

    /**
     * 添加表达式（用于内部组合，
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
 * 路径引用构建噌
 * Path Reference Builder
 */
class PathBuilder(private val path: PropertyPath) {
    /**
     * 转换为标量引用
     * Convert to scalar reference
     */
    fun <T> asScalar(): ScalarReference<T> = ScalarReference(path)

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
     * 不等于比辌
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
     * 集合成员判断（in，
     * Set membership (in)
     */
    fun inValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    fun inValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    fun inValues(vararg values: Long): InExpression<Long> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    /**
     * 非集合成员判断（not in，
     * Negated set membership (not in)
     */
    fun notInValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    fun notInValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    /**
     * 空值检查（is null，
     * Null check (is null)
     */
    fun isNull(): NullCheck = NullCheck(path, NullCheckType.IsNull)

    /**
     * 非空检查（is not null，
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
     * 非模式匹酌
     * Negated pattern match
     */
    fun notLike(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Like, negated = true)
}

/**
 * 类型化路径引用构建器
 * Typed path reference builder
 */
class TypedPathBuilder<E, T>(
    val property: KProperty1<E, T>,
    val path: PropertyPath = PropertyPath.parse(property.name)
) {
    /**
     * 转换为标量引用
     * Convert to scalar reference
     */
    fun asScalar(): ScalarReference<T> = ScalarReference(path)

    /**
     * 等于比较
     * Equal comparison
     */
    infix fun eq(value: T): Comparison<T> =
        Comparison(ComparisonOperator.Eq, asScalar(), ScalarConstant(value))

    /**
     * 不等于比较
     * Not equal comparison
     */
    infix fun ne(value: T): Comparison<T> =
        Comparison(ComparisonOperator.Ne, asScalar(), ScalarConstant(value))

    /**
     * 列-列等于比较
     * Column-column equal comparison
     */
    infix fun eq(other: TypedPathBuilder<E, T>): Comparison<T> =
        Comparison(ComparisonOperator.Eq, asScalar(), other.asScalar())

    /**
     * 列-列不等于比较
     * Column-column not equal comparison
     */
    infix fun ne(other: TypedPathBuilder<E, T>): Comparison<T> =
        Comparison(ComparisonOperator.Ne, asScalar(), other.asScalar())

    /**
     * 集合成员判断
     * Set membership
     */
    fun inValues(vararg values: T): InExpression<T> =
        InExpression(asScalar(), values.map { ScalarConstant(it) })

    /**
     * 非集合成员判断
     * Negated set membership
     */
    fun notInValues(vararg values: T): InExpression<T> =
        InExpression(asScalar(), values.map { ScalarConstant(it) }, negated = true)
}

/**
 * 手写谓词 schema
 * Hand-written predicate schema
 */
abstract class PredicateSchema<E> {
    /**
     * 创建类型化字段
     * Create typed field
     */
    protected fun <T> field(property: KProperty1<E, T>): TypedPathBuilder<E, T> = prop(property)
}

/**
 * 使用具体 schema 类型构造谓词
 * Build predicate with concrete schema type
 */
fun <E, S : PredicateSchema<E>> S.predicate(block: S.() -> BooleanExpression): BooleanExpression = block()

// ========== DSL 入口函数 / DSL Entry Functions ==========

/**
 * 创建路径引用
 * Create path reference
 */
fun path(name: String): PathBuilder = PathBuilder(PropertyPath.parse(name))

/**
 * 创建属性引用
 * Create property reference
 */
fun <E, T> prop(property: KProperty1<E, T>): TypedPathBuilder<E, T> = TypedPathBuilder(property)

/**
 * 创建标量引用
 * Create scalar reference
 */
fun <T> scalarPath(name: String): ScalarReference<T> = ScalarReference(PropertyPath.parse(name))

/**
 * 创建布尔常量
 * Create boolean constant
 */
fun bool(value: Boolean): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 创建布尔常量（三值逻辑，
 * Create boolean constant (three-valued logic)
 */
fun trivalent(value: Boolean?): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 逻辑与操佌
 * Logical AND operation
 */
infix fun BooleanExpression.and(other: BooleanExpression): AndExpression {
    val operands = mutableListOf<BooleanExpression>()
    if (this is AndExpression) operands.addAll(this.operands) else operands.add(this)
    if (other is AndExpression) operands.addAll(other.operands) else operands.add(other)
    return AndExpression(operands)
}

/**
 * 逻辑或操佌
 * Logical OR operation
 */
infix fun BooleanExpression.or(other: BooleanExpression): OrExpression {
    val operands = mutableListOf<BooleanExpression>()
    if (this is OrExpression) operands.addAll(this.operands) else operands.add(this)
    if (other is OrExpression) operands.addAll(other.operands) else operands.add(other)
    return OrExpression(operands)
}

/**
 * 逻辑非操佌
 * Logical NOT operation
 */
operator fun BooleanExpression.not(): NotExpression = NotExpression(this)

/**
 * 布尔表达弌DSL 入口
 * Boolean expression DSL entry
 */
fun booleanExpression(block: BooleanExpressionBuilder.() -> BooleanExpression): BooleanExpression {
    val builder = BooleanExpressionBuilder()
    return builder.apply { add(block()) }.build()
}

// ========== 快捷构造函敌/ Convenience Constructors ==========

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
 * 快速创建不等于表达弌
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
 * 快速创廌in 表达弌
 * Quick create in expression
 */
fun <T> inExpr(path: String, values: List<T>): InExpression<T> {
    return InExpression(
        ScalarReference(PropertyPath.parse(path)),
        values.map { ScalarConstant(it) }
    )
}

/**
 * 快速创廌not in 表达弌
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
 * 快速创廌is null 表达弌
 * Quick create is null expression
 */
fun isNull(path: String): NullCheck = NullCheck(PropertyPath.parse(path), NullCheckType.IsNull)

/**
 * 快速创廌is not null 表达弌
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

// ========== 类型化路径操作 / Typed Path Operations ==========

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.lt(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Lt, asScalar(), ScalarConstant(value))

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.le(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Le, asScalar(), ScalarConstant(value))

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.gt(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Gt, asScalar(), ScalarConstant(value))

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.ge(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Ge, asScalar(), ScalarConstant(value))

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.lt(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Lt, asScalar(), other.asScalar())

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.le(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Le, asScalar(), other.asScalar())

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.gt(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Gt, asScalar(), other.asScalar())

infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.ge(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Ge, asScalar(), other.asScalar())

fun <E, T> TypedPathBuilder<E, T?>.isNull(): NullCheck = NullCheck(path, NullCheckType.IsNull)

fun <E, T> TypedPathBuilder<E, T?>.isNotNull(): NullCheck = NullCheck(path, NullCheckType.IsNotNull)

infix fun <E> TypedPathBuilder<E, String>.like(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Like)

infix fun <E> TypedPathBuilder<E, String>.likeExact(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Exact)

infix fun <E> TypedPathBuilder<E, String>.likePrefix(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Prefix)

infix fun <E> TypedPathBuilder<E, String>.likeSuffix(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Suffix)

infix fun <E> TypedPathBuilder<E, String>.likeContains(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Contains)

fun <E> TypedPathBuilder<E, String>.notLike(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Like, negated = true)

// ========== 标量函数 / Scalar Functions ==========

@Suppress("UNCHECKED_CAST")
private fun anyScalar(expr: ScalarExpression<*>): ScalarExpression<Any?> = expr as ScalarExpression<Any?>

private fun function(name: String, arguments: List<ScalarExpression<*>>): ScalarFunction<Any?> {
    return ScalarFunction(name, arguments.map { anyScalar(it) })
}

fun abs(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Abs, listOf(expr))

fun abs(path: PathBuilder): ScalarFunction<Any?> = abs(path.asScalar<Any?>())

infix fun ScalarExpression<*>.eq(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Eq, anyScalar(this), ScalarConstant(value))

infix fun ScalarExpression<*>.ne(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Ne, anyScalar(this), ScalarConstant(value))

infix fun ScalarExpression<*>.lt(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Lt, anyScalar(this), ScalarConstant(value))

infix fun ScalarExpression<*>.le(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Le, anyScalar(this), ScalarConstant(value))

infix fun ScalarExpression<*>.gt(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Gt, anyScalar(this), ScalarConstant(value))

infix fun ScalarExpression<*>.ge(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Ge, anyScalar(this), ScalarConstant(value))
