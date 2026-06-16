package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.*

/** An emergency liferaft with its weight and balance index. */
data class Liferaft(
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
)

/** Aircraft fuselage properties including dry operating weight, index, and balanced arm. */
data class Fuselage(
    val liferaft: Liferaft?,
    val dow: Quantity<Flt64>,
    val doi: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>,
)
