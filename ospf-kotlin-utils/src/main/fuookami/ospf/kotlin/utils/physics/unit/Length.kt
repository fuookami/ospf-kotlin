package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Meter : PhysicalUnit() {
    override val name = "meter"
    override val symbol = "m"

    override val quantity = Length
    override val system = SI
    override val scale = Scale()
}

object Picometer : DerivedPhysicalUnit(Meter * Scale.pico) {
    override val name = "picometer"
    override val symbol = "pm"
}

object Nanometer : DerivedPhysicalUnit(Meter * Scale.nano) {
    override val name = "nanometer"
    override val symbol = "nm"
}

object Micrometer : DerivedPhysicalUnit(Meter * Scale.micro) {
    override val name = "micrometer"
    override val symbol = "µm"
}

object Millimeter : DerivedPhysicalUnit(Meter * Scale.milli) {
    override val name = "millimeter"
    override val symbol = "mm"
}

object Centimeter : DerivedPhysicalUnit(Meter * Scale.centi) {
    override val name = "centimeter"
    override val symbol = "cm"
}

object Decimeter : DerivedPhysicalUnit(Meter * Scale.deci) {
    override val name = "decimeter"
    override val symbol = "dm"
}

object Hectometer : DerivedPhysicalUnit(Meter * Scale.hecto) {
    override val name = "hectometer"
    override val symbol = "hm"
}

object Kilometer : DerivedPhysicalUnit(Meter * Scale.kilo) {
    override val name = "kilometer"
    override val symbol = "km"
}

object NauticalMile : DerivedPhysicalUnit(Kilometer * 1.852) {
    override val name = "nautical mile"
    override val symbol = "nmi"
}

object FRANauticalMile : DerivedPhysicalUnit(Kilometer * 1.85327) {
    override val name = "fra nautical mile"
    override val symbol = "fra.nmi"
}

object GBRNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85455) {
    override val name = "gbr nautical mile"
    override val symbol = "gbr.nmi"
}

object RUSNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85578) {
    override val name = "rus nautical mile"
    override val symbol = "rus.nmi"
}

object USANauticalMile : DerivedPhysicalUnit(Kilometer * 1.85101) {
    override val name = "usa nautical mile"
    override val symbol = "usa.nmi"
}

object Fathom : DerivedPhysicalUnit(NauticalMile * Scale(10, -3)) {
    override val name = "fathom"
    override val symbol = "fm"
}

object Cable : DerivedPhysicalUnit(NauticalMile * Scale(10, -1)) {
    override val name = "cable"
    override val symbol = "cab"
}

object Inch : DerivedPhysicalUnit(Centimeter * 2.54) {
    override val name = "inch"
    override val symbol = "in"
}

object Foot : DerivedPhysicalUnit(Inch * 12) {
    override val name = "foot"
    override val symbol = "ft"
}

object Yard : DerivedPhysicalUnit(Foot * 3) {
    override val name = "yard"
    override val symbol = "yd"
}

object Mile : DerivedPhysicalUnit(Yard * 1760) {
    override val name = "mile"
    override val symbol = "mi"
}

object AstronomicalUnit : DerivedPhysicalUnit(Meter * 149597870700.0) {
    override val name = "astronomical unit"
    override val symbol = "au"
}

object LightSecond : DerivedPhysicalUnit(Meter * 299792458) {
    override val name = "light second"
    override val symbol = "lsc"
}

object LightMinute : DerivedPhysicalUnit(LightSecond * 60) {
    override val name = "light minute"
    override val symbol = "lmn"
}

object LightHour : DerivedPhysicalUnit(LightMinute * 60) {
    override val name = "light hour"
    override val symbol = "lhr"
}

object LightDay : DerivedPhysicalUnit(LightHour * 24) {
    override val name = "light day"
    override val symbol = "ldy"
}

object LightYear : DerivedPhysicalUnit(LightDay * 365.25) {
    override val name = "light year"
    override val symbol = "ly"
}

object Parsec : DerivedPhysicalUnit(AstronomicalUnit * 20265) {
    override val name = "parsec"
    override val symbol = "pc"
}

object Kiloparsec : DerivedPhysicalUnit(Parsec * Scale.kilo) {
    override val name = "kiloparsec"
    override val symbol = "kpc"
}

object Megaparsec : DerivedPhysicalUnit(Parsec * Scale.mega) {
    override val name = "megaparsec"
    override val symbol = "Mpc"
}

object Gigaparsec : DerivedPhysicalUnit(Parsec * Scale.giga) {
    override val name = "gigaparsec"
    override val symbol = "Gpc"
}
