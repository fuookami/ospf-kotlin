/**
 * 更新赋值模�?
 * Update Assignment Model
 *
 * 定义 UPDATE SET 语句的赋值规则�?
 * Defines assignment rules for UPDATE SET statements.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.reflect.KProperty1
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
         * 设置值
         * Set value
         *
         * @param path 字段路径 / Field path
         * @param value 要设置的值 / Value to set
         * @return 更新赋值集合 / Update assignments
        */
        fun set(path: String, value: Any?): UpdateAssignments =
            UpdateAssignments(listOf(SetValue(path, value)))

        /**
         * 按属性设置值
         * Set value by property
         *
         * @param property 属性引用 / Property reference
         * @param value 要设置的值 / Value to set
         * @return 更新赋值集合 / Update assignments
        */
        fun <E, T> set(property: KProperty1<E, T>, value: T): UpdateAssignments =
            set(property.name, value)

        /**
         * 设置 NULL
         * Set NULL
         *
         * @param path 字段路径 / Field path
         * @return 更新赋值集合 / Update assignments
        */
        fun setNull(path: String): UpdateAssignments =
            UpdateAssignments(listOf(SetNull(path)))

        /**
         * 按属性设置 NULL
         * Set NULL by property
         *
         * @param property 属性引用 / Property reference
         * @return 更新赋值集合 / Update assignments
        */
        fun <E, T> setNull(property: KProperty1<E, T?>): UpdateAssignments =
            setNull(property.name)

        /**
         * 从表达式设置
         * Set from expression
         *
         * @param path 字段路径 / Field path
         * @param expr 标量表达式 / Scalar expression
         * @return 更新赋值集合 / Update assignments
        */
        fun setExpr(path: String, expr: ScalarExpression<*>): UpdateAssignments =
            UpdateAssignments(listOf(SetFromExpression(path, expr)))

        /**
         * 按属性从表达式设置
         * Set from expression by property
         *
         * @param property 属性引用 / Property reference
         * @param expr 标量表达式 / Scalar expression
         * @return 更新赋值集合 / Update assignments
        */
        fun <E, T> setExpr(property: KProperty1<E, T>, expr: ScalarExpression<*>): UpdateAssignments =
            setExpr(property.name, expr)
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
     *
     * @param path 字段路径 / Field path
     * @param value 要设置的值 / Value to set
     * @return 更新赋值集合 / Update assignments
    */
    fun thenSet(path: String, value: Any?): UpdateAssignments =
        UpdateAssignments(items + SetValue(path, value))

    /**
     * 添加属性设置值项
     * Add set value item by property
     *
     * @param property 属性引用 / Property reference
     * @param value 要设置的值 / Value to set
     * @return 更新赋值集合 / Update assignments
    */
    fun <E, T> thenSet(property: KProperty1<E, T>, value: T): UpdateAssignments =
        thenSet(property.name, value)

    /**
     * 添加设置 NULL 项
     * Add set NULL item
     *
     * @param path 字段路径 / Field path
     * @return 更新赋值集合 / Update assignments
    */
    fun thenSetNull(path: String): UpdateAssignments =
        UpdateAssignments(items + SetNull(path))

    /**
     * 添加属性设置 NULL 项
     * Add set NULL item by property
     *
     * @param property 属性引用 / Property reference
     * @return 更新赋值集合 / Update assignments
    */
    fun <E, T> thenSetNull(property: KProperty1<E, T?>): UpdateAssignments =
        thenSetNull(property.name)

    /**
     * 添加表达式设置项
     * Add expression set item
     *
     * @param path 字段路径 / Field path
     * @param expr 标量表达式 / Scalar expression
     * @return 更新赋值集合 / Update assignments
    */
    fun thenSetExpr(path: String, expr: ScalarExpression<*>): UpdateAssignments =
        UpdateAssignments(items + SetFromExpression(path, expr))

    /**
     * 添加属性表达式设置项
     * Add expression set item by property
     *
     * @param property 属性引用 / Property reference
     * @param expr 标量表达式 / Scalar expression
     * @return 更新赋值集合 / Update assignments
    */
    fun <E, T> thenSetExpr(property: KProperty1<E, T>, expr: ScalarExpression<*>): UpdateAssignments =
        thenSetExpr(property.name, expr)

    /**
     * 是否为空
     * Check if empty
     *
     * @return 如果为空则返回 true / true if empty
    */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * 是否非空
     * Check if not empty
     *
     * @return 如果非空则返回 true / true if not empty
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
