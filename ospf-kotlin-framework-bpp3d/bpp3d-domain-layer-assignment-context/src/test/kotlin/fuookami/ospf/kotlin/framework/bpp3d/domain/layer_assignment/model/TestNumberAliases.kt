package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX

typealias InfraNumber = FltX

fun infraScalar(value: Double): InfraNumber {
    return InfraNumber(value)
}

