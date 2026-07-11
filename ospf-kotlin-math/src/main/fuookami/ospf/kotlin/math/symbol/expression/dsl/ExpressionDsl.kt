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

import kotlin.reflect.KProperty1
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 布尔表达式构建器
 * Boolean Expression Builder
*/
class BooleanExpressionBuilder {
    internal var expression: BooleanExpression? = null

    /**
     * 构建最终的布尔表达式，未设置时返回 null
     * Build the final boolean expression, or null when unset
     *
     * @return the built boolean expression, or null if no expression was set / 构建的布尔表达式，未设置时返回 null
    */
    fun buildOrNull(): BooleanExpression? {
        return expression
    }

    /**
     * 构建最终的布尔表达式
     * Build the final boolean expression
     *
     * @return 布尔表达式结果 / Boolean expression result
    */
    fun build(): Ret<BooleanExpression> {
        return expression?.let { Ok(it) }
            ?: Failed(ErrorCode.ApplicationError, "No expression built.")
    }

    /**
     * 添加表达式（用于内部组合，
     * Add expression (for internal composition)
     *
     * @param expr the boolean expression to add / 要添加的布尔表达式
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

    /**
     * 构建最终的标量表达式
     * Build the final scalar expression
     *
     * @return 标量表达式或 null / Scalar expression or null
    */
    fun buildOrNull(): ScalarExpression<T>? {
        return expression
    }

    /**
     * 构建最终的标量表达式
     * Build the final scalar expression
     *
     * @return 标量表达式结果 / Scalar expression result
    */
    fun build(): Ret<ScalarExpression<T>> {
        return expression?.let { Ok(it) }
            ?: Failed(ErrorCode.ApplicationError, "No expression built.")
    }
}

/**
 * 路径引用构建器
 * Path Reference Builder
 *
 * @property path 属性路径 / Property path
*/
class PathBuilder(private val path: PropertyPath) {

    /**
     * 转换为标量引用
     * Convert to scalar reference
     *
     * @return 标量引用 / Scalar reference
    */
    fun <T> asScalar(): ScalarReference<T> = ScalarReference(path)

    /**
     * 等于比较（String）
     * Equal comparison (String)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: String): Comparison<String> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 等于比较（Int）
     * Equal comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 等于比较（Long）
     * Equal comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 等于比较（Double）
     * Equal comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 等于比较（Boolean）
     * Equal comparison (Boolean)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: Boolean): Comparison<Boolean> =
        Comparison(ComparisonOperator.Eq, ScalarReference(path), ScalarConstant(value))

    /**
     * 不等于比较（String）
     * Not equal comparison (String)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(value: String): Comparison<String> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    /**
     * 不等于比较（Int）
     * Not equal comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    /**
     * 不等于比较（Long）
     * Not equal comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    /**
     * 不等于比较（Double）
     * Not equal comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Ne, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于比较（Int）
     * Less than comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun lt(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于比较（Long）
     * Less than comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun lt(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于比较（Double）
     * Less than comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun lt(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Lt, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于等于比较（Int）
     * Less than or equal comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun le(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于等于比较（Long）
     * Less than or equal comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun le(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    /**
     * 小于等于比较（Double）
     * Less than or equal comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun le(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Le, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于比较（Int）
     * Greater than comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun gt(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于比较（Long）
     * Greater than comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun gt(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于比较（Double）
     * Greater than comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun gt(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Gt, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于等于比较（Int）
     * Greater than or equal comparison (Int)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ge(value: Int): Comparison<Int> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于等于比较（Long）
     * Greater than or equal comparison (Long)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ge(value: Long): Comparison<Long> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    /**
     * 大于等于比较（Double）
     * Greater than or equal comparison (Double)
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ge(value: Double): Comparison<Double> =
        Comparison(ComparisonOperator.Ge, ScalarReference(path), ScalarConstant(value))

    /**
     * 集合成员判断（in，String）
     * Set membership (in, String)
     *
     * @param values 候选值 / Candidate values
     * @return In 表达式 / In expression
    */
    fun inValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    /**
     * 集合成员判断（in，Int）
     * Set membership (in, Int)
     *
     * @param values 候选值 / Candidate values
     * @return In 表达式 / In expression
    */
    fun inValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    /**
     * 集合成员判断（in，Long）
     * Set membership (in, Long)
     *
     * @param values 候选值 / Candidate values
     * @return In 表达式 / In expression
    */
    fun inValues(vararg values: Long): InExpression<Long> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) })

    /**
     * 非集合成员判断（not in，String）
     * Negated set membership (not in, String)
     *
     * @param values 候选值 / Candidate values
     * @return Not In 表达式 / Not In expression
    */
    fun notInValues(vararg values: String): InExpression<String> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    /**
     * 非集合成员判断（not in，Int）
     * Negated set membership (not in, Int)
     *
     * @param values 候选值 / Candidate values
     * @return Not In 表达式 / Not In expression
    */
    fun notInValues(vararg values: Int): InExpression<Int> =
        InExpression(ScalarReference(path), values.map { ScalarConstant(it) }, negated = true)

    /**
     * 空值检查（is null）
     * Null check (is null)
     *
     * @return 空值检查表达式 / Null check expression
    */
    fun isNull(): NullCheck = NullCheck(path, NullCheckType.IsNull)

    /**
     * 非空检查（is not null）
     * Not null check (is not null)
     *
     * @return 非空检查表达式 / Not null check expression
    */
    fun isNotNull(): NullCheck = NullCheck(path, NullCheckType.IsNotNull)

    /**
     * 模式匹配（LIKE）
     * Pattern match (LIKE)
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 模式匹配表达式 / Pattern match expression
    */
    infix fun like(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Like)

    /**
     * 精确匹配
     * Exact match
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 模式匹配表达式 / Pattern match expression
    */
    infix fun likeExact(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Exact)

    /**
     * 前缀匹配
     * Prefix match
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 模式匹配表达式 / Pattern match expression
    */
    infix fun likePrefix(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Prefix)

    /**
     * 后缀匹配
     * Suffix match
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 模式匹配表达式 / Pattern match expression
    */
    infix fun likeSuffix(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Suffix)

    /**
     * 包含匹配
     * Contains match
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 模式匹配表达式 / Pattern match expression
    */
    infix fun likeContains(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Contains)

    /**
     * 非模式匹配（LIKE）
     * Negated pattern match (LIKE)
     *
     * @param pattern 匹配模式 / Match pattern
     * @return 否定的模式匹配表达式 / Negated pattern match expression
    */
    fun notLike(pattern: String): PatternMatch<String> =
        PatternMatch(ScalarReference(path), ScalarConstant(pattern), PatternMatchMode.Like, negated = true)
}

