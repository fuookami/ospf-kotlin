/**
 * 信息单位 / Information units
 *
 * 用于表示数据量的单位，如 bit, byte 等。
 * Units for representing data quantities, such as bits, bytes, etc.
 *
 * 单位常量来源 / Unit constant sources:
 * - Bit: 基本单位 / Base unit
 * - Byte: 1 B = 8 bit
 * - SI前缀单位 (Kilobit, Megabit等) 使用十进制前缀 / SI prefix units (Kilobit, Megabit, etc.) use decimal prefixes
 * - 二进制前缀单位可使用 Scale(1024) 等进行派生 / Binary prefix units can be derived using Scale(1024) etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.*

/**
 * 比特（基本单位） / Bit (base unit)
 *
 * 信息的最小单位，表示一个二进制位。
 * The smallest unit of information, representing a single binary digit.
 *
 * 符号 / Symbol: bit
 * 换算关系 / Conversion: 1 bit = 1 bit (基本单位 / base unit)
 */
object Bit : PhysicalUnit() {
    /** 单位名称：bit / Unit name: bit */
    override val name = "bit"
    /** 单位符号：bit / Unit symbol: bit */
    override val symbol = "bit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 比例因子：1（基本单位） / Scale factor: 1 (base unit) */
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

/**
 * 千比特 / Kilobit
 *
 * 一千比特，使用SI十进制前缀。
 * One thousand bits, using SI decimal prefix.
 *
 * 符号 / Symbol: kbit
 * 换算关系 / Conversion: 1 kbit = 10³ bit = 1000 bit
 */
object Kilobit : PhysicalUnit() {
    /** 单位名称：kilobit / Unit name: kilobit */
    override val name = "kilobit"
    /** 单位符号：kbit / Unit symbol: kbit */
    override val symbol = "kbit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10³ / Scale factor: 10³ */
    override val conversionRule = UnitConversionRule.Linear(Scale.kilo)
}

/**
 * 兆比特 / Megabit
 *
 * 一百万比特。
 * One million bits.
 *
 * 符号 / Symbol: Mbit
 * 换算关系 / Conversion: 1 Mbit = 10⁶ bit = 1000000 bit
 */
object Megabit : PhysicalUnit() {
    /** 单位名称：megabit / Unit name: megabit */
    override val name = "megabit"
    /** 单位符号：Mbit / Unit symbol: Mbit */
    override val symbol = "Mbit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10⁶ / Scale factor: 10⁶ */
    override val conversionRule = UnitConversionRule.Linear(Scale.mega)
}

/**
 * 吉比特 / Gigabit
 *
 * 十亿比特。
 * One billion bits.
 *
 * 符号 / Symbol: Gbit
 * 换算关系 / Conversion: 1 Gbit = 10⁹ bit
 */
object Gigabit : PhysicalUnit() {
    /** 单位名称：gigabit / Unit name: gigabit */
    override val name = "gigabit"
    /** 单位符号：Gbit / Unit symbol: Gbit */
    override val symbol = "Gbit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10⁹ / Scale factor: 10⁹ */
    override val conversionRule = UnitConversionRule.Linear(Scale.giga)
}

/**
 * 太比特 / Terabit
 *
 * 一万亿比特。
 * One trillion bits.
 *
 * 符号 / Symbol: Tbit
 * 换算关系 / Conversion: 1 Tbit = 10¹² bit
 */
object Terabit : PhysicalUnit() {
    /** 单位名称：terabit / Unit name: terabit */
    override val name = "terabit"
    /** 单位符号：Tbit / Unit symbol: Tbit */
    override val symbol = "Tbit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹² / Scale factor: 10¹² */
    override val conversionRule = UnitConversionRule.Linear(Scale.tera)
}

/**
 * 拍比特 / Petabit
 *
 * 一千万亿比特。
 * One quadrillion bits.
 *
 * 符号 / Symbol: Pbit
 * 换算关系 / Conversion: 1 Pbit = 10¹⁵ bit
 */
object Petabit : PhysicalUnit() {
    /** 单位名称：petabit / Unit name: petabit */
    override val name = "petabit"
    /** 单位符号：Pbit / Unit symbol: Pbit */
    override val symbol = "Pbit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹⁵ / Scale factor: 10¹⁵ */
    override val conversionRule = UnitConversionRule.Linear(Scale.peta)
}

/**
 * 艾比特 / Exabit
 *
 * 一百亿亿比特。
 * One quintillion bits.
 *
 * 符号 / Symbol: Ebit
 * 换算关系 / Conversion: 1 Ebit = 10¹⁸ bit
 */
object Exabit : PhysicalUnit() {
    /** 单位名称：exabit / Unit name: exabit */
    override val name = "exabit"
    /** 单位符号：Ebit / Unit symbol: Ebit */
    override val symbol = "Ebit"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹⁸ / Scale factor: 10¹⁸ */
    override val conversionRule = UnitConversionRule.Linear(Scale.exa)
}

/**
 * 字节 / Byte
 *
 * 由8个比特组成的基本存储单位。
 * A basic storage unit consisting of 8 bits.
 *
 * 符号 / Symbol: B
 * 换算关系 / Conversion: 1 B = 8 bit
 */
object Byte : PhysicalUnit() {
    /** 单位名称：byte / Unit name: byte */
    override val name = "byte"
    /** 单位符号：B / Unit symbol: B */
    override val symbol = "B"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 比例因子：8 / Scale factor: 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale(8))
}

