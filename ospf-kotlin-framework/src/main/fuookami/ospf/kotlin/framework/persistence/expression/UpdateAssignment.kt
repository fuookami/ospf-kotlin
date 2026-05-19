/**
 * 更新赋值模�?
 * Update Assignment Model
 *
 * 定义 UPDATE SET 语句的赋值规则�?
 * Defines assignment rules for UPDATE SET statements.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpression

/**
 * 更新赋值集�?
 * Update Assignments
 *
 * 表示一个或多个更新赋值项的集合�?
 * Represents a collection of one or more update assignment items.
 *
 * 示例 / Example:
 * ```kotlin
 * val assignments = UpdateAssignments.set("status", "inactive") +
 *                   UpdateAssignments.setNull("deletedAt")
 * ```
 */
data class UpdateAssignments(
    val items: List<UpdateAssignment>
) {
    companion object {
        /**
         * 空赋�?
         * Empty assignments
         */
        val empty = UpdateAssignments(emptyList())

        /**
         * 设置�?
         * Set value
         */
        fun set(path: String, value: Any?): UpdateAssignments =
            UpdateAssignments(listOf(SetValue(path, value)))

        /**
         * 设置 NULL
         * Set NULL
         */
        fun setNull(path: String): UpdateAssignments =
            UpdateAssignments(listOf(SetNull(path)))

        /**
         * 从表达式设置
         * Set from expression
         */
        fun setExpr(path: String, expr: ScalarExpression<*>): UpdateAssignments =
            UpdateAssignments(listOf(SetFromExpression(path, expr)))
    }

    /**
     * 组合多个赋�?
     * Combine multiple assignments
     */
    operator fun plus(other: UpdateAssignments): UpdateAssignments =
        UpdateAssignments(items + other.items)

    /**
     * 添加设置值项
     * Add set value item
     */
    fun thenSet(path: String, value: Any?): UpdateAssignments =
        UpdateAssignments(items + SetValue(path, value))

    /**
     * 添加设置 NULL �?
     * Add set NULL item
     */
    fun thenSetNull(path: String): UpdateAssignments =
        UpdateAssignments(items + SetNull(path))

    /**
     * 添加表达式设置项
     * Add expression set item
     */
    fun thenSetExpr(path: String, expr: ScalarExpression<*>): UpdateAssignments =
        UpdateAssignments(items + SetFromExpression(path, expr))

    /**
     * 是否为空
     * Check if empty
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * 是否非空
     * Check if not empty
     */
    fun isNotEmpty(): Boolean = items.isNotEmpty()
}

/**
 * 更新赋值项
 * Update Assignment Item
 *
 * 表示单个字段的更新赋值规则�?
 * Represents update assignment rule for a single field.
 */
sealed interface UpdateAssignment {
    /**
     * 字段路径
     * Field path
     */
    val path: String
}

/**
 * 设置�?
 * Set Value
 *
 * 将字段设置为指定值�?
 * Set field to specified value.
 *
 * @property path 字段路径 / Field path
 * @property value 要设置的�?/ Value to set
 */
data class SetValue(
    override val path: String,
    val value: Any?
) : UpdateAssignment

/**
 * 设置 NULL
 * Set NULL
 *
 * 将字段设置为 NULL�?
 * Set field to NULL.
 *
 * @property path 字段路径 / Field path
 */
data class SetNull(
    override val path: String
) : UpdateAssignment

/**
 * 从表达式设置
 * Set From Expression
 *
 * 将字段设置为表达式的结果�?
 * Set field to result of expression.
 *
 * @property path 字段路径 / Field path
 * @property expression 标量表达�?/ Scalar expression
 */
data class SetFromExpression(
    override val path: String,
    val expression: ScalarExpression<*>
) : UpdateAssignment