package fuookami.ospf.kotlin.utils.math.operator

interface Plus<in Rhs, out Ret> {
    operator fun plus(rhs: Rhs): Ret
}

interface PlusTrait<Self, in Rhs, out Ret> {
    operator fun Self.plus(rhs: Rhs): Ret
}

interface PlusAssign<in Rhs> {
    operator fun plusAssign(rhs: Rhs)
}

interface Inc<Self> {
    operator fun inc(): Inc<Self>
}
