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

object Picometer : PhysicalUnit() {
    private val unit = Meter * Scale.pico

    override val name = "picometer"
    override val symbol = "pm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Nanometer : PhysicalUnit() {
    private val unit = Meter * Scale.nano

    override val name = "nanometer"
    override val symbol = "nm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Micrometer : PhysicalUnit() {
    private val unit = Meter * Scale.micro

    override val name = "micrometer"
    override val symbol = "µm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Millimeter : PhysicalUnit() {
    private val unit = Meter * Scale.milli

    override val name = "millimeter"
    override val symbol = "mm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Centimeter : PhysicalUnit() {
    private val unit = Meter * Scale.centi

    override val name = "centimeter"
    override val symbol = "cm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Decimeter : PhysicalUnit() {
    private val unit = Meter * Scale.deci

    override val name = "decimeter"
    override val symbol = "dm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Hectometer : PhysicalUnit() {
    private val unit = Meter * Scale.hecto

    override val name = "hectometer"
    override val symbol = "hm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Kilometer : PhysicalUnit() {
    private val unit = Meter * Scale.kilo

    override val name = "kilometer"
    override val symbol = "km"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object NauticalMile : PhysicalUnit() {
    private val unit = Kilometer * Scale(1.852, 1)

    override val name = "nautical mile"
    override val symbol = "nmi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object FRANauticalMile : PhysicalUnit() {
    private val unit = Meter * Scale(1.85327, 1)

    override val name = "fra nautical mile"
    override val symbol = "fra.nmi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object GBRNauticalMile : PhysicalUnit() {
    private val unit = Meter * Scale(1.85455, 1)

    override val name = "gbr nautical mile"
    override val symbol = "gbr.nmi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object RUSNauticalMile : PhysicalUnit() {
    private val unit = Meter * Scale(1.85578, 1)

    override val name = "rus nautical mile"
    override val symbol = "rus.nmi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object USANauticalMile : PhysicalUnit() {
    private val unit = Meter * Scale(1.85101, 2)

    override val name = "usa nautical mile"
    override val symbol = "usa.nmi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Fathom : PhysicalUnit() {
    private val unit = NauticalMile * Scale(10, -3)

    override val name = "fathom"
    override val symbol = "fm"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Cable : PhysicalUnit() {
    private val unit = NauticalMile * Scale(10, -1)

    override val name = "cable"
    override val symbol = "cab"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Inch : PhysicalUnit() {
    private val unit = Centimeter * 2.54

    override val name = "inch"
    override val symbol = "in"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Foot : PhysicalUnit() {
    private val unit = Inch * 12

    override val name = "foot"
    override val symbol = "ft"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Yard : PhysicalUnit() {
    private val unit = Foot * 3

    override val name = "yard"
    override val symbol = "yd"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Mile : PhysicalUnit() {
    private val unit = Yard * 1760

    override val name = "mile"
    override val symbol = "mi"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object AstronomicalUnit : PhysicalUnit() {
    private val unit = Meter * 149597870700.0

    override val name = "astronomical unit"
    override val symbol = "au"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object LightSecond : PhysicalUnit() {
    private val unit = Meter * 299792458

    override val name = "light second"
    override val symbol = "lsc"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object LightMinute : PhysicalUnit() {
    private val unit = LightSecond * 60

    override val name = "light minute"
    override val symbol = "lmn"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object LightHour : PhysicalUnit() {
    private val unit = LightMinute * 60

    override val name = "light hour"
    override val symbol = "lhr"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object LightDay : PhysicalUnit() {
    private val unit = LightHour * 24

    override val name = "light day"
    override val symbol = "ldy"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object LightYear : PhysicalUnit() {
    private val unit = LightDay * 365.25

    override val name = "light year"
    override val symbol = "ly"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Parsec : PhysicalUnit() {
    private val unit = AstronomicalUnit * 20265

    override val name = "parsec"
    override val symbol = "pc"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Kiloparsec : PhysicalUnit() {
    private val unit = Parsec * Scale.kilo

    override val name = "kiloparsec"
    override val symbol = "kpc"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Megaparsec : PhysicalUnit() {
    private val unit = Parsec * Scale.mega

    override val name = "megaparsec"
    override val symbol = "Mpc"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Gigaparsec : PhysicalUnit() {
    private val unit = Parsec * Scale.giga

    override val name = "gigaparsec"
    override val symbol = "Gpc"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}
