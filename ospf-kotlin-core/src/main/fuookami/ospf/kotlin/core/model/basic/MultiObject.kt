package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class MultiObjectLocation<V>(
    val priority: UInt64,
    val weight: V
) where V : RealNumber<V>, V : NumberField<V>