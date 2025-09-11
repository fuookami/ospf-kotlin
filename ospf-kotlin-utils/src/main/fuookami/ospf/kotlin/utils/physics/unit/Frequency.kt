package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Hertz : DerivedPhysicalUnit(Second.reciprocal()) {
    override val name = "hertz"
    override val symbol = "Hz"

    override val quantity = Frequency
}

object Kilohertz : DerivedPhysicalUnit(Hertz * Scale.kilo) {
    override val name = "kilohertz"
    override val symbol = "kHz"

    override val quantity = Frequency
}

object Megahertz : DerivedPhysicalUnit(Hertz * Scale.mega) {
    override val name = "megahertz"
    override val symbol = "mHz"

    override val quantity = Frequency
}

object Gigahertz : DerivedPhysicalUnit(Hertz * Scale.giga) {
    override val name = "gigahertz"
    override val symbol = "gHz"

    override val quantity = Frequency
}

object CyclePerHour : DerivedPhysicalUnit(Hertz * Second.to(Hour)!!) {
    override val name = "cycle per hour"
    override val symbol = "cph"

    override val quantity = Frequency
}
