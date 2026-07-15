/**
 * 布尔表达式顶层 DSL
 * Top-level Boolean Expression DSL
 *
 * 提供避免扩展函数命名冲突的仓储谓词构造入口。
 * Provides repository predicate builders that avoid extension-function name conflicts.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.reflect.KProperty1
import fuookami.ospf.kotlin.math.symbol.expression.*

/**
 * 构造等值比较
 * Build an equality comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 等值比较表达式 / Equality comparison expression
*/
fun <E, T> eq(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Eq, value)
}

/**
 * 构造不等值比较
 * Build a not-equal comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 不等值比较表达式 / Not-equal comparison expression
*/
fun <E, T> ne(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Ne, value)
}

/**
 * 构造大于比较
 * Build a greater-than comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 大于比较表达式 / Greater-than comparison expression
*/
fun <E, T : Comparable<T>> gt(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Gt, value)
}

/**
 * 构造大于等于比较
 * Build a greater-than-or-equal comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 大于等于比较表达式 / Greater-than-or-equal comparison expression
*/
fun <E, T : Comparable<T>> ge(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Ge, value)
}

/**
 * 构造小于比较
 * Build a less-than comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 小于比较表达式 / Less-than comparison expression
*/
fun <E, T : Comparable<T>> lt(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Lt, value)
}

/**
 * 构造小于等于比较
 * Build a less-than-or-equal comparison
 *
 * @param property 属性引用 / Property reference
 * @param value 比较值 / Comparison value
 * @return 小于等于比较表达式 / Less-than-or-equal comparison expression
*/
fun <E, T : Comparable<T>> le(property: KProperty1<E, T>, value: T): Comparison<T> {
    return compare(property, ComparisonOperator.Le, value)
}

/**
 * 构造集合成员判断
 * Build an in-list expression
 *
 * @param property 属性引用 / Property reference
 * @param values 候选值集合 / Candidate values
 * @return In 表达式 / In expression
*/
fun <E, T> inValues(property: KProperty1<E, T>, values: Collection<T>): InExpression<T> {
    return InExpression(reference(property), values.map { ScalarConstant(it) })
}

/**
 * 构造集合成员判断
 * Build an in-list expression
 *
 * @param property 属性引用 / Property reference
 * @param values 候选值 / Candidate values
 * @return In 表达式 / In expression
*/
fun <E, T> inValues(property: KProperty1<E, T>, vararg values: T): InExpression<T> {
    return inValues(property, values.asList())
}

/**
 * 构造非集合成员判断
 * Build a not-in-list expression
 *
 * @param property 属性引用 / Property reference
 * @param values 候选值集合 / Candidate values
 * @return Not In 表达式 / Not In expression
*/
fun <E, T> notInValues(property: KProperty1<E, T>, values: Collection<T>): InExpression<T> {
    return InExpression(reference(property), values.map { ScalarConstant(it) }, negated = true)
}

/**
 * 构造非集合成员判断
 * Build a not-in-list expression
 *
 * @param property 属性引用 / Property reference
 * @param values 候选值 / Candidate values
 * @return Not In 表达式 / Not In expression
*/
fun <E, T> notInValues(property: KProperty1<E, T>, vararg values: T): InExpression<T> {
    return notInValues(property, values.asList())
}

/**
 * 构造字段为空判断
 * Build an is-null check
 *
 * @param property 属性引用 / Property reference
 * @return 空值检查表达式 / Null-check expression
*/
fun <E, T> isNull(property: KProperty1<E, T>): NullCheck {
    return NullCheck(path(property), NullCheckType.IsNull)
}

/**
 * 构造字段非空判断
 * Build an is-not-null check
 *
 * @param property 属性引用 / Property reference
 * @return 非空检查表达式 / Not-null-check expression
*/
fun <E, T> isNotNull(property: KProperty1<E, T>): NullCheck {
    return NullCheck(path(property), NullCheckType.IsNotNull)
}

/**
 * 构造 AND 组合表达式
 * Build an AND expression
 *
 * @param expressions 子表达式 / Child expressions
 * @return AND 表达式；空集合返回 true，单元素返回原表达式 / AND expression; empty returns true, single returns itself
*/
fun and(vararg expressions: BooleanExpression): BooleanExpression {
    return combineAnd(expressions.toList())
}

/**
 * 构造 lambda 风格的 AND 组合表达式
 * Build an AND expression with a lambda scope
 *
 * @param init 子表达式收集逻辑 / Child expression collection logic
 * @return AND 表达式 / AND expression
*/
fun and(init: BooleanExpressionScope.() -> Unit): BooleanExpression {
    val scope = BooleanExpressionScope()
    scope.init()
    return scope.buildAnd()
}

/**
 * 构造 OR 组合表达式
 * Build an OR expression
 *
 * @param expressions 子表达式 / Child expressions
 * @return OR 表达式；空集合返回 false，单元素返回原表达式 / OR expression; empty returns false, single returns itself
*/
fun or(vararg expressions: BooleanExpression): BooleanExpression {
    return combineOr(expressions.toList())
}

