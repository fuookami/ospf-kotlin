/**
 * 电荷单位
 * Electric charge units
 *
 * 提供电荷量纲的 SI 单位定义，包括库仑、毫库仑、安时、毫安时等。
 * 同时提供电容单位，包括法拉、毫法、微法、纳法、皮法等。
 * Provides SI unit definitions for electric charge dimension, including coulomb, millicoulomb, ampere-hour, milliampere-hour, etc.
 * Also provides capacitance units, including farad, millifarad, microfarad, nanofarad, picofarad, etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Capacitance
import fuookami.ospf.kotlin.quantities.dimension.ElectricCharge

// ============================================================================
// 电荷单位 / Electric Charge Units
// ============================================================================

/**
 * 库仑
 * Coulomb
 *
 * 电荷的 SI 导出单位。
 * SI derived unit for electric charge.
 *
 * 定义：1 C = 1 A × 1 s
 * Definition: 1 C = 1 A × 1 s
 */
object Coulomb : DerivedPhysicalUnit(Ampere * Second) {
    override val name = "coulomb"
    override val symbol = "C"

    override val quantity = ElectricCharge
}

/**
 * 毫库仑
 * Millicoulomb
 *
 * 定义：1 mC = 10⁻³ C
 * Definition: 1 mC = 10⁻³ C
 */
object Millicoulomb : DerivedPhysicalUnit(Coulomb * Scale.milli) {
    override val name = "millicoulomb"
    override val symbol = "mC"

    override val quantity = ElectricCharge
}

/**
 * 微库仑
 * Microcoulomb
 *
 * 定义：1 µC = 10⁻⁶ C
 * Definition: 1 µC = 10⁻⁶ C
 */
object Microcoulomb : DerivedPhysicalUnit(Coulomb * Scale.micro) {
    override val name = "microcoulomb"
    override val symbol = "µC"

    override val quantity = ElectricCharge
}

/**
 * 千库仑
 * Kilocoulomb
 *
 * 定义：1 kC = 10³ C
 * Definition: 1 kC = 10³ C
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
 * 常用于电池容量表示。
 * Commonly used for battery capacity.
 *
 * 定义：1 Ah = 1 A × 1 h = 3600 C
 * Definition: 1 Ah = 1 A × 1 h = 3600 C
 */
object AmpereHour : DerivedPhysicalUnit(Ampere * Hour) {
    override val name = "ampere-hour"
    override val symbol = "Ah"

    override val quantity = ElectricCharge
}

/**
 * 毫安时
 * Milliampere-hour
 *
 * 定义：1 mAh = 10⁻³ Ah = 3.6 C
 * Definition: 1 mAh = 10⁻³ Ah = 3.6 C
 */
object MilliampereHour : DerivedPhysicalUnit(Milliampere * Hour) {
    override val name = "milliampere-hour"
    override val symbol = "mAh"

    override val quantity = ElectricCharge
}

/**
 * 微安时
 * Microampere-hour
 *
 * 定义：1 µAh = 10⁻⁶ Ah = 3.6 × 10⁻³ C
 * Definition: 1 µAh = 10⁻⁶ Ah = 3.6 × 10⁻³ C
 */
object MicroampereHour : DerivedPhysicalUnit(Microampere * Hour) {
    override val name = "microampere-hour"
    override val symbol = "µAh"

    override val quantity = ElectricCharge
}

/**
 * 千安时
 * Kiloampere-hour
 *
 * 定义：1 kAh = 10³ Ah = 3.6 × 10⁶ C
 * Definition: 1 kAh = 10³ Ah = 3.6 × 10⁶ C
 */
object KiloampereHour : DerivedPhysicalUnit(Kiloampere * Hour) {
    override val name = "kiloampere-hour"
    override val symbol = "kAh"

    override val quantity = ElectricCharge
}

/**
 * 安秒
 * Ampere-second
 *
 * 定义：1 As = 1 C
 * Definition: 1 As = 1 C
 */
object AmpereSecond : DerivedPhysicalUnit(Ampere * Second) {
    override val name = "ampere-second"
    override val symbol = "As"

    override val quantity = ElectricCharge
}

// ============================================================================
// 电容单位 / Capacitance Units
// ============================================================================

/**
 * 法拉
 * Farad
 *
 * 电容的 SI 导出单位。
 * SI derived unit for capacitance.
 *
 * 定义：1 F = 1 C / V
 * Definition: 1 F = 1 C / V
 */
object Farad : DerivedPhysicalUnit(Coulomb / Volt) {
    override val name = "farad"
    override val symbol = "F"

    override val quantity = Capacitance
}

/**
 * 毫法
 * Millifarad
 *
 * 定义：1 mF = 10⁻³ F
 * Definition: 1 mF = 10⁻³ F
 */
object Millifarad : DerivedPhysicalUnit(Farad * Scale.milli) {
    override val name = "millifarad"
    override val symbol = "mF"

    override val quantity = Capacitance
}

/**
 * 微法
 * Microfarad
 *
 * 定义：1 µF = 10⁻⁶ F
 * Definition: 1 µF = 10⁻⁶ F
 */
object Microfarad : DerivedPhysicalUnit(Farad * Scale.micro) {
    override val name = "microfarad"
    override val symbol = "µF"

    override val quantity = Capacitance
}

/**
 * 纳法
 * Nanofarad
 *
 * 定义：1 nF = 10⁻⁹ F
 * Definition: 1 nF = 10⁻⁹ F
 */
object Nanofarad : DerivedPhysicalUnit(Farad * Scale.nano) {
    override val name = "nanofarad"
    override val symbol = "nF"

    override val quantity = Capacitance
}

/**
 * 皮法
 * Picofarad
 *
 * 定义：1 pF = 10⁻¹² F
 * Definition: 1 pF = 10⁻¹² F
 */
object Picofarad : DerivedPhysicalUnit(Farad * Scale.pico) {
    override val name = "picofarad"
    override val symbol = "pF"

    override val quantity = Capacitance
}