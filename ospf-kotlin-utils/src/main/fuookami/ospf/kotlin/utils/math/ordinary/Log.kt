package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.*

fun <T : FloatingNumber<T>> ln(x: T, constants: FloatingNumberConstants<T>): T? {
    return if (x leq constants.zero) {
        constants.nan
    } else {
        var value = constants.zero
        var xp = x.copy()
        if (xp ls constants.one) {
            while (xp leq constants.e) {
                xp *= constants.e
                value -= constants.one
            }
        } else if (xp gr constants.one) {
            while (xp geq constants.e) {
                xp /= constants.e
                value += constants.one
            }
        }
        var base = xp - constants.one
        var signed = constants.one
        var i = constants.one
        while (true) {
            val thisItem = signed * base / i
            value += thisItem
            base *= xp - constants.one
            signed = -signed
            i += constants.one

            if (thisItem leq constants.epsilon) {
                break
            }
        }
        value
    }
}

fun <T : FloatingNumber<T>> log(x: T, base: T, constants: FloatingNumberConstants<T>): T? {
    return ln(x, constants)?.let { lhs ->
        ln(base, constants)?.let {
            lhs / it
        }
    } ?: constants.nan
}
