package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.Current

/**
 * Electric current units - 电流单位
 * Electric current units - SI electric current units
 *
 * 提供电流量纲的 SI 单位定义，包括安培、毫安、微安等。
 * Provides SI unit definitions for electric current dimension, including ampere, milliampere, microampere, etc.
 */

/**
 * 安培（基本单位）
 * Ampere (base unit)
 */
object Ampere : PhysicalUnit() {
    override val name = "ampere"
    override val symbol = "A"

    override val quantity = Current
    override val scale = Scale()
}

/**
 * 毫安
 * Milliampere
 */
object Milliampere : DerivedPhysicalUnit(Ampere * Scale.milli) {
    override val name = "milliampere"
    override val symbol = "mA"

    override val quantity = Current
}

/**
 * 微安
 * Microampere
 */
object Microampere : DerivedPhysicalUnit(Ampere * Scale.micro) {
    override val name = "microampere"
    override val symbol = "µA"

    override val quantity = Current
}

/**
 * 千安
 * Kiloampere
 */
object Kiloampere : DerivedPhysicalUnit(Ampere * Scale.kilo) {
    override val name = "kiloampere"
    override val symbol = "kA"

    override val quantity = Current
}
