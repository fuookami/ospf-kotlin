package fuookami.ospf.kotlin.utils.concept

interface Copyable<Self> : Movable<Self> {
    override fun move() = copy()
    fun copy(): Self
}

fun <T : Copyable<T>> copy(ele: T) = ele.copy()
