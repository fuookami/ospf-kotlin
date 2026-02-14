package fuookami.ospf.kotlin.utils.math.ordinary

import java.math.*
import fuookami.ospf.kotlin.utils.math.*
import kotlin.reflect.full.companionObjectInstance

@Suppress("UNCHECKED_CAST")
fun <T : FloatingNumber<T>> ln(
    x: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits!!,
    precision: T = constants.epsilon
): T? {
    return if (x leq constants.zero) {
        constants.nan
    } else if (x leq constants.two) {
        val y = (x - constants.one) / (x + constants.one)
        var yPow = y
        var value = y
        var i = constants.one
        while (true) {
            yPow = yPow * y * y
            if (yPow is FltX) {
                yPow = yPow.withScale(digits, RoundingMode.HALF_UP) as T
            }
            var term =  yPow / (constants.two * i + constants.one)
            if (term is FltX) {
                term = term.withScale(digits, RoundingMode.HALF_UP) as T
            }
            value += term
            i += constants.one

            if (term.abs() <= precision) {
                break
            }
        }
        value * constants.two
    } else {
        var m = x
        var k = constants.zero

        while (m >= constants.two) {
            m /= constants.two
            k += constants.one
        }
        while (m < constants.one) {
            m *= constants.two
            k -= constants.one
        }
        ln(m, constants)!! + k * constants.lg2
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> ln(
    x: T,
    digits: Int = (T::class::companionObjectInstance as FloatingNumberConstants<T>).decimalDigits!!,
    precision: T = (T::class::companionObjectInstance as FloatingNumberConstants<T>).epsilon
): T? {
    return ln(
        x = x,
        constants = T::class::companionObjectInstance as FloatingNumberConstants<T>,
        digits = digits,
        precision = precision
    )
}

fun <T : FloatingNumber<T>> log(
    x: T,
    base: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits!!,
    precision: T = constants.epsilon
): T? {
    return ln(
        x = x,
        constants = constants,
        digits = digits,
        precision = precision
    )?.let { lhs ->
        ln(
            x = base,
            constants = constants,
            digits = digits,
            precision = precision
        )?.let {
            lhs / it
        }
    } ?: constants.nan
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> log(
    x: T,
    base: T,
    digits: Int = (T::class::companionObjectInstance as FloatingNumberConstants<T>).decimalDigits!!,
    precision: T = (T::class::companionObjectInstance as FloatingNumberConstants<T>).epsilon
): T? {
    return log(
        x = x,
        base = base,
        constants = T::class::companionObjectInstance as FloatingNumberConstants<T>,
        digits = digits,
        precision = precision
    )
}
