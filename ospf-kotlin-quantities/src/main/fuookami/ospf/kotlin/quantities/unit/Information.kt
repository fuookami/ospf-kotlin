package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Information

/**
 * 信息单位
 * Information units
 *
 * 用于表示数据量的单位，如 bit, byte 等。
 * Units for representing data quantities, such as bits, bytes, etc.
 */

/**
 * 比特（基本单位）
 * Bit (base unit)
 */
object Bit : PhysicalUnit() {
    override val name = "bit"
    override val symbol = "bit"

    override val quantity = Information
    override val scale = Scale()
}

/**
 * 千比特
 * Kilobit
 */
object Kilobit : PhysicalUnit() {
    override val name = "kilobit"
    override val symbol = "kbit"

    override val quantity = Information
    override val scale = Scale.kilo
}

/**
 * 兆比特
 * Megabit
 */
object Megabit : PhysicalUnit() {
    override val name = "megabit"
    override val symbol = "Mbit"

    override val quantity = Information
    override val scale = Scale.mega
}

/**
 * 吉比特
 * Gigabit
 */
object Gigabit : PhysicalUnit() {
    override val name = "gigabit"
    override val symbol = "Gbit"

    override val quantity = Information
    override val scale = Scale.giga
}

/**
 * 太比特
 * Terabit
 */
object Terabit : PhysicalUnit() {
    override val name = "terabit"
    override val symbol = "Tbit"

    override val quantity = Information
    override val scale = Scale.tera
}

/**
 * 拍比特
 * Petabit
 */
object Petabit : PhysicalUnit() {
    override val name = "petabit"
    override val symbol = "Pbit"

    override val quantity = Information
    override val scale = Scale.peta
}

/**
 * 艾比特
 * Exabit
 */
object Exabit : PhysicalUnit() {
    override val name = "exabit"
    override val symbol = "Ebit"

    override val quantity = Information
    override val scale = Scale.exa
}

/**
 * 字节
 * Byte
 */
object Byte : PhysicalUnit() {
    override val name = "byte"
    override val symbol = "B"

    override val quantity = Information
    override val scale = Scale(8)
}

/**
 * 千字节
 * Kilobyte
 */
object Kilobyte : PhysicalUnit() {
    override val name = "kilobyte"
    override val symbol = "KB"

    override val quantity = Information
    override val scale = Scale.kilo * Scale(8)
}

/**
 * 兆字节
 * Megabyte
 */
object Megabyte : PhysicalUnit() {
    override val name = "megabyte"
    override val symbol = "MB"

    override val quantity = Information
    override val scale = Scale.mega * Scale(8)
}

/**
 * 吉字节
 * Gigabyte
 */
object Gigabyte : PhysicalUnit() {
    override val name = "gigabyte"
    override val symbol = "GB"

    override val quantity = Information
    override val scale = Scale.giga * Scale(8)
}

/**
 * 太字节
 * Terabyte
 */
object Terabyte : PhysicalUnit() {
    override val name = "terabyte"
    override val symbol = "TB"

    override val quantity = Information
    override val scale = Scale.tera * Scale(8)
}

/**
 * 拍字节
 * Petabyte
 */
object Petabyte : PhysicalUnit() {
    override val name = "petabyte"
    override val symbol = "PB"

    override val quantity = Information
    override val scale = Scale.peta * Scale(8)
}

/**
 * 艾字节
 * Exabyte
 */
object Exabyte : PhysicalUnit() {
    override val name = "exabyte"
    override val symbol = "EB"

    override val quantity = Information
    override val scale = Scale.exa * Scale(8)
}