/**
 * 千字节 / Kilobyte
 *
 * 一千字节，使用SI十进制前缀。
 * One thousand bytes, using SI decimal prefix.
 *
 * 符号 / Symbol: KB
 * 换算关系 / Conversion: 1 KB = 10³ B = 8000 bit
 */
object Kilobyte : PhysicalUnit() {
    /** 单位名称：kilobyte / Unit name: kilobyte */
    override val name = "kilobyte"
    /** 单位符号：KB / Unit symbol: KB */
    override val symbol = "KB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10³ × 8 = 8000 / Scale factor: 10³ × 8 = 8000 */
    override val conversionRule = UnitConversionRule.Linear(Scale.kilo * Scale(8))
}

/**
 * 兆字节 / Megabyte
 *
 * 一百万字节。
 * One million bytes.
 *
 * 符号 / Symbol: MB
 * 换算关系 / Conversion: 1 MB = 10⁶ B = 8000000 bit
 */
object Megabyte : PhysicalUnit() {
    /** 单位名称：megabyte / Unit name: megabyte */
    override val name = "megabyte"
    /** 单位符号：MB / Unit symbol: MB */
    override val symbol = "MB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10⁶ × 8 / Scale factor: 10⁶ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale.mega * Scale(8))
}

/**
 * 吉字节 / Gigabyte
 *
 * 十亿字节。
 * One billion bytes.
 *
 * 符号 / Symbol: GB
 * 换算关系 / Conversion: 1 GB = 10⁹ B
 */
object Gigabyte : PhysicalUnit() {
    /** 单位名称：gigabyte / Unit name: gigabyte */
    override val name = "gigabyte"
    /** 单位符号：GB / Unit symbol: GB */
    override val symbol = "GB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10⁹ × 8 / Scale factor: 10⁹ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale.giga * Scale(8))
}

/**
 * 太字节 / Terabyte
 *
 * 一万亿字节。
 * One trillion bytes.
 *
 * 符号 / Symbol: TB
 * 换算关系 / Conversion: 1 TB = 10¹² B
 */
object Terabyte : PhysicalUnit() {
    /** 单位名称：terabyte / Unit name: terabyte */
    override val name = "terabyte"
    /** 单位符号：TB / Unit symbol: TB */
    override val symbol = "TB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹² × 8 / Scale factor: 10¹² × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale.tera * Scale(8))
}

/**
 * 拍字节 / Petabyte
 *
 * 一千万亿字节。
 * One quadrillion bytes.
 *
 * 符号 / Symbol: PB
 * 换算关系 / Conversion: 1 PB = 10¹⁵ B
 */
