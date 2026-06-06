/**
 * 体积单位 / Volume units
 *
 * 提供体积量纲的单位定义，包括立方米、升、毫升、立方英寸、立方英尺、立方码、英制液体盎司、
 * 美制液体盎司、英制加仑、美制加仑等。
 * Provides unit definitions for volume dimension, including cubic meter, liter, milliliter, cubic inch,
 * cubic foot, cubic yard, UK fluid ounce, US fluid ounce, UK gallon, US gallon, etc.
 *
 * 来源：SI 导出单位及常用体积单位定义
 * Source: SI derived units and common volume unit definitions
 * - Liter: 1 L = 1 dm³ (exact, 1964 CGPM)
 * - UKFluidOunce: 1 UK fl oz = 28.4130625 mL (exact, Weights and Measures Act 1985)
 * - USFluidOunce: 1 US fl oz = 29.5735295625 mL (exact, 1 US gallon = 128 US fl oz)
 * - UKGallon: 1 UK gal = 4.54609 L (exact, Weights and Measures Act 1985)
 * - USGallon: 1 US gal = 3.78541178 L (exact, NIST Handbook 44)
 * - CubicYard: 1 yd³ = 27 ft³ (geometric)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Volume

/**
 * 立方毫米
 * Cubic millimeter
 *
 * 名称：立方毫米
 * Name: cubic millimeter
 *
 * 符号：mm³
 * Symbol: mm³
 *
 * 定义：1 mm³ = (1 mm)³ = 10⁻⁹ m³
 * Definition: 1 mm³ = (1 mm)³ = 10⁻⁹ m³
 *
 * 来源：SI 导出体积单位
 * Source: SI derived volume unit
 */
object CubicMillimeter : DerivedPhysicalUnit(SquareMillimeter * Millimeter) {
    override val name = "cubic millimeter"
    override val symbol = "mm3"

    override val quantity = Volume
}

/**
 * 立方厘米
 * Cubic centimeter
 *
 * 名称：立方厘米
 * Name: cubic centimeter
 *
 * 符号：cm³
 * Symbol: cm³
 *
 * 定义：1 cm³ = (1 cm)³ = 10⁻⁶ m³
 * Definition: 1 cm³ = (1 cm)³ = 10⁻⁶ m³
 *
 * 来源：SI 导出体积单位，常用于医学和化学
 * Source: SI derived volume unit, commonly used in medicine and chemistry
 */
object CubicCentimeter : DerivedPhysicalUnit(SquareCentimeter * Centimeter) {
    override val name = "cubic centimeter"
    override val symbol = "cm3"

    override val quantity = Volume
}

/**
 * 立方分米
 * Cubic decimeter
 *
 * 名称：立方分米
 * Name: cubic decimeter
 *
 * 符号：dm³
 * Symbol: dm³
 *
 * 定义：1 dm³ = (1 dm)³ = 10⁻³ m³ = 1 L
 * Definition: 1 dm³ = (1 dm)³ = 10⁻³ m³ = 1 L
 *
 * 来源：SI 导出体积单位
 * Source: SI derived volume unit
 */
object CubicDecimeter : DerivedPhysicalUnit(SquareDecimeter * Decimeter) {
    override val name = "cubic decimeter"
    override val symbol = "dm3"

    override val quantity = Volume
}

/**
 * 立方米（SI 导出单位）
 * Cubic meter (SI derived unit)
 *
 * 名称：立方米
 * Name: cubic meter
 *
 * 符号：m³
 * Symbol: m³
 *
 * 定义：1 m³ = (1 m)³
 * Definition: 1 m³ = (1 m)³
 *
 * 来源：SI 导出单位，体积的国际单位
 * Source: SI derived unit, international unit for volume
 */
object CubicMeter : DerivedPhysicalUnit(SquareMeter * Meter) {
    override val name = "cubic meter"
    override val symbol = "m3"

    override val quantity = Volume
}

/**
 * 立方十米
 * Cubic decameter
 *
 * 名称：立方十米
 * Name: cubic decameter
 *
 * 符号：dam³
 * Symbol: dam³
 *
 * 定义：1 dam³ = (1 dam)³ = 10³ m³
 * Definition: 1 dam³ = (1 dam)³ = 10³ m³
 *
 * 来源：SI 导出体积单位，常用于水资源计量
 * Source: SI derived volume unit, commonly used for water resource measurement
 */
object CubicDecameter : DerivedPhysicalUnit(Decameter * Decameter * Decameter) {
    override val name = "cubic decameter"
    override val symbol = "dam3"

    override val quantity = Volume
}

