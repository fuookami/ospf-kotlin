package fuookami.ospf.kotlin.utils.concept

interface Movable<Self> {
    fun move(): Self
}

fun <T : Movable<T>> move(ele: T) = ele.move()
