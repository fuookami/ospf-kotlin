/**
 * 物质的量单位
 * Amount of substance units
 *
 * 提供物质的量量纲的 SI 单位定义。
 * Provides SI unit definitions for amount of substance dimension.
 *
 * SI 基本单位：摩尔 (mol)
 * SI base unit: mole (mol)
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.AmountOfSubstance

/**
 * 摩尔（基本单位）
 * Mole (base unit)
 *
 * SI 基本单位，用于表示物质的量。
 * SI base unit for amount of substance.
 *
 * 定义：摩尔是包含精确 6.02214076×10²³ 个基本实体的物质的量。
 * Definition: The mole is the amount of substance containing exactly 6.02214076×10²³ elementary entities.
 */
object Mole : PhysicalUnit() {
    override val name = "mole"
    override val symbol = "mol"

    override val quantity = AmountOfSubstance
    override val conversionRule = UnitConversionRule.Linear(Scale())
}