/**
 * 立方百米
 * Cubic hectometer
 *
 * 名称：立方百米
 * Name: cubic hectometer
 *
 * 符号：hm³
 * Symbol: hm³
 *
 * 定义：1 hm³ = (1 hm)³ = 10⁶ m³
 * Definition: 1 hm³ = (1 hm)³ = 10⁶ m³
 *
 * 来源：SI 导出体积单位，常用于水库库容计量
 * Source: SI derived volume unit, commonly used for reservoir capacity measurement
 */
object CubicHectometer : DerivedPhysicalUnit(Hectometer * Hectometer * Hectometer) {
    override val name = "cubic hectometer"
    override val symbol = "hm3"

    override val quantity = Volume
}

/**
 * 立方千米
 * Cubic kilometer
 *
 * 名称：立方千米
 * Name: cubic kilometer
 *
 * 符号：km³
 * Symbol: km³
 *
 * 定义：1 km³ = (1 km)³ = 10⁹ m³
 * Definition: 1 km³ = (1 km)³ = 10⁹ m³
 *
 * 来源：SI 导出体积单位，常用于地质和海洋体积计量
 * Source: SI derived volume unit, commonly used for geological and oceanic volume measurement
 */
object CubicKilometer : DerivedPhysicalUnit(Kilometer * Kilometer * Kilometer) {
    override val name = "cubic kilometer"
    override val symbol = "km3"

    override val quantity = Volume
}

/**
 * 升
 * Liter
 *
 * 名称：升
 * Name: liter
 *
 * 符号：L
 * Symbol: L
 *
 * 定义：1 L = 1 dm³ = 10⁻³ m³（精确，1964 年 CGPM）
 * Definition: 1 L = 1 dm³ = 10⁻³ m³ (exact, 1964 CGPM)
 *
 * 来源：常用体积单位，广泛用于日常生活和科学
 * Source: Common volume unit, widely used in daily life and science
 */
object Liter : DerivedPhysicalUnit(CubicDecimeter) {
    override val name = "liter"
    override val symbol = "L"

    override val quantity = Volume
}

/**
 * 微升
 * Microliter
 *
 * 名称：微升
 * Name: microliter
 *
 * 符号：μL
 * Symbol: μL
 *
 * 定义：1 μL = 10⁻⁶ L
 * Definition: 1 μL = 10⁻⁶ L
 *
 * 来源：SI 体积单位，常用于实验室微量分析
 * Source: SI volume unit, commonly used in laboratory microanalysis
 */
object Microliter : DerivedPhysicalUnit(Liter * Scale.micro) {
    override val name = "microliter"
    override val symbol = "μL"

    override val quantity = Volume
}

/**
 * 毫升
 * Milliliter
 *
 * 名称：毫升
 * Name: milliliter
 *
 * 符号：mL
 * Symbol: mL
 *
 * 定义：1 mL = 10⁻³ L = 1 cm³
 * Definition: 1 mL = 10⁻³ L = 1 cm³
 *
 * 来源：SI 体积单位，广泛用于医学和日常生活
 * Source: SI volume unit, widely used in medicine and daily life
 */
object Milliliter : DerivedPhysicalUnit(Liter * Scale.milli) {
    override val name = "milliliter"
    override val symbol = "mL"

    override val quantity = Volume
}

/**
 * 厘升
 * Centiliter
 *
 * 名称：厘升
 * Name: centiliter
 *
 * 符号：cL
 * Symbol: cL
 *
 * 定义：1 cL = 10⁻² L
 * Definition: 1 cL = 10⁻² L
 *
 * 来源：SI 体积单位
 * Source: SI volume unit
 */
object Centiliter : DerivedPhysicalUnit(Liter * Scale.centi) {
    override val name = "centiliter"
    override val symbol = "cL"

    override val quantity = Volume
}

/**
 * 分升
 * Deciliter
 *
 * 名称：分升
 * Name: deciliter
 *
 * 符号：dL
 * Symbol: dL
 *
 * 定义：1 dL = 10⁻¹ L
 * Definition: 1 dL = 10⁻¹ L
 *
 * 来源：SI 体积单位，常用于医学检验
 * Source: SI volume unit, commonly used in medical tests
 */
object Deciliter : DerivedPhysicalUnit(Liter * Scale.deci) {
    override val name = "deciliter"
    override val symbol = "dL"

    override val quantity = Volume
}

/**
 * 百升
 * Hectoliter
 *
 * 名称：百升
 * Name: hectoliter
 *
 * 符号：hL
 * Symbol: hL
 *
 * 定义：1 hL = 10² L
 * Definition: 1 hL = 10² L
 *
 * 来源：SI 体积单位，常用于酿造业和农业
 * Source: SI volume unit, commonly used in brewing and agriculture
 */
object Hectoliter : DerivedPhysicalUnit(Liter * Scale.hecto) {
    override val name = "hectoliter"
    override val symbol = "hL"

    override val quantity = Volume
}

