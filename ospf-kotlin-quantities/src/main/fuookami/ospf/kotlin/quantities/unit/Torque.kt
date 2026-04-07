/**
 * 扭矩单位 / Torque units
 *
 * 提供扭矩量纲的单位定义，包括牛顿米、千克力米等。
 * Provides unit definitions for torque dimension, including newton meter, kilogram force meter, etc.
 *
 * 来源：SI 导出单位及常用扭矩单位定义
 * Source: SI derived unit and common torque unit definitions
 * - Newton meter: N·m, SI derived unit for torque
 * - Kilogram force meter: kgf·m, engineering unit for torque
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Torque

/**
 * 牛顿米（SI 导出单位）
 * Newton meter (SI derived unit)
 *
 * 名称：牛顿米
 * Name: newton meter
 *
 * 符号：N·m
 * Symbol: N·m
 *
 * 定义：1 N·m = 1 N × 1 m
 * Definition: 1 N·m = 1 N × 1 m
 *
 * 来源：SI 导出单位，扭矩的国际单位
 * Source: SI derived unit, international unit for torque
 */
object NewtonMeter : DerivedPhysicalUnit(Newton * Meter) {
    override val name = "newton meter"
    override val symbol = "N·m"

    override val quantity = Torque
}

/**
 * 千克力米
 * Kilogram force meter
 *
 * 名称：千克力米
 * Name: kilogram force meter
 *
 * 符号：kgf·m
 * Symbol: kgf·m
 *
 * 定义：1 kgf·m = 1 kgf × 1 m ≈ 9.80665 N·m
 * Definition: 1 kgf·m = 1 kgf × 1 m ≈ 9.80665 N·m
 *
 * 来源：工程单位制，常用于机械工程
 * Source: Engineering unit system, commonly used in mechanical engineering
 */
object KilogramForceMeter : DerivedPhysicalUnit(KilogramForce * Meter) {
    override val name = "kilogram meter"
    override val symbol = "kgf·m"

    override val quantity = Torque
}