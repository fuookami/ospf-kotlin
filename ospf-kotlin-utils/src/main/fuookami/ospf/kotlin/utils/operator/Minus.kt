package fuookami.ospf.kotlin.utils.operator

interface Minus<in Rhs, out Ret> {
    operator fun minus(rhs: Rhs): Ret
}

interface MinusAssign<in Rhs> {
    operator fun minusAssign(rhs: Rhs)
}

interface Dec<Self> {
    operator fun dec(): Dec<Self>
}