/**
 * 立方英寸
 * Cubic inch
 *
 * 名称：立方英寸
 * Name: cubic inch
 *
 * 符号：cu.in
 * Symbol: cu.in
 *
 * 定义：1 in³ = (1 in)³ ≈ 16.387064 cm³
 * Definition: 1 in³ = (1 in)³ ≈ 16.387064 cm³
 *
 * 来源：英制体积单位
 * Source: Imperial volume unit
 */
object CubicInch : DerivedPhysicalUnit(SquareInch * Inch) {
    override val name = "cubic inch"
    override val symbol = "cu.in"

    override val quantity = Volume
}

/**
 * 立方英尺
 * Cubic foot
 *
 * 名称：立方英尺
 * Name: cubic foot
 *
 * 符号：cu.ft
 * Symbol: cu.ft
 *
 * 定义：1 ft³ = (1 ft)³ = 1728 in³ ≈ 28.316846592 L
 * Definition: 1 ft³ = (1 ft)³ = 1728 in³ ≈ 28.316846592 L
 *
 * 来源：英制体积单位，常用于建筑和工程
 * Source: Imperial volume unit, commonly used in construction and engineering
 */
object CubicFoot : DerivedPhysicalUnit(SquareFoot * Foot) {
    override val name = "cubic foot"
    override val symbol = "cu.ft"

    override val quantity = Volume
}

/**
 * 立方码
 * Cubic yard
 *
 * 名称：立方码
 * Name: cubic yard
 *
 * 符号：cu.yd
 * Symbol: cu.yd
 *
 * 定义：1 yd³ = (1 yd)³ = 27 ft³ ≈ 764.554857984 L
 * Definition: 1 yd³ = (1 yd)³ = 27 ft³ ≈ 764.554857984 L
 *
 * 来源：英制体积单位，常用于建筑材料
 * Source: Imperial volume unit, commonly used for construction materials
 */
object CubicYard : DerivedPhysicalUnit(Yard * Yard * Yard) {
    override val name = "cubic yard"
    override val symbol = "cu.yd"

    override val quantity = Volume
}

/**
 * 英制液体盎司
 * UK fluid ounce
 *
 * 名称：英制液体盎司
 * Name: UK fluid ounce
 *
 * 符号：uk.fl.oz
 * Symbol: uk.fl.oz
 *
 * 定义：1 UK fl oz = 28.4130625 mL（精确，《度量衡法》1985）
 * Definition: 1 UK fl oz = 28.4130625 mL (exact, Weights and Measures Act 1985)
 *
 * 来源：英制体积单位
 * Source: Imperial volume unit
 */
object UKFluidOunce : DerivedPhysicalUnit(Milliliter * 28.4130625) {
    override val name = "uk fluid ounce"
    override val symbol = "uk.fl.oz"

    override val quantity = Volume
}

/**
 * 美制液体盎司
 * US fluid ounce
 *
 * 名称：美制液体盎司
 * Name: US fluid ounce
 *
 * 符号：us.fl.oz
 * Symbol: us.fl.oz
 *
 * 定义：1 US fl oz = 29.5735295625 mL（精确，1 美制加仑 = 128 美制液体盎司）
 * Definition: 1 US fl oz = 29.5735295625 mL (exact, 1 US gallon = 128 US fl oz)
 *
 * 来源：美制体积单位
 * Source: US customary volume unit
 */
object USFluidOunce : DerivedPhysicalUnit(Milliliter * 29.5735295625) {
    override val name = "us fluid ounce"
    override val symbol = "us.fl.oz"

    override val quantity = Volume
}

/**
 * 英制加仑
 * UK gallon
 *
 * 名称：英制加仑
 * Name: UK gallon
 *
 * 符号：uk.gal
 * Symbol: uk.gal
 *
 * 定义：1 UK gal = 4.54609 L（精确，《度量衡法》1985）
 * Definition: 1 UK gal = 4.54609 L (exact, Weights and Measures Act 1985)
 *
 * 来源：英制体积单位
 * Source: Imperial volume unit
 */
object UKGallon : DerivedPhysicalUnit(Liter * 4.54609) {
    override val name = "uk gallon"
    override val symbol = "uk.gal"

    override val quantity = Volume
}

/**
 * 美制加仑
 * US gallon
 *
 * 名称：美制加仑
 * Name: US gallon
 *
 * 符号：us.gal
 * Symbol: us.gal
 *
 * 定义：1 US gal = 3.78541178 L（精确，NIST 手册 44）
 * Definition: 1 US gal = 3.78541178 L (exact, NIST Handbook 44)
 *
 * 来源：美制体积单位，常用于美国燃油销售
 * Source: US customary volume unit, commonly used for fuel sales in the US
 */
object USGallon : DerivedPhysicalUnit(Liter * 3.78541178) {
    override val name = "us gallon"
    override val symbol = "us.gal"

    override val quantity = Volume
}
