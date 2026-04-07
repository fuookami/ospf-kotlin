/**
 * 角速度单位 / Angular velocity units
 *
 * 提供角速度量纲的 SI 单位定义，包括弧度每秒、度每秒等。
 * Provides SI unit definitions for angular velocity dimension, including radian per second,
 * degree per second, etc.
 *
 * 来源：SI 导出单位
 * Source: SI derived units
 * - RadianPerSecond: rad/s, SI derived unit for angular velocity
 * - DegreePerSecond: °/s, common unit for angular velocity
 */

package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.AngularVelocity

/**
 * 弧度每秒（SI 导出单位）
 * Radian per second (SI derived unit)
 *
 * 名称：弧度每秒
 * Name: radian per second
 *
 * 符号：rad/s
 * Symbol: rad/s
 *
 * 定义：1 rad/s = 1 rad / 1 s
 * Definition: 1 rad/s = 1 rad / 1 s
 *
 * 来源：SI 导出单位，角速度的国际单位
 * Source: SI derived unit, international unit for angular velocity
 */
object RadianPerSecond : DerivedPhysicalUnit(Radian / Second) {
    override val name = "radian per second"
    override val symbol = "rad/s"

    override val quantity = AngularVelocity
}

/**
 * 度每秒
 * Degree per second
 *
 * 名称：度每秒
 * Name: degree per second
 *
 * 符号：°/s
 * Symbol: °/s
 *
 * 定义：1 °/s = 1 ° / 1 s = π/180 rad/s
 * Definition: 1 °/s = 1 ° / 1 s = π/180 rad/s
 *
 * 来源：常用角速度单位，广泛用于工程和导航
 * Source: Common angular velocity unit, widely used in engineering and navigation
 */
object DegreePerSecond : DerivedPhysicalUnit(Degree / Second) {
    override val name = "degree per second"
    override val symbol = "°/s"

    override val quantity = AngularVelocity
}