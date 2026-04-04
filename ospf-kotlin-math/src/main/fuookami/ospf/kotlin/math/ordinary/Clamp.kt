package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.utils.functional.Ord

fun <T : Ord<T>> clamp(v: T, min: T, max: T): T = if (v < min) min else if (v > max) max else v




