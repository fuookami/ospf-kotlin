/**
 * Quadruple data class and type aliases for uniform tuple types.
 * 四元组数据类和统一元组类型的类型别名。
*/
package fuookami.ospf.kotlin.utils.functional

import kotlinx.serialization.Serializable

/**
 * Type alias for a Pair with both elements of the same type.
 * 两个元素类型相同的 Pair 类型别名。
*/
typealias Pair2<T> = Pair<T, T>

/**
 * Type alias for a Triple with all elements of the same type.
 * 所有元素类型相同的 Triple 类型别名。
*/
typealias Triple3<T> = Triple<T, T, T>

/**
 * Type alias for a Quadruple with all elements of the same type.
 * 所有元素类型相同的 Quadruple 类型别名。
*/
typealias Quadruple4<T> = Quadruple<T, T, T, T>

/**
 * Represents a tuple of four values.
 * 表示包含四个值的元组。
 *
 * @param A The type of the first element / 第一个元素的类型
 * @param B The type of the second element / 第二个元素的类型
 * @param C The type of the third element / 第三个元素的类型
 * @param D The type of the fourth element / 第四个元素的类型
 * @property first The first element / 第一个元素
 * @property second The second element / 第二个元素
 * @property third The third element / 第三个元素
 * @property fourth The fourth element / 第四个元素
*/
@Serializable
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

operator fun <A, B, C, D> Quadruple<A, B, C, D>.component1() = first

/**
 * Component operator for destructuring the second element.
 * 四元组第二个元素的解构操作符。
 *
 * @return the second element / 第二个元素
*/
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component2() = second

/**
 * Component operator for destructuring the third element.
 * 四元组第三个元素的解构操作符。
 *
 * @return the third element / 第三个元素
*/
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component3() = third

/**
 * Component operator for destructuring the fourth element.
 * 四元组第四个元素的解构操作符。
 *
 * @return the fourth element / 第四个元素
*/
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component4() = fourth

/**
 * Converts a homogeneous Quadruple to a List.
 * 将均质四元组转换为列表。
 *
 * @param T the type of all elements / 所有元素的类型
 * @return a list containing the four elements / 包含四个元素的列表
*/
fun <T> Quadruple<T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth)
