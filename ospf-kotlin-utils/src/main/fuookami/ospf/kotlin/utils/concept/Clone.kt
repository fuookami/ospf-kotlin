package fuookami.ospf.kotlin.utils.concept

interface Cloneable<Self> {
    fun clone(): Self
}

interface Copyable<Self> : Cloneable<Self>, Movable<Self> {
    override fun move() = clone()
}

fun <T : Cloneable<T>> clone(ele: T) = ele.clone()
