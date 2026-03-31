package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.concept.Integer
import fuookami.ospf.kotlin.utils.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.utils.operator.Div
import fuookami.ospf.kotlin.utils.operator.Minus
import fuookami.ospf.kotlin.utils.operator.Pow
import fuookami.ospf.kotlin.utils.operator.Rem
import kotlin.reflect.full.companionObjectInstance

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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> factorize(num: I): List<Pair<I, Int>> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return factorizeImpl(num, (I::class.companionObjectInstance as RealNumberConstants<I>))
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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> defactorize(
    factors: Iterable<Pair<I, Int>>
): I where I : Integer<I>, I : Pow<I> {
    return defactorizeImpl(
        factors = factors,
        constants = (I::class.companionObjectInstance as RealNumberConstants<I>)
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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> divisors(
    factors: List<Pair<I, Int>>
): List<I> where I : Integer<I>, I : Pow<I> {
    return divisorsImpl(
        factors = factors,
        constants = (I::class.companionObjectInstance as RealNumberConstants<I>)
    )
}

@Suppress("UNCHECKED_CAST")
inline fun <reified I> divisors(
    num: I
): List<I> where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Pow<I> {
    return divisorsImpl(
        num = num,
        constants = (I::class.companionObjectInstance as RealNumberConstants<I>)
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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> divisorCount(
    num: I
): Int where I : Integer<I>, I : Div<I, I>, I : Rem<I, I> {
    return divisorCount(factorize(num))
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

@Suppress("UNCHECKED_CAST")
inline fun <reified I> eulerTotient(
    num: I
): I where I : Integer<I>, I : Div<I, I>, I : Rem<I, I>, I : Minus<I, I> {
    return eulerTotientImpl(
        num = num,
        constants = (I::class.companionObjectInstance as RealNumberConstants<I>)
    )
}






