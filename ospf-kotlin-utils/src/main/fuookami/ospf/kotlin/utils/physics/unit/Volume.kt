package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object CubicMillimeter : DerivedPhysicalUnit(SquareMillimeter * Millimeter) {
    override val name = "cubic millimeter"
    override val symbol = "mm3"

    override val quantity = Volume
}

object CubicCentimeter : DerivedPhysicalUnit(SquareCentimeter * Centimeter) {
    override val name = "cubic centimeter"
    override val symbol = "cm3"

    override val quantity = Volume
}

object CubicDecimeter : DerivedPhysicalUnit(SquareDecimeter * Decimeter) {
    override val name = "cubic decimeter"
    override val symbol = "dm3"

    override val quantity = Volume
}

object CubicMeter : DerivedPhysicalUnit(SquareMeter * Meter) {
    override val name = "cubic meter"
    override val symbol = "m3"

    override val quantity = Volume
}

object Liter : DerivedPhysicalUnit(CubicDecimeter) {
    override val name = "liter"
    override val symbol = "L"

    override val quantity = Volume
}

object Microliter : DerivedPhysicalUnit(Liter * Scale.micro) {
    override val name = "microliter"
    override val symbol = "μL"

    override val quantity = Volume
}

object Milliliter : DerivedPhysicalUnit(Liter * Scale.milli) {
    override val name = "milliliter"
    override val symbol = "mL"

    override val quantity = Volume
}

object Centiliter : DerivedPhysicalUnit(Liter * Scale.centi) {
    override val name = "centiliter"
    override val symbol = "cL"

    override val quantity = Volume
}

object Deciliter : DerivedPhysicalUnit(Liter * Scale.deci) {
    override val name = "deciliter"
    override val symbol = "dL"

    override val quantity = Volume
}

object Hectoliter : DerivedPhysicalUnit(Liter * Scale.hecto) {
    override val name = "hectoliter"
    override val symbol = "hL"

    override val quantity = Volume
}

object CubicInch : DerivedPhysicalUnit(SquareInch * Inch) {
    override val name = "cubic inch"
    override val symbol = "cu.in"

    override val quantity = Volume
}

object CubicFoot : DerivedPhysicalUnit(SquareFoot * Foot) {
    override val name = "cubic foot"
    override val symbol = "cu.ft"

    override val quantity = Volume
}

object CubicYard : DerivedPhysicalUnit(SquareFoot * Foot) {
    override val name = "cubic yard"
    override val symbol = "cu.yd"

    override val quantity = Volume
}

object UKFluidOunce : DerivedPhysicalUnit(Millimeter * 28.4130625) {
    override val name = "uk fluid ounce"
    override val symbol = "uk.fl.oz"

    override val quantity = Volume
}

object USFluidOunce : DerivedPhysicalUnit(Millimeter * 29.5735295625) {
    override val name = "us fluid ounce"
    override val symbol = "us.fl.oz"

    override val quantity = Volume
}

object UKGallon : DerivedPhysicalUnit(UKFluidOunce * 160) {
    override val name = "uk gallon"
    override val symbol = "uk.gal"

    override val quantity = Volume
}

object USGallon : DerivedPhysicalUnit(USFluidOunce * 128) {
    override val name = "us gallon"
    override val symbol = "us.gal"

    override val quantity = Volume
}