/**
 * 构造 lambda 风格的 OR 组合表达式
 * Build an OR expression with a lambda scope
 *
 * @param init 子表达式收集逻辑 / Child expression collection logic
 * @return OR 表达式 / OR expression
*/
fun or(init: BooleanExpressionScope.() -> Unit): BooleanExpression {
    val scope = BooleanExpressionScope()
    scope.init()
    return scope.buildOr()
}

/**
 * 构造字段等值范围组合
 * Build a field equality scope combined with extra predicates
 *
 * @param field 范围字段 / Scope field
 * @param value 范围值 / Scope value
 * @param additional 额外条件 / Additional predicates
 * @return 组合后的表达式 / Combined expression
*/
fun <E, T> scopedAnd(
    field: KProperty1<E, T>,
    value: T,
    vararg additional: BooleanExpression
): BooleanExpression {
    return and(eq(field, value), *additional)
}

/**
 * 布尔表达式收集作用域
 * Boolean expression collection scope
*/
class BooleanExpressionScope internal constructor() {

    /** 已收集的子表达式 / Collected child expressions */
    private val expressions = ArrayList<BooleanExpression>()

    /**
     * 构造并收集等值比较
     * Build and collect an equality comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 等值比较表达式 / Equality comparison expression
    */
    fun <E, T> eq(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Eq, value))
    }

    /**
     * 构造并收集不等值比较
     * Build and collect a not-equal comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 不等值比较表达式 / Not-equal comparison expression
    */
    fun <E, T> ne(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Ne, value))
    }

    /**
     * 构造并收集大于比较
     * Build and collect a greater-than comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 大于比较表达式 / Greater-than comparison expression
    */
    fun <E, T : Comparable<T>> gt(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Gt, value))
    }

    /**
     * 构造并收集大于等于比较
     * Build and collect a greater-than-or-equal comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 大于等于比较表达式 / Greater-than-or-equal comparison expression
    */
    fun <E, T : Comparable<T>> ge(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Ge, value))
    }

    /**
     * 构造并收集小于比较
     * Build and collect a less-than comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 小于比较表达式 / Less-than comparison expression
    */
    fun <E, T : Comparable<T>> lt(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Lt, value))
    }

    /**
     * 构造并收集小于等于比较
     * Build and collect a less-than-or-equal comparison
     *
     * @param property 属性引用 / Property reference
     * @param value 比较值 / Comparison value
     * @return 小于等于比较表达式 / Less-than-or-equal comparison expression
    */
    fun <E, T : Comparable<T>> le(property: KProperty1<E, T>, value: T): Comparison<T> {
        return add(compare(property, ComparisonOperator.Le, value))
    }

    /**
     * 构造并收集集合成员判断
     * Build and collect an in-list expression
     *
     * @param property 属性引用 / Property reference
     * @param values 候选值集合 / Candidate values
     * @return In 表达式 / In expression
    */
    fun <E, T> inValues(property: KProperty1<E, T>, values: Collection<T>): InExpression<T> {
        return add(InExpression(reference(property), values.map { ScalarConstant(it) }))
    }

    /**
     * 构造并收集集合成员判断
     * Build and collect an in-list expression
     *
     * @param property 属性引用 / Property reference
     * @param values 候选值 / Candidate values
     * @return In 表达式 / In expression
    */
    fun <E, T> inValues(property: KProperty1<E, T>, vararg values: T): InExpression<T> {
        return inValues(property, values.asList())
    }

    /**
     * 构造并收集非集合成员判断
     * Build and collect a not-in-list expression
     *
     * @param property 属性引用 / Property reference
     * @param values 候选值集合 / Candidate values
     * @return Not In 表达式 / Not In expression
    */
    fun <E, T> notInValues(property: KProperty1<E, T>, values: Collection<T>): InExpression<T> {
        return add(InExpression(reference(property), values.map { ScalarConstant(it) }, negated = true))
    }

    /**
     * 构造并收集非集合成员判断
     * Build and collect a not-in-list expression
     *
     * @param property 属性引用 / Property reference
     * @param values 候选值 / Candidate values
     * @return Not In 表达式 / Not In expression
    */
    fun <E, T> notInValues(property: KProperty1<E, T>, vararg values: T): InExpression<T> {
        return notInValues(property, values.asList())
    }

    /**
     * 构造并收集字段为空判断
     * Build and collect an is-null check
     *
     * @param property 属性引用 / Property reference
     * @return 空值检查表达式 / Null-check expression
    */
    fun <E, T> isNull(property: KProperty1<E, T>): NullCheck {
        return add(NullCheck(path(property), NullCheckType.IsNull))
    }

    /**
     * 构造并收集字段非空判断
     * Build and collect an is-not-null check
     *
     * @param property 属性引用 / Property reference
     * @return 非空检查表达式 / Not-null-check expression
    */
    fun <E, T> isNotNull(property: KProperty1<E, T>): NullCheck {
        return add(NullCheck(path(property), NullCheckType.IsNotNull))
    }

    /**
     * 构造并收集 AND 组合表达式
     * Build and collect an AND expression
     *
     * @param expressions 子表达式 / Child expressions
     * @return AND 表达式 / AND expression
    */
    fun and(vararg expressions: BooleanExpression): BooleanExpression {
        removeCollectedSuffix(expressions.toList())
        return add(combineAnd(expressions.toList()))
    }

    /**
     * 构造并收集 lambda 风格的 AND 组合表达式
     * Build and collect an AND expression with a lambda scope
     *
     * @param init 子表达式收集逻辑 / Child expression collection logic
     * @return AND 表达式 / AND expression
    */
    fun and(init: BooleanExpressionScope.() -> Unit): BooleanExpression {
        val scope = BooleanExpressionScope()
        scope.init()
        return add(scope.buildAnd())
    }

    /**
     * 构造并收集 OR 组合表达式
     * Build and collect an OR expression
     *
     * @param expressions 子表达式 / Child expressions
     * @return OR 表达式 / OR expression
    */
    fun or(vararg expressions: BooleanExpression): BooleanExpression {
        removeCollectedSuffix(expressions.toList())
        return add(combineOr(expressions.toList()))
    }

    /**
     * 构造并收集 lambda 风格的 OR 组合表达式
     * Build and collect an OR expression with a lambda scope
     *
     * @param init 子表达式收集逻辑 / Child expression collection logic
     * @return OR 表达式 / OR expression
    */
    fun or(init: BooleanExpressionScope.() -> Unit): BooleanExpression {
        val scope = BooleanExpressionScope()
        scope.init()
        return add(scope.buildOr())
    }

    /**
     * 以 AND 语义构造当前作用域表达式
     * Build the current scope expression with AND semantics
     *
     * @return 当前作用域表达式 / Current scope expression
    */
    internal fun buildAnd(): BooleanExpression {
        return combineAnd(expressions)
    }

    /**
     * 以 OR 语义构造当前作用域表达式
     * Build the current scope expression with OR semantics
     *
     * @return 当前作用域表达式 / Current scope expression
    */
    internal fun buildOr(): BooleanExpression {
        return combineOr(expressions)
    }

    /**
     * 记录表达式并返回原表达式
     * Record an expression and return it unchanged
     *
     * @param expression 待记录表达式 / Expression to record
     * @return 原表达式 / Original expression
    */
    private fun <T : BooleanExpression> add(expression: T): T {
        expressions.add(expression)
        return expression
    }

    /**
     * 移除已经由 vararg 参数求值过程收集的尾部表达式
     * Remove tail expressions already collected while evaluating vararg arguments
     *
     * @param suffix 待移除的尾部表达式 / Tail expressions to remove
    */
    private fun removeCollectedSuffix(suffix: List<BooleanExpression>) {
        if (suffix.isEmpty() || suffix.size > expressions.size) {
            return
        }

        val offset = expressions.size - suffix.size
        if (suffix.indices.all { index -> expressions[offset + index] == suffix[index] }) {
            repeat(suffix.size) {
                expressions.removeAt(expressions.lastIndex)
            }
        }
    }
}

