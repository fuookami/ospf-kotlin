package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object KilogramMeterPerSecond : DerivedPhysicalUnit(KilogramForceMeter / Second) {
    override val name = "kilogram meter per second"
    override val symbol = "kg·m/s"

    override val quantity = Momentum
}
