/**
 * 时间单位 / Time units
 *
 * 提供时间量纲的单位定义，包括秒、毫秒、微秒、纳秒、分、时、天、周、年等。
 * Provides unit definitions for time dimension, including second, millisecond, microsecond, nanosecond,
 * minute, hour, day, week, year, etc.
 *
 * 来源：SI 基本单位及常用时间单位定义
 * Source: SI base unit and common time unit definitions
 * - Second: SI base unit for time
 * - Minute: 60 seconds
 * - Hour: 60 minutes = 3600 seconds
 * - Day: 24 hours = 86400 seconds
 * - Week: 7 days = 604800 seconds
 * - Year: 365.25 days (Julian year)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Time

/**
 * 秒（SI 基本单位）
 * Second (SI base unit)
 *
 * 名称：秒
 * Name: second
 *
 * 符号：s
 * Symbol: s
 *
 * 定义：铯-133 原子基态两个超精细能级之间跃迁所对应辐射的 9,192,631,770 个周期持续的时间
 * Definition: Duration of 9,192,631,770 periods of the radiation corresponding to the transition
 * between the two hyperfine levels of the ground state of the caesium-133 atom
 *
 * 来源：SI 七个基本单位之一
 * Source: One of the seven SI base units
 */
object Second : PhysicalUnit() {
    override val name: String = "second"
    override val symbol: String = "s"

    override val quantity = Time
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

/**
 * 纳秒
 * Nanosecond
 *
 * 名称：纳秒
 * Name: nanosecond
 *
 * 符号：ns
 * Symbol: ns
 *
 * 定义：1 ns = 10⁻⁹ s
 * Definition: 1 ns = 10⁻⁹ s
 *
 * 来源：SI 时间单位，常用于计算机科学和高精度计时
 * Source: SI time unit, commonly used in computer science and high-precision timing
 */
object Nanosecond : DerivedPhysicalUnit(Second * Scale.nano) {
    override val name: String = "nanosecond"
    override val symbol: String = "ns"

    override val quantity = Time
}

/**
 * 微秒
 * Microsecond
 *
 * 名称：微秒
 * Name: microsecond
 *
 * 符号：μs
 * Symbol: μs
 *
 * 定义：1 μs = 10⁻⁶ s
 * Definition: 1 μs = 10⁻⁶ s
 *
 * 来源：SI 时间单位，常用于科学实验和工程测量
 * Source: SI time unit, commonly used in scientific experiments and engineering measurements
 */
object Microsecond : DerivedPhysicalUnit(Second * Scale.micro) {
    override val name: String = "microsecond"
    override val symbol: String = "μs"

    override val quantity = Time
}

/**
 * 毫秒
 * Millisecond
 *
 * 名称：毫秒
 * Name: millisecond
 *
 * 符号：ms
 * Symbol: ms
 *
 * 定义：1 ms = 10⁻³ s
 * Definition: 1 ms = 10⁻³ s
 *
 * 来源：SI 时间单位，常用于日常计时和计算机编程
 * Source: SI time unit, commonly used in daily timing and computer programming
 */
object Millisecond : DerivedPhysicalUnit(Second * Scale.milli) {
    override val name: String = "millisecond"
    override val symbol: String = "ms"

    override val quantity = Time
}

/**
 * 分
 * Minute
 *
 * 名称：分
 * Name: minute
 *
 * 符号：min
 * Symbol: min
 *
 * 定义：1 min = 60 s
 * Definition: 1 min = 60 s
 *
 * 来源：常用时间单位
 * Source: Common time unit
 */
object Minute : DerivedPhysicalUnit(Second * 60) {
    override val name: String = "minute"
    override val symbol: String = "min"

    override val quantity = Time
}

/**
 * 小时
 * Hour
 *
 * 名称：小时
 * Name: hour
 *
 * 符号：h
 * Symbol: h
 *
 * 定义：1 h = 60 min = 3600 s
 * Definition: 1 h = 60 min = 3600 s
 *
 * 来源：常用时间单位
 * Source: Common time unit
 */
object Hour : DerivedPhysicalUnit(Minute * 60) {
    override val name: String = "hour"
    override val symbol: String = "h"

    override val quantity = Time
}

/**
 * 天
 * Day
 *
 * 名称：天
 * Name: day
 *
 * 符号：d
 * Symbol: d
 *
 * 定义：1 d = 24 h = 86400 s
 * Definition: 1 d = 24 h = 86400 s
 *
 * 来源：常用时间单位，基于地球自转周期
 * Source: Common time unit, based on Earth's rotation period
 */
object Day : DerivedPhysicalUnit(Hour * 24) {
    override val name: String = "day"
    override val symbol: String = "d"

    override val quantity = Time
}

/**
 * 周
 * Week
 *
 * 名称：周
 * Name: week
 *
 * 符号：wk
 * Symbol: wk
 *
 * 定义：1 wk = 7 d = 604800 s
 * Definition: 1 wk = 7 d = 604800 s
 *
 * 来源：常用时间单位
 * Source: Common time unit
 */
object Week : DerivedPhysicalUnit(Day * 7) {
    override val name: String = "week"
    override val symbol: String = "wk"

    override val quantity = Time
}

/**
 * 年（儒略年）
 * Year (Julian year)
 *
 * 名称：年
 * Name: year
 *
 * 符号：yr
 * Symbol: yr
 *
 * 定义：1 yr = 365.25 d = 31557600 s
 * Definition: 1 yr = 365.25 d = 31557600 s
 *
 * 来源：儒略年，常用于天文学和科学计算
 * Source: Julian year, commonly used in astronomy and scientific calculations
 */
object Year : DerivedPhysicalUnit(Day * 365.25) {
    override val name: String = "year"
    override val symbol: String = "yr"

    override val quantity = Time
}
