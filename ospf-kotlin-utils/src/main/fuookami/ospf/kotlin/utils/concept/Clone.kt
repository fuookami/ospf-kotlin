package fuookami.ospf.kotlin.utils.concept

interface Copyable<Self> : Movable<Self> {
    override fun move() = copy()
    fun copy(): Self
}

@JvmName("copyNotNull")
fun <T : Copyable<T>> copy(ele: T) = ele.copy()
@JvmName("copyNullable")
fun <T : Copyable<T>> copy(ele: T?) = ele?.copy()

@JvmName("copyIfNotNullOrT")
fun <T : Copyable<T>> T?.copyIfNotNullOr(default: () -> T): T = this?.copy() ?: default()
fun <T : Copyable<T>> copyIfNotNullOr(ele: T?, default: () -> T): T = ele?.copy() ?: default()
