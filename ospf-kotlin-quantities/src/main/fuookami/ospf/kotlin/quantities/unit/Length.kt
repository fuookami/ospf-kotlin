/**
 * 长度单位 / Length units
 *
 * 用于测量空间距离的单位。
 * Units for measuring spatial distances.
 *
 * 单位常量来源 / Unit constant sources:
 * - Meter: SI基本单位 / SI base unit
 * - Inch: 1 in = 2.54 cm（精确值，NIST Handbook 44）/ 1 in = 2.54 cm (exact, NIST Handbook 44)
 * - Rod: 1 rd = 5.0292 m（美国测量英尺定义）/ 1 rd = 5.0292 m (US survey foot definition)
 * - NauticalMile: 1 nmi = 1.852 km（精确值，ISO 80000-3）/ 1 nmi = 1.852 km (exact, ISO 80000-3)
 * - AstronomicalUnit: 1 AU = 149597870700 m（精确值，IAU 2012）/ 1 AU = 149597870700 m (exact, IAU 2012)
 * - Parsec: 1 pc = 648000/π AU ≈ 206265 AU（IAU 2015 Resolution B2）/ 1 pc = 648000/π AU ≈ 206265 AU (IAU 2015 Resolution B2)
 * - LightYear: 1 ly = c × 365.25 × 86400 s（儒略年）/ 1 ly = c × 365.25 × 86400 s (Julian year)
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Length

/**
 * 米（基本单位）/ Meter (base unit)
 *
 * 长度的SI基本单位，定义为光在真空中1/299792458秒内行进的距离。
 * The SI base unit of length, defined as the distance light travels in vacuum in 1/299792458 second.
 *
 * 符号 / Symbol: m
 * 换算关系 / Conversion: 1 m = 1 m（基本单位 / base unit）
*/
object Meter : PhysicalUnit() {

    /** 单位名称：meter / Unit name: meter */
    override val name = "meter"

    /** 单位符号：m / Unit symbol: m */
    override val symbol = "m"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length

    /** 比例因子：1（基本单位）/ Scale factor: 1 (base unit) */
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

/**
 * 皮米 / Picometer
 *
 * 万亿分之一米。
 * One trillionth of a meter.
 *
 * 符号 / Symbol: pm
 * 换算关系 / Conversion: 1 pm = 10⁻¹² m
*/
object Picometer : DerivedPhysicalUnit(Meter * Scale.pico) {

    /** 单位名称：picometer / Unit name: picometer */
    override val name = "picometer"

    /** 单位符号：pm / Unit symbol: pm */
    override val symbol = "pm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 纳米 / Nanometer
 *
 * 十亿分之一米。
 * One billionth of a meter.
 *
 * 符号 / Symbol: nm
 * 换算关系 / Conversion: 1 nm = 10⁻⁹ m
*/
object Nanometer : DerivedPhysicalUnit(Meter * Scale.nano) {

    /** 单位名称：nanometer / Unit name: nanometer */
    override val name = "nanometer"

    /** 单位符号：nm / Unit symbol: nm */
    override val symbol = "nm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 微米 / Micrometer
 *
 * 百万分之一米。
 * One millionth of a meter.
 *
 * 符号 / Symbol: µm
 * 换算关系 / Conversion: 1 µm = 10⁻⁶ m
*/
object Micrometer : DerivedPhysicalUnit(Meter * Scale.micro) {

    /** 单位名称：micrometer / Unit name: micrometer */
    override val name = "micrometer"

    /** 单位符号：µm / Unit symbol: µm */
    override val symbol = "µm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 毫米 / Millimeter
 *
 * 千分之一米。
 * One thousandth of a meter.
 *
 * 符号 / Symbol: mm
 * 换算关系 / Conversion: 1 mm = 10⁻³ m
*/
object Millimeter : DerivedPhysicalUnit(Meter * Scale.milli) {

    /** 单位名称：millimeter / Unit name: millimeter */
    override val name = "millimeter"

    /** 单位符号：mm / Unit symbol: mm */
    override val symbol = "mm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 厘米 / Centimeter
 *
 * 百分之一米。
 * One hundredth of a meter.
 *
 * 符号 / Symbol: cm
 * 换算关系 / Conversion: 1 cm = 10⁻² m
*/
object Centimeter : DerivedPhysicalUnit(Meter * Scale.centi) {

    /** 单位名称：centimeter / Unit name: centimeter */
    override val name = "centimeter"

    /** 单位符号：cm / Unit symbol: cm */
    override val symbol = "cm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 分米 / Decimeter
 *
 * 十分之一米。
 * One tenth of a meter.
 *
 * 符号 / Symbol: dm
 * 换算关系 / Conversion: 1 dm = 10⁻¹ m
*/
object Decimeter : DerivedPhysicalUnit(Meter * Scale.deci) {

