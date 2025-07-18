package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Second : PhysicalUnit() {
    override val name: String = "second"
    override val symbol: String = "s"

    override val quantity = Time
    override val system = SI
    override val scale: Scale = Scale()
}

object Nanosecond : DerivedPhysicalUnit(Second * Scale.nano) {
    override val name: String = "nanosecond"
    override val symbol: String = "ns"

    override val quantity = Time
}

object Microsecond : DerivedPhysicalUnit(Second * Scale.micro) {
    override val name: String = "microsecond"
    override val symbol: String = "μs"

    override val quantity = Time
}

object Millisecond : DerivedPhysicalUnit(Second * Scale.milli) {
    override val name: String = "millisecond"
    override val symbol: String = "ms"

    override val quantity = Time
}

object Minute : DerivedPhysicalUnit(Second * 60) {
    override val name: String = "minute"
    override val symbol: String = "min"

    override val quantity = Time
}

object Hour : DerivedPhysicalUnit(Minute * 60) {
    override val name: String = "hour"
    override val symbol: String = "h"

    override val quantity = Time
}

object Day : DerivedPhysicalUnit(Hour * 24) {
    override val name: String = "day"
    override val symbol: String = "d"

    override val quantity = Time
}

object Week : DerivedPhysicalUnit(Day * 7) {
    override val name: String = "week"
    override val symbol: String = "wk"

    override val quantity = Time
}

object Year : DerivedPhysicalUnit(Day * 365.25) {
    override val name: String = "year"
    override val symbol: String = "yr"

    override val quantity = Time
}
