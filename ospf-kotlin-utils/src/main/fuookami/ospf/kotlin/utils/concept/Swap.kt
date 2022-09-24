package fuookami.ospf.kotlin.utils.concept

interface Swappable<Self> {
    infix fun swap(rhs: Self)
}

fun <T : Swappable<T>> swap(lhs: T, rhs: T) {
    lhs swap rhs
}
