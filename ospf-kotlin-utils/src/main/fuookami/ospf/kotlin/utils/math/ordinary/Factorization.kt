package fuookami.ospf.kotlin.utils.math.ordinary

import kotlin.reflect.full.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

fun <I> factorizeImpl(num: I, constants: RealNumberConstants<I>): List<Pair<I, Int>> where I: Integer<I>, I: Div<I, I>, I: Rem<I, I> {
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
inline fun <reified I> factorize(num: I): List<Pair<I, Int>> where I: Integer<I>, I: Div<I, I>, I: Rem<I, I> {
    return factorizeImpl(num, (I::class.companionObjectInstance as RealNumberConstants<I>))
}