    /** 单位名称：decimeter / Unit name: decimeter */
    override val name = "decimeter"

    /** 单位符号：dm / Unit symbol: dm */
    override val symbol = "dm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 十米 / Decameter
 *
 * 十米。
 * Ten meters.
 *
 * 符号 / Symbol: dam
 * 换算关系 / Conversion: 1 dam = 10¹ m = 10 m
*/
object Decameter : DerivedPhysicalUnit(Meter * Scale.deca) {

    /** 单位名称：decameter / Unit name: decameter */
    override val name = "decameter"

    /** 单位符号：dam / Unit symbol: dam */
    override val symbol = "dam"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 百米 / Hectometer
 *
 * 一百米。
 * One hundred meters.
 *
 * 符号 / Symbol: hm
 * 换算关系 / Conversion: 1 hm = 10² m = 100 m
*/
object Hectometer : DerivedPhysicalUnit(Meter * Scale.hecto) {

    /** 单位名称：hectometer / Unit name: hectometer */
    override val name = "hectometer"

    /** 单位符号：hm / Unit symbol: hm */
    override val symbol = "hm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 千米 / Kilometer
 *
 * 一千米。
 * One thousand meters.
 *
 * 符号 / Symbol: km
 * 换算关系 / Conversion: 1 km = 10³ m = 1000 m
*/
object Kilometer : DerivedPhysicalUnit(Meter * Scale.kilo) {

    /** 单位名称：kilometer / Unit name: kilometer */
    override val name = "kilometer"

    /** 单位符号：km / Unit symbol: km */
    override val symbol = "km"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 海里 / Nautical Mile
 *
 * 国际海里，用于航海和航空。
 * International nautical mile, used in navigation and aviation.
 *
 * 符号 / Symbol: nmi
 * 换算关系 / Conversion: 1 nmi = 1.852 km（精确值）/ 1 nmi = 1.852 km (exact)
*/
object NauticalMile : DerivedPhysicalUnit(Kilometer * 1.852) {

    /** 单位名称：nautical mile / Unit name: nautical mile */
    override val name = "nautical mile"

    /** 单位符号：nmi / Unit symbol: nmi */
    override val symbol = "nmi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 法式海里 / French Nautical Mile
 *
 * 法国历史上使用的海里单位。
 * Historical French nautical mile unit.
 *
 * 符号 / Symbol: fr.nmi
 * 换算关系 / Conversion: 1 fr.nmi ≈ 1.85327 km
*/
object FRNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85327) {

    /** 单位名称：fra nautical mile / Unit name: fra nautical mile */
    override val name = "fra nautical mile"

    /** 单位符号：fr.nmi / Unit symbol: fr.nmi */
    override val symbol = "fr.nmi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 英式海里 / UK Nautical Mile
 *
 * 英国历史上使用的海里单位。
 * Historical UK nautical mile unit.
 *
 * 符号 / Symbol: uk.nmi
 * 换算关系 / Conversion: 1 uk.nmi ≈ 1.85455 km
*/
object UKNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85455) {

    /** 单位名称：uk nautical mile / Unit name: uk nautical mile */
    override val name = "uk nautical mile"

    /** 单位符号：uk.nmi / Unit symbol: uk.nmi */
    override val symbol = "uk.nmi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 俄式海里 / Russian Nautical Mile
 *
 * 俄国历史上使用的海里单位。
 * Historical Russian nautical mile unit.
 *
 * 符号 / Symbol: ru.nmi
 * 换算关系 / Conversion: 1 ru.nmi ≈ 1.85578 km
*/
object RUNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85578) {

    /** 单位名称：rus nautical mile / Unit name: rus nautical mile */
    override val name = "rus nautical mile"

    /** 单位符号：ru.nmi / Unit symbol: ru.nmi */
    override val symbol = "ru.nmi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 美式海里 / US Nautical Mile
 *
 * 美国历史上使用的海里单位。
 * Historical US nautical mile unit.
 *
 * 符号 / Symbol: us.nmi
 * 换算关系 / Conversion: 1 us.nmi ≈ 1.85101 km
*/
object USNauticalMile : DerivedPhysicalUnit(Kilometer * 1.85101) {

    /** 单位名称：usa nautical mile / Unit name: usa nautical mile */
    override val name = "usa nautical mile"

    /** 单位符号：us.nmi / Unit symbol: us.nmi */
    override val symbol = "us.nmi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 英寻 / Fathom
 *
 * 航海中使用的深度单位。
 * A depth unit used in navigation.
 *
 * 符号 / Symbol: fm
 * 换算关系 / Conversion: 1 fm = 1/1000 nmi = 1.852 m
*/
object Fathom : DerivedPhysicalUnit(NauticalMile * Scale(10, -3)) {

