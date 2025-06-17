package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

internal fun <I> gcdImpl(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    val zero = x.constants.zero
    assert(x >= zero)
    assert(y >= zero)

    if (x == zero) {
        return y
    }
    if (y == zero) {
        return x
    }

    var a = x
    var b = y
    while (b != zero) {
        if (a > b) {
            val temp = b
            b = a
            a = temp
        }
        b -= a
    }
    return a
}

internal fun <I> gcdImpl(numbers: List<I>): I where I : Integer<I>, I : Rem<I, I> {
    assert(numbers.isNotEmpty())

    val zero = numbers.first().constants.zero
    val restNumbers = numbers.toMutableList()

    while (restNumbers.first() neq restNumbers.last()) {
        for (i in 0 until restNumbers.lastIndex) {
            if (restNumbers[i] % restNumbers[i + 1] eq zero) {
                restNumbers[i] = restNumbers[i + 1]
            } else {
                restNumbers[i] = restNumbers[i] % restNumbers[i + 1]
            }
        }
        restNumbers.sortDescending()
    }
    return restNumbers.first()
}

fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    return gcdImpl(x.abs(), y.abs())
}

fun <I> gcd(numbers: List<I>): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(numbers.map { it.abs() }.sortedDescending())
}

fun <I> gcd(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(listOf(x, y, z) + numbers.toList())
}
