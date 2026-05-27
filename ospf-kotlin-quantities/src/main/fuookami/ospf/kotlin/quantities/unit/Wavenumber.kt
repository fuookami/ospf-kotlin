/**
 * 波数单位 / Wavenumber units
 *
 * 提供波数量纲的单位定义，包括每米等。
 * Provides unit definitions for wavenumber dimension, including reciprocal meter, etc.
 *
 * 来源：SI 导出单位
 * Source: SI derived units
 * - Reciprocal meter: 1/m, SI derived unit for wavenumber
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.WaveNumber

/**
 * 每米（SI 导出单位）
 * Reciprocal meter (SI derived unit)
 *
 * 名称：每米
 * Name: reciprocal meter
 *
 * 符号：1/m 或 m⁻¹
 * Symbol: 1/m or m⁻¹
 *
 * 定义：1 m⁻¹ = 1 / 1 m
 * Definition: 1 m⁻¹ = 1 / 1 m
 *
 * 来源：SI 导出单位，波数的国际单位
 * 用于光谱学中描述波的频率与光速的关系
 * Source: SI derived unit, international unit for wavenumber
 * Used in spectroscopy to describe the relationship between wave frequency and speed of light
 */
object ReciprocalMeter : DerivedPhysicalUnit(Meter.reciprocal()) {
    override val name = "reciprocal meter"
    override val symbol = "1/m"

    override val quantity = WaveNumber
}