    /** 单位名称：fathom / Unit name: fathom */
    override val name = "fathom"

    /** 单位符号：fm / Unit symbol: fm */
    override val symbol = "fm"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 链 / Cable
 *
 * 航海中使用的长度单位。
 * A length unit used in navigation.
 *
 * 符号 / Symbol: cab
 * 换算关系 / Conversion: 1 cab = 1/10 nmi = 185.2 m
*/
object Cable : DerivedPhysicalUnit(NauticalMile * Scale(10, -1)) {

    /** 单位名称：cable / Unit name: cable */
    override val name = "cable"

    /** 单位符号：cab / Unit symbol: cab */
    override val symbol = "cab"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 英寸 / Inch
 *
 * 英制长度单位。
 * Imperial length unit.
 *
 * 符号 / Symbol: in
 * 换算关系 / Conversion: 1 in = 2.54 cm（精确值）/ 1 in = 2.54 cm (exact)
*/
object Inch : DerivedPhysicalUnit(Centimeter * 2.54) {

    /** 单位名称：inch / Unit name: inch */
    override val name = "inch"

    /** 单位符号：in / Unit symbol: in */
    override val symbol = "in"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 英尺 / Foot
 *
 * 英制长度单位。
 * Imperial length unit.
 *
 * 符号 / Symbol: ft
 * 换算关系 / Conversion: 1 ft = 12 in = 30.48 cm
*/
object Foot : DerivedPhysicalUnit(Inch * 12) {

    /** 单位名称：foot / Unit name: foot */
    override val name = "foot"

    /** 单位符号：ft / Unit symbol: ft */
    override val symbol = "ft"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 码 / Yard
 *
 * 英制长度单位。
 * Imperial length unit.
 *
 * 符号 / Symbol: yd
 * 换算关系 / Conversion: 1 yd = 3 ft = 0.9144 m
*/
object Yard : DerivedPhysicalUnit(Foot * 3) {

    /** 单位名称：yard / Unit name: yard */
    override val name = "yard"

    /** 单位符号：yd / Unit symbol: yd */
    override val symbol = "yd"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 链（英制）/ Chain
 *
 * 英制测量中使用的长度单位。
 * Length unit used in imperial surveying.
 *
 * 符号 / Symbol: ch
 * 换算关系 / Conversion: 1 ch = 22 yd = 20.1168 m
*/
object Chain : DerivedPhysicalUnit(Yard * 22) {

    /** 单位名称：chain / Unit name: chain */
    override val name = "chain"

    /** 单位符号：ch / Unit symbol: ch */
    override val symbol = "ch"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 杆 / Rod
 *
 * 英制测量中使用的长度单位。
 * Length unit used in imperial surveying.
 *
 * 符号 / Symbol: rd
 * 换算关系 / Conversion: 1 rd = 5.0292 m
*/
object Rod : DerivedPhysicalUnit(Meter * 5.0292) {

    /** 单位名称：rod / Unit name: rod */
    override val name = "rod"

    /** 单位符号：rd / Unit symbol: rd */
    override val symbol = "rd"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 英里 / Mile
 *
 * 英制长度单位。
 * Imperial length unit.
 *
 * 符号 / Symbol: mi
 * 换算关系 / Conversion: 1 mi = 1760 yd = 1609.344 m
*/
object Mile : DerivedPhysicalUnit(Yard * 1760) {

    /** 单位名称：mile / Unit name: mile */
    override val name = "mile"

    /** 单位符号：mi / Unit symbol: mi */
    override val symbol = "mi"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 天文单位 / Astronomical Unit
 *
 * 天文学中使用的长度单位，约等于地球到太阳的平均距离。
 * Length unit used in astronomy, approximately the average distance from Earth to Sun.
 *
 * 符号 / Symbol: au
 * 换算关系 / Conversion: 1 au = 149597870700 m（精确值，IAU 2012）/ 1 au = 149597870700 m (exact, IAU 2012)
*/
object AstronomicalUnit : DerivedPhysicalUnit(Meter * 149597870700.0) {

    /** 单位名称：astronomical unit / Unit name: astronomical unit */
    override val name = "astronomical unit"

    /** 单位符号：au / Unit symbol: au */
    override val symbol = "au"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 光秒 / Light Second
 *
 * 光在真空中一秒内行进的距离。
 * Distance light travels in vacuum in one second.
 *
 * 符号 / Symbol: lsc
 * 换算关系 / Conversion: 1 lsc = 299792458 m（精确值）/ 1 lsc = 299792458 m (exact)
*/
object LightSecond : DerivedPhysicalUnit(Meter * 299792458) {

