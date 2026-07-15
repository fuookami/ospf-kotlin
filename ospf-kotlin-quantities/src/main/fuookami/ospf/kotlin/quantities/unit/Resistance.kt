/**
 * 电阻单位 / Electrical resistance units
 *
 * 用于测量电阻的单位。
 * Units for measuring electrical resistance.
 *
 * 单位常量来源 / Unit constant sources:
 * - Ohm: SI导出单位，定义为伏特每安培 / SI derived unit, defined as volt per ampere
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Resistance

/**
 * 欧姆 / Ohm
 *
 * 电阻的SI导出单位，定义为伏特每安培。
 * The SI derived unit of electrical resistance, defined as volt per ampere.
 *
 * 符号 / Symbol: Ω
 * 换算关系 / Conversion: 1 Ω = 1 V/A = 1 kg·m²/(s³·A²)
*/
object Ohm : DerivedPhysicalUnit(Volt / Ampere) {

    /** 单位名称：ohm / Unit name: ohm */
    override val name = "ohm"

    /** 单位符号：Ω / Unit symbol: Ω */
    override val symbol = "Ω"

    /** 对应物理量：电阻 / Corresponding quantity: Resistance */
    override val quantity = Resistance
}

/**
 * 千欧 / Kiloohm
 *
 * 一千欧姆。
 * One thousand ohms.
 *
 * 符号 / Symbol: kΩ
 * 换算关系 / Conversion: 1 kΩ = 10³ Ω = 1000 Ω
*/
object Kiloohm : DerivedPhysicalUnit(Ohm * Scale.kilo) {

    /** 单位名称：kiloohm / Unit name: kiloohm */
    override val name = "kiloohm"

    /** 单位符号：kΩ / Unit symbol: kΩ */
    override val symbol = "kΩ"

    /** 对应物理量：电阻 / Corresponding quantity: Resistance */
    override val quantity = Resistance
}

/**
 * 兆欧 / Megaohm
 *
 * 一百万欧姆。
 * One million ohms.
 *
 * 符号 / Symbol: MΩ
 * 换算关系 / Conversion: 1 MΩ = 10⁶ Ω = 1000000 Ω
*/
object Megaohm : DerivedPhysicalUnit(Ohm * Scale.mega) {

    /** 单位名称：megaohm / Unit name: megaohm */
    override val name = "megaohm"

    /** 单位符号：MΩ / Unit symbol: MΩ */
    override val symbol = "MΩ"

    /** 对应物理量：电阻 / Corresponding quantity: Resistance */
    override val quantity = Resistance
}