/**
 * 从属性引用解析属性路径
 * Resolve a property path from a property reference
 *
 * @param property 属性引用 / Property reference
 * @return 属性路径 / Property path
*/
private fun <E, T> path(property: KProperty1<E, T>): PropertyPath {
    return PropertyPath.parse(property.name)
}

/**
 * 从属性引用构造标量引用
 * Build a scalar reference from a property reference
 *
 * @param property 属性引用 / Property reference
 * @return 标量引用 / Scalar reference
*/
private fun <E, T> reference(property: KProperty1<E, T>): ScalarReference<T> {
    return ScalarReference(path(property))
}

/**
 * 构造属性与常量之间的比较表达式
 * Build a comparison between a property and a constant
 *
 * @param property 属性引用 / Property reference
 * @param operator 比较操作符 / Comparison operator
 * @param value 比较值 / Comparison value
 * @return 比较表达式 / Comparison expression
*/
private fun <E, T> compare(
    property: KProperty1<E, T>,
    operator: ComparisonOperator,
    value: T
): Comparison<T> {
    return Comparison(operator, reference(property), ScalarConstant(value))
}

/**
 * 按 AND 单位元规则组合表达式
 * Combine expressions with AND identity semantics
 *
 * @param expressions 子表达式 / Child expressions
 * @return 组合后的表达式 / Combined expression
*/
private fun combineAnd(expressions: List<BooleanExpression>): BooleanExpression {
    return when (expressions.size) {
        0 -> BooleanConstant.true_()
        1 -> expressions.first()
        else -> AndExpression(expressions)
    }
}

/**
 * 按 OR 单位元规则组合表达式
 * Combine expressions with OR identity semantics
 *
 * @param expressions 子表达式 / Child expressions
 * @return 组合后的表达式 / Combined expression
*/
private fun combineOr(expressions: List<BooleanExpression>): BooleanExpression {
    return when (expressions.size) {
        0 -> BooleanConstant.false_()
        1 -> expressions.first()
        else -> OrExpression(expressions)
    }
}
