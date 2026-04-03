package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.operator.Div
import fuookami.ospf.kotlin.utils.math.operator.Minus
import fuookami.ospf.kotlin.utils.math.operator.Pow
import fuookami.ospf.kotlin.utils.math.operator.Rem

fun <I> factorizeImpl(num: I, constants: RealNumberConstants<I>): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    if (num <= constants.one) {
        return emptyList()
    }

    var n = num
    val factors = ArrayList<Pair<I, Int>>()

    for (prime in getPrimesImpl(num, constants)) {
        if (prime * prime > num) {
            break
        }

        var index = 0
        while (n % prime eq constants.zero) {
            index += 1
            n /= prime
        }
        if (index != 0) {
            factors.add(prime to index)
        }
    }

    if (n > constants.one) {
        factors.add(n to 1)
    }

    return factors
}

fun <I> factorize(
    num: I,
    constants: RealNumberConstants<I>
): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return factorizeImpl(num, constants)
}

inline fun <reified I> factorize(num: I): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return factorize(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

fun <I> defactorizeImpl(
    factors: Iterable<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I> {
    var value = constants.one
    for ((factor, index) in factors) {
        if (index < 0) {
            throw IllegalArgumentException("Negative factor index is not supported: $index.")
        }
        if (index == 0) {
            continue
        }
        value *= factor.pow(index)
    }
    return value
}

fun <I> defactorize(
    factors: Iterable<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Pow<I> {
    return defactorizeImpl(factors, constants)
}

inline fun <reified I> defactorize(
    factors: Iterable<Pair<I, Int>>
): I where I : Integer<I>, I : Pow<I> {
    return defactorize(
        factors = factors,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

fun <I> divisorsImpl(
    factors: List<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Pow<I> {
    var values = listOf(constants.one)
    for ((factor, index) in factors) {
        if (index <= 0) {
            continue
        }
        val nextValues = ArrayList<I>(values.size * (index + 1))
        var factorPower = constants.one
        for (k in 0..index) {
            for (value in values) {
                nextValues.add(value * factorPower)
            }
            factorPower *= factor
        }
        values = nextValues
    }
    return values.sorted()
}

fun <I> divisorsImpl(
    num: I,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisorsImpl(factorizeImpl(num, constants), constants)
}

fun <I> divisors(
    factors: List<Pair<I, Int>>,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Pow<I> {
    return divisorsImpl(factors, constants)
}

inline fun <reified I> divisors(
    factors: List<Pair<I, Int>>
): List<I> where I : Integer<I>, I : Pow<I> {
    return divisors(
        factors = factors,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

fun <I> divisors(
    num: I,
    constants: RealNumberConstants<I>
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisorsImpl(num, constants)
}

inline fun <reified I> divisors(
    num: I
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisors(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

fun <I> divisorCount(factors: Iterable<Pair<I, Int>>): Int where I : Integer<I> {
    var count = 1
    for ((_, index) in factors) {
        if (index > 0) {
            count *= (index + 1)
        }
    }
    return count
}

fun <I> divisorCount(
    num: I,
    constants: RealNumberConstants<I>
): Int where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return divisorCount(factorizeImpl(num, constants))
}

inline fun <reified I> divisorCount(
    num: I
): Int where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return divisorCount(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}

fun <I> eulerTotientImpl(
    num: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    if (num == constants.zero || num == constants.one) {
        return num
    }
    var value = num
    val one = constants.one
    for ((prime, _) in factorizeImpl(num, constants)) {
        value = (value / prime) * (prime - one)
    }
    return value
}

fun <I> eulerTotient(
    num: I,
    constants: RealNumberConstants<I>
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    return eulerTotientImpl(num, constants)
}

inline fun <reified I> eulerTotient(
    num: I
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    return eulerTotient(
        num = num,
        constants = resolveRealNumberConstants<I>("Factorization")
    )
}
