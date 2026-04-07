/**
 * 面积单位
 * Area units
 *
 * 提供面积量纲的 SI 单位和英制单位定义，包括平方米、平方千米、公顷、平方英尺、英亩等。
 * Provides SI and imperial unit definitions for area dimension, including square meter, square kilometer, hectare, square foot, acre, etc.
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Area

/**
 * 平方毫米
 * Square millimeter
 *
 * 定义：1 mm² = (10⁻³ m)² = 10⁻⁶ m²
 * Definition: 1 mm² = (10⁻³ m)² = 10⁻⁶ m²
 */
object SquareMillimeter : DerivedPhysicalUnit(Millimeter * Millimeter) {
    override val name = "square millimeter"
    override val symbol = "mm2"

    override val quantity = Area
}

/**
 * 平方厘米
 * Square centimeter
 *
 * 定义：1 cm² = (10⁻² m)² = 10⁻⁴ m²
 * Definition: 1 cm² = (10⁻² m)² = 10⁻⁴ m²
 */
object SquareCentimeter : DerivedPhysicalUnit(Centimeter * Centimeter) {
    override val name = "square centimeter"
    override val symbol = "cm2"

    override val quantity = Area
}

/**
 * 平方分米
 * Square decimeter
 *
 * 定义：1 dm² = (10⁻¹ m)² = 10⁻² m²
 * Definition: 1 dm² = (10⁻¹ m)² = 10⁻² m²
 */
object SquareDecimeter : DerivedPhysicalUnit(Decimeter * Decimeter) {
    override val name = "square decimeter"
    override val symbol = "dm2"

    override val quantity = Area
}

/**
 * 平方米（基本单位）
 * Square meter (base unit)
 *
 * SI 面积基本单位。
 * SI base unit for area.
 */
object SquareMeter : DerivedPhysicalUnit(Meter * Meter) {
    override val name = "square meter"
    override val symbol = "m2"

    override val quantity = Area
}

/**
 * 平方千米
 * Square kilometer
 *
 * 定义：1 km² = (10³ m)² = 10⁶ m²
 * Definition: 1 km² = (10³ m)² = 10⁶ m²
 */
object SquareKilometer : DerivedPhysicalUnit(Kilometer * Kilometer) {
    override val name = "square kilometer"
    override val symbol = "km2"

    override val quantity = Area
}

/**
 * 公亩
 * Are
 *
 * 定义：1 are = 100 m²
 * Definition: 1 are = 100 m²
 */
object Are : DerivedPhysicalUnit(SquareMeter * 100) {
    override val name = "are"
    override val symbol = "are"

    override val quantity = Area
}

/**
 * 公顷
 * Hectare
 *
 * 定义：1 ha = 10,000 m² = 1 hm²
 * Definition: 1 ha = 10,000 m² = 1 hm²
 *
 * 常用于土地面积测量。
 * Commonly used for land area measurement.
 */
object Hectare : DerivedPhysicalUnit(Hectometer * Hectometer) {
    override val name = "hectare"
    override val symbol = "ha"

    override val quantity = Area
}

/**
 * 平方英寸
 * Square inch
 *
 * 定义：1 sq.in = (1 in)² = 6.4516 cm²
 * Definition: 1 sq.in = (1 in)² = 6.4516 cm²
 */
object SquareInch : DerivedPhysicalUnit(Inch * Inch) {
    override val name = "square inch"
    override val symbol = "sq.in"

    override val quantity = Area
}

/**
 * 平方英尺
 * Square foot
 *
 * 定义：1 sq.ft = (1 ft)² = 144 sq.in = 0.09290304 m²
 * Definition: 1 sq.ft = (1 ft)² = 144 sq.in = 0.09290304 m²
 */
object SquareFoot : DerivedPhysicalUnit(Foot * Foot) {
    override val name = "square foot"
    override val symbol = "sq.ft"

    override val quantity = Area
}

/**
 * 平方码
 * Square yard
 *
 * 定义：1 sq.yd = (1 yd)² = 9 sq.ft = 0.83612736 m²
 * Definition: 1 sq.yd = (1 yd)² = 9 sq.ft = 0.83612736 m²
 */
object SquareYard : DerivedPhysicalUnit(Yard * Yard) {
    override val name = "square yard"
    override val symbol = "sq.yd"

    override val quantity = Area
}

/**
 * 平方链
 * Square chain
 *
 * 定义：1 sq.ch = (1 ch)² = 66 ft × 66 ft = 404.68564224 m²
 * Definition: 1 sq.ch = (1 ch)² = 66 ft × 66 ft = 404.68564224 m²
 */
object SquareChain : DerivedPhysicalUnit(Chain * Chain) {
    override val name = "square chain"
    override val symbol = "sq.ch"

    override val quantity = Area
}

/**
 * 平方杆
 * Square rod
 *
 * 定义：1 sq.rd = (1 rd)² = 25.29285264 m²
 * Definition: 1 sq.rd = (1 rd)² = 25.29285264 m²
 */
object SquareRod : DerivedPhysicalUnit(Rod * Rod) {
    override val name = "square rod"
    override val symbol = "sq.rd"

    override val quantity = Area
}

/**
 * 平方英里
 * Square mile
 *
 * 定义：1 sq.mi = (1 mi)² = 2.589988110336 km²
 * Definition: 1 sq.mi = (1 mi)² = 2.589988110336 km²
 */
object SquareMile : DerivedPhysicalUnit(Mile * Mile) {
    override val name = "square mile"
    override val symbol = "sq.mi"

    override val quantity = Area
}

/**
 * 英亩
 * Acre
 *
 * 定义：1 acre = 43,560 sq.ft = 4,046.8564224 m²
 * Definition: 1 acre = 43,560 sq.ft = 4,046.8564224 m²
 *
 * 英制面积单位，常用于土地测量。
 * Imperial unit for area, commonly used in land surveying.
 */
object Acre : DerivedPhysicalUnit(SquareChain * 10) {
    override val name = "acre"
    override val symbol = "acre"

    override val quantity = Area
}