    /** 单位名称：light second / Unit name: light second */
    override val name = "light second"

    /** 单位符号：lsc / Unit symbol: lsc */
    override val symbol = "lsc"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 光分 / Light Minute
 *
 * 光在真空中一分钟内行进的距离。
 * Distance light travels in vacuum in one minute.
 *
 * 符号 / Symbol: lmn
 * 换算关系 / Conversion: 1 lmn = 60 lsc = 17987547480 m
*/
object LightMinute : DerivedPhysicalUnit(LightSecond * 60) {

    /** 单位名称：light minute / Unit name: light minute */
    override val name = "light minute"

    /** 单位符号：lmn / Unit symbol: lmn */
    override val symbol = "lmn"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 光时 / Light Hour
 *
 * 光在真空中一小时内行进的距离。
 * Distance light travels in vacuum in one hour.
 *
 * 符号 / Symbol: lhr
 * 换算关系 / Conversion: 1 lhr = 60 lmn = 1079252848800 m
*/
object LightHour : DerivedPhysicalUnit(LightMinute * 60) {

    /** 单位名称：light hour / Unit name: light hour */
    override val name = "light hour"

    /** 单位符号：lhr / Unit symbol: lhr */
    override val symbol = "lhr"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 光日 / Light Day
 *
 * 光在真空中一天内行进的距离。
 * Distance light travels in vacuum in one day.
 *
 * 符号 / Symbol: ldy
 * 换算关系 / Conversion: 1 ldy = 24 lhr = 25902068371200 m
*/
object LightDay : DerivedPhysicalUnit(LightHour * 24) {

    /** 单位名称：light day / Unit name: light day */
    override val name = "light day"

    /** 单位符号：ldy / Unit symbol: ldy */
    override val symbol = "ldy"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 光年 / Light Year
 *
 * 光在真空中一年内行进的距离，使用儒略年定义。
 * Distance light travels in vacuum in one year, using Julian year definition.
 *
 * 符号 / Symbol: ly
 * 换算关系 / Conversion: 1 ly = 365.25 ldy ≈ 9.461 × 10¹⁵ m
*/
object LightYear : DerivedPhysicalUnit(LightDay * 365.25) {

    /** 单位名称：light year / Unit name: light year */
    override val name = "light year"

    /** 单位符号：ly / Unit symbol: ly */
    override val symbol = "ly"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 秒差距 / Parsec
 *
 * 天文学中使用的长度单位，基于视差测量。
 * Length unit used in astronomy, based on parallax measurements.
 *
 * 符号 / Symbol: pc
 * 换算关系 / Conversion: 1 pc ≈ 3.086 × 10¹⁶ m ≈ 3.26 ly
*/
object Parsec : DerivedPhysicalUnit(Meter * 30856775814913673.0) {

    /** 单位名称：parsec / Unit name: parsec */
    override val name = "parsec"

    /** 单位符号：pc / Unit symbol: pc */
    override val symbol = "pc"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 千秒差距 / Kiloparsec
 *
 * 一千秒差距。
 * One thousand parsecs.
 *
 * 符号 / Symbol: kpc
 * 换算关系 / Conversion: 1 kpc = 10³ pc
*/
object Kiloparsec : DerivedPhysicalUnit(Parsec * Scale.kilo) {

    /** 单位名称：kiloparsec / Unit name: kiloparsec */
    override val name = "kiloparsec"

    /** 单位符号：kpc / Unit symbol: kpc */
    override val symbol = "kpc"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 兆秒差距 / Megaparsec
 *
 * 一百万秒差距。
 * One million parsecs.
 *
 * 符号 / Symbol: Mpc
 * 换算关系 / Conversion: 1 Mpc = 10⁶ pc
*/
object Megaparsec : DerivedPhysicalUnit(Parsec * Scale.mega) {

    /** 单位名称：megaparsec / Unit name: megaparsec */
    override val name = "megaparsec"

    /** 单位符号：Mpc / Unit symbol: Mpc */
    override val symbol = "Mpc"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}

/**
 * 吉秒差距 / Gigaparsec
 *
 * 十亿秒差距。
 * One billion parsecs.
 *
 * 符号 / Symbol: Gpc
 * 换算关系 / Conversion: 1 Gpc = 10⁹ pc
*/
object Gigaparsec : DerivedPhysicalUnit(Parsec * Scale.giga) {

    /** 单位名称：gigaparsec / Unit name: gigaparsec */
    override val name = "gigaparsec"

    /** 单位符号：Gpc / Unit symbol: Gpc */
    override val symbol = "Gpc"

    /** 对应物理量：长度 / Corresponding quantity: Length */
    override val quantity = Length
}