/**
 * 类型化路径引用构建器
 * Typed path reference builder
 *
 * @property property Kotlin 属性引用 / Kotlin property reference
 * @property path 属性路径，默认从 property.name 解析 / Property path, defaults to parsed from property.name
*/
class TypedPathBuilder<E, T>(
    val property: KProperty1<E, T>,
    val path: PropertyPath = PropertyPath.parse(property.name)
) {

    /**
     * 转换为标量引用
     * Convert to scalar reference
     *
     * @return 标量引用 / Scalar reference
    */
    fun asScalar(): ScalarReference<T> = ScalarReference(path)

    /**
     * 等于比较
     * Equal comparison
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(value: T): Comparison<T> =
        Comparison(ComparisonOperator.Eq, asScalar(), ScalarConstant(value))

    /**
     * 不等于比较
     * Not equal comparison
     *
     * @param value 比较值 / Comparison value
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(value: T): Comparison<T> =
        Comparison(ComparisonOperator.Ne, asScalar(), ScalarConstant(value))

    /**
     * 列-列等于比较
     * Column-column equal comparison
     *
     * @param other 另一个类型化路径构建器 / Another typed path builder
     * @return 比较表达式 / Comparison expression
    */
    infix fun eq(other: TypedPathBuilder<E, T>): Comparison<T> =
        Comparison(ComparisonOperator.Eq, asScalar(), other.asScalar())

    /**
     * 列-列不等于比较
     * Column-column not equal comparison
     *
     * @param other 另一个类型化路径构建器 / Another typed path builder
     * @return 比较表达式 / Comparison expression
    */
    infix fun ne(other: TypedPathBuilder<E, T>): Comparison<T> =
        Comparison(ComparisonOperator.Ne, asScalar(), other.asScalar())

    /**
     * 集合成员判断
     * Set membership
     *
     * @param values 候选值 / Candidate values
     * @return In 表达式 / In expression
    */
    fun inValues(vararg values: T): InExpression<T> =
        InExpression(asScalar(), values.map { ScalarConstant(it) })

    /**
     * 非集合成员判断
     * Negated set membership
     *
     * @param values 候选值 / Candidate values
     * @return Not In 表达式 / Not In expression
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
     *
     * @param property Kotlin 属性引用 / Kotlin property reference
     * @return 类型化路径构建器 / Typed path builder
    */
    protected fun <T> field(property: KProperty1<E, T>): TypedPathBuilder<E, T> = prop(property)
}

