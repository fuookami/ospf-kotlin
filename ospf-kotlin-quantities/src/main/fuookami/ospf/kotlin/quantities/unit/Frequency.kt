/**
 * 频率单位
 * Frequency units
 *
 * 提供频率量纲的 SI 单位定义，包括赫兹、千赫、兆赫、吉赫等。
 * Provides SI unit definitions for frequency dimension, including hertz, kilohertz, megahertz, gigahertz, etc.
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Frequency

/**
 * 赫兹
 * Hertz
 *
 * 频率的 SI 导出单位。
 * SI derived unit for frequency.
 *
 * 定义：1 Hz = 1 / s = 1 s⁻¹
 * Definition: 1 Hz = 1 / s = 1 s⁻¹
*/
object Hertz : DerivedPhysicalUnit(Second.reciprocal()) {
    override val name = "hertz"
    override val symbol = "Hz"

    override val quantity = Frequency
}

/**
 * 千赫
 * Kilohertz
 *
 * 定义：1 kHz = 10³ Hz
 * Definition: 1 kHz = 10³ Hz
*/
object Kilohertz : DerivedPhysicalUnit(Hertz * Scale.kilo) {
    override val name = "kilohertz"
    override val symbol = "kHz"

    override val quantity = Frequency
}

/**
 * 兆赫
 * Megahertz
 *
 * 定义：1 MHz = 10⁶ Hz
 * Definition: 1 MHz = 10⁶ Hz
*/
object Megahertz : DerivedPhysicalUnit(Hertz * Scale.mega) {
    override val name = "megahertz"
    override val symbol = "MHz"

    override val quantity = Frequency
}

/**
 * 吉赫
 * Gigahertz
 *
 * 定义：1 GHz = 10⁹ Hz
 * Definition: 1 GHz = 10⁹ Hz
*/
object Gigahertz : DerivedPhysicalUnit(Hertz * Scale.giga) {
    override val name = "gigahertz"
    override val symbol = "GHz"

    override val quantity = Frequency
}

/**
 * 每小时周期数
 * Cycle per hour
 *
 * 定义：1 cph = 1 / 3600 Hz
 * Definition: 1 cph = 1 / 3600 Hz
*/
object CyclePerHour : DerivedPhysicalUnit(Hertz * Second.to(Hour)!!) {
    override val name = "cycle per hour"
    override val symbol = "cph"

    override val quantity = Frequency
}
