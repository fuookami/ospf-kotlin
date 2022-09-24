package fuookami.ospf.kotlin.utils.operator

interface Div<in Rhs, out Ret> {
    operator fun div(rhs: Rhs): Ret
}

interface DivAssign<in Rhs> {
    operator fun divAssign(rhs: Rhs)
}

interface IntDiv<in Rhs, out Ret> {
    infix fun intDiv(rhs: Rhs): Ret
}


interface IntDivAssign<in Rhs> {
    infix fun intDivAssign(rhs: Rhs)
}
