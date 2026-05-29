/**
 * 包含关系运算笌
 * Contains Operator
 *
 * 定义统一的包含关系接口，用于表达当前对象是否包含给定值。
 * Defines a unified containment interface for expressing whether the current object contains a given value.
 *
 * @param T 被检查值的类型
 * @param T The type of the value being checked
 */
package fuookami.ospf.kotlin.math.operator

interface Contains<in T> {
    /**
     * 判断当前对象是否包含指定值
     * Determines whether the current object contains the specified value
     *
     * @param value 要检查的值
     * @return 如果包含该值则返回 true
     *
     * @param value The value to check
     * @return True if the value is contained
     */
    operator fun contains(value: T): Boolean
}
