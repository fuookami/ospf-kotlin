/**
 * 量纲符号转换工具 / Quantity symbol conversion utilities
 *
 * 提供带有物理单位的符号量纲转换扩展函数。
 *
 * Provides extension functions for converting symbol quantities with physical units.
 */
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import kotlin.jvm.JvmName

/**
 * 将量纲符号转换为目标单位 / Convert a quantity symbol to the target unit
 *
 * @param unit 目标物理单位 / Target physical unit
 * @return 转换后的量纲符号，若单位不可转换则返回 null / Converted quantity symbol, or null if units are incompatible
 */
@JvmName("convertQuantitySymbol")
fun <V : Symbol> Quantity<V>.to(unit: PhysicalUnit): Quantity<V>? {
    return if (this.unit.canConvertTo(unit)) {
        Quantity(this.value, unit)
    } else {
        null
    }
}
