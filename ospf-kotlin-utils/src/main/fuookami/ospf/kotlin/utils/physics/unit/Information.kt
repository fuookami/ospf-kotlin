package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Byte : PhysicalUnit() {
    override val name = "byte"
    override val symbol = "byte"

    override val quantity = Information
    override val system = SI
    override val scale = Scale()
}

object HalfWord : DerivedPhysicalUnit(Byte * 2) {
    override val name = "half word"
    override val symbol = "hword"

    override val quantity = Information
}

object Word : DerivedPhysicalUnit(Byte * 4) {
    override val name = "word"
    override val symbol = "word"

    override val quantity = Information
}

object Kilobyte : DerivedPhysicalUnit(Byte * Scale(2, 10)) {
    override val name = "kilobyte"
    override val symbol = "kb"

    override val quantity = Information
}

object Megabyte : DerivedPhysicalUnit(Kilobyte * Scale(2, 10)) {
    override val name = "megabyte"
    override val symbol = "Mb"

    override val quantity = Information
}

object Gigabyte : DerivedPhysicalUnit(Megabyte * Scale(2, 10)) {
    override val name = "gigabyte"
    override val symbol = "gb"

    override val quantity = Information
}

object Terabyte : DerivedPhysicalUnit(Gigabyte * Scale(2, 10)) {
    override val name = "terabyte"
    override val symbol = "tb"

    override val quantity = Information
}

object Petabyte : DerivedPhysicalUnit(Terabyte * Scale(2, 10)) {
    override val name = "petabyte"
    override val symbol = "pb"

    override val quantity = Information
}

object Exabyte : DerivedPhysicalUnit(Petabyte * Scale(2, 10)) {
    override val name = "exabyte"
    override val symbol = "eb"

    override val quantity = Information
}

object Bit : DerivedPhysicalUnit(Byte * Scale(2, -3)) {
    override val name = "bit"
    override val symbol = "bit"

    override val quantity = Information
}

object Kilobit : DerivedPhysicalUnit(Bit * Scale(2, 10)) {
    override val name = "kilobit"
    override val symbol = "kbit"

    override val quantity = Information
}

object Megabit : DerivedPhysicalUnit(Kilobit * Scale(2, 10)) {
    override val name = "megabit"
    override val symbol = "mbit"

    override val quantity = Information
}

object Gigabit : DerivedPhysicalUnit(Megabit * Scale(2, 10)) {
    override val name = "gigabit"
    override val symbol = "gbit"

    override val quantity = Information
}

object Terabit : DerivedPhysicalUnit(Gigabit * Scale(2, 10)) {
    override val name = "terabit"
    override val symbol = "tbit"

    override val quantity = Information
}

object Petabit : DerivedPhysicalUnit(Terabit * Scale(2, 10)) {
    override val name = "petabit"
    override val symbol = "pbit"

    override val quantity = Information
}

object Exabit : DerivedPhysicalUnit(Petabit * Scale(2, 10)) {
    override val name = "exabit"
    override val symbol = "ebit"

    override val quantity = Information
}
