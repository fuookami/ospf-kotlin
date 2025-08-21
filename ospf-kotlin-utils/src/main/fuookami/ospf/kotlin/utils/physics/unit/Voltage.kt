package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Volt : DerivedPhysicalUnit(Watt / Ampere) {
    override val name = "volt"
    override val symbol = "V"

    override val quantity = Voltage
}

object Microvolt : DerivedPhysicalUnit(Volt * Scale.micro) {
    override val name = "microvolt"
    override val symbol = "µV"

    override val quantity = Voltage
}

object Millivolt : DerivedPhysicalUnit(Volt * Scale.milli) {
    override val name = "millivolt"
    override val symbol = "mV"

    override val quantity = Voltage
}

object Kilovolt : DerivedPhysicalUnit(Volt * Scale.kilo) {
    override val name = "kilovolt"
    override val symbol = "kV"

    override val quantity = Voltage
}
