/**
 * 力单位
 * Force units
 *
 * 提供力量纲的 SI 单位和英制单位定义，包括牛顿、千克力、达因、磅力等。
 * Provides SI and imperial unit definitions for force dimension, including newton, kilogram-force, dyne, pound-force, etc.
 *
 * 单位常量来源 / Unit constant sources:
 * - Newton: SI 基本单位 (1 N = 1 kg·m/s²) / SI base unit (1 N = 1 kg·m/s²)
 * - KilogramForce: 标准重力 g = 9.80665 m/s² (ISO 80000-4) / Standard gravity g = 9.80665 m/s² (ISO 80000-4)
 * - PoundForce: 1 lbf = 4.4482216152605 N (NIST Special Publication 814)
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Force

/**
 * 牛顿
 * Newton
 *
 * 力的 SI 导出单位。
 * SI derived unit for force.
 *
 * 定义：1 N = 1 kg × 1 m/s²
 * Definition: 1 N = 1 kg × 1 m/s²
*/
object Newton : DerivedPhysicalUnit(Kilogram * MeterPerSecondSquared) {
    override val name = "newton"
    override val symbol = "N"

    override val quantity = Force
}

/**
 * 千牛
 * Kilonewton
 *
 * 一千牛顿，常用于结构工程。
 * One thousand newtons, commonly used in structural engineering.
 *
 * 定义：1 kN = 10³ N
 * Definition: 1 kN = 10³ N
*/
object Kilonewton : DerivedPhysicalUnit(Newton * Scale.kilo) {
    override val name = "kilonewton"
    override val symbol = "kN"

    override val quantity = Force
}

/**
 * 兆牛
 * Meganewton
 *
 * 一百万牛顿，常用于大型结构工程。
 * One million newtons, commonly used in large structural engineering.
 *
 * 定义：1 MN = 10⁶ N
 * Definition: 1 MN = 10⁶ N
*/
object Meganewton : DerivedPhysicalUnit(Newton * Scale.mega) {
    override val name = "meganewton"
    override val symbol = "MN"

    override val quantity = Force
}

/**
 * 千克力
 * Kilogram-force
 *
 * 重力单位，表示 1 千克物体在标准重力加速度下受到的重力。
 * Gravitational unit, representing the force on 1 kg under standard gravity.
 *
 * 定义：1 kgf = 1 kg × 9.80665 m/s² = 9.80665 N
 * Definition: 1 kgf = 1 kg × 9.80665 m/s² = 9.80665 N
*/
object KilogramForce : DerivedPhysicalUnit(Newton * 9.80665) {
    override val name = "kilogram force"
    override val symbol = "kgf"

    override val quantity = Force
}

/**
 * 克力
 * Gram-force
 *
 * 定义：1 gf = 10⁻³ kgf = 9.80665 × 10⁻³ N
 * Definition: 1 gf = 10⁻³ kgf = 9.80665 × 10⁻³ N
*/
object GramForce : DerivedPhysicalUnit(KilogramForce / Scale.kilo) {
    override val name = "gram force"
    override val symbol = "gf"

    override val quantity = Force
}

/**
 * 达因
 * Dyne
 *
 * CGS 单位制中的力单位。
 * Force unit in the CGS system.
 *
 * 定义：1 dyn = 1 g × 1 cm/s² = 10⁻⁵ N
 * Definition: 1 dyn = 1 g × 1 cm/s² = 10⁻⁵ N
*/
object Dyne : DerivedPhysicalUnit(Gram * CentimeterPerSecondSquared) {
    override val name = "dyne"
    override val symbol = "dyn"

    override val quantity = Force
}

/**
 * 磅力
 * Pound-force
 *
 * 英制力单位。
 * Imperial unit for force.
 *
 * 定义：1 lbf = 4.4482216152605 N
 * Definition: 1 lbf = 4.4482216152605 N
*/
object PoundForce : DerivedPhysicalUnit(Newton * 4.4482216152605) {
    override val name = "pound force"
    override val symbol = "lbf"

    override val quantity = Force
}

/**
 * 千磅力
 * Kilopound-force
 *
 * 定义：1 klbf = 10³ lbf
 * Definition: 1 klbf = 10³ lbf
*/
object KilopoundForce : DerivedPhysicalUnit(PoundForce * Scale.kilo) {
    override val name = "kilopound force"
    override val symbol = "klbf"

    override val quantity = Force
}
