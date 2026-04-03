package fuookami.ospf.kotlin.utils.math.operator

interface Rem<in Rhs, out Ret> {
    operator fun rem(rhs: Rhs): Ret

    infix fun mod(rhs: Rhs) = this % rhs
}

interface RemAssign<in Rhs> {
    operator fun remAssign(rhs: Rhs)
}
