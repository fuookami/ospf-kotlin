package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

private fun <I> gcdImpl(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    val remainder = x % y

    return if (remainder eq remainder.constants.zero) {
        y
    } else {
        gcdImpl(y, remainder)
    }
}

private fun <I> gcdImpl(numbers: List<I>): I where I: Integer<I>, I : Rem<I, I> {
    assert(numbers.isNotEmpty())

    val zero = numbers.first().constants.zero
    val restNumbers = numbers.sortedDescending().toMutableList()

    while (restNumbers.first() neq restNumbers.last()) {
        for (i in 0 until (restNumbers.size - 1)) {
            if (restNumbers[i] % restNumbers[i + 1] eq zero) {
                restNumbers[i] = restNumbers[i + 1]
            } else {
                restNumbers[i] = restNumbers[i] % restNumbers[i + 1]
            }
        }
    }
    return restNumbers.first()
}

fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(x.abs(), y.abs())
}

fun <I> gcd(numbers: List<I>): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(numbers.map { it.abs() }.sortedDescending())
}

fun <I> gcd(x: I, y: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(listOf(x) + listOf(y) + numbers.toList())
}
