package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.operator.*

fun <T : Ord<T>> clamp(v: T, min: T, max: T): T = if (v < min) min else if (v > max) max else v
