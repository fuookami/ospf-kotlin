/**
 * 复制概念接口
 *
 * Copy concept interface providing copy semantics for types.
 * 为类型提供复制语义的复制概念接口。
 *
 * This interface extends Movable and provides a default move implementation
 * that delegates to copy, since copying is a valid implementation of moving.
 * 此接口扩展了 Movable，并提供了默认的移动实现委托给复制，
 * 因为复制是移动的有效实现方式。
 */
package fuookami.ospf.kotlin.utils.concept

/**
 * 可复制接口
 *
 * Interface for types that support copy semantics.
 * 支持复制语义的类型接口。
 *
 * Types implementing this interface can create a deep copy of themselves.
 * The default move implementation delegates to copy.
 * 实现此接口的类型可以创建自身的深拷贝。
 * 默认的移动实现委托给复制。
 *
 * @param Self 实现此接口的类型本身 / The type implementing this interface
 */
interface Copyable<Self> : Movable<Self> {
    /**
     * 默认移动实现，委托给复制
     *
     * Default move implementation that delegates to copy.
     * 默认的移动实现委托给复制。
     *
     * @return 复制后的新实例 / A copy of the instance
     */
    override fun move() = copy()

    /**
     * 创建实例的深拷贝
     *
     * Creates a deep copy of the instance.
     * 创建实例的深拷贝。
     *
     * @return 复制后的新实例 / A copy of the instance
     */
    fun copy(): Self
}

/**
 * 复制元素的便捷函数
 *
 * Convenience function to copy a non-null element.
 * 复制非空元素的便捷函数。
 *
 * @param T 可复制类型 / The copyable type
 * @param ele 要复制的元素 / The element to copy
 * @return 复制后的新实例 / A copy of the element
 */
@JvmName("copyNotNull")
fun <T : Copyable<T>> copy(ele: T) = ele.copy()

/**
 * 复制可空元素的便捷函数
 *
 * Convenience function to copy a nullable element.
 * 复制可空元素的便捷函数。
 *
 * @param T 可复制类型 / The copyable type
 * @param ele 要复制的可空元素 / The nullable element to copy
 * @return 复制后的新实例，如果输入为 null 则返回 null /
 *         A copy of the element, or null if the input was null
 */
@JvmName("copyNullable")
fun <T : Copyable<T>> copy(ele: T?) = ele?.copy()

/**
 * 如果元素非空则复制，否则返回默认值
 *
 * Copies the element if not null, otherwise returns the default value.
 * 如果元素非空则复制，否则返回默认值。
 *
 * @param T 可复制类型 / The copyable type
 * @param default 默认值提供函数 / Function to provide the default value
 * @return 复制后的实例或默认值 / A copy of the element or the default value
 */
@JvmName("copyIfNotNullOrT")
fun <T : Copyable<T>> T?.copyIfNotNullOr(default: () -> T): T = this?.copy() ?: default()

/**
 * 如果元素非空则复制，否则返回默认值
 *
 * Copies the element if not null, otherwise returns the default value.
 * 如果元素非空则复制，否则返回默认值。
 *
 * @param T 可复制类型 / The copyable type
 * @param ele 要复制的可空元素 / The nullable element to copy
 * @param default 默认值提供函数 / Function to provide the default value
 * @return 复制后的实例或默认值 / A copy of the element or the default value
 */
fun <T : Copyable<T>> copyIfNotNullOr(ele: T?, default: () -> T): T = ele?.copy() ?: default()
