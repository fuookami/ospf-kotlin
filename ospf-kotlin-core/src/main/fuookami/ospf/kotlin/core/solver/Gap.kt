package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.math.algebra.number.Flt64

fun gap(obj: Flt64, possibleBestObj: Flt64): Flt64 {
    return (obj - possibleBestObj + Flt64.decimalPrecision) / (obj + Flt64.decimalPrecision)
}


