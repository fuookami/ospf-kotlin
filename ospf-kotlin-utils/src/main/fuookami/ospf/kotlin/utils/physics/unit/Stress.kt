package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object PoundForcePerSquareInch : DerivedPhysicalUnit(PoundForce / SquareInch) {
    override val name = "pound force per square inch"
    override val symbol = "psi"

    override val quantity = Stress
}

object PoundForcePerSquareFoot : DerivedPhysicalUnit(PoundForce / SquareFoot) {
    override val name = "pound force per square foot"
    override val symbol = "psf"

    override val quantity = Stress
}

object KilogramForcePerSquareCentimeter : DerivedPhysicalUnit(KilogramForce / SquareCentimeter) {
    override val name = "kilogram force per square centimeter"
    override val symbol = "kgf/cm2"

    override val quantity = Stress
}

object KilogramForcePerSquareMeter : DerivedPhysicalUnit(KilogramForce / SquareMeter) {
    override val name = "kilogram force per square meter"
    override val symbol = "kgf/m2"

    override val quantity = Stress
}
