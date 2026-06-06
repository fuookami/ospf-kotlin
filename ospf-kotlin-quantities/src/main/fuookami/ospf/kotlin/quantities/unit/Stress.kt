/**
 * 应力单位 / Stress units
 *
 * 提供应力量纲的单位定义，包括磅力每平方英寸、磅力每平方英尺、千克力每平方厘米、千克力每平方米等。
 * Provides unit definitions for stress dimension, including pound force per square inch,
 * pound force per square foot, kilogram force per square centimeter, kilogram force per square meter, etc.
 *
 * 来源：常用应力单位定义
 * Source: Common stress unit definitions
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Stress

/**
 * 磅力每平方英寸
 * Pound force per square inch
 *
 * 名称：磅力每平方英寸
 * Name: pound force per square inch
 *
 * 符号：psi
 * Symbol: psi
 *
 * 定义：1 psi = 1 lbf / 1 in²
 * Definition: 1 psi = 1 lbf / 1 in²
 *
 * 来源：英制应力单位，常用于美国工程领域
 * Source: Imperial stress unit, commonly used in US engineering
 */
object PoundForcePerSquareInch : DerivedPhysicalUnit(PoundForce / SquareInch) {
    override val name = "pound force per square inch"
    override val symbol = "psi"

    override val quantity = Stress
}

/**
 * 磅力每平方英尺
 * Pound force per square foot
 *
 * 名称：磅力每平方英尺
 * Name: pound force per square foot
 *
 * 符号：psf
 * Symbol: psf
 *
 * 定义：1 psf = 1 lbf / 1 ft²
 * Definition: 1 psf = 1 lbf / 1 ft²
 *
 * 来源：英制应力单位，常用于建筑结构计算
 * Source: Imperial stress unit, commonly used in structural engineering
 */
object PoundForcePerSquareFoot : DerivedPhysicalUnit(PoundForce / SquareFoot) {
    override val name = "pound force per square foot"
    override val symbol = "psf"

    override val quantity = Stress
}

/**
 * 千克力每平方厘米
 * Kilogram force per square centimeter
 *
 * 名称：千克力每平方厘米
 * Name: kilogram force per square centimeter
 *
 * 符号：kgf/cm²
 * Symbol: kgf/cm²
 *
 * 定义：1 kgf/cm² = 1 kgf / 1 cm²
 * Definition: 1 kgf/cm² = 1 kgf / 1 cm²
 *
 * 来源：工程单位制，常用于亚洲国家工程领域
 * Source: Engineering unit system, commonly used in Asian engineering
 */
object KilogramForcePerSquareCentimeter : DerivedPhysicalUnit(KilogramForce / SquareCentimeter) {
    override val name = "kilogram force per square centimeter"
    override val symbol = "kgf/cm2"

    override val quantity = Stress
}

/**
 * 千克力每平方米
 * Kilogram force per square meter
 *
 * 名称：千克力每平方米
 * Name: kilogram force per square meter
 *
 * 符号：kgf/m²
 * Symbol: kgf/m²
 *
 * 定义：1 kgf/m² = 1 kgf / 1 m²
 * Definition: 1 kgf/m² = 1 kgf / 1 m²
 *
 * 来源：工程单位制
 * Source: Engineering unit system
 */
object KilogramForcePerSquareMeter : DerivedPhysicalUnit(KilogramForce / SquareMeter) {
    override val name = "kilogram force per square meter"
    override val symbol = "kgf/m2"

    override val quantity = Stress
}

/**
 * 帕斯卡（应力）
 * Pascal (stress)
 *
 * 名称：帕斯卡（应力）
 * Name: pascal (stress)
 *
 * 符号：Pa
 * Symbol: Pa
 *
 * 定义：1 Pa = 1 N/m²
 * Definition: 1 Pa = 1 N/m²
 *
 * 来源：SI 导出应力单位，与压力帕斯卡量纲相同
 * Source: SI derived stress unit, same dimension as pressure pascal
 */
object PascalStress : DerivedPhysicalUnit(Newton / SquareMeter) {
    override val name = "pascal"
    override val symbol = "Pa"

    override val quantity = Stress
}

/**
 * 千帕（应力）
 * Kilopascal (stress)
 *
 * 名称：千帕（应力）
 * Name: kilopascal (stress)
 *
 * 符号：kPa
 * Symbol: kPa
 *
 * 定义：1 kPa = 10³ Pa
 * Definition: 1 kPa = 10³ Pa
 *
 * 来源：SI 导出应力单位，常用于材料力学
 * Source: SI derived stress unit, commonly used in mechanics of materials
 */
object KilopascalStress : DerivedPhysicalUnit(PascalStress * Scale.kilo) {
    override val name = "kilopascal"
    override val symbol = "kPa"

    override val quantity = Stress
}

/**
 * 兆帕（应力）
 * Megapascal (stress)
 *
 * 名称：兆帕（应力）
 * Name: megapascal (stress)
 *
 * 符号：MPa
 * Symbol: MPa
 *
 * 定义：1 MPa = 10⁶ Pa
 * Definition: 1 MPa = 10⁶ Pa
 *
 * 来源：SI 导出应力单位，常用于结构工程和材料科学
 * Source: SI derived stress unit, commonly used in structural engineering and materials science
 */
object MegapascalStress : DerivedPhysicalUnit(PascalStress * Scale.mega) {
    override val name = "megapascal"
    override val symbol = "MPa"

    override val quantity = Stress
}
