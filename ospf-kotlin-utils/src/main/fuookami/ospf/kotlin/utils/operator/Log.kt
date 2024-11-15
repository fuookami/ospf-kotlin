package fuookami.ospf.kotlin.utils.operator

interface Log<in Base, out Ret> {
    fun log(base: Base): Ret?
    fun lg(): Ret?
    fun lg2(): Ret?
    fun ln(): Ret?
}

fun <Base, Natural, Ret> log(base: Base, natural: Natural): Ret? where Natural : Log<Base, Ret> {
    return natural.log(base)
}

fun <Base, Natural, Ret> lg(natural: Natural): Ret? where Natural : Log<Base, Ret> {
    return natural.lg()
}

fun <Base, Natural, Ret> lg2(natural: Natural): Ret? where Natural : Log<Base, Ret> {
    return natural.lg2()
}

fun <Base, Natural, Ret> ln(natural: Natural): Ret? where Natural : Log<Base, Ret> {
    return natural.ln()
}
