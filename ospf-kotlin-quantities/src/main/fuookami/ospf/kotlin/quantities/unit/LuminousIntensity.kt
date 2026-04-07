/**
 * 发光强度单位 / Luminous intensity units
 *
 * 用于测量光源发光强度的单位。
 * Units for measuring the luminous intensity of light sources.
 *
 * 单位常量来源 / Unit constant sources:
 * - Candela: SI基本单位（国际单位制）/ Candela: SI base unit (International System of Units)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.LuminousIntensity

/**
 * 坎德拉（基本单位）/ Candela (base unit)
 *
 * 发光强度的SI基本单位，定义为频率为540×10¹²Hz的单色辐射在给定方向上的发光强度，
 * 其辐射强度在该方向上为1/683瓦特每球面度。
 * The SI base unit of luminous intensity, defined as the luminous intensity in a given direction
 * of a source that emits monochromatic radiation of frequency 540×10¹²Hz with a radiant intensity
 * of 1/683 watt per steradian in that direction.
 *
 * 符号 / Symbol: cd
 * 换算关系 / Conversion: 1 cd = 1 cd（基本单位）/ 1 cd = 1 cd (base unit)
 */
object Candela : PhysicalUnit() {
    /** 单位名称：candela / Unit name: candela */
    override val name: String = "candela"
    /** 单位符号：cd / Unit symbol: cd */
    override val symbol: String = "cd"

    /** 对应物理量：发光强度 / Corresponding quantity: LuminousIntensity */
    override val quantity = LuminousIntensity
    /** 比例因子：1（基本单位）/ Scale factor: 1 (base unit) */
    override val scale = Scale()
}