package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.*

private tailrec fun <T : TimesSemiGroup<T>> powPosImpl(value: T, base: T, index: Int): T =
    if (index == 1) value * base
    else powPosImpl(value * base, base, index - 1)

private tailrec fun <T : TimesGroup<T>> powNegImpl(value: T, base: T, index: Int): T =
    if (index == -1) value / base
    else powNegImpl(value / base, base, index + 1)

@Throws(IllegalArgumentException::class)
fun <T> pow(base: T, index: Int, constants: RealNumberConstants<T>): T where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(constants.one, base, index)
    } else if (index <= -1) {
        throw IllegalArgumentException("Invalid argument for negative index exponential function: ${base.javaClass}")
    } else {
        constants.one
    }
}

fun <T> pow(base: T, index: Int, constants: RealNumberConstants<T>): T where T : TimesGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(constants.one, base, index)
    } else if (index <= -1) {
        powNegImpl(constants.one, base, index)
    } else {
        constants.one
    }
}

fun <T : FloatingNumber<T>> pow(base: T, index: T, constants: FloatingNumberConstants<T>): T {
    // todo: use taylor formula to replace it
    return base
}
