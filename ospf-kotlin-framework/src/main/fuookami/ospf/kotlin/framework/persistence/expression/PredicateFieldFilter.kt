/**
 * 谓词字段过滤提取
 * Predicate field filter extraction
 *
 * 将简单仓储谓词还原为字段级过滤条件，便于轻量仓储实现复用。
 * Converts simple repository predicates into field-level filters for lightweight repositories.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.*

/**
 * 字段级过滤条件
 * Field-level filter
 *
 * @property eq 等值条件 / Equality condition
 * @property inValues IN 集合条件 / IN-list condition
 * @property gt 大于条件 / Greater-than condition
 * @property ge 大于等于条件 / Greater-than-or-equal condition
 * @property lt 小于条件 / Less-than condition
 * @property le 小于等于条件 / Less-than-or-equal condition
 * @property isNull 空值检查；true 表示 IS NULL，false 表示 IS NOT NULL / Null check; true means IS NULL, false means IS NOT NULL
*/
data class FieldFilter(
    val eq: Any? = null,
    val inValues: List<Any?>? = null,
    val gt: Any? = null,
    val ge: Any? = null,
    val lt: Any? = null,
    val le: Any? = null,
    val isNull: Boolean? = null
)

/**
 * 尝试将谓词解析为字段名到等值条件的映射
 * Try to parse a predicate into field-to-equality filters
 *
 * @return 解析成功时返回字段过滤映射，不支持时返回 null / Field filter map on success, or null when unsupported
*/
fun BooleanExpression?.eqFilters(): Map<String, Any?>? {
    if (this == null) {
        return emptyMap()
    }

    val filters = linkedMapOf<String, Any?>()

    /**
     * Collects equality filters from the expression tree.
     * 从表达式树中收集等值过滤条件。
     *
     * @param expression the expression to collect from / 要收集的表达式
     * @return true if all sub-expressions were successfully collected / 是否所有子表达式都成功收集
    */
    fun collect(expression: BooleanExpression): Boolean {
        return when (expression) {
            is AndExpression -> expression.operands.all { collect(it) }
            is Comparison<*> -> {
                val field = expression.fieldComparison() ?: return false
                if (field.operator != ComparisonOperator.Eq) {
                    return false
                }
                filters[field.path] = field.value
                true
            }
            else -> false
        }
    }

    return if (collect(this)) filters else null
}

/**
 * 尝试将谓词解析为字段名到等值或 IN 条件的映射
 * Try to parse a predicate into field-to-equality-or-IN filters
 *
 * @return 解析成功时返回字段过滤映射，不支持时返回 null / Field filter map on success, or null when unsupported
*/
fun BooleanExpression?.eqOrInFilters(): Map<String, Any?>? {
    if (this == null) {
        return emptyMap()
    }

    val filters = linkedMapOf<String, Any?>()

    /**
     * Collects equality or IN filters from the expression tree.
     * 从表达式树中收集等值或 IN 过滤条件。
     *
     * @param expression the expression to collect from / 要收集的表达式
     * @return true if all sub-expressions were successfully collected / 是否所有子表达式都成功收集
    */
    fun collect(expression: BooleanExpression): Boolean {
        return when (expression) {
            is AndExpression -> expression.operands.all { collect(it) }
            is Comparison<*> -> {
                val field = expression.fieldComparison() ?: return false
                if (field.operator != ComparisonOperator.Eq) {
                    return false
                }
                filters[field.path] = field.value
                true
            }
            is InExpression<*> -> {
                val field = expression.fieldInValues() ?: return false
                filters[field.path] = field.values
                true
            }
            else -> false
        }
    }

    return if (collect(this)) filters else null
}

