package fuookami.ospf.kotlin.utils.math.ordinary

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

fun <I> gcdImpl(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
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

fun <I> gcdImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I : Integer<I>, I : Rem<I, I> {
    if (numbers.firstOrNull() == null) {
        return constants.one
    }

    val restNumbers = numbers.toMutableList()
    while (restNumbers.first() neq restNumbers.last()) {
        for (i in 0 until restNumbers.lastIndex) {
            if (restNumbers[i] % restNumbers[i + 1] eq constants.zero) {
                restNumbers[i] = restNumbers[i + 1]
            } else {
                restNumbers[i] = restNumbers[i] % restNumbers[i + 1]
            }
        }
        restNumbers.sortDescending()
    }
    return restNumbers.first()
}

@Suppress("UNCHECKED_CAST")
fun <I> gcd(x: I, y: I): I where I : Integer<I>, I : Minus<I, I> {
    return gcdImpl(x, y)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified I> gcd(numbers: Iterable<I>): I where I : Integer<I>, I : Rem<I, I> {
    return gcdImpl(
        numbers.map { it.abs() }.sortedDescending(),
        I::class.companionObjectInstance as RealNumberConstants<I>
    )
}

inline fun <reified I> gcd(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Rem<I, I> {
    return gcd(listOf(x, y, z) + numbers.toList())
}

fun gcd(x: FltX, y: FltX): FltX {
    return gcd(listOf(x, y))
}

fun gcd(numbers: Iterable<FltX>): FltX {
    var factor = 0
    var scaledNumbers = numbers.toList()
    val integerNumbers = ArrayList<IntX>()
    while (true) {
        integerNumbers.clear()
        for (num in scaledNumbers) {
            if (num.round() eq num) {
                integerNumbers.add(num.toIntX().abs())
            } else {
                factor += 1
                scaledNumbers = scaledNumbers.map { it * FltX.ten }
                break
            }
        }
        if (integerNumbers.size == scaledNumbers.size) {
            break
        }
    }
    val integerGCD = gcdImpl(
        integerNumbers.sortedDescending(),
        IntX
    ).toFltX()
    return integerGCD / FltX.ten.pow(factor)
}

fun <F: FltX> gcd(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return gcd(listOf(x, y, z) + numbers.toList())
}

fun gcd(x: RtnX, y: RtnX): RtnX {
    return gcd(listOf(x, y))
}

fun gcd(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        gcd(numbers.map { it.num }),
        lcm(numbers.map { it.den })
    )
}

fun gcd(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return gcd(listOf(x, y, z) + numbers.toList())
}
