/**
 * 角加速度单位 / Angular acceleration units
 *
 * 提供角加速度量纲的 SI 单位定义，包括弧度每二次方秒、度每二次方秒等。
 * Provides SI unit definitions for angular acceleration dimension, including radian per second squared,
 * degree per second squared, etc.
 *
 * 来源：SI 导出单位
 * Source: SI derived units
 * - RadianPerSecondSquared: rad/s², SI derived unit for angular acceleration
 * - DegreePerSecondSquared: °/s², common unit for angular acceleration
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.AngularAcceleration

/**
 * 弧度每二次方秒（SI 导出单位）
 * Radian per second squared (SI derived unit)
 *
 * 名称：弧度每二次方秒
 * Name: radian per second squared
 *
 * 符号：rad/s²
 * Symbol: rad/s²
 *
 * 定义：1 rad/s² = 1 rad/s / 1 s
 * Definition: 1 rad/s² = 1 rad/s / 1 s
 *
 * 来源：SI 导出单位，角加速度的国际单位
 * Source: SI derived unit, international unit for angular acceleration
 */
object RadianPerSecondSquared : DerivedPhysicalUnit(RadianPerSecond / Second) {
    override val name = "radian per second squared"
    override val symbol = "rad/s²"

    override val quantity = AngularAcceleration
}

/**
 * 度每二次方秒
 * Degree per second squared
 *
 * 名称：度每二次方秒
 * Name: degree per second squared
 *
 * 符号：°/s²
 * Symbol: °/s²
 *
 * 定义：1 °/s² = 1 °/s / 1 s = π/180 rad/s²
 * Definition: 1 °/s² = 1 °/s / 1 s = π/180 rad/s²
 *
 * 来源：常用角加速度单位，广泛用于工程和导航
 * Source: Common angular acceleration unit, widely used in engineering and navigation
 */
object DegreePerSecondSquared : DerivedPhysicalUnit(DegreePerSecond / Second) {
    override val name = "degree per second squared"
    override val symbol = "°/s²"

    override val quantity = AngularAcceleration
}
