package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Watt : DerivedPhysicalUnit(Newton * MeterPerSecond) {
    override val name = "watt"
    override val symbol = "W"

    override val quantity = Power
}

object Kilowatt : DerivedPhysicalUnit(Watt * Scale.kilo) {
    override val name = "kilowatt"
    override val symbol = "kW"

    override val quantity = Power
}

object JoulePerSecond : DerivedPhysicalUnit(Watt) {
    override val name = "Joule per second"
    override val symbol = "J/s"

    override val quantity = Power
}

object NewtonMeterPerSecond : DerivedPhysicalUnit(Watt) {
    override val name = "Newton meter per second"
    override val symbol = "Nm/s"

    override val quantity = Power
}

object Horsepower : DerivedPhysicalUnit(Watt * 735) {
    override val name = "horsepower"
    override val symbol = "ps"

    override val quantity = Power
}

object UKHorsepower : DerivedPhysicalUnit(Watt * 550) {
    override val name = "uk horsepower"
    override val symbol = "uk.ps"

    override val quantity = Power
}
