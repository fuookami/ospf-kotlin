package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring

interface Coefficient<out T> {
    val value: T
}

data class RingCoefficient<T>(
    override val value: T
) : Coefficient<T> where T : Ring<T>

