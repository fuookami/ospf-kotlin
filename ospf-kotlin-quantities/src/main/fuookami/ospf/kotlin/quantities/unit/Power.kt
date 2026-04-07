/**
 * 功率单位 / Power units
 *
 * 用于测量能量转换速率的单位。
 * Units for measuring the rate of energy conversion.
 *
 * 单位常量来源 / Unit constant sources:
 * - Watt: SI导出单位，定义为焦耳每秒 / SI derived unit, defined as joule per second
 * - Horsepower: 1 ps = 735 W（公制马力）/ 1 ps = 735 W (metric horsepower)
 * - UKHorsepower: 1 hp = 550 ft·lbf/s ≈ 745.7 W（英制马力）/ 1 hp = 550 ft·lbf/s ≈ 745.7 W (imperial horsepower)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Power

/**
 * 瓦特 / Watt
 *
 * 功率的SI导出单位，定义为每秒转换一焦耳能量。
 * The SI derived unit of power, defined as one joule of energy converted per second.
 *
 * 符号 / Symbol: W
 * 换算关系 / Conversion: 1 W = 1 J/s = 1 N·m/s = 1 kg·m²/s³
 */
object Watt : DerivedPhysicalUnit(Newton * MeterPerSecond) {
    /** 单位名称：watt / Unit name: watt */
    override val name = "watt"
    /** 单位符号：W / Unit symbol: W */
    override val symbol = "W"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 千瓦 / Kilowatt
 *
 * 一千瓦。
 * One thousand watts.
 *
 * 符号 / Symbol: kW
 * 换算关系 / Conversion: 1 kW = 10³ W = 1000 W
 */
object Kilowatt : DerivedPhysicalUnit(Watt * Scale.kilo) {
    /** 单位名称：kilowatt / Unit name: kilowatt */
    override val name = "kilowatt"
    /** 单位符号：kW / Unit symbol: kW */
    override val symbol = "kW"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 兆瓦 / Megawatt
 *
 * 一百万瓦。
 * One million watts.
 *
 * 符号 / Symbol: MW
 * 换算关系 / Conversion: 1 MW = 10⁶ W = 1000000 W
 */
object Megawatt : DerivedPhysicalUnit(Watt * Scale.mega) {
    /** 单位名称：megawatt / Unit name: megawatt */
    override val name = "megawatt"
    /** 单位符号：MW / Unit symbol: MW */
    override val symbol = "MW"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 毫瓦 / Milliwatt
 *
 * 千分之一瓦。
 * One thousandth of a watt.
 *
 * 符号 / Symbol: mW
 * 换算关系 / Conversion: 1 mW = 10⁻³ W = 0.001 W
 */
object Milliwatt : DerivedPhysicalUnit(Watt * Scale.milli) {
    /** 单位名称：milliwatt / Unit name: milliwatt */
    override val name = "milliwatt"
    /** 单位符号：mW / Unit symbol: mW */
    override val symbol = "mW"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 焦耳每秒 / Joule per second
 *
 * 等价于瓦特。
 * Equivalent to watt.
 *
 * 符号 / Symbol: J/s
 * 换算关系 / Conversion: 1 J/s = 1 W
 */
object JoulePerSecond : DerivedPhysicalUnit(Watt) {
    /** 单位名称：Joule per second / Unit name: Joule per second */
    override val name = "Joule per second"
    /** 单位符号：J/s / Unit symbol: J/s */
    override val symbol = "J/s"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 牛顿米每秒 / Newton meter per second
 *
 * 等价于瓦特。
 * Equivalent to watt.
 *
 * 符号 / Symbol: Nm/s
 * 换算关系 / Conversion: 1 Nm/s = 1 W
 */
object NewtonMeterPerSecond : DerivedPhysicalUnit(Watt) {
    /** 单位名称：Newton meter per second / Unit name: Newton meter per second */
    override val name = "Newton meter per second"
    /** 单位符号：Nm/s / Unit symbol: Nm/s */
    override val symbol = "Nm/s"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 公制马力 / Horsepower (metric)
 *
 * 公制马力单位，主要用于欧洲。
 * Metric horsepower unit, primarily used in Europe.
 *
 * 符号 / Symbol: ps
 * 换算关系 / Conversion: 1 ps = 735 W
 */
object Horsepower : DerivedPhysicalUnit(Watt * 735) {
    /** 单位名称：horsepower / Unit name: horsepower */
    override val name = "horsepower"
    /** 单位符号：ps / Unit symbol: ps */
    override val symbol = "ps"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}

/**
 * 英制马力 / UK Horsepower
 *
 * 英制马力单位，定义为550英尺磅力每秒。
 * Imperial horsepower unit, defined as 550 foot-pound-force per second.
 *
 * 符号 / Symbol: uk.ps
 * 换算关系 / Conversion: 1 hp = 550 ft·lbf/s ≈ 745.7 W
 */
object UKHorsepower : DerivedPhysicalUnit(Watt * 550) {
    /** 单位名称：uk horsepower / Unit name: uk horsepower */
    override val name = "uk horsepower"
    /** 单位符号：uk.ps / Unit symbol: uk.ps */
    override val symbol = "uk.ps"

    /** 对应物理量：功率 / Corresponding quantity: Power */
    override val quantity = Power
}