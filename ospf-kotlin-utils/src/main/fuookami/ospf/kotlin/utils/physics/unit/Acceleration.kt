package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object MeterPerSquareSecond : DerivedPhysicalUnit(MeterPerSecond / Second) {
    override val name = "meter per square second"
    override val symbol = "mps2"

    override val quantity = Acceleration
}

object CentimeterPerSquareSecond : DerivedPhysicalUnit(CentimeterPerSecond / Second) {
    override val name = "centimeter per square second"
    override val symbol = "cmps2"

    override val quantity = Acceleration
}
