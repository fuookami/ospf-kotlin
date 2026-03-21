package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

/**
 * Electric charge units - 电荷单位
 * Electric charge units - SI electric charge units
 *
 * 提供电荷量纲的 SI 单位定义，包括库仑、毫库仑、安时、毫安时等。
 * Provides SI unit definitions for electric charge dimension, including coulomb, millicoulomb, ampere-hour, milliampere-hour, etc.
 */

/**
 * 库仑（基本单位）
 * Coulomb (base unit)
 *
 * 定义：1 库仑 = 1 安培 × 1 秒
 * Definition: 1 coulomb = 1 ampere × 1 second
 */
object Coulomb : DerivedPhysicalUnit(Ampere * Second) {
    override val name = "coulomb"
    override val symbol = "C"

    override val quantity = ElectricCharge
}

/**
 * 毫库仑
 * Millicoulomb
 */
object Millicoulomb : DerivedPhysicalUnit(Coulomb * Scale.milli) {
    override val name = "millicoulomb"
    override val symbol = "mC"

    override val quantity = ElectricCharge
}

/**
 * 微库仑
 * Microcoulomb
 */
object Microcoulomb : DerivedPhysicalUnit(Coulomb * Scale.micro) {
    override val name = "microcoulomb"
    override val symbol = "µC"

    override val quantity = ElectricCharge
}

/**
 * 千库仑
 * Kilocoulomb
 */
object Kilocoulomb : DerivedPhysicalUnit(Coulomb * Scale.kilo) {
    override val name = "kilocoulomb"
    override val symbol = "kC"

    override val quantity = ElectricCharge
}

/**
 * 安时
 * Ampere-hour
 *
 * 定义：1 安时 = 1 安培 × 1 小时
 * Definition: 1 ampere-hour = 1 ampere × 1 hour
 */
object AmpereHour : DerivedPhysicalUnit(Ampere * Hour) {
    override val name = "ampere-hour"
    override val symbol = "Ah"

    override val quantity = ElectricCharge
}

/**
 * 毫安时
 * Milliampere-hour
 */
object MilliampereHour : DerivedPhysicalUnit(Milliampere * Hour) {
    override val name = "milliampere-hour"
    override val symbol = "mAh"

    override val quantity = ElectricCharge
}

/**
 * 微安时
 * Microampere-hour
 */
object MicroampereHour : DerivedPhysicalUnit(Microampere * Hour) {
    override val name = "microampere-hour"
    override val symbol = "µAh"

    override val quantity = ElectricCharge
}

/**
 * 千安时
 * Kiloampere-hour
 */
object KiloampereHour : DerivedPhysicalUnit(Kiloampere * Hour) {
    override val name = "kiloampere-hour"
    override val symbol = "kAh"

    override val quantity = ElectricCharge
}

/**
 * 安秒
 * Ampere-second
 */
object AmpereSecond : DerivedPhysicalUnit(Ampere * Second) {
    override val name = "ampere-second"
    override val symbol = "As"

    override val quantity = ElectricCharge
}

/**
 * 法拉（电容单位，1F = 1C/1V）
 * Farad (capacitance unit)
 *
 * 定义：1 法拉 = 1 库仑 / 伏特
 * Definition: 1 farad = 1 coulomb / volt
 */
object Farad : DerivedPhysicalUnit(Coulomb / Volt) {
    override val name = "farad"
    override val symbol = "F"

    override val quantity = Capacitance
}

/**
 * 毫法
 * Millifarad
 */
object Millifarad : DerivedPhysicalUnit(Farad * Scale.milli) {
    override val name = "millifarad"
    override val symbol = "mF"

    override val quantity = Capacitance
}

/**
 * 微法
 * Microfarad
 */
object Microfarad : DerivedPhysicalUnit(Farad * Scale.micro) {
    override val name = "microfarad"
    override val symbol = "µF"

    override val quantity = Capacitance
}

/**
 * 纳法
 * Nanofarad
 */
object Nanofarad : DerivedPhysicalUnit(Farad * Scale.nano) {
    override val name = "nanofarad"
    override val symbol = "nF"

    override val quantity = Capacitance
}

/**
 * 皮法
 * Picofarad
 */
object Picofarad : DerivedPhysicalUnit(Farad * Scale.pico) {
    override val name = "picofarad"
    override val symbol = "pF"

    override val quantity = Capacitance
}