package fuookami.ospf.kotlin.utils.functional

import java.io.Serializable

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : Serializable {
    public override fun toString(): String = "($first, $second, $third, $fourth)"
}

fun <T> Quadruple<T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth)
