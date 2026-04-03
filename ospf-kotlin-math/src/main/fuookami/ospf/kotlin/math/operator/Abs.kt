package fuookami.ospf.kotlin.math.operator

interface Abs<out Ret> {
    fun abs(): Ret
}

fun <T, U : Abs<T>> abs(num: U): T {
    return num.abs()
}
