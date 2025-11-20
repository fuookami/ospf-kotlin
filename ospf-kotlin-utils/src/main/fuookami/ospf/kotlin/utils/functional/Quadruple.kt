package fuookami.ospf.kotlin.utils.functional

import kotlinx.serialization.*

typealias Pair2<T> = Pair<T, T>
typealias Triple3<T> = Triple<T, T, T>
typealias Quadruple4<T> = Quadruple<T, T, T, T>

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
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component2() = second
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component3() = third
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component4() = fourth

fun <T> Quadruple<T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth)
