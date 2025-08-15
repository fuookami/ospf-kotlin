package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Newton : DerivedPhysicalUnit(Kilogram * MeterPerSquareSecond) {
    override val name = "newton"
    override val symbol = "N"

    override val quantity = Force
}

object KilogramForce : DerivedPhysicalUnit(Newton * 9.80665) {
    override val name = "kilogram force"
    override val symbol = "kgf"

    override val quantity = Force
}

object GramForce : DerivedPhysicalUnit(KilogramForce / Scale.kilo) {
    override val name = "gram force"
    override val symbol = "gf"

    override val quantity = Force
}

object Dyne : DerivedPhysicalUnit(Gram * CentimeterPerSquareSecond) {
    override val name = "dyne"
    override val symbol = "dyn"

    override val quantity = Force
}

object PoundForce : DerivedPhysicalUnit(GramForce * Gram.to(Pound)!!) {
    override val name = "pound force"
    override val symbol = "lbf"

    override val quantity = Force
}

object KilopoundForce : DerivedPhysicalUnit(PoundForce * Scale.kilo) {
    override val name = "kilopound force"
    override val symbol = "klbf"

    override val quantity = Force
}
