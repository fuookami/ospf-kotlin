/**
 * 流量单位
 * Flow rate units
 *
 * 提供流量量纲的单位定义，包括立方米每秒、升每秒等。
 * Provides unit definitions for flow rate dimension, including cubic meter per second, liter per second, etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.FlowRate

/**
 * 立方米每秒
 * Cubic meter per second
 *
 * 流量的 SI 导出单位。
 * SI derived unit for flow rate.
 *
 * 定义：1 m³/s = 1 m³ / 1 s
 * Definition: 1 m³/s = 1 m³ / 1 s
 */
object CubicMeterPerSecond : DerivedPhysicalUnit(CubicMeter / Second) {
    override val name = "cubic meter per second"
    override val symbol = "m3ps"

    override val quantity = FlowRate
}

/**
 * 升每秒
 * Liter per second
 *
 * 定义：1 L/s = 10⁻³ m³/s
 * Definition: 1 L/s = 10⁻³ m³/s
 */
object LiterPerSecond : DerivedPhysicalUnit(Liter / Second) {
    override val name = "liter per second"
    override val symbol = "Lps"

    override val quantity = FlowRate
}