package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*

data class Liferaft(
    val weight: Quantity<Flt64>,
    val index: Quantity<Flt64>
)

data class Fuselage(
    val liferaft: Liferaft?,
    val dow: Quantity<Flt64>,
    val doi: Quantity<Flt64>,
    val balancedArm: Quantity<Flt64>,
)

