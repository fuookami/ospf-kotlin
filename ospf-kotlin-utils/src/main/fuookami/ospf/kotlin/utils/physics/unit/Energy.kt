package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

/**
 * Energy units - 能量单位
 * Energy units - SI energy units
 *
 * 提供能量量纲的 SI 单位定义，包括焦耳、千焦、兆焦、吉焦、电子伏特、千瓦时等。
 * Provides SI unit definitions for energy dimension, including joule, kilojoule, megajoule, gigajoule, electronvolt, kilowatt-hour, etc.
 */

/**
 * 焦耳（基本单位）
 * Joule (base unit)
 *
 * 定义：1 焦耳 = 1 牛顿·米
 * Definition: 1 joule = 1 newton-meter
 */
object Joule : DerivedPhysicalUnit(Newton * Meter) {
    override val name = "joule"
    override val symbol = "J"

    override val quantity = Energy
}

/**
 * 千焦
 * Kilojoule
 */
object Kilojoule : DerivedPhysicalUnit(Joule * Scale.kilo) {
    override val name = "kilojoule"
    override val symbol = "kJ"

    override val quantity = Energy
}

/**
 * 兆焦
 * Megajoule
 */
object Megajoule : DerivedPhysicalUnit(Joule * Scale.mega) {
    override val name = "megajoule"
    override val symbol = "MJ"

    override val quantity = Energy
}

/**
 * 吉焦
 * Gigajoule
 */
object Gigajoule : DerivedPhysicalUnit(Joule * Scale.giga) {
    override val name = "gigajoule"
    override val symbol = "GJ"

    override val quantity = Energy
}

/**
 * 电子伏特
 * Electronvolt
 *
 * 定义：1 电子伏特 = 1.602176634×10⁻¹⁹ 焦耳
 * Definition: 1 electronvolt = 1.602176634×10⁻¹⁹ joules
 */
object Electronvolt : DerivedPhysicalUnit(Joule * FltX("1.602176634e-19")) {
    override val name = "electronvolt"
    override val symbol = "eV"

    override val quantity = Energy
}

/**
 * 千电子伏特
 * Kiloelectronvolt
 */
object Kiloelectronvolt : DerivedPhysicalUnit(Electronvolt * Scale.kilo) {
    override val name = "kiloelectronvolt"
    override val symbol = "keV"

    override val quantity = Energy
}

/**
 * 兆电子伏特
 * Megaelectronvolt
 */
object Megaelectronvolt : DerivedPhysicalUnit(Electronvolt * Scale.mega) {
    override val name = "megaelectronvolt"
    override val symbol = "MeV"

    override val quantity = Energy
}

/**
 * 吉电子伏特
 * Gigaelectronvolt
 */
object Gigaelectronvolt : DerivedPhysicalUnit(Electronvolt * Scale.giga) {
    override val name = "gigaelectronvolt"
    override val symbol = "GeV"

    override val quantity = Energy
}

/**
 * 千瓦时
 * Kilowatt-hour
 *
 * 定义：1 千瓦时 = 1 千瓦 × 1 小时
 * Definition: 1 kilowatt-hour = 1 kilowatt × 1 hour
 */
object KilowattHour : DerivedPhysicalUnit(Kilowatt * Hour) {
    override val name = "kilowatt-hour"
    override val symbol = "kWh"

    override val quantity = Energy
}

/**
 * 兆瓦时
 * Megawatt-hour
 */
object MegawattHour : DerivedPhysicalUnit(KilowattHour * Scale.kilo) {
    override val name = "megawatt-hour"
    override val symbol = "MWh"

    override val quantity = Energy
}

/**
 * 吉瓦时
 * Gigawatt-hour
 */
object GigawattHour : DerivedPhysicalUnit(KilowattHour * Scale.mega) {
    override val name = "gigawatt-hour"
    override val symbol = "GWh"

    override val quantity = Energy
}

/**
 * 瓦时
 * Watt-hour
 */
object WattHour : DerivedPhysicalUnit(KilowattHour / Scale.kilo) {
    override val name = "watt-hour"
    override val symbol = "Wh"

    override val quantity = Energy
}

/**
 * 卡路里（热化学卡）
 * Calorie (thermochemical calorie)
 *
 * 定义：1 卡路里 = 4.184 焦耳
 * Definition: 1 calorie = 4.184 joules
 */
object Calorie : DerivedPhysicalUnit(Joule * FltX("4.184")) {
    override val name = "calorie"
    override val symbol = "cal"

    override val quantity = Energy
}

/**
 * 千卡（大卡）
 * Kilocalorie (large calorie / food calorie)
 */
object Kilocalorie : DerivedPhysicalUnit(Calorie * Scale.kilo) {
    override val name = "kilocalorie"
    override val symbol = "kcal"

    override val quantity = Energy
}

/**
 * 英热单位
 * British thermal unit (BTU)
 *
 * 定义：1 BTU ≈ 1055.06 焦耳
 * Definition: 1 BTU ≈ 1055.06 joules
 */
object BritishThermalUnit : DerivedPhysicalUnit(Joule * FltX("1055.06")) {
    override val name = "british thermal unit"
    override val symbol = "BTU"

    override val quantity = Energy
}

/**
 * 尔格
 * Erg
 *
 * 定义：1 尔格 = 10⁻⁷ 焦耳 (CGS 单位制)
 * Definition: 1 erg = 10⁻⁷ joules (CGS unit system)
 */
object Erg : DerivedPhysicalUnit(Joule * FltX("1e-7")) {
    override val name = "erg"
    override val symbol = "erg"

    override val quantity = Energy
}