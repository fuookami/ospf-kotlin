package fuookami.ospf.kotlin.math.symbol

class TmpDefaultTypeArgCheck<T>(val value: T)

fun tmpDefaultTypeArgCheck() {
    val v = TmpDefaultTypeArgCheck(1)
    if (v.value != 1) {
        throw IllegalStateException("unexpected")
    }
}
