package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

interface Exponent<T> where T : Comparable<T> {
    val value: T
}

data class NonNegativeExponent<T>(
    override val value: T
) : Exponent<T> where T : Comparable<T>
