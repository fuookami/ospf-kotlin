/**
 * 立体角单位 / Solid angle units
 *
 * 用于测量三维空间中角度的单位。
 * Units for measuring angles in three-dimensional space.
 *
 * 单位常量来源 / Unit constant sources:
 * - Steradian: SI导出单位，定义为半径为1的球面上面积为1的球面所对的立体角 / SI derived unit, defined as the solid angle subtended by a surface of area 1 on a sphere of radius 1
 * - SquareDegree: 1 deg² = (π/180)² sr ≈ 0.000304617 sr
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.SolidAngle

/**
 * 球面度（基本单位）/ Steradian (base unit)
 *
 * 立体角的SI导出单位，定义为半径为1的球面上面积为1的球面所对的立体角。
 * The SI derived unit of solid angle, defined as the solid angle subtended by a surface
 * of area 1 on a sphere of radius 1.
 *
 * 符号 / Symbol: sr
 * 换算关系 / Conversion: 1 sr = 1 sr（基本单位）/ 1 sr = 1 sr (base unit)
 */
object Steradian : PhysicalUnit() {
    /** 单位名称：steradian / Unit name: steradian */
    override val name = "steradian"
    /** 单位符号：sr / Unit symbol: sr */
    override val symbol = "sr"

    /** 对应物理量：立体角 / Corresponding quantity: SolidAngle */
    override val quantity = SolidAngle
    /** 比例因子：1（基本单位）/ Scale factor: 1 (base unit) */
    override val scale = Scale()
}

/**
 * 平方度 / Square degree
 *
 * 以平方度表示的立体角单位。
 * A solid angle unit expressed in square degrees.
 *
 * 符号 / Symbol: deg²
 * 换算关系 / Conversion: 1 deg² = (π/180)² sr ≈ 0.000304617 sr, 1 sr = (180/π)² deg² ≈ 3282.806 deg²
 */
object SquareDegree : DerivedPhysicalUnit(Steradian * 0.00030461741978670857) {
    /** 单位名称：square degree / Unit name: square degree */
    override val name = "square degree"
    /** 单位符号：deg² / Unit symbol: deg² */
    override val symbol = "deg²"

    /** 对应物理量：立体角 / Corresponding quantity: SolidAngle */
    override val quantity = SolidAngle
}