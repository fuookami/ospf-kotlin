/**
 * 压力单位 / Pressure units
 *
 * 用于测量单位面积上力的单位。
 * Units for measuring force per unit area.
 *
 * 单位常量来源 / Unit constant sources:
 * - Pascal: SI导出单位，定义为牛顿每平方米 / SI derived unit, defined as newton per square meter
 * - StandardAtmosphericPressure: 1 atm = 101325 Pa（精确值）/ 1 atm = 101325 Pa (exact)
 * - Bar: 1 bar = 10⁵ Pa
 * - MeterMercury: 1 mHg = 0.76 atm
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Pressure

/**
 * 帕斯卡 / Pascal
 *
 * 压力的SI导出单位，定义为牛顿每平方米。
 * The SI derived unit of pressure, defined as newton per square meter.
 *
 * 符号 / Symbol: Pa
 * 换算关系 / Conversion: 1 Pa = 1 N/m² = 1 kg/(m·s²)
 */
object Pascal : DerivedPhysicalUnit(Newton / SquareMeter) {
    /** 单位名称：pascal / Unit name: pascal */
    override val name = "pascal"
    /** 单位符号：Pa / Unit symbol: Pa */
    override val symbol = "Pa"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 百帕 / Hectopascal
 *
 * 一百帕斯卡，常用于气象学。
 * One hundred pascals, commonly used in meteorology.
 *
 * 符号 / Symbol: hPa
 * 换算关系 / Conversion: 1 hPa = 100 Pa
 */
object Hectopascal : DerivedPhysicalUnit(Pascal * Scale.hecto) {
    /** 单位名称：hectopascal / Unit name: hectopascal */
    override val name = "hectopascal"
    /** 单位符号：hPa / Unit symbol: hPa */
    override val symbol = "hPa"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 千帕 / Kilopascal
 *
 * 一千帕斯卡。
 * One thousand pascals.
 *
 * 符号 / Symbol: kPa
 * 换算关系 / Conversion: 1 kPa = 1000 Pa
 */
object Kilopascal : DerivedPhysicalUnit(Pascal * Scale.kilo) {
    /** 单位名称：kilopascal / Unit name: kilopascal */
    override val name = "kilopascal"
    /** 单位符号：kPa / Unit symbol: kPa */
    override val symbol = "kPa"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 兆帕 / Megapascal
 *
 * 一百万帕斯卡。
 * One million pascals.
 *
 * 符号 / Symbol: MPa
 * 换算关系 / Conversion: 1 MPa = 10⁶ Pa = 1000000 Pa
 */
object Megapascal : DerivedPhysicalUnit(Pascal * Scale.mega) {
    /** 单位名称：megapascal / Unit name: megapascal */
    override val name = "megapascal"
    /** 单位符号：MPa / Unit symbol: MPa */
    override val symbol = "MPa"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 标准大气压 / Standard atmospheric pressure
 *
 * 地球大气在海平面上的平均压力。
 * Average pressure of Earth's atmosphere at sea level.
 *
 * 符号 / Symbol: atm
 * 换算关系 / Conversion: 1 atm = 101325 Pa（精确值）/ 1 atm = 101325 Pa (exact)
 */
object StandardAtmosphericPressure : DerivedPhysicalUnit(Pascal * 101325) {
    /** 单位名称：standard atmospheric pressure / Unit name: standard atmospheric pressure */
    override val name = "standard atmospheric pressure"
    /** 单位符号：atm / Unit symbol: atm */
    override val symbol = "atm"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 米汞柱 / Meter of mercury
 *
 * 以汞柱高度表示的压力单位。
 * Pressure unit expressed as height of mercury column.
 *
 * 符号 / Symbol: mHg
 * 换算关系 / Conversion: 1 mHg = 0.76 atm = 760 mmHg
 */
object MeterMercury : DerivedPhysicalUnit(StandardAtmosphericPressure * 0.76) {
    /** 单位名称：meter mercury / Unit name: meter mercury */
    override val name = "meter mercury"
    /** 单位符号：mHg / Unit symbol: mHg */
    override val symbol = "mHg"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 毫米汞柱 / Millimeter of mercury
 *
 * 以毫米汞柱高度表示的压力单位，常用于血压测量。
 * Pressure unit expressed as millimeters of mercury column, commonly used for blood pressure.
 *
 * 符号 / Symbol: mmHg
 * 换算关系 / Conversion: 1 mmHg = 1/1000 mHg ≈ 133.322 Pa
 */
object MillimeterMercury : DerivedPhysicalUnit(MeterMercury * Scale.milli) {
    /** 单位名称：millimeter mercury / Unit name: millimeter mercury */
    override val name = "millimeter mercury"
    /** 单位符号：mmHg / Unit symbol: mmHg */
    override val symbol = "mmHg"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 英寸汞柱 / Inch of mercury
 *
 * 以英寸汞柱高度表示的压力单位，常用于美国。
 * Pressure unit expressed as inches of mercury column, commonly used in the US.
 *
 * 符号 / Symbol: inHg
 * 换算关系 / Conversion: 1 inHg ≈ 3386.39 Pa
 */
object InchOfMercury : DerivedPhysicalUnit(MeterMercury / Meter.to(Inch)!!) {
    /** 单位名称：inch of mercury / Unit name: inch of mercury */
    override val name = "inch of mercury"
    /** 单位符号：inHg / Unit symbol: inHg */
    override val symbol = "inHg"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 巴 / Bar
 *
 * 压力单位，等于100千帕。
 * A pressure unit equal to 100 kilopascals.
 *
 * 符号 / Symbol: bar
 * 换算关系 / Conversion: 1 bar = 10⁵ Pa = 100 kPa
 */
object Bar : DerivedPhysicalUnit(Pascal * Scale(10, 5)) {
    /** 单位名称：bar / Unit name: bar */
    override val name = "bar"
    /** 单位符号：bar / Unit symbol: bar */
    override val symbol = "bar"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}

/**
 * 毫巴 / Millibar
 *
 * 千分之一巴，等于百帕。
 * One thousandth of a bar, equal to one hectopascal.
 *
 * 符号 / Symbol: mbar
 * 换算关系 / Conversion: 1 mbar = 10⁻³ bar = 100 Pa = 1 hPa
 */
object Millibar : DerivedPhysicalUnit(Bar * Scale.milli) {
    /** 单位名称：millibar / Unit name: millibar */
    override val name = "millibar"
    /** 单位符号：mbar / Unit symbol: mbar */
    override val symbol = "mbar"

    /** 对应物理量：压力 / Corresponding quantity: Pressure */
    override val quantity = Pressure
}