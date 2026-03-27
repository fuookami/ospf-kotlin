package fuookami.ospf.kotlin.utils.math.symbol.generic

interface Exponent<T> where T : Comparable<T> {
    val value: T
}

data class NonNegativeExponent<T>(
    override val value: T
) : Exponent<T> where T : Comparable<T>
