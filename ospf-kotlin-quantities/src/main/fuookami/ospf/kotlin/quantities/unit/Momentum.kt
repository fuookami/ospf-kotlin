package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Momentum

object KilogramMeterPerSecond : DerivedPhysicalUnit(Kilogram * Meter / Second) {
    override val name = "kilogram meter per second"
    override val symbol = "kg·m/s"

    override val quantity = Momentum
}
