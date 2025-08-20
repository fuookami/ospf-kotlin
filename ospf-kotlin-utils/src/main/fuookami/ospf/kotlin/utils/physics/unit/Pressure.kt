package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Pascal : DerivedPhysicalUnit(Newton / SquareMeter) {
    override val name = "pascal"
    override val symbol = "Pa"

    override val quantity = Pressure
}

object Hectopascal : DerivedPhysicalUnit(Pascal * Scale.hecto) {
    override val name = "hectopascal"
    override val symbol = "hPa"

    override val quantity = Pressure
}

object Kilopascal : DerivedPhysicalUnit(Pascal * Scale.kilo) {
    override val name = "kilopascal"
    override val symbol = "kPa"

    override val quantity = Pressure
}

object Megapascal : DerivedPhysicalUnit(Pascal * Scale.mega) {
    override val name = "megapascal"
    override val symbol = "mPa"

    override val quantity = Pressure
}

object StandardAtmosphericPressure : DerivedPhysicalUnit(Pascal * 101325) {
    override val name = "standard atmospheric pressure"
    override val symbol = "atm"

    override val quantity = Pressure
}

object MeterMercury : DerivedPhysicalUnit(StandardAtmosphericPressure * 0.76) {
    override val name = "meter mercury"
    override val symbol = "mHg"

    override val quantity = Pressure
}

object MillimeterMercury : DerivedPhysicalUnit(MeterMercury * Scale.milli) {
    override val name = "millimeter mercury"
    override val symbol = "mmHg"

    override val quantity = Pressure
}

object InchOfMercury : DerivedPhysicalUnit(MeterMercury / Meter.to(Inch)!!) {
    override val name = "inch of mercury"
    override val symbol = "inHg"

    override val quantity = Pressure
}

object Bar : DerivedPhysicalUnit(Pascal * Scale(10, 5)) {
    override val name = "bar"
    override val symbol = "bar"

    override val quantity = Pressure
}

object Millibar : DerivedPhysicalUnit(Bar * Scale.milli) {
    override val name = "millibar"
    override val symbol = "mbar"

    override val quantity = Pressure
}