object Petabyte : PhysicalUnit() {
    /** 单位名称：petabyte / Unit name: petabyte */
    override val name = "petabyte"
    /** 单位符号：PB / Unit symbol: PB */
    override val symbol = "PB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹⁵ × 8 / Scale factor: 10¹⁵ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale.peta * Scale(8))
}

/**
 * 艾字节 / Exabyte
 *
 * 一百亿亿字节。
 * One quintillion bytes.
 *
 * 符号 / Symbol: EB
 * 换算关系 / Conversion: 1 EB = 10¹⁸ B
 */
object Exabyte : PhysicalUnit() {
    /** 单位名称：exabyte / Unit name: exabyte */
    override val name = "exabyte"
    /** 单位符号：EB / Unit symbol: EB */
    override val symbol = "EB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：10¹⁸ × 8 / Scale factor: 10¹⁸ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale.exa * Scale(8))
}

/**
 * 千字节（二进制前缀）/ Kibibyte
 *
 * 1024字节，使用IEC二进制前缀。
 * 1024 bytes, using IEC binary prefix.
 *
 * 符号 / Symbol: KiB
 * 换算关系 / Conversion: 1 KiB = 1024 B = 8192 bit
 */
object Kibibyte : PhysicalUnit() {
    /** 单位名称：kibibyte / Unit name: kibibyte */
    override val name = "kibibyte"
    /** 单位符号：KiB / Unit symbol: KiB */
    override val symbol = "KiB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：1024 × 8 = 8192 / Scale factor: 1024 × 8 = 8192 */
    override val conversionRule = UnitConversionRule.Linear(Scale(1024) * Scale(8))
}

/**
 * 兆字节（二进制前缀）/ Mebibyte
 *
 * 1024千字节（二进制），使用IEC二进制前缀。
 * 1024 kibibytes, using IEC binary prefix.
 *
 * 符号 / Symbol: MiB
 * 换算关系 / Conversion: 1 MiB = 1024 KiB = 1048576 B
 */
object Mebibyte : PhysicalUnit() {
    /** 单位名称：mebibyte / Unit name: mebibyte */
    override val name = "mebibyte"
    /** 单位符号：MiB / Unit symbol: MiB */
    override val symbol = "MiB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：1024² × 8 / Scale factor: 1024² × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale(1024) * Scale(1024) * Scale(8))
}

/**
 * 吉字节（二进制前缀）/ Gibibyte
 *
 * 1024兆字节（二进制），使用IEC二进制前缀。
 * 1024 mebibytes, using IEC binary prefix.
 *
 * 符号 / Symbol: GiB
 * 换算关系 / Conversion: 1 GiB = 1024 MiB = 1073741824 B
 */
object Gibibyte : PhysicalUnit() {
    /** 单位名称：gibibyte / Unit name: gibibyte */
    override val name = "gibibyte"
    /** 单位符号：GiB / Unit symbol: GiB */
    override val symbol = "GiB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：1024³ × 8 / Scale factor: 1024³ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale(1024) * Scale(1024) * Scale(1024) * Scale(8))
}

/**
 * 太字节（二进制前缀）/ Tebibyte
 *
 * 1024吉字节（二进制），使用IEC二进制前缀。
 * 1024 gibibytes, using IEC binary prefix.
 *
 * 符号 / Symbol: TiB
 * 换算关系 / Conversion: 1 TiB = 1024 GiB = 1099511627776 B
 */
object Tebibyte : PhysicalUnit() {
    /** 单位名称：tebibyte / Unit name: tebibyte */
    override val name = "tebibyte"
    /** 单位符号：TiB / Unit symbol: TiB */
    override val symbol = "TiB"

    /** 对应物理量：信息 / Corresponding quantity: Information */
    override val quantity = Information
    /** 取值域：连续 / Value domain: continuous */
    override val domain = QuantityDomain.Continuous
    /** 比例因子：1024⁴ × 8 / Scale factor: 1024⁴ × 8 */
    override val conversionRule = UnitConversionRule.Linear(Scale(1024) * Scale(1024) * Scale(1024) * Scale(1024) * Scale(8))
}
