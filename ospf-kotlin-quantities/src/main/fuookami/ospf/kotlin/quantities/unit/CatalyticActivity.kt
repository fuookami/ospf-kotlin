/**
 * 催化活度单位 / Catalytic activity units
 *
 * 提供催化活度量纲的 SI 单位定义，包括开特、毫开特、微开特、酶单位等。
 * Provides SI unit definitions for catalytic activity dimension, including katal, millikatal,
 * microkatal, enzyme unit, etc.
 *
 * 来源：SI 基本单位定义
 * Source: SI base unit definitions
 * - Katal: mol/s (SI derived unit for catalytic activity)
 * - Enzyme Unit (U): 1 μmol/min = 1/60 μmol/s ≈ 16.67 nmol/s
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.dimension.CatalyticActivity

/**
 * 开特（SI 导出单位）
 * Katal (SI derived unit)
 *
 * 名称：开特
 * Name: katal
 *
 * 符号：kat
 * Symbol: kat
 *
 * 定义：1 kat = 1 mol / 1 s
 * Definition: 1 kat = 1 mol / 1 s
 *
 * 来源：SI 导出单位，催化活度的国际单位
 * 于 1999 年第 21 届国际计量大会采纳
 * Source: SI derived unit, international unit for catalytic activity
 * Adopted at the 21st General Conference on Weights and Measures in 1999
 */
object Katal : DerivedPhysicalUnit(Mole / Second) {
    override val name = "katal"
    override val symbol = "kat"

    override val quantity = CatalyticActivity
}

/**
 * 毫开特
 * Millikatal
 *
 * 名称：毫开特
 * Name: millikatal
 *
 * 符号：mkat
 * Symbol: mkat
 *
 * 定义：1 mkat = 10⁻³ kat
 * Definition: 1 mkat = 10⁻³ kat
 *
 * 来源：SI 催化活度单位
 * Source: SI catalytic activity unit
 */
object Millikatal : DerivedPhysicalUnit(Katal * Scale.milli) {
    override val name = "millikatal"
    override val symbol = "mkat"

    override val quantity = CatalyticActivity
}

/**
 * 微开特
 * Microkatal
 *
 * 名称：微开特
 * Name: microkatal
 *
 * 符号：μkat
 * Symbol: μkat
 *
 * 定义：1 μkat = 10⁻⁶ kat
 * Definition: 1 μkat = 10⁻⁶ kat
 *
 * 来源：SI 催化活度单位，常用于酶学
 * Source: SI catalytic activity unit, commonly used in enzymology
 */
object Microkatal : DerivedPhysicalUnit(Katal * Scale.micro) {
    override val name = "microkatal"
    override val symbol = "μkat"

    override val quantity = CatalyticActivity
}

/**
 * 酶单位（U）
 * Enzyme unit (U)
 *
 * 名称：酶单位
 * Name: enzyme unit
 *
 * 符号：U
 * Symbol: U
 *
 * 定义：1 U = 1 μmol/min = 1/60 μmol/s ≈ 16.67 nmol/s
 * Definition: 1 U = 1 μmol/min = 1/60 μmol/s ≈ 16.67 nmol/s
 *
 * 来源：生化领域常用单位，定义酶催化反应速率
 * 1 U ≈ 16.67 nkat
 * Source: Common unit in biochemistry, defines enzyme catalytic reaction rate
 * 1 U ≈ 16.67 nkat
 */
object EnzymeUnit : DerivedPhysicalUnit(Katal / FltX(60000000.0)) {
    override val name = "enzyme unit"
    override val symbol = "U"

    override val quantity = CatalyticActivity
}
