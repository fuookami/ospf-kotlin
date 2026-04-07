/**
 * 带宽单位
 * Bandwidth units
 *
 * 提供带宽度纲的单位定义，包括比特每秒、千比特每秒、兆比特每秒等。
 * Provides unit definitions for bandwidth dimension, including bit per second, kilobit per second, megabit per second, etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Bandwidth

/**
 * 比特每秒
 * Bit per second
 *
 * 带宽基本单位。
 * Base unit for bandwidth.
 *
 * 定义：1 bit/s = 1 比特 / 秒
 * Definition: 1 bit/s = 1 bit / second
 */
object BitPerSecond : DerivedPhysicalUnit(Bit / Second) {
    override val name = "bit per second"
    override val symbol = "bit/s"

    override val quantity = Bandwidth
}

/**
 * 千比特每秒
 * Kilobit per second
 *
 * 定义：1 kbit/s = 10³ bit/s
 * Definition: 1 kbit/s = 10³ bit/s
 */
object KilobitPerSecond : DerivedPhysicalUnit(Kilobit / Second) {
    override val name = "kilobit per second"
    override val symbol = "kbit/s"

    override val quantity = Bandwidth
}

/**
 * 兆比特每秒
 * Megabit per second
 *
 * 定义：1 Mbit/s = 10⁶ bit/s
 * Definition: 1 Mbit/s = 10⁶ bit/s
 */
object MegabitPerSecond : DerivedPhysicalUnit(Megabit / Second) {
    override val name = "megabit per second"
    override val symbol = "mbit/s"

    override val quantity = Bandwidth
}

/**
 * 吉比特每秒
 * Gigabit per second
 *
 * 定义：1 Gbit/s = 10⁹ bit/s
 * Definition: 1 Gbit/s = 10⁹ bit/s
 */
object GigabitPerSecond : DerivedPhysicalUnit(Gigabit / Second) {
    override val name = "gigabit per second"
    override val symbol = "gbit/s"

    override val quantity = Bandwidth
}

/**
 * 太比特每秒
 * Terabit per second
 *
 * 定义：1 Tbit/s = 10¹² bit/s
 * Definition: 1 Tbit/s = 10¹² bit/s
 */
object TerabitPerSecond : DerivedPhysicalUnit(Terabit / Second) {
    override val name = "terabit per second"
    override val symbol = "tbit/s"

    override val quantity = Bandwidth
}

/**
 * 拍比特每秒
 * Petabit per second
 *
 * 定义：1 Pbit/s = 10¹⁵ bit/s
 * Definition: 1 Pbit/s = 10¹⁵ bit/s
 */
object PetabitPerSecond : DerivedPhysicalUnit(Petabit / Second) {
    override val name = "petabit per second"
    override val symbol = "pbit/s"

    override val quantity = Bandwidth
}

/**
 * 艾比特每秒
 * Exabit per second
 *
 * 定义：1 Ebit/s = 10¹⁸ bit/s
 * Definition: 1 Ebit/s = 10¹⁸ bit/s
 */
object ExabitPerSecond : DerivedPhysicalUnit(Exabit / Second) {
    override val name = "exabit per second"
    override val symbol = "ebit/s"

    override val quantity = Bandwidth
}