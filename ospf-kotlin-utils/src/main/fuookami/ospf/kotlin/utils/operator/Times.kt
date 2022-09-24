package fuookami.ospf.kotlin.utils.operator

interface Times<in Rhs, out Ret> {
    operator fun times(rhs: Rhs): Ret
}

interface TimesAssign<in Rhs> {
    operator fun timesAssign(rhs: Rhs)
}

interface Cross<in Rhs, out Ret> {
    infix fun x(rhs: Rhs): Ret
}