/**
 * 使用具体 schema 类型构造谓词
 * Build predicate with concrete schema type
 *
 * @param block 构建器 lambda / Builder lambda
 * @return 布尔表达式 / Boolean expression
*/
fun <E, S : PredicateSchema<E>> S.predicate(block: S.() -> BooleanExpression): BooleanExpression = block()

// ========== DSL 入口函数 / DSL Entry Functions ==========

/**
 * 创建路径引用
 * Create path reference
 *
 * @param name 路径名称 / Path name
 * @return 路径构建器 / Path builder
*/
fun path(name: String): PathBuilder = PathBuilder(PropertyPath.parse(name))

/**
 * 创建属性引用
 * Create property reference
 *
 * @param property Kotlin 属性引用 / Kotlin property reference
 * @return 类型化路径构建器 / Typed path builder
*/
fun <E, T> prop(property: KProperty1<E, T>): TypedPathBuilder<E, T> = TypedPathBuilder(property)

/**
 * 创建标量引用
 * Create scalar reference
 *
 * @param name 路径名称 / Path name
 * @return 标量引用 / Scalar reference
*/
fun <T> scalarPath(name: String): ScalarReference<T> = ScalarReference(PropertyPath.parse(name))

/**
 * 创建布尔常量
 * Create boolean constant
 *
 * @param value 布尔值 / Boolean value
 * @return 布尔常量 / Boolean constant
*/
fun bool(value: Boolean): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 创建布尔常量（三值逻辑）
 * Create boolean constant (three-valued logic)
 *
 * @param value 可空布尔值 / Nullable boolean value
 * @return 布尔常量 / Boolean constant
*/
fun trivalent(value: Boolean?): BooleanConstant = BooleanConstant(Trivalent(value))

/**
 * 逻辑与操作
 * Logical AND operation
 *
 * @param other 另一个布尔表达式 / Another boolean expression
 * @return 逻辑与表达式 / Logical AND expression
*/
infix fun BooleanExpression.and(other: BooleanExpression): AndExpression {
    val operands = mutableListOf<BooleanExpression>()
    if (this is AndExpression) operands.addAll(this.operands) else operands.add(this)
    if (other is AndExpression) operands.addAll(other.operands) else operands.add(other)
    return AndExpression(operands)
}

/**
 * 逻辑或操作
 * Logical OR operation
 *
 * @param other 另一个布尔表达式 / Another boolean expression
 * @return 逻辑或表达式 / Logical OR expression
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
 * 布尔表达式 DSL 入口
 * Boolean expression DSL entry
 *
 * @param block 构建器 lambda / Builder lambda
 * @return 布尔表达式 / Boolean expression
*/
fun booleanExpression(block: BooleanExpressionBuilder.() -> BooleanExpression): Ret<BooleanExpression> {
    val builder = BooleanExpressionBuilder()
    return builder.apply { add(block()) }.build()
}

// ========== 快捷构造函敌/ Convenience Constructors ==========

/**
 * 快速创建比较表达式
 * Quick create comparison expression
 *
 * @param path 路径字符串 / Path string
 * @param op 比较操作符 / Comparison operator
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
fun <T> compare(path: String, op: ComparisonOperator, value: T): Comparison<T> {
    val ref: ScalarExpression<T> = ScalarReference(PropertyPath.parse(path))
    val const: ScalarExpression<T> = ScalarConstant(value)
    return Comparison(op, ref, const)
}

/**
 * 快速创建等于表达式
 * Quick create equals expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 等于比较表达式 / Equal comparison expression
*/
fun <T> eq(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Eq, value)

/**
 * 快速创建不等于表达式
 * Quick create not equals expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 不等于比较表达式 / Not equal comparison expression
*/
fun <T> ne(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Ne, value)

/**
 * 快速创建小于表达式
 * Quick create less than expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 小于比较表达式 / Less than comparison expression
*/
fun <T> lt(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Lt, value)

/**
 * 快速创建小于等于表达式
 * Quick create less than or equal expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 小于等于比较表达式 / Less than or equal comparison expression
*/
fun <T> le(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Le, value)

/**
 * 快速创建大于表达式
 * Quick create greater than expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 大于比较表达式 / Greater than comparison expression
*/
fun <T> gt(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Gt, value)

