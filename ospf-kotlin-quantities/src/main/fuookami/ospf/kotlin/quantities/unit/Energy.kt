/**
 * 能量单位
 * Energy units
 *
 * 提供能量量纲的 SI 单位定义，包括焦耳、千焦、兆焦、吉焦、电子伏特、千瓦时、卡路里、英热单位等。
 * Provides SI unit definitions for energy dimension, including joule, kilojoule, megajoule, gigajoule, electronvolt, kilowatt-hour, calorie, BTU, etc.
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.dimension.Energy

/**
 * 焦耳
 * Joule
 *
 * 能量的 SI 导出单位。
 * SI derived unit for energy.
 *
 * 定义：1 J = 1 N × 1 m = 1 kg × m² / s²
 * Definition: 1 J = 1 N × 1 m = 1 kg × m² / s²
*/
object Joule : DerivedPhysicalUnit(Newton * Meter) {
    override val name = "joule"
    override val symbol = "J"

    override val quantity = Energy
}

/**
 * 千焦
 * Kilojoule
 *
 * 定义：1 kJ = 10³ J
 * Definition: 1 kJ = 10³ J
*/
object Kilojoule : DerivedPhysicalUnit(Joule * Scale.kilo) {
    override val name = "kilojoule"
    override val symbol = "kJ"

    override val quantity = Energy
}

/**
 * 兆焦
 * Megajoule
 *
 * 定义：1 MJ = 10⁶ J
 * Definition: 1 MJ = 10⁶ J
*/
object Megajoule : DerivedPhysicalUnit(Joule * Scale.mega) {
    override val name = "megajoule"
    override val symbol = "MJ"

    override val quantity = Energy
}

/**
 * 吉焦
 * Gigajoule
 *
 * 定义：1 GJ = 10⁹ J
 * Definition: 1 GJ = 10⁹ J
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
 * 原子和粒子物理学中常用的能量单位。
 * Energy unit commonly used in atomic and particle physics.
 *
 * 定义：1 eV = 1.602176634 × 10⁻¹⁹ J
 * Definition: 1 eV = 1.602176634 × 10⁻¹⁹ J
*/
object Electronvolt : DerivedPhysicalUnit(Joule * FltX("1.602176634e-19")) {
    override val name = "electronvolt"
    override val symbol = "eV"

    override val quantity = Energy
}

/**
 * 千电子伏特
 * Kiloelectronvolt
 *
 * 定义：1 keV = 10³ eV
 * Definition: 1 keV = 10³ eV
*/
object Kiloelectronvolt : DerivedPhysicalUnit(Electronvolt * Scale.kilo) {
    override val name = "kiloelectronvolt"
    override val symbol = "keV"

    override val quantity = Energy
}

/**
 * 兆电子伏特
 * Megaelectronvolt
 *
 * 定义：1 MeV = 10⁶ eV
 * Definition: 1 MeV = 10⁶ eV
*/
object Megaelectronvolt : DerivedPhysicalUnit(Electronvolt * Scale.mega) {
    override val name = "megaelectronvolt"
    override val symbol = "MeV"

    override val quantity = Energy
}

/**
 * 吉电子伏特
 * Gigaelectronvolt
 *
 * 定义：1 GeV = 10⁹ eV
 * Definition: 1 GeV = 10⁹ eV
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
 * 电力计量中常用的能量单位。
 * Energy unit commonly used in electricity metering.
 *
 * 定义：1 kWh = 1 kW × 1 h = 3.6 × 10⁶ J
 * Definition: 1 kWh = 1 kW × 1 h = 3.6 × 10⁶ J
*/
object KilowattHour : DerivedPhysicalUnit(Kilowatt * Hour) {
    override val name = "kilowatt-hour"
    override val symbol = "kWh"

    override val quantity = Energy
}

/**
 * 兆瓦时
 * Megawatt-hour
 *
 * 定义：1 MWh = 10³ kWh
 * Definition: 1 MWh = 10³ kWh
*/
object MegawattHour : DerivedPhysicalUnit(KilowattHour * Scale.kilo) {
    override val name = "megawatt-hour"
    override val symbol = "MWh"

    override val quantity = Energy
}

/**
 * 吉瓦时
 * Gigawatt-hour
 *
 * 定义：1 GWh = 10⁶ kWh
 * Definition: 1 GWh = 10⁶ kWh
*/
object GigawattHour : DerivedPhysicalUnit(KilowattHour * Scale.mega) {
    override val name = "gigawatt-hour"
    override val symbol = "GWh"

    override val quantity = Energy
}

/**
 * 瓦时
 * Watt-hour
 *
 * 定义：1 Wh = 10⁻³ kWh = 3600 J
 * Definition: 1 Wh = 10⁻³ kWh = 3600 J
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
 * 热力学中常用的能量单位。
 * Energy unit commonly used in thermodynamics.
 *
 * 定义：1 cal = 4.184 J
 * Definition: 1 cal = 4.184 J
*/
object Calorie : DerivedPhysicalUnit(Joule * FltX("4.184")) {
    override val name = "calorie"
    override val symbol = "cal"

    override val quantity = Energy
}

/**
 * 千卡（大卡）
 * Kilocalorie (large calorie / food calorie)
 *
 * 常用于食品能量标示。
 * Commonly used for food energy labeling.
 *
 * 定义：1 kcal = 10³ cal = 4184 J
 * Definition: 1 kcal = 10³ cal = 4184 J
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
 * 英制能量单位，常用于暖通空调领域。
 * Imperial energy unit, commonly used in HVAC.
 *
 * 定义：1 BTU ≈ 1055.06 J
 * Definition: 1 BTU ≈ 1055.06 J
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
 * CGS 单位制中的能量单位。
 * Energy unit in the CGS system.
 *
 * 定义：1 erg = 10⁻⁷ J
 * Definition: 1 erg = 10⁻⁷ J
*/
object Erg : DerivedPhysicalUnit(Joule * FltX("1e-7")) {
    override val name = "erg"
    override val symbol = "erg"

    override val quantity = Energy
}
