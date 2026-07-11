/**
 * 速度单位 / Velocity units
 *
 * 提供速度量纲的单位定义，包括米每秒、厘米每秒、千米每秒、千米每小时、马赫、节、光速、
 * 英寸每秒、英尺每秒、英里每小时等。
 * Provides unit definitions for velocity dimension, including meter per second, centimeter per second,
 * kilometer per second, kilometer per hour, mach, knot, light speed, inch per second, foot per second,
 * mile per hour, etc.
 *
 * 来源：SI 导出单位及常用速度单位定义
 * Source: SI derived units and common velocity unit definitions
 * - MeterPerSecond: m/s, SI derived unit for velocity
 * - Mach: Speed of sound at sea level (≈340.3 m/s)
 * - LightSpeed: c = 299,792,458 m/s (exact)
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Velocity

/**
 * 厘米每秒
 * Centimeter per second
 *
 * 名称：厘米每秒
 * Name: centimeter per second
 *
 * 符号：cm/s
 * Symbol: cm/s
 *
 * 定义：1 cm/s = 1 cm / 1 s = 10⁻² m/s
 * Definition: 1 cm/s = 1 cm / 1 s = 10⁻² m/s
 *
 * 来源：SI 导出速度单位
 * Source: SI derived velocity unit
*/
object CentimeterPerSecond : DerivedPhysicalUnit(Centimeter / Second) {
    override val name = "centimeter per second"
    override val symbol = "cmps"

    override val quantity = Velocity
}

/**
 * 米每秒（SI 导出单位）
 * Meter per second (SI derived unit)
 *
 * 名称：米每秒
 * Name: meter per second
 *
 * 符号：m/s
 * Symbol: m/s
 *
 * 定义：1 m/s = 1 m / 1 s
 * Definition: 1 m/s = 1 m / 1 s
 *
 * 来源：SI 导出单位，速度的国际单位
 * Source: SI derived unit, international unit for velocity
*/
object MeterPerSecond : DerivedPhysicalUnit(Meter / Second) {
    override val name = "meter per second"
    override val symbol = "mps"

    override val quantity = Velocity
}

/**
 * 千米每秒
 * Kilometer per second
 *
 * 名称：千米每秒
 * Name: kilometer per second
 *
 * 符号：km/s
 * Symbol: km/s
 *
 * 定义：1 km/s = 1 km / 1 s = 10³ m/s
 * Definition: 1 km/s = 1 km / 1 s = 10³ m/s
 *
 * 来源：SI 导出速度单位，常用于航天和天文学
 * Source: SI derived velocity unit, commonly used in aerospace and astronomy
*/
object KilometerPerSecond : DerivedPhysicalUnit(Kilometer / Second) {
    override val name = "kilometer per second"
    override val symbol = "kmps"

    override val quantity = Velocity
}

/**
 * 千米每小时
 * Kilometer per hour
 *
 * 名称：千米每小时
 * Name: kilometer per hour
 *
 * 符号：km/h
 * Symbol: km/h
 *
 * 定义：1 km/h = 1 km / 1 h = 1/3.6 m/s
 * Definition: 1 km/h = 1 km / 1 h = 1/3.6 m/s
 *
 * 来源：常用速度单位，广泛用于车辆速度表示
 * Source: Common velocity unit, widely used for vehicle speed representation
*/
object KilometerPerHour : DerivedPhysicalUnit(Kilometer / Hour) {
    override val name = "kilometers per hour"
    override val symbol = "kmph"

    override val quantity = Velocity
}

/**
 * 马赫
 * Mach
 *
 * 名称：马赫
 * Name: mach
 *
 * 符号：Ma
 * Symbol: Ma
 *
 * 定义：1 Ma ≈ 340.3 m/s（海平面标准大气压下的音速）
 * Definition: 1 Ma ≈ 340.3 m/s (speed of sound at sea level standard atmospheric pressure)
 *
 * 来源：航空领域常用单位，以恩斯特·马赫命名
 * Source: Common unit in aviation, named after Ernst Mach
*/
object Mach : DerivedPhysicalUnit(MeterPerSecond * 340.3) {
    override val name = "mach"
    override val symbol = "ma"

    override val quantity = Velocity
}

/**
 * 节（国际海里）
 * Knot (International nautical mile)
 *
 * 名称：节
 * Name: knot
 *
 * 符号：kn
 * Symbol: kn
 *
 * 定义：1 kn = 1 海里 / 1 小时 ≈ 0.514444 m/s
 * Definition: 1 kn = 1 nautical mile / 1 hour ≈ 0.514444 m/s
 *
 * 来源：航海和航空领域常用速度单位
 * Source: Common velocity unit in maritime and aviation
*/
object Knot : DerivedPhysicalUnit(NauticalMile / Hour) {
    override val name = "knot"
    override val symbol = "kn"

    override val quantity = Velocity
}

