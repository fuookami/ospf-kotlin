package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.Voltage

/**
 * Voltage units - 电压单位
 * Voltage units - SI voltage units
 *
 * 提供电压量纲的 SI 单位定义，包括伏特、毫伏、千伏等。
 * Provides SI unit definitions for voltage dimension, including volt, millivolt, kilovolt, etc.
 */

/**
 * 伏特（基本单位）
 * Volt (base unit)
 *
 * 定义：1 伏特 = 1 瓦特 / 安培
 * Definition: 1 volt = 1 watt / ampere
 */
object Volt : DerivedPhysicalUnit(Watt / Ampere) {
    override val name = "volt"
    override val symbol = "V"

    override val quantity = Voltage
}

/**
 * 微伏
 * Microvolt
 */
object Microvolt : DerivedPhysicalUnit(Volt * Scale.micro) {
    override val name = "microvolt"
    override val symbol = "µV"

    override val quantity = Voltage
}

/**
 * 毫伏
 * Millivolt
 */
object Millivolt : DerivedPhysicalUnit(Volt * Scale.milli) {
    override val name = "millivolt"
    override val symbol = "mV"

    override val quantity = Voltage
}

/**
 * 千伏
 * Kilovolt
 */
object Kilovolt : DerivedPhysicalUnit(Volt * Scale.kilo) {
    override val name = "kilovolt"
    override val symbol = "kV"

    override val quantity = Voltage
}

/**
 * 兆伏
 * Megavolt
 */
object Megavolt : DerivedPhysicalUnit(Volt * Scale.mega) {
    override val name = "megavolt"
    override val symbol = "MV"

    override val quantity = Voltage
}
