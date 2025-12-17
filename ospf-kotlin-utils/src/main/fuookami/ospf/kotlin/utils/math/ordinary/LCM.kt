package fuookami.ospf.kotlin.utils.math.ordinary

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

fun <I> lcmImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I: Integer<I>, I: Pow<I>, I: Div<I, I>, I: Rem<I, I> {
    val factors = numbers.map { factorizeImpl(it, constants) }
    if (factors.isEmpty()) {
        return constants.one
    } else if (factors.any { it.isEmpty() }) {
        return constants.zero
    } else if (factors.size == 1) {
        return numbers.first()
    }

    val mergedFactors = factors
        .flatten()
        .groupBy { it.first }
        .mapValues { it.value.maxOf { (_, value) -> value } }
    return mergedFactors.entries.fold(constants.one) { lhs, (factor, index) ->
        lhs * factor.pow(index)
    }
}

inline fun <reified I> lcm(x: I, y: I): I where I : Integer<I>, I : Rem<I, I>, I : Minus<I, I>, I: Div<I, I> {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(px, py)
    return (px / thisGCD) * py
}

@Suppress("UNCHECKED_CAST")
inline fun <reified I> lcm(numbers: Iterable<I>): I where I : Integer<I>, I: Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcmImpl(numbers, (I::class.companionObjectInstance as RealNumberConstants<I>))
}

inline fun <reified I> lcm(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I: Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(listOf(x, y, z) + numbers.toList())
}

fun lcm(x: FltX, y: FltX): FltX {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(px, py)
    return (px / thisGCD) * py
}

fun lcm(numbers: Iterable<FltX>): FltX {
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
    return lcmImpl(integerNumbers.sortedDescending(), IntX).toFltX() / FltX.ten.pow(factor)
}

fun <F: FltX> lcm(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return lcm(listOf(x, y, z) + numbers.toList())
}

fun lcm(x: RtnX, y: RtnX): RtnX {
    return lcm(listOf(x, y))
}

fun lcm(numbers: Iterable<RtnX>): RtnX {
    return RtnX(
        lcm(numbers.map { it.num }),
        gcd(numbers.map { it.den })
    )
}

fun lcm(x: RtnX, y: RtnX, z: RtnX, vararg numbers: RtnX): RtnX {
    return lcm(listOf(x, y, z) + numbers.toList())
}
