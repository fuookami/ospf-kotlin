package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object SquareMillimeter : DerivedPhysicalUnit(Millimeter * Millimeter) {
    override val name = "square millimeter"
    override val symbol = "mm2"

    override val quantity = Area
}

object SquareCentimeter : DerivedPhysicalUnit(Centimeter * Centimeter) {
    override val name = "square centimeter"
    override val symbol = "cm2"

    override val quantity = Area
}

object SquareDecimeter : DerivedPhysicalUnit(Decimeter * Decimeter) {
    override val name = "square decimeter"
    override val symbol = "dm2"

    override val quantity = Area
}

object SquareMeter : DerivedPhysicalUnit(Meter * Meter) {
    override val name = "square meter"
    override val symbol = "m2"

    override val quantity = Area
}

object SquareKilometer : DerivedPhysicalUnit(Kilometer * Kilometer) {
    override val name = "square kilometer"
    override val symbol = "km2"

    override val quantity = Area
}

object Are : DerivedPhysicalUnit(Decimeter * Decimeter) {
    override val name = "are"
    override val symbol = "are"

    override val quantity = Area
}

object Hectare : DerivedPhysicalUnit(Hectometer * Hectometer) {
    override val name = "hectare"
    override val symbol = "ha"

    override val quantity = Area
}

object SquareInch : DerivedPhysicalUnit(Inch * Inch) {
    override val name = "square inch"
    override val symbol = "sq.in"

    override val quantity = Area
}

object SquareFoot : DerivedPhysicalUnit(Foot * Foot) {
    override val name = "square foot"
    override val symbol = "sq.ft"

    override val quantity = Area
}

object SquareYard : DerivedPhysicalUnit(Yard * Yard) {
    override val name = "square yard"
    override val symbol = "sq.yd"

    override val quantity = Area
}

object SquareChain : DerivedPhysicalUnit(Chain * Chain) {
    override val name = "square chain"
    override val symbol = "sq.ch"

    override val quantity = Area
}

object SquareRod : DerivedPhysicalUnit(Rod * Rod) {
    override val name = "square rod"
    override val symbol = "sq.rd"

    override val quantity = Area
}

object SquareMile : DerivedPhysicalUnit(Mile * Mile) {
    override val name = "square mile"
    override val symbol = "sq.mi"

    override val quantity = Area
}

object Acre : DerivedPhysicalUnit(SquareChain * 10) {
    override val name = "acre"
    override val symbol = "acre"

    override val quantity = Area
}
