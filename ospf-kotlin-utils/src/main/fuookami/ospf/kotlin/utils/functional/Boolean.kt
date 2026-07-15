/**
 * Boolean 扩展函数
 *
 * Extension functions for Boolean type, providing nullable-safe logical operations.
 * Boolean 类型的扩展函数，提供空值安全的逻辑操作。
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 可空 Boolean 的逻辑与操作
 *
 * Logical AND operation for nullable Boolean values.
 * 可空 Boolean 值的逻辑与操作。
 *
 * @param other 另一个可空 Boolean 值 / Another nullable Boolean value
 * @return 两个值都非空时返回逻辑与结果，否则返回 null / Returns logical AND result if both values are non-null, otherwise null
*/
infix fun Boolean?.and(other: Boolean?) = if (this != null && other != null) {
    this && other
} else {
    null
}