/**
 * 快速创建大于等于表达式
 * Quick create greater than or equal expression
 *
 * @param path 路径字符串 / Path string
 * @param value 比较值 / Comparison value
 * @return 大于等于比较表达式 / Greater than or equal comparison expression
*/
fun <T> ge(path: String, value: T): Comparison<T> = compare(path, ComparisonOperator.Ge, value)

/**
 * 快速创建 in 表达式
 * Quick create in expression
 *
 * @param path 路径字符串 / Path string
 * @param values 候选值列表 / Candidate values list
 * @return In 表达式 / In expression
*/
fun <T> inExpr(path: String, values: List<T>): InExpression<T> {
    return InExpression(
        ScalarReference(PropertyPath.parse(path)),
        values.map { ScalarConstant(it) }
    )
}

/**
 * 快速创建 not in 表达式
 * Quick create not in expression
 *
 * @param path 路径字符串 / Path string
 * @param values 候选值列表 / Candidate values list
 * @return Not In 表达式 / Not In expression
*/
fun <T> notInExpr(path: String, values: List<T>): InExpression<T> {
    return InExpression(
        ScalarReference(PropertyPath.parse(path)),
        values.map { ScalarConstant(it) },
        negated = true
    )
}

/**
 * 快速创建 is null 表达式
 * Quick create is null expression
 *
 * @param path 路径字符串 / Path string
 * @return 空值检查表达式 / Null check expression
*/
fun isNull(path: String): NullCheck = NullCheck(PropertyPath.parse(path), NullCheckType.IsNull)

/**
 * 快速创建 is not null 表达式
 * Quick create is not null expression
 *
 * @param path 路径字符串 / Path string
 * @return 非空检查表达式 / Not null check expression
*/
fun isNotNull(path: String): NullCheck = NullCheck(PropertyPath.parse(path), NullCheckType.IsNotNull)

/**
 * 快速创建逻辑与表达式
 * Quick create AND expression
 *
 * @param expressions 布尔表达式 / Boolean expressions
 * @return 逻辑与表达式 / Logical AND expression
*/
fun and(vararg expressions: BooleanExpression): AndExpression = AndExpression(expressions.toList())

/**
 * 快速创建逻辑或表达式
 * Quick create OR expression
 *
 * @param expressions 布尔表达式 / Boolean expressions
 * @return 逻辑或表达式 / Logical OR expression
*/
fun or(vararg expressions: BooleanExpression): OrExpression = OrExpression(expressions.toList())

/**
 * 快速创建逻辑非表达式
 * Quick create NOT expression
 *
 * @param expression 布尔表达式 / Boolean expression
 * @return 逻辑非表达式 / Logical NOT expression
*/
fun notExpr(expression: BooleanExpression): NotExpression = NotExpression(expression)

// ========== 类型化路径操作 / Typed Path Operations ==========

/**
 * 小于比较（值）
 * Less than comparison (value)
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.lt(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Lt, asScalar(), ScalarConstant(value))

/**
 * 小于等于比较（值）
 * Less than or equal comparison (value)
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.le(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Le, asScalar(), ScalarConstant(value))

/**
 * 大于比较（值）
 * Greater than comparison (value)
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.gt(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Gt, asScalar(), ScalarConstant(value))

/**
 * 大于等于比较（值）
 * Greater than or equal comparison (value)
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.ge(value: T): Comparison<T> =
    Comparison(ComparisonOperator.Ge, asScalar(), ScalarConstant(value))

/**
 * 小于比较（列）
 * Less than comparison (column)
 *
 * @param other 另一个类型化路径构建器 / Another typed path builder
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.lt(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Lt, asScalar(), other.asScalar())

/**
 * 小于等于比较（列）
 * Less than or equal comparison (column)
 *
 * @param other 另一个类型化路径构建器 / Another typed path builder
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.le(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Le, asScalar(), other.asScalar())

/**
 * 大于比较（列）
 * Greater than comparison (column)
 *
 * @param other 另一个类型化路径构建器 / Another typed path builder
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.gt(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Gt, asScalar(), other.asScalar())

/**
 * 大于等于比较（列）
 * Greater than or equal comparison (column)
 *
 * @param other 另一个类型化路径构建器 / Another typed path builder
 * @return 比较表达式 / Comparison expression
*/
infix fun <E, T : Comparable<T>> TypedPathBuilder<E, T>.ge(other: TypedPathBuilder<E, T>): Comparison<T> =
    Comparison(ComparisonOperator.Ge, asScalar(), other.asScalar())

