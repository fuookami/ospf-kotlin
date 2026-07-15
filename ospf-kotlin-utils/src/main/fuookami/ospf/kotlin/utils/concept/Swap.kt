/**
 * 交换概念接口
 *
 * Swap concept interface providing swap semantics for types.
 * 为类型提供交换语义的交换概念接口。
 *
 * This interface defines a swap operation that exchanges the internal state
 * between two instances of the same type.
 * 此接口定义了一个交换操作，用于在两个相同类型的实例之间交换内部状态。
*/
package fuookami.ospf.kotlin.utils.concept

/**
 * 可交换接口
 *
 * Interface for types that support swap semantics.
 * 支持交换语义的类型接口。
 *
 * Types implementing this interface can exchange their internal state
 * with another instance of the same type.
 * 实现此接口的类型可以与另一个相同类型的实例交换内部状态。
 *
 * @param Self 实现此接口的类型本身 / The type implementing this interface
*/
interface Swappable<Self> {

    /**
     * 与另一个实例交换状态
     *
     * Swaps the internal state with another instance.
     * 与另一个实例交换内部状态。
     *
     * @param rhs 要交换的另一个实例 / The other instance to swap with
    */
    infix fun swap(rhs: Self)
}

/**
 * 交换两个元素的便捷函数
 *
 * Convenience function to swap two elements.
 * 交换两个元素的便捷函数。
 *
 * @param T 可交换类型 / The swappable type
 * @param lhs 第一个元素 / The first element
 * @param rhs 第二个元素 / The second element
*/
fun <T : Swappable<T>> swap(lhs: T, rhs: T) {
    lhs swap rhs
}
