package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Torque

object NewtonMeter : DerivedPhysicalUnit(Newton * Meter) {
    override val symbol = "newton meter"
    override val name = "N·m"

    override val quantity = Torque
}

object KilogramForceMeter : DerivedPhysicalUnit(KilogramForce * Meter) {
    override val symbol = "kilogram meter"
    override val name = "kgf·m"

    override val quantity = Torque
}
