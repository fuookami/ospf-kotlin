/**
 * 质量密度单位 / Mass density units
 *
 * 用于测量物质密度（单位体积质量）的单位。
 * Units for measuring the density of substances (mass per unit volume).
 *
 * 单位常量来源 / Unit constant sources:
 * - KilogramPerCubicMeter: SI导出单位 / SI derived unit
 * - KilogramPerLiter: 1 kg/L = 1000 kg/m³
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.MassDensity

/**
 * 千克每立方米 / Kilogram per cubic meter
 *
 * 质量密度的SI导出单位。
 * The SI derived unit of mass density.
 *
 * 符号 / Symbol: kg/m³
 * 换算关系 / Conversion: 1 kg/m³ = 1 kg/m³（基本单位）/ 1 kg/m³ = 1 kg/m³ (base unit)
*/
object KilogramPerCubicMeter : DerivedPhysicalUnit(Kilogram / CubicMeter) {

    /** 单位名称：kilogram per cubic meter / Unit name: kilogram per cubic meter */
    override val name = "kilogram per cubic meter"

    /** 单位符号：kg/m³ / Unit symbol: kg/m³ */
    override val symbol = "kgpm3"

    /** 对应物理量：质量密度 / Corresponding quantity: MassDensity */
    override val quantity = MassDensity
}

/**
 * 千克每升 / Kilogram per liter
 *
 * 常用于液体密度的单位。
 * A unit commonly used for liquid density.
 *
 * 符号 / Symbol: kg/L
 * 换算关系 / Conversion: 1 kg/L = 1000 kg/m³
*/
object KilogramPerLiter : DerivedPhysicalUnit(Kilogram / Liter) {

    /** 单位名称：kilogram per liter / Unit name: kilogram per liter */
    override val name = "kilogram per liter"

    /** 单位符号：kg/L / Unit symbol: kg/L */
    override val symbol = "kgpL"

    /** 对应物理量：质量密度 / Corresponding quantity: MassDensity */
    override val quantity = MassDensity
}

/**
 * 千克每立方厘米 / Kilogram per cubic centimeter
 *
 * 用于高密度物质的单位。
 * A unit used for high-density substances.
 *
 * 符号 / Symbol: kg/cm³
 * 换算关系 / Conversion: 1 kg/cm³ = 10⁶ kg/m³
*/
object KilogramPerCubicCentimeter : DerivedPhysicalUnit(Kilogram / CubicCentimeter) {

    /** 单位名称：kilogram per cubic centimeter / Unit name: kilogram per cubic centimeter */
    override val name = "kilogram per cubic meter"

    /** 单位符号：kg/cm³ / Unit symbol: kg/cm³ */
    override val symbol = "kgpm3"

    /** 对应物理量：质量密度 / Corresponding quantity: MassDensity */
    override val quantity = MassDensity
}

/**
 * 克每立方厘米 / Gram per cubic centimeter
 *
 * 常用于固体和液体密度的单位。
 * A unit commonly used for solid and liquid densities.
 *
 * 符号 / Symbol: g/cm³
 * 换算关系 / Conversion: 1 g/cm³ = 1000 kg/m³
*/
object GramPerCubicCentimeter : DerivedPhysicalUnit(Gram / CubicCentimeter) {

    /** 单位名称：gram per cubic centimeter / Unit name: gram per cubic centimeter */
    override val name = "gram per cubic meter"

    /** 单位符号：g/cm³ / Unit symbol: g/cm³ */
    override val symbol = "gpm3"

    /** 对应物理量：质量密度 / Corresponding quantity: MassDensity */
    override val quantity = MassDensity
}