/**
 * 法国节
 * French knot
 *
 * 名称：法国节
 * Name: French knot
 *
 * 符号：fr.kn
 * Symbol: fr.kn
 *
 * 定义：基于法国海里定义的节
 * Definition: Knot based on French nautical mile definition
 *
 * 来源：法国使用的航海速度单位
 * Source: Maritime velocity unit used in France
*/
object FRKnot : DerivedPhysicalUnit(FRNauticalMile / Hour) {
    override val name = "fr knot"
    override val symbol = "fr.kn"

    override val quantity = Velocity
}

/**
 * 英国节
 * UK knot
 *
 * 名称：英国节
 * Name: UK knot
 *
 * 符号：uk.kn
 * Symbol: uk.kn
 *
 * 定义：基于英国海里定义的节
 * Definition: Knot based on UK nautical mile definition
 *
 * 来源：英国使用的航海速度单位
 * Source: Maritime velocity unit used in the UK
*/
object UKKnot : DerivedPhysicalUnit(UKNauticalMile / Hour) {
    override val name = "uk knot"
    override val symbol = "uk.kn"

    override val quantity = Velocity
}

/**
 * 俄罗斯节
 * Russian knot
 *
 * 名称：俄罗斯节
 * Name: Russian knot
 *
 * 符号：ru.kn
 * Symbol: ru.kn
 *
 * 定义：基于俄罗斯海里定义的节
 * Definition: Knot based on Russian nautical mile definition
 *
 * 来源：俄罗斯使用的航海速度单位
 * Source: Maritime velocity unit used in Russia
*/
object RUKnot : DerivedPhysicalUnit(RUNauticalMile / Hour) {
    override val name = "ru knot"
    override val symbol = "ru.kn"

    override val quantity = Velocity
}

/**
 * 美国节
 * US knot
 *
 * 名称：美国节
 * Name: US knot
 *
 * 符号：us.kn
 * Symbol: us.kn
 *
 * 定义：基于美国海里定义的节
 * Definition: Knot based on US nautical mile definition
 *
 * 来源：美国使用的航海速度单位
 * Source: Maritime velocity unit used in the US
*/
object USKnot : DerivedPhysicalUnit(USNauticalMile / Hour) {
    override val name = "us knot"
    override val symbol = "us.kn"

    override val quantity = Velocity
}

/**
 * 光速
 * Light speed
 *
 * 名称：光速
 * Name: light speed
 *
 * 符号：c
 * Symbol: c
 *
 * 定义：c = 299,792,458 m/s（精确值）
 * Definition: c = 299,792,458 m/s (exact value)
 *
 * 来源：物理学基本常数，相对论中的速度上限
 * Source: Fundamental constant in physics, speed limit in relativity
*/
object LightSpeed : DerivedPhysicalUnit(LightSecond / Second) {
    override val name = "lightspeed"
    override val symbol = "c"

    override val quantity = Velocity
}

/**
 * 英寸每秒
 * Inch per second
 *
 * 名称：英寸每秒
 * Name: inch per second
 *
 * 符号：in/s
 * Symbol: in/s
 *
 * 定义：1 in/s = 1 in / 1 s ≈ 0.0254 m/s
 * Definition: 1 in/s = 1 in / 1 s ≈ 0.0254 m/s
 *
 * 来源：英制速度单位
 * Source: Imperial velocity unit
*/
object InchPerSecond : DerivedPhysicalUnit(Inch / Second) {
    override val name = "inch per second"
    override val symbol = "ips"

    override val quantity = Velocity
}

/**
 * 英尺每秒
 * Foot per second
 *
 * 名称：英尺每秒
 * Name: foot per second
 *
 * 符号：ft/s
 * Symbol: ft/s
 *
 * 定义：1 ft/s = 1 ft / 1 s ≈ 0.3048 m/s
 * Definition: 1 ft/s = 1 ft / 1 s ≈ 0.3048 m/s
 *
 * 来源：英制速度单位，常用于美国工程领域
 * Source: Imperial velocity unit, commonly used in US engineering
*/
object FootPerSecond : DerivedPhysicalUnit(Foot / Second) {
    override val name = "foot per second"
    override val symbol = "fps"

    override val quantity = Velocity
}

/**
 * 英里每小时
 * Mile per hour
 *
 * 名称：英里每小时
 * Name: mile per hour
 *
 * 符号：mph
 * Symbol: mph
 *
 * 定义：1 mph = 1 mile / 1 h ≈ 0.44704 m/s
 * Definition: 1 mph = 1 mile / 1 h ≈ 0.44704 m/s
 *
 * 来源：英制速度单位，广泛用于美国和英国的车辆速度表示
 * Source: Imperial velocity unit, widely used for vehicle speed in the US and UK
*/
object MilePerHour : DerivedPhysicalUnit(Mile / Hour) {
    override val name = "mile per hour"
    override val symbol = "mph"

    override val quantity = Velocity
}

/** 兼容别名：千米每秒（复数形式）/ Compatibility alias: kilometers per second (plural form) */
typealias KilometersPerSecond = KilometerPerSecond
