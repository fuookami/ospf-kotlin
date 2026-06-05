/**
 * 电流单位
 * Electric current units
 *
 * 提供电流量纲的 SI 单位定义，包括安培、毫安、微安、千安等。
 * Provides SI unit definitions for electric current dimension, including ampere, milliampere, microampere, kiloampere, etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Current

/**
 * 安培（基本单位）
 * Ampere (base unit)
 *
 * SI 基本单位，用于表示电流。
 * SI base unit for electric current.
 *
 * 定义：安培是恒定电流，若保持在真空中相距 1 米的两无限长平行直导线内，
 *       则两导线之间产生的力在每米长度上等于 2×10⁻⁷ 牛顿。
 * Definition: The ampere is that constant current which, if maintained in two
 *             straight parallel conductors of infinite length, of negligible
 *             circular cross-section, and placed 1 meter apart in vacuum, would
 *             produce between these conductors a force equal to 2×10⁻⁷ newton per meter of length.
 */
object Ampere : PhysicalUnit() {
    override val name = "ampere"
    override val symbol = "A"

    override val quantity = Current
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

/**
 * 毫安
 * Milliampere
 *
 * 定义：1 mA = 10⁻³ A
 * Definition: 1 mA = 10⁻³ A
 */
object Milliampere : DerivedPhysicalUnit(Ampere * Scale.milli) {
    override val name = "milliampere"
    override val symbol = "mA"

    override val quantity = Current
}

/**
 * 微安
 * Microampere
 *
 * 定义：1 µA = 10⁻⁶ A
 * Definition: 1 µA = 10⁻⁶ A
 */
object Microampere : DerivedPhysicalUnit(Ampere * Scale.micro) {
    override val name = "microampere"
    override val symbol = "µA"

    override val quantity = Current
}

/**
 * 千安
 * Kiloampere
 *
 * 定义：1 kA = 10³ A
 * Definition: 1 kA = 10³ A
 */
object Kiloampere : DerivedPhysicalUnit(Ampere * Scale.kilo) {
    override val name = "kiloampere"
    override val symbol = "kA"

    override val quantity = Current
}
