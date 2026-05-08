package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Centralized boundary cast functions for solver adapter layer.
 * All unchecked casts from Flt64 to V are consolidated here.
 * When V == Flt64 (the only current instantiation), these are identity casts.
 */
@Suppress("UNCHECKED_CAST")
internal fun <V> flt64ToV(value: Flt64): V where V : RealNumber<V>, V : NumberField<V> {
    return value as V
}

@Suppress("UNCHECKED_CAST")
internal fun <V> flt64ToV(value: Flt64, zero: V): V where V : RealNumber<V>, V : NumberField<V> {
    return value as V
}