/**
 * 尝试将谓词解析为字段级条件
 * Try to parse a predicate into field-level filters
 *
 * @return 解析成功时返回字段过滤映射，不支持时返回 null / Field filter map on success, or null when unsupported
*/
fun BooleanExpression?.fieldFilters(): Map<String, FieldFilter>? {
    if (this == null) {
        return emptyMap()
    }

    /**
     * Mutable field filter for building field-level filters.
     * 用于构建字段级过滤条件的可变过滤器。
    */
    data class MutableFieldFilter(
        var eq: Any? = null,
        var inValues: List<Any?>? = null,
        var gt: Any? = null,
        var ge: Any? = null,
        var lt: Any? = null,
        var le: Any? = null,
        var isNull: Boolean? = null
    )

    val filters = linkedMapOf<String, MutableFieldFilter>()

    /**
     * Gets or creates a mutable filter for the given path.
     * 获取或创建指定路径的可变过滤器。
     *
     * @param path the field path / 字段路径
     * @return the mutable filter for the path / 指定路径的可变过滤器
    */
    fun mutableFilter(path: String): MutableFieldFilter {
        return filters.getOrPut(path) { MutableFieldFilter() }
    }

    /**
     * Collects a comparison expression into the filter map.
     * 将比较表达式收集到过滤器映射中。
     *
     * @param comparison the comparison expression / 比较表达式
     * @return true if the comparison was successfully collected / 是否成功收集比较表达式
    */
    fun collectComparison(comparison: Comparison<*>): Boolean {
        val field = comparison.fieldComparison() ?: return false
        val filter = mutableFilter(field.path)
        return when (field.operator) {
            ComparisonOperator.Eq -> {
                if (field.value == null) {
                    filter.isNull = true
                } else {
                    filter.eq = field.value
                }
                true
            }
            ComparisonOperator.Gt -> {
                filter.gt = field.value
                true
            }
            ComparisonOperator.Ge -> {
                filter.ge = field.value
                true
            }
            ComparisonOperator.Lt -> {
                filter.lt = field.value
                true
            }
            ComparisonOperator.Le -> {
                filter.le = field.value
                true
            }
            else -> false
        }
    }

    /**
     * Collects filters from the expression tree.
     * 从表达式树中收集过滤条件。
     *
     * @param expression the expression to collect from / 要收集的表达式
     * @return true if all sub-expressions were successfully collected / 是否所有子表达式都成功收集
    */
    fun collect(expression: BooleanExpression): Boolean {
        return when (expression) {
            is AndExpression -> expression.operands.all { collect(it) }
            is Comparison<*> -> collectComparison(expression)
            is InExpression<*> -> {
                val field = expression.fieldInValues() ?: return false
                mutableFilter(field.path).inValues = field.values
                true
            }
            is NullCheck -> {
                mutableFilter(expression.path.value).isNull = expression.type == NullCheckType.IsNull
                true
            }
            else -> false
        }
    }

    return if (collect(this)) {
        filters.mapValues { (_, filter) ->
            FieldFilter(
                eq = filter.eq,
                inValues = filter.inValues,
                gt = filter.gt,
                ge = filter.ge,
                lt = filter.lt,
                le = filter.le,
                isNull = filter.isNull
            )
        }
    } else {
        null
    }
}

/**
 * 字段与常量比较的解析结果
 * Parsed field-to-constant comparison
 *
 * @property path 字段路径 / Field path
 * @property operator 比较操作符 / Comparison operator
 * @property value 常量值 / Constant value
*/
private data class FieldComparison(
    val path: String,
    val operator: ComparisonOperator,
    val value: Any?
)

/**
 * 字段 IN 条件的解析结果
 * Parsed field IN condition
 *
 * @property path 字段路径 / Field path
 * @property values 候选常量值 / Candidate constant values
*/
private data class FieldInValues(
    val path: String,
    val values: List<Any?>
)

/**
 * 尝试解析字段与常量之间的比较
 * Try to parse a field-to-constant comparison
 *
 * @return 解析结果；不支持列列比较或函数比较时返回 null / Parsed result, or null for column-column or function comparisons
*/
private fun Comparison<*>.fieldComparison(): FieldComparison? {
    val leftRef = left as? ScalarReference<*>
    val leftConst = left as? ScalarConstant<*>
    val rightRef = right as? ScalarReference<*>
    val rightConst = right as? ScalarConstant<*>

    return when {
        leftRef != null && rightConst != null -> {
            FieldComparison(leftRef.path.value, operator, rightConst.value)
        }
        rightRef != null && leftConst != null -> {
            FieldComparison(rightRef.path.value, operator.flipComparisonSide(), leftConst.value)
        }
        else -> null
    }
}

/**
 * 尝试解析字段 IN 常量集合
 * Try to parse a field IN constant list
 *
 * @return 解析结果；not-in、空集合或非常量候选值返回 null / Parsed result, or null for not-in, empty list, or non-constant candidates
*/
private fun InExpression<*>.fieldInValues(): FieldInValues? {
    if (negated) {
        return null
    }

    val ref = value as? ScalarReference<*> ?: return null
    val values = candidates.map { candidate ->
        (candidate as? ScalarConstant<*>)?.value ?: return null
    }
    if (values.isEmpty()) {
        return null
    }
    return FieldInValues(ref.path.value, values)
}

/**
 * 反转比较表达式左右两侧时修正操作符方向
 * Adjust operator direction when flipping comparison sides
 *
 * @return 反转后的比较操作符 / Flipped comparison operator
*/
private fun ComparisonOperator.flipComparisonSide(): ComparisonOperator {
    return when (this) {
        ComparisonOperator.Eq -> ComparisonOperator.Eq
        ComparisonOperator.Ne -> ComparisonOperator.Ne
        ComparisonOperator.Lt -> ComparisonOperator.Gt
        ComparisonOperator.Le -> ComparisonOperator.Ge
        ComparisonOperator.Gt -> ComparisonOperator.Lt
        ComparisonOperator.Ge -> ComparisonOperator.Le
    }
}
