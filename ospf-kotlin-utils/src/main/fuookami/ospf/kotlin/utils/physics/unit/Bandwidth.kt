package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object BitPerSecond : DerivedPhysicalUnit(Bit / Second) {
    override val name = "bit per second"
    override val symbol = "bit/s"

    override val quantity = Bandwidth
}

object KilobitPerSecond : DerivedPhysicalUnit(Kilobit / Second) {
    override val name = "kilobit"
    override val symbol = "kbit/s"

    override val quantity = Bandwidth
}

object MegabitPerSecond : DerivedPhysicalUnit(Megabit / Second) {
    override val name = "megabit per second"
    override val symbol = "mbit/s"

    override val quantity = Bandwidth
}

object GigabitPerSecond : DerivedPhysicalUnit(Gigabit / Second) {
    override val name = "gigabit per second"
    override val symbol = "gbit/s"

    override val quantity = Bandwidth
}

object TerabitPerSecond : DerivedPhysicalUnit(Terabit / Second) {
    override val name = "terabit"
    override val symbol = "tbit/s"

    override val quantity = Bandwidth
}

object PetabitPerSecond : DerivedPhysicalUnit(Petabit / Second) {
    override val name = "petabit per second"
    override val symbol = "pbit/s"

    override val quantity = Bandwidth
}

object ExabitPerSecond : DerivedPhysicalUnit(Exabit / Second) {
    override val name = "exabit per second"
    override val symbol = "ebit/s"

    override val quantity = Bandwidth
}
