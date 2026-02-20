package fuookami.ospf.kotlin.utils.math.ordinary

import java.math.*
import fuookami.ospf.kotlin.utils.math.*
import kotlin.reflect.full.companionObjectInstance

private tailrec fun <T : TimesSemiGroup<T>> powPosImpl(
    value: T,
    base: T,
    index: Int,
    digits: Int,
    precision: T
): T {
    return if (index == 1) {
        value * base
    } else if (index % 2 == 0) {
        powPosImpl(
            value = value,
            base = base * base,
            index = index / 2,
            digits = digits,
            precision = precision
        )
    } else {
        powPosImpl(
            value = value * base,
            base = base,
            index = index - 1,
            digits = digits,
            precision = precision
        )
    }
}

private tailrec fun <T : TimesGroup<T>> powNegImpl(
    value: T,
    base: T,
    index: Int,
    digits: Int,
    precision: T
): T {
    return if (index == -1) {
        value / base
    } else if (index % 2 == 0) {
        powNegImpl(
            value = value,
            base = base * base,
            index = index / 2,
            digits = digits,
            precision = precision
        )
    } else {
        powNegImpl(
            value = value / base,
            base = base,
            index = index + 1,
            digits = digits,
            precision = precision
        )
    }
}


@Throws(IllegalArgumentException::class)
fun <T> pow(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else if (index <= -1) {
        throw IllegalArgumentException("Invalid argument for negative index exponential function: ${base.javaClass}")
    } else {
        constants.one
    }
}

fun <T> pow(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T where T : TimesGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else if (index <= -1) {
        powNegImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else {
        constants.one
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> pow(
    base: T,
    index: Int,
    digits: Int = (T::class::companionObjectInstance as RealNumberConstants<T>).decimalDigits ?: 0,
    precision: T = (T::class::companionObjectInstance as RealNumberConstants<T>).epsilon
): T where T : TimesGroup<T>, T : RealNumber<T> {
    return pow(
        base = base,
        index = index,
        constants = T::class::companionObjectInstance as RealNumberConstants<T>,
        digits = digits,
        precision = precision
    )
}

fun <T : FloatingNumber<T>> powf(
    base: T,
    index: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T {
    val lnBase = ln(
        x = base,
        constants = constants,
        digits = digits,
        precision = precision
    )!!
    return exp(
        index = index * lnBase,
        constants = constants,
        digits = digits,
        precision = precision
    )
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> powf(
    base: T,
    index: T,
    digits: Int = (T::class::companionObjectInstance as RealNumberConstants<T>).decimalDigits ?: 0,
    precision: T = (T::class::companionObjectInstance as RealNumberConstants<T>).epsilon
): T {
    return powf(
        base = base,
        index = index,
        constants = T::class::companionObjectInstance as FloatingNumberConstants<T>,
        digits = digits,
        precision = precision
    )
}

@Suppress("UNCHECKED_CAST")
fun <T : FloatingNumber<T>> exp(
    index: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T {
    var value  = constants.one
    var term = constants.one
    var i = constants.one
    while (true) {
        var thisItem = (term * index) / i
        if (thisItem is FltX) {
            thisItem = thisItem.withScale(digits, RoundingMode.HALF_UP) as T
        }
        value += thisItem
        i += constants.one

        if (thisItem.abs() leq precision) {
            break
        }
        term = thisItem
    }
    return value
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> exp(
    index: T,
    digits: Int = (T::class::companionObjectInstance as RealNumberConstants<T>).decimalDigits ?: 0,
    precision: T = (T::class::companionObjectInstance as RealNumberConstants<T>).epsilon
): T {
    return exp(
        index = index,
        constants = T::class::companionObjectInstance as FloatingNumberConstants<T>,
        digits = digits,
        precision = precision
    )
}