package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Acceleration

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
