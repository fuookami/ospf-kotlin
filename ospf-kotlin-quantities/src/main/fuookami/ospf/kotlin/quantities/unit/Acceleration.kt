/**
 * 加速度单位 / Acceleration units
 *
 * 提供加速度量纲的 SI 单位定义，包括米每秒平方、千米每秒平方、标准重力加速度等。
 * Provides SI unit definitions for acceleration dimension, including meter per second squared,
 * kilometer per second squared, standard gravity, etc.
 *
 * 来源：SI 基本单位定义
 * Source: SI base unit definitions
 * - Standard gravity (g): 9.80665 m/s² (ISO 80000-3)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.dimension.Acceleration

/**
 * 米每秒平方（SI 导出单位）
 * Meter per second squared (SI derived unit)
 *
 * 名称：米每秒平方
 * Name: meter per second squared
 *
 * 符号：m/s²
 * Symbol: m/s²
 *
 * 定义：1 m/s² = 1 m/s / 1 s
 * Definition: 1 m/s² = 1 m/s / 1 s
 *
 * 来源：SI 导出单位，加速度的国际单位
 * Source: SI derived unit, international unit for acceleration
 */
object MeterPerSecondSquared : DerivedPhysicalUnit(MeterPerSecond / Second) {
    override val name = "meter per second squared"
    override val symbol = "mps2"

    override val quantity = Acceleration
}

/**
 * 厘米每秒平方
 * Centimeter per second squared
 *
 * 名称：厘米每秒平方
 * Name: centimeter per second squared
 *
 * 符号：cm/s²
 * Symbol: cm/s²
 *
 * 定义：1 cm/s² = 1 cm/s / 1 s = 10⁻² m/s²
 * Definition: 1 cm/s² = 1 cm/s / 1 s = 10⁻² m/s²
 *
 * 来源：SI 导出加速度单位，常用于精密测量
 * Source: SI derived acceleration unit, commonly used in precision measurements
 */
object CentimeterPerSecondSquared : DerivedPhysicalUnit(CentimeterPerSecond / Second) {
    override val name = "centimeter per second squared"
    override val symbol = "cmps2"

    override val quantity = Acceleration
}

/**
 * 千米每秒平方
 * Kilometer per second squared
 *
 * 名称：千米每秒平方
 * Name: kilometer per second squared
 *
 * 符号：km/s²
 * Symbol: km/s²
 *
 * 定义：1 km/s² = 1 km/s / 1 s = 10³ m/s²
 * Definition: 1 km/s² = 1 km/s / 1 s = 10³ m/s²
 *
 * 来源：SI 导出加速度单位，常用于航天领域
 * Source: SI derived acceleration unit, commonly used in aerospace
 */
object KilometerPerSecondSquared : DerivedPhysicalUnit(KilometerPerSecond / Second) {
    override val name = "kilometer per second squared"
    override val symbol = "kmps2"

    override val quantity = Acceleration
}

/**
 * 英寸每秒平方
 * Inch per second squared
 *
 * 名称：英寸每秒平方
 * Name: inch per second squared
 *
 * 符号：in/s²
 * Symbol: in/s²
 *
 * 定义：1 in/s² = 1 in/s / 1 s ≈ 0.0254 m/s²
 * Definition: 1 in/s² = 1 in/s / 1 s ≈ 0.0254 m/s²
 *
 * 来源：英制加速度单位
 * Source: Imperial acceleration unit
 */
object InchPerSecondSquared : DerivedPhysicalUnit(InchPerSecond / Second) {
    override val name = "inch per second squared"
    override val symbol = "ips2"

    override val quantity = Acceleration
}

/**
 * 英尺每秒平方
 * Foot per second squared
 *
 * 名称：英尺每秒平方
 * Name: foot per second squared
 *
 * 符号：ft/s²
 * Symbol: ft/s²
 *
 * 定义：1 ft/s² = 1 ft/s / 1 s ≈ 0.3048 m/s²
 * Definition: 1 ft/s² = 1 ft/s / 1 s ≈ 0.3048 m/s²
 *
 * 来源：英制加速度单位，常用于美国工程领域
 * Source: Imperial acceleration unit, commonly used in US engineering
 */
object FootPerSecondSquared : DerivedPhysicalUnit(FootPerSecond / Second) {
    override val name = "foot per second squared"
    override val symbol = "fps2"

    override val quantity = Acceleration
}

/**
 * 标准重力加速度
 * Standard gravity
 *
 * 名称：标准重力加速度
 * Name: standard gravity
 *
 * 符号：g
 * Symbol: g
 *
 * 定义：g = 9.80665 m/s²
 * Definition: g = 9.80665 m/s²
 *
 * 来源：ISO 80000-3 标准重力加速度定义，地球表面的平均重力加速度
 * Source: ISO 80000-3 standard gravity definition, average gravitational acceleration at Earth's surface
 */
object StandardGravity : DerivedPhysicalUnit(MeterPerSecondSquared * FltX(9.80665)) {
    override val name = "standard gravity"
    override val symbol = "g"

    override val quantity = Acceleration
}

/** 兼容别名：米每二次方秒 / Compatibility alias: meter per square second */
typealias MeterPerSquareSecond = MeterPerSecondSquared

/** 兼容别名：厘米每二次方秒 / Compatibility alias: centimeter per square second */
typealias CentimeterPerSquareSecond = CentimeterPerSecondSquared

/** 兼容别名：千米每二次方秒 / Compatibility alias: kilometer per square second */
typealias KilometerPerSquareSecond = KilometerPerSecondSquared

/** 兼容别名：英寸每二次方秒 / Compatibility alias: inch per square second */
typealias InchPerSquareSecond = InchPerSecondSquared

/** 兼容别名：英尺每二次方秒 / Compatibility alias: foot per square second */
typealias FootPerSquareSecond = FootPerSecondSquared