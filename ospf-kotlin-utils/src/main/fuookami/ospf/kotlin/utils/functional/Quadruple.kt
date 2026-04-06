/**
 * 四元组
 *
 * Quadruple data class and type aliases for uniform tuple types.
 * 四元组数据类和统一元组类型的类型别名。
 */
package fuookami.ospf.kotlin.utils.functional

import kotlinx.serialization.Serializable

/**
 * 均质二元组类型别名
 *
 * Type alias for a Pair with both elements of the same type.
 * 两个元素类型相同的 Pair 类型别名。
 */
typealias Pair2<T> = Pair<T, T>

/**
 * 均质三元组类型别名
 *
 * Type alias for a Triple with all elements of the same type.
 * 所有元素类型相同的 Triple 类型别名。
 */
typealias Triple3<T> = Triple<T, T, T>

/**
 * 均质四元组类型别名
 *
 * Type alias for a Quadruple with all elements of the same type.
 * 所有元素类型相同的 Quadruple 类型别名。
 */
typealias Quadruple4<T> = Quadruple<T, T, T, T>

/**
 * 四元组数据类
 *
 * Represents a tuple of four values.
 * 表示包含四个值的元组。
 *
 * @param A 第一个元素的类型 / The type of the first element
 * @param B 第二个元素的类型 / The type of the second element
 * @param C 第三个元素的类型 / The type of the third element
 * @param D 第四个元素的类型 / The type of the fourth element
 * @param first 第一个元素 / The first element
 * @param second 第二个元素 / The second element
 * @param third 第三个元素 / The third element
 * @param fourth 第四个元素 / The fourth element
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

/**
 * 四元组第一个元素的解构操作符
 *
 * Component operator for destructuring the first element.
 * 解构第一个元素的操作符。
 *
 * @return 第一个元素 / The first element
 */
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component1() = first

/**
 * 四元组第二个元素的解构操作符
 *
 * Component operator for destructuring the second element.
 * 解构第二个元素的操作符。
 *
 * @return 第二个元素 / The second element
 */
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component2() = second

/**
 * 四元组第三个元素的解构操作符
 *
 * Component operator for destructuring the third element.
 * 解构第三个元素的操作符。
 *
 * @return 第三个元素 / The third element
 */
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component3() = third

/**
 * 四元组第四个元素的解构操作符
 *
 * Component operator for destructuring the fourth element.
 * 解构第四个元素的操作符。
 *
 * @return 第四个元素 / The fourth element
 */
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component4() = fourth

/**
 * 将均质四元组转换为列表
 *
 * Converts a homogeneous Quadruple to a List.
 * 将均质四元组转换为列表。
 *
 * @param T 所有元素的类型 / The type of all elements
 * @return 包含四个元素的列表 / A list containing the four elements
 */
fun <T> Quadruple<T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth)
