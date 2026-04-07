package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Torque

object NewtonMeter : DerivedPhysicalUnit(Newton * Meter) {
    override val name = "newton meter"
    override val symbol = "N·m"

    override val quantity = Torque
}

object KilogramForceMeter : DerivedPhysicalUnit(KilogramForce * Meter) {
    override val name = "kilogram meter"
    override val symbol = "kgf·m"

    override val quantity = Torque
}
