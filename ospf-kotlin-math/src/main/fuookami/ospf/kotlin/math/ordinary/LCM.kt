package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.IntX
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Pow
import fuookami.ospf.kotlin.math.operator.Rem

fun <I> lcmImpl(numbers: Iterable<I>, constants: RealNumberConstants<I>): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
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

@Suppress("UNCHECKED_CAST")
fun <I> lcmByFactorization(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmImpl(numbers, constants)
}

inline fun <reified I> lcmByFactorization(numbers: Iterable<I>): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcmByFactorization(
    x: I,
    y: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = listOf(x, y),
        constants = constants
    )
}

inline fun <reified I> lcmByFactorization(x: I, y: I): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        x = x,
        y = y,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcmByFactorization(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

inline fun <reified I> lcmByFactorization(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Pow<I>, I : Div<I, I>, I : Rem<I, I> {
    return lcmByFactorization(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcm(
    x: I,
    y: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Rem<I, I>, I : Div<I, I> {
    val px = x.abs()
    val py = y.abs()
    val thisGCD = gcd(
        numbers = listOf(px, py),
        constants = constants
    )
    return (px / thisGCD) * py
}

inline fun <reified I> lcm(x: I, y: I): I where I : Integer<I>, I : Rem<I, I>, I : Minus<I, I>, I : Div<I, I> {
    return lcm(
        x = x,
        y = y,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

@Suppress("UNCHECKED_CAST")
fun <I> lcm(
    numbers: Iterable<I>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcmImpl(numbers, constants)
}

inline fun <reified I> lcm(numbers: Iterable<I>): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
}

fun <I> lcm(
    x: I,
    y: I,
    z: I,
    vararg numbers: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        numbers = listOf(x, y, z) + numbers.toList(),
        constants = constants
    )
}

inline fun <reified I> lcm(x: I, y: I, z: I, vararg numbers: I): I where I : Integer<I>, I : Pow<I>, I : Rem<I, I>, I : Div<I, I> {
    return lcm(
        x = x,
        y = y,
        z = z,
        numbers = numbers,
        constants = resolveRealNumberConstants<I>("LCM")
    )
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

fun <F : FloatingNumber<*>> lcm(x: FltX, y: FltX, z: FltX, vararg numbers: F): FltX {
    return lcm(listOf(x, y, z) + numbers.map { it.toFltX() })
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
