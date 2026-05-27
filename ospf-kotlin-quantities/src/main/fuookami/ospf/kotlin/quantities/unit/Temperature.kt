/**
 * 温度单位 / Temperature units
 *
 * 提供温度量纲的单位定义，包括开尔文、摄氏度、华氏度、兰氏度等。
 * Provides unit definitions for temperature dimension, including kelvin, celsius, fahrenheit, rankine, etc.
 *
 * 来源：SI 基本单位及常用温度单位定义
 * Source: SI base unit and common temperature unit definitions
 * - Kelvin: SI base unit for thermodynamic temperature
 * - Celsius: °C = K - 273.15
 * - Fahrenheit: °F = °C × 9/5 + 32
 * - Rankine: °R = K × 9/5
 */
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.Temperature

/**
 * 开尔文（SI 基本单位）
 * Kelvin (SI base unit)
 *
 * 名称：开尔文
 * Name: kelvin
 *
 * 符号：K
 * Symbol: K
 *
 * 定义：热力学温度的基本单位，水的三相点热力学温度的 1/273.16
 * Definition: Base unit of thermodynamic temperature, 1/273.16 of the thermodynamic temperature
 * of the triple point of water
 *
 * 来源：SI 七个基本单位之一
 * Source: One of the seven SI base units
 */
object Kelvin : PhysicalUnit() {
    override val name: String = "kelvin"
    override val symbol: String = "K"

    override val quantity = Temperature
    override val scale = Scale()
}

/**
 * 摄氏度
 * Celsius
 *
 * 名称：摄氏度
 * Name: celsius
 *
 * 符号：°C
 * Symbol: °C
 *
 * 定义：t/°C = T/K - 273.15
 * Definition: t/°C = T/K - 273.15
 *
 * 来源：常用温度单位，基于水的冰点（0°C）和沸点（100°C）
 * Source: Common temperature unit, based on water's freezing point (0°C) and boiling point (100°C)
 */
object Celsius : PhysicalUnit() {
    override val name: String = "celsius"
    override val symbol: String = "°C"

    override val quantity = Temperature
    override val scale = Scale()
}

/**
 * 华氏度
 * Fahrenheit
 *
 * 名称：华氏度
 * Name: fahrenheit
 *
 * 符号：°F
 * Symbol: °F
 *
 * 定义：t/°F = t/°C × 9/5 + 32
 * Definition: t/°F = t/°C × 9/5 + 32
 *
 * 换算因子：相对于开尔文的缩放比例为 5/9
 * Conversion factor: Scale factor relative to kelvin is 5/9
 *
 * 来源：常用温度单位，主要使用于美国
 * Source: Common temperature unit, primarily used in the United States
 */
object Fahrenheit : PhysicalUnit() {
    override val name: String = "fahrenheit"
    override val symbol: String = "°F"

    override val quantity = Temperature
    override val scale = Scale(5.0 / 9.0)
}

/**
 * 兰氏度
 * Rankine
 *
 * 名称：兰氏度
 * Name: rankine
 *
 * 符号：°R
 * Symbol: °R
 *
 * 定义：T/°R = T/K × 9/5
 * Definition: T/°R = T/K × 9/5
 *
 * 换算因子：相对于开尔文的缩放比例为 5/9
 * Conversion factor: Scale factor relative to kelvin is 5/9
 *
 * 来源：热力学温度单位，华氏温标的绝对温标
 * Source: Thermodynamic temperature unit, absolute scale of Fahrenheit
 */
object Rankine : PhysicalUnit() {
    override val name: String = "rankine"
    override val symbol: String = "°R"

    override val quantity = Temperature
    override val scale = Scale(5.0 / 9.0)
}
