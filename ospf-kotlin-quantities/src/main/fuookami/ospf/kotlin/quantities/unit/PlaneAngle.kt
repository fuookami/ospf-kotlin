/**
 * 平面角单位 / Plane angle units
 *
 * 用于测量平面内角度的单位。
 * Units for measuring angles in a plane.
 *
 * 单位常量来源 / Unit constant sources:
 * - Radian: SI导出单位，定义为弧长等于半径的圆心角 / SI derived unit, defined as the angle subtended by an arc equal in length to the radius
 * - Degree: 1° = π/180 rad
 * - Gradian: 1 gon = π/200 rad
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.PlaneAngle

/**
 * 弧度（基本单位）/ Radian (base unit)
 *
 * 平面角的SI导出单位，定义为圆周上弧长等于半径的圆心角。
 * The SI derived unit of plane angle, defined as the angle subtended at the center
 * of a circle by an arc equal in length to the radius.
 *
 * 符号 / Symbol: rad
 * 换算关系 / Conversion: 1 rad = 1 rad（基本单位）/ 1 rad = 1 rad (base unit)
 */
object Radian : PhysicalUnit() {
    /** 单位名称：radian / Unit name: radian */
    override val name = "radian"
    /** 单位符号：rad / Unit symbol: rad */
    override val symbol = "rad"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
    /** 比例因子：1（基本单位）/ Scale factor: 1 (base unit) */
    override val scale = Scale()
}

/**
 * 毫弧度 / Milliradian
 *
 * 千分之一弧度。
 * One thousandth of a radian.
 *
 * 符号 / Symbol: mrad
 * 换算关系 / Conversion: 1 mrad = 10⁻³ rad
 */
object Milliradian : DerivedPhysicalUnit(Radian * Scale.milli) {
    /** 单位名称：milliradian / Unit name: milliradian */
    override val name = "milliradian"
    /** 单位符号：mrad / Unit symbol: mrad */
    override val symbol = "mrad"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 周角 / Round angle
 *
 * 完整圆周的角度。
 * The angle of a complete circle.
 *
 * 符号 / Symbol: round angle
 * 换算关系 / Conversion: 1 round angle = 2π rad = 360°
 */
object RoundAngle : DerivedPhysicalUnit(Radian * (FltX.two * FltX.pi)) {
    /** 单位名称：round angle / Unit name: round angle */
    override val name = "round angle"
    /** 单位符号：round angle / Unit symbol: round angle */
    override val symbol = "round angle"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 直角 / Right angle
 *
 * 四分之一圆周的角度。
 * The angle of a quarter circle.
 *
 * 符号 / Symbol: right angle
 * 换算关系 / Conversion: 1 right angle = π/2 rad = 90°
 */
object RightAngle : DerivedPhysicalUnit(Radian / FltX(4.0)) {
    /** 单位名称：right angle / Unit name: right angle */
    override val name = "right angle"
    /** 单位符号：right angle / Unit symbol: right angle */
    override val symbol = "right angle"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 度 / Degree
 *
 * 最常用的角度单位，将圆周分为360等份。
 * The most commonly used angle unit, dividing a circle into 360 equal parts.
 *
 * 符号 / Symbol: °
 * 换算关系 / Conversion: 1° = π/180 rad ≈ 0.01745 rad
 */
object Degree : DerivedPhysicalUnit(RoundAngle / FltX(360.0)) {
    /** 单位名称：degree / Unit name: degree */
    override val name = "degree"
    /** 单位符号：° / Unit symbol: ° */
    override val symbol = "°"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 角分 / Minute of arc
 *
 * 度的六十分之一。
 * One sixtieth of a degree.
 *
 * 符号 / Symbol: '
 * 换算关系 / Conversion: 1' = 1/60° = π/10800 rad
 */
object MinuteAngle : DerivedPhysicalUnit(Degree / FltX(60.0)) {
    /** 单位名称：minute angle / Unit name: minute angle */
    override val name = "minute angle"
    /** 单位符号：' / Unit symbol: ' */
    override val symbol = "'"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 角秒 / Second of arc
 *
 * 角分的六十分之一。
 * One sixtieth of a minute of arc.
 *
 * 符号 / Symbol: ''
 * 换算关系 / Conversion: 1'' = 1/60' = 1/3600° = π/648000 rad
 */
object SecondAngle : DerivedPhysicalUnit(MinuteAngle / FltX(60.0)) {
    /** 单位名称：second angle / Unit name: second angle */
    override val name = "second angle"
    /** 单位符号：'' / Unit symbol: '' */
    override val symbol = "''"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}

/**
 * 百分度 / Gradian
 *
 * 将直角分为100等份的角度单位。
 * An angle unit dividing a right angle into 100 equal parts.
 *
 * 符号 / Symbol: gon
 * 换算关系 / Conversion: 1 gon = 1/100 right angle = π/200 rad = 0.9°
 */
object Gradian : DerivedPhysicalUnit(RightAngle / FltX(100.0)) {
    /** 单位名称：gradian / Unit name: gradian */
    override val name = "gradian"
    /** 单位符号：gon / Unit symbol: gon */
    override val symbol = "gon"

    /** 对应物理量：平面角 / Corresponding quantity: PlaneAngle */
    override val quantity = PlaneAngle
}