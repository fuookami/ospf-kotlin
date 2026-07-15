/**
 * 条件类
 *
 * Condition class for lazy predicate evaluation.
 * 条件类，用于延迟谓词求值。
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 条件数据类
 *
 * Represents a value with an associated predicate for lazy evaluation.
 * 表示一个带有关联谓词的值，用于延迟求值。
 *
 * @param T 值的类型 / The type of the value
 * @param value 被检查的值 / The value to be checked
 * @param predicate 用于检查值的谓词 / The predicate to check the value
*/
data class Condition<T>(
    val value: T,
    val predicate: Predicate<T>
) {

    /**
     * 谓词对值求值的惰性结果
     *
     * Lazy result of applying the predicate to the value.
     * 对值应用谓词的惰性结果。
    */
    val result: Boolean by lazy { predicate(value) }

    /**
     * 将条件转换为可空值
     *
     * Converts the condition to a nullable value.
     * Returns null if the predicate is true, otherwise returns the value.
     * 将条件转换为可空值。如果谓词为真则返回 null，否则返回值。
     *
     * @return 谓词为真时返回 null，否则返回原值 / Returns null if predicate is true, otherwise the original value
    */
    fun asNull(): T? = if (predicate(value)) null else value
}

/**
 * 创建条件对象
 *
 * Creates a Condition object for a value with the given predicate.
 * 使用给定谓词为值创建 Condition 对象。
 *
 * @param T 值的类型 / The type of the value
 * @param predicate 用于检查值的谓词 / The predicate to check the value
 * @return 包含值和谓词的 Condition 对象 / A Condition object containing the value and predicate
*/
fun <T> T.ifTrue(predicate: Predicate<T>) = Condition(this, predicate)
