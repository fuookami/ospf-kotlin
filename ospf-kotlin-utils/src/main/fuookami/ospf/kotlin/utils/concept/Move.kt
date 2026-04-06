/**
 * 移动概念接口
 *
 * Move concept interface providing move semantics for types.
 * 为类型提供移动语义的移动概念接口。
 *
 * This interface defines a move operation that transfers ownership of resources
 * from the current instance to a new instance, similar to C++ move semantics.
 * 此接口定义了一个移动操作，将资源的所有权从当前实例转移到新实例，
 * 类似于 C++ 的移动语义。
 */
package fuookami.ospf.kotlin.utils.concept

/**
 * 可移动接口
 *
 * Interface for types that support move semantics.
 * 支持移动语义的类型接口。
 *
 * Types implementing this interface can transfer their internal state
 * to a new instance, leaving the original instance in a valid but unspecified state.
 * 实现此接口的类型可以将其内部状态转移到新实例，
 * 原实例保持在有效但未指定的状态。
 *
 * @param Self 实现此接口的类型本身 / The type implementing this interface
 */
interface Movable<Self> {
    /**
     * 执行移动操作
     *
     * Performs a move operation, returning a new instance with the transferred state.
     * 执行移动操作，返回一个带有转移状态的新实例。
     *
     * @return 移动后的新实例 / The new instance after the move
     */
    fun move(): Self
}

/**
 * 移动元素的便捷函数
 *
 * Convenience function to move an element.
 * 移动元素的便捷函数。
 *
 * @param T 可移动类型 / The movable type
 * @param ele 要移动的元素 / The element to move
 * @return 移动后的新实例 / The new instance after the move
 */
fun <T : Movable<T>> move(ele: T) = ele.move()