/**
 * 空值检查
 * Null check
 *
 * @return 空值检查表达式 / Null check expression
*/
fun <E, T> TypedPathBuilder<E, T?>.isNull(): NullCheck = NullCheck(path, NullCheckType.IsNull)

/**
 * 非空检查
 * Not null check
 *
 * @return 非空检查表达式 / Not null check expression
*/
fun <E, T> TypedPathBuilder<E, T?>.isNotNull(): NullCheck = NullCheck(path, NullCheckType.IsNotNull)

/**
 * LIKE 模式匹配
 * LIKE pattern match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 模式匹配表达式 / Pattern match expression
*/
infix fun <E> TypedPathBuilder<E, String>.like(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Like)

/**
 * 精确匹配
 * Exact match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 模式匹配表达式 / Pattern match expression
*/
infix fun <E> TypedPathBuilder<E, String>.likeExact(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Exact)

/**
 * 前缀匹配
 * Prefix match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 模式匹配表达式 / Pattern match expression
*/
infix fun <E> TypedPathBuilder<E, String>.likePrefix(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Prefix)

/**
 * 后缀匹配
 * Suffix match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 模式匹配表达式 / Pattern match expression
*/
infix fun <E> TypedPathBuilder<E, String>.likeSuffix(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Suffix)

/**
 * 包含匹配
 * Contains match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 模式匹配表达式 / Pattern match expression
*/
infix fun <E> TypedPathBuilder<E, String>.likeContains(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Contains)

/**
 * 非 LIKE 模式匹配
 * Negated LIKE pattern match
 *
 * @param pattern 匹配模式 / Match pattern
 * @return 否定的模式匹配表达式 / Negated pattern match expression
*/
fun <E> TypedPathBuilder<E, String>.notLike(pattern: String): PatternMatch<String> =
    PatternMatch(asScalar(), ScalarConstant(pattern), PatternMatchMode.Like, negated = true)

// ========== 标量函数 / Scalar Functions ==========

/**
 * 将标量表达式类型转换为 Any? 通配类型
 * Cast a scalar expression to the Any? wildcard type
 *
 * @param expr the scalar expression to cast / 要转换的标量表达式
 * @return the cast scalar expression / 转换后的标量表达式
*/
@Suppress("UNCHECKED_CAST")
private fun anyScalar(expr: ScalarExpression<*>): ScalarExpression<Any?> = expr as ScalarExpression<Any?>

/**
 * 创建指定名称和参数列表的标量函数表达式
 * Create a scalar function expression with the given name and argument list
 *
 * @param name the function name / 函数名称
 * @param arguments the list of scalar expression arguments / 标量表达式参数列表
 * @return the scalar function expression / 标量函数表达式
*/
private fun function(name: String, arguments: List<ScalarExpression<*>>): ScalarFunction<Any?> {
    return ScalarFunction(name, arguments.map { anyScalar(it) })
}

/**
 * 绝对值函数
 * Absolute value function
 *
 * @param expr 标量表达式 / Scalar expression
 * @return 函数调用表达式 / Function call expression
*/
fun abs(expr: ScalarExpression<*>): ScalarFunction<Any?> =
    function(ScalarFunctionNames.Abs, listOf(expr))

/**
 * 绝对值函数（路径形式）
 * Absolute value function (path form)
 *
 * @param path 路径构建器 / Path builder
 * @return 函数调用表达式 / Function call expression
*/
fun abs(path: PathBuilder): ScalarFunction<Any?> = abs(path.asScalar<Any?>())

/**
 * 等于比较
 * Equal comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.eq(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Eq, anyScalar(this), ScalarConstant(value))

/**
 * 不等于比较
 * Not equal comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.ne(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Ne, anyScalar(this), ScalarConstant(value))

/**
 * 小于比较
 * Less than comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.lt(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Lt, anyScalar(this), ScalarConstant(value))

/**
 * 小于等于比较
 * Less than or equal comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.le(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Le, anyScalar(this), ScalarConstant(value))

/**
 * 大于比较
 * Greater than comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.gt(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Gt, anyScalar(this), ScalarConstant(value))

/**
 * 大于等于比较
 * Greater than or equal comparison
 *
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
infix fun ScalarExpression<*>.ge(value: Any?): Comparison<Any?> =
    Comparison(ComparisonOperator.Ge, anyScalar(this), ScalarConstant(value))
