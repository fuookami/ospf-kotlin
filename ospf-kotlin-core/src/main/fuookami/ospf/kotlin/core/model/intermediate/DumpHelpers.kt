package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.number.Flt64

internal fun Flt64.clampCoefficient(): Flt64 {
    val threshold = Flt64.decimalPrecision.reciprocal()
    return when {
        isInfinity() || this geq threshold -> threshold
        isNegativeInfinity() || this leq -threshold -> -threshold
        else -> this
    }
}