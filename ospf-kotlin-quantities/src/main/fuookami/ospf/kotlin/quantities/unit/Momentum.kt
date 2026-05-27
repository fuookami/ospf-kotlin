/**
 * 动量单位 / Momentum units
 *
 * 用于测量物体动量（质量乘以速度）的单位。
 * Units for measuring the momentum of objects (mass times velocity).
 *
 * 单位常量来源 / Unit constant sources:
 * - KilogramMeterPerSecond: SI导出单位 / SI derived unit
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Momentum

/**
 * 千克米每秒 / Kilogram meter per second
 *
 * 动量的SI导出单位。
 * The SI derived unit of momentum.
 *
 * 符号 / Symbol: kg·m/s
 * 换算关系 / Conversion: 1 kg·m/s = 1 N·s（牛顿秒）/ 1 kg·m/s = 1 N·s (newton second)
 */
object KilogramMeterPerSecond : DerivedPhysicalUnit(Kilogram * Meter / Second) {
    /** 单位名称：kilogram meter per second / Unit name: kilogram meter per second */
    override val name = "kilogram meter per second"
    /** 单位符号：kg·m/s / Unit symbol: kg·m/s */
    override val symbol = "kg·m/s"

    /** 对应物理量：动量 / Corresponding quantity: Momentum */
    override val quantity = Momentum
}
