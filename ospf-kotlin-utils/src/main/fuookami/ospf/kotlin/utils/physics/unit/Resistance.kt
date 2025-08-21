package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

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
