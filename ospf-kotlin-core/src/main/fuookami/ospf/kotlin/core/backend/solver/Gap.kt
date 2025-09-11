package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.utils.math.*

fun gap(obj: Flt64, possibleBestObj: Flt64): Flt64 {
    return (obj - possibleBestObj + Flt64.decimalPrecision) / (obj + Flt64.decimalPrecision)
}
