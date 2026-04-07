package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Frequency

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
    override val symbol = "MHz"

    override val quantity = Frequency
}

object Gigahertz : DerivedPhysicalUnit(Hertz * Scale.giga) {
    override val name = "gigahertz"
    override val symbol = "GHz"

    override val quantity = Frequency
}

object CyclePerHour : DerivedPhysicalUnit(Hertz * Second.to(Hour)!!) {
    override val name = "cycle per hour"
    override val symbol = "cph"

    override val quantity = Frequency
}
