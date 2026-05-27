/**
 * 电压单位 / Voltage units
 *
 * 提供电压量纲的 SI 单位定义，包括伏特、毫伏、千伏、兆伏等。
 * Provides SI unit definitions for voltage dimension, including volt, millivolt, kilovolt, megavolt, etc.
 *
 * 来源：SI 导出单位
 * Source: SI derived units
 * - Volt: V = W/A, SI derived unit for voltage
 * - 1 volt = 1 watt / 1 ampere
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Voltage

/**
 * 伏特（SI 导出单位）
 * Volt (SI derived unit)
 *
 * 名称：伏特
 * Name: volt
 *
 * 符号：V
 * Symbol: V
 *
 * 定义：1 V = 1 W / 1 A
 * Definition: 1 V = 1 W / 1 A
 *
 * 来源：SI 导出单位，电压的国际单位，以亚历山大·伏特命名
 * Source: SI derived unit, international unit for voltage, named after Alessandro Volta
 */
object Volt : DerivedPhysicalUnit(Watt / Ampere) {
    override val name = "volt"
    override val symbol = "V"

    override val quantity = Voltage
}

/**
 * 微伏
 * Microvolt
 *
 * 名称：微伏
 * Name: microvolt
 *
 * 符号：μV
 * Symbol: μV
 *
 * 定义：1 μV = 10⁻⁶ V
 * Definition: 1 μV = 10⁻⁶ V
 *
 * 来源：SI 电压单位，常用于精密电子测量
 * Source: SI voltage unit, commonly used in precision electronic measurements
 */
object Microvolt : DerivedPhysicalUnit(Volt * Scale.micro) {
    override val name = "microvolt"
    override val symbol = "µV"

    override val quantity = Voltage
}

/**
 * 毫伏
 * Millivolt
 *
 * 名称：毫伏
 * Name: millivolt
 *
 * 符号：mV
 * Symbol: mV
 *
 * 定义：1 mV = 10⁻³ V
 * Definition: 1 mV = 10⁻³ V
 *
 * 来源：SI 电压单位，常用于电子电路和传感器
 * Source: SI voltage unit, commonly used in electronic circuits and sensors
 */
object Millivolt : DerivedPhysicalUnit(Volt * Scale.milli) {
    override val name = "millivolt"
    override val symbol = "mV"

    override val quantity = Voltage
}

/**
 * 千伏
 * Kilovolt
 *
 * 名称：千伏
 * Name: kilovolt
 *
 * 符号：kV
 * Symbol: kV
 *
 * 定义：1 kV = 10³ V
 * Definition: 1 kV = 10³ V
 *
 * 来源：SI 电压单位，常用于电力输配电
 * Source: SI voltage unit, commonly used in power transmission and distribution
 */
object Kilovolt : DerivedPhysicalUnit(Volt * Scale.kilo) {
    override val name = "kilovolt"
    override val symbol = "kV"

    override val quantity = Voltage
}

/**
 * 兆伏
 * Megavolt
 *
 * 名称：兆伏
 * Name: megavolt
 *
 * 符号：MV
 * Symbol: MV
 *
 * 定义：1 MV = 10⁶ V
 * Definition: 1 MV = 10⁶ V
 *
 * 来源：SI 电压单位，常用于高压电力系统
 * Source: SI voltage unit, commonly used in high-voltage power systems
 */
object Megavolt : DerivedPhysicalUnit(Volt * Scale.mega) {
    override val name = "megavolt"
    override val symbol = "MV"

    override val quantity = Voltage
}
