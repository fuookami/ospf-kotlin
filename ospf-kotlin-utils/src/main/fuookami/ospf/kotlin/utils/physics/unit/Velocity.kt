package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object CentimeterPerSecond : DerivedPhysicalUnit(Centimeter / Second) {
    override val name = "centimeter per second"
    override val symbol = "cmps"

    override val quantity = Velocity
}

object MeterPerSecond : DerivedPhysicalUnit(Meter / Second) {
    override val name = "meter per second"
    override val symbol = "mps"

    override val quantity = Velocity
}

object KilometersPerSecond : DerivedPhysicalUnit(Kilometer / Second) {
    override val name = "kilometers per second"
    override val symbol = "kmps"

    override val quantity = Velocity
}

object KilometerPerHour : DerivedPhysicalUnit(Kilometer / Hour) {
    override val name = "kilometers per hour"
    override val symbol = "kmph"

    override val quantity = Velocity
}

object Mach : DerivedPhysicalUnit(MeterPerSecond * 340.3) {
    override val name = "mach"
    override val symbol = "ma"

    override val quantity = Velocity
}

object Knot : DerivedPhysicalUnit(NauticalMile / Hour) {
    override val name = "knot"
    override val symbol = "kn"

    override val quantity = Velocity
}

object FRKnot : DerivedPhysicalUnit(FRNauticalMile / Hour) {
    override val name = "fr knot"
    override val symbol = "fr.kn"

    override val quantity = Velocity
}

object UKKnot: DerivedPhysicalUnit(UKNauticalMile / Hour) {
    override val name = "uk knot"
    override val symbol = "uk.kn"

    override val quantity = Velocity
}

object RUKnot: DerivedPhysicalUnit(RUNauticalMile / Hour) {
    override val name = "ru knot"
    override val symbol = "ru.kn"

    override val quantity = Velocity
}

object USKnot: DerivedPhysicalUnit(USNauticalMile / Hour) {
    override val name = "us knot"
    override val symbol = "us.kn"

    override val quantity = Velocity
}

object LightSpeed : DerivedPhysicalUnit(LightSecond / Second) {
    override val name = "lightspeed"
    override val symbol = "c"

    override val quantity = Velocity
}

object InchPerSecond : DerivedPhysicalUnit(Inch / Second) {
    override val name = "inch per second"
    override val symbol = "ips"

    override val quantity = Velocity
}

object MilePerHour : DerivedPhysicalUnit(Mile / Hour) {
    override val name = "mile per hour"
    override val symbol = "mph"

    override val quantity = Velocity
}
