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

    override val quantity = Length
}

object Nanometer : DerivedPhysicalUnit(Meter * Scale.nano) {
    override val name = "nanometer"
    override val symbol = "nm"

    override val quantity = Length
}

object Micrometer : DerivedPhysicalUnit(Meter * Scale.micro) {
    override val name = "micrometer"
    override val symbol = "µm"

    override val quantity = Length
}

object Millimeter : DerivedPhysicalUnit(Meter * Scale.milli) {
    override val name = "millimeter"
    override val symbol = "mm"

    override val quantity = Length
}

object Centimeter : DerivedPhysicalUnit(Meter * Scale.centi) {
    override val name = "centimeter"
    override val symbol = "cm"

    override val quantity = Length
}

object Decimeter : DerivedPhysicalUnit(Meter * Scale.deci) {
    override val name = "decimeter"
    override val symbol = "dm"

    override val quantity = Length
}

object Hectometer : DerivedPhysicalUnit(Meter * Scale.hecto) {
    override val name = "hectometer"
    override val symbol = "hm"

    override val quantity = Length
}

object Kilometer : DerivedPhysicalUnit(Meter * Scale.kilo) {
    override val name = "kilometer"
    override val symbol = "km"

    override val quantity = Length
}

object NauticalMile : DerivedPhysicalUnit(Kilometer * 1.852) {
    override val name = "nautical mile"
    override val symbol = "nmi"

    override val quantity = Length
}

object FRNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85327) {
    override val name = "fra nautical mile"
    override val symbol = "fr.nmi"

    override val quantity = Length
}

object UKNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85455) {
    override val name = "uk nautical mile"
    override val symbol = "uk.nmi"

    override val quantity = Length
}

object RUNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85578) {
    override val name = "rus nautical mile"
    override val symbol = "ru.nmi"

    override val quantity = Length
}

object USNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85101) {
    override val name = "usa nautical mile"
    override val symbol = "us.nmi"

    override val quantity = Length
}

object Fathom : DerivedPhysicalUnit(NauticalMile * Scale(10, -3)) {
    override val name = "fathom"
    override val symbol = "fm"

    override val quantity = Length
}

object Cable : DerivedPhysicalUnit(NauticalMile * Scale(10, -1)) {
    override val name = "cable"
    override val symbol = "cab"

    override val quantity = Length
}

object Inch : DerivedPhysicalUnit(Centimeter * 2.54) {
    override val name = "inch"
    override val symbol = "in"

    override val quantity = Length
}

object Foot : DerivedPhysicalUnit(Inch * 12) {
    override val name = "foot"
    override val symbol = "ft"

    override val quantity = Length
}

object Yard : DerivedPhysicalUnit(Foot * 3) {
    override val name = "yard"
    override val symbol = "yd"

    override val quantity = Length
}

object Chain : DerivedPhysicalUnit(Yard * 22) {
    override val name = "chain"
    override val symbol = "ch"

    override val quantity = Length
}

object Rod : DerivedPhysicalUnit(Inch * 198.838) {
    override val name = "rod"
    override val symbol = "rd"

    override val quantity = Length
}

object Mile : DerivedPhysicalUnit(Yard * 1760) {
    override val name = "mile"
    override val symbol = "mi"

    override val quantity = Length
}

object AstronomicalUnit : DerivedPhysicalUnit(Meter * 149597870700.0) {
    override val name = "astronomical unit"
    override val symbol = "au"

    override val quantity = Length
}

object LightSecond : DerivedPhysicalUnit(Meter * 299792458) {
    override val name = "light second"
    override val symbol = "lsc"

    override val quantity = Length
}

object LightMinute : DerivedPhysicalUnit(LightSecond * 60) {
    override val name = "light minute"
    override val symbol = "lmn"

    override val quantity = Length
}

object LightHour : DerivedPhysicalUnit(LightMinute * 60) {
    override val name = "light hour"
    override val symbol = "lhr"

    override val quantity = Length
}

object LightDay : DerivedPhysicalUnit(LightHour * 24) {
    override val name = "light day"
    override val symbol = "ldy"

    override val quantity = Length
}

object LightYear : DerivedPhysicalUnit(LightDay * 365.25) {
    override val name = "light year"
    override val symbol = "ly"

    override val quantity = Length
}

object Parsec : DerivedPhysicalUnit(AstronomicalUnit * 20265) {
    override val name = "parsec"
    override val symbol = "pc"

    override val quantity = Length
}

object Kiloparsec : DerivedPhysicalUnit(Parsec * Scale.kilo) {
    override val name = "kiloparsec"
    override val symbol = "kpc"

    override val quantity = Length
}

object Megaparsec : DerivedPhysicalUnit(Parsec * Scale.mega) {
    override val name = "megaparsec"
    override val symbol = "Mpc"

    override val quantity = Length
}

object Gigaparsec : DerivedPhysicalUnit(Parsec * Scale.giga) {
    override val name = "gigaparsec"
    override val symbol = "Gpc"

    override val quantity = Length
}
