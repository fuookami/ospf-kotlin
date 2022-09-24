package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.Integer
import fuookami.ospf.kotlin.utils.operator.Rem

private fun <I> gcdImpl(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    val remainder = x % y

    return if (remainder eq remainder.constants.zero) y
    else gcd(y, remainder)
}

fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(x.abs(), y.abs())
}
