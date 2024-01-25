package fuookami.ospf.kotlin.utils.operator

interface Exp<out Ret> {
    fun exp(): Ret
}

fun <Index, Ret> exp(index: Index): Ret where Index : Exp<Ret> {
    return index.exp()
}
