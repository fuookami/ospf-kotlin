package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

fun <I> lcm(x: I, y: I): I where I : Integer<I>, I : Rem<I, I>, I : Minus<I, I>, I : Div<I, I> {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(px, py)
    return (px / thisGCD) * py
}

fun <I> lcm(numbers: List<I>): I where I : Integer<I>, I : Rem<I, I>, I : Div<I, I> {
    val pn = numbers.map { it.abs() }.sortedDescending()
    val thisGCD = gcdImpl(pn)
    return pn.fold(numbers.first().constants.one) { lhs, rhs ->
        lhs * (rhs / thisGCD)
    } * thisGCD
}

fun <I> lcm(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(listOf(x, y, z) + numbers.toList())
}
