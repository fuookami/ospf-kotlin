/**
 * 表面密度单位 / Surface density units
 *
 * 提供表面密度量纲的单位定义，包括千克每平方米、克每平方米等。
 * Provides unit definitions for surface density dimension, including kilogram per square meter,
 * gram per square meter, etc.
 *
 * 来源：SI 导出单位
 * Source: SI derived units
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.SurfaceDensity

/**
 * 千克每平方米（基本单位）
 * Kilogram per square meter (base unit)
 *
 * 名称：千克每平方米
 * Name: kilogram per square meter
 *
 * 符号：kg/m²
 * Symbol: kg/m²
 *
 * 定义：1 kg/m² = 1 kg / 1 m²
 * Definition: 1 kg/m² = 1 kg / 1 m²
 *
 * 来源：SI 导出单位，表面密度的国际单位
 * Source: SI derived unit, international unit for surface density
 */
object KilogramPerSquareMeter : DerivedPhysicalUnit(Kilogram / SquareMeter) {
    override val name = "kilogram per square meter"
    override val symbol = "kgpm2"

    override val quantity = SurfaceDensity
}

/**
 * 克每平方米
 * Gram per square meter
 *
 * 名称：克每平方米
 * Name: gram per square meter
 *
 * 符号：g/m²
 * Symbol: g/m²
 *
 * 定义：1 g/m² = 10⁻³ kg/m²
 * Definition: 1 g/m² = 10⁻³ kg/m²
 *
 * 来源：SI 导出单位，常用于纸张、纺织品的表面密度
 * Source: SI derived unit, commonly used for paper and textile surface density
 */
object GramPerSquareMeter : DerivedPhysicalUnit(Gram / SquareMeter) {
    override val name = "gram per square meter"
    override val symbol = "gpm2"

    override val quantity = SurfaceDensity
}
