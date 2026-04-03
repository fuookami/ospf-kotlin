package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Resistance

object Ohm : DerivedPhysicalUnit(Volt / Ampere) {
    override val name = "ohm"
    override val symbol = "Ω"

    override val quantity = Resistance
}

object Kiloohm : DerivedPhysicalUnit(Ohm * Scale.kilo) {
    override val name = "kiloohm"
    override val symbol = "kΩ"

    override val quantity = Resistance
}
