/**
 * 携带量纲信息的符号
 * Symbol with Dimension Information
 *
 * 用于量纲语义校验，确保符号表达式具备物理意义。
 * Used for dimension semantic validation to ensure symbol expressions have physical meaning.
 *
 * 主要用途 / Main use cases:
 * - 在符号表达式中携带量纲信息 / Carry dimension information in symbol expressions
 * - 验证运算的量纲正确性 / Validate dimension correctness of operations
 * - 推导运算结果的量纲 / Infer dimension of operation results
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = DimensionedSymbol("x", "distance", DerivedQuantity.Length, Meter)
 * val time = DimensionedSymbol("t", "time", DerivedQuantity.Time, Second)
 * val speedDimension = distance.divideBy(time)  // DerivedQuantity.Speed
 * ```
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.dimension.*

/**
 * 携带量纲信息的符号
 * Symbol with dimension information
 *
 * 用于量纲语义校验，确保符号表达式具备物理意义。
 * Used for dimension semantic validation to ensure symbol expressions have physical meaning.
 *
 * 示例 / Example:
 * ```kotlin
 * val x = DimensionedSymbol(
 *     name = "x",
 *     displayName = "distance",
 *     quantity = DerivedQuantity.Length,
 *     preferredUnit = Meter
 * )
 * ```
 *
 * @property name 符号名称 / Symbol name
 * @property displayName 显示名称（可选）/ Display name (optional)
 * @property quantity 量纲 / Dimension
 * @property preferredUnit 首选单位（可选）/ Preferred unit (optional)
 */
data class DimensionedSymbol(
    override val name: String,
    override val displayName: String? = null,
    val quantity: DerivedQuantity,
    val preferredUnit: PhysicalUnit? = null
) : Symbol {

    /**
     * 检查是否可以与另一个符号相加
     * Check if this symbol can be added to another
     *
     * 只有量纲相同的符号才能相加。
     * Only symbols with the same dimension can be added.
     *
     * 示例 / Example:
     * ```kotlin
     * val x = DimensionedSymbol("x", null, DerivedQuantity.Length, Meter)
     * val y = DimensionedSymbol("y", null, DerivedQuantity.Length, Meter)
     * val canAdd = x.canAddTo(y)  // true
     *
     * val z = DimensionedSymbol("z", null, DerivedQuantity.Time, Second)
     * val canAdd2 = x.canAddTo(z)  // false
     * ```
     *
     * @param other 另一个符号 / Another symbol
     * @return 是否可以相加 / Whether can be added
     */
    fun canAddTo(other: DimensionedSymbol): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 与另一个符号相乘得到的新量纲
     * Get the resulting dimension from multiplying with another symbol
     *
     * 示例 / Example:
     * ```kotlin
     * val length = DimensionedSymbol("L", null, DerivedQuantity.Length, Meter)
     * val length2 = DimensionedSymbol("L2", null, DerivedQuantity.Length, Meter)
     * val areaDimension = length.multiplyWith(length2)  // DerivedQuantity.Area (Length^2)
     * ```
     *
     * @param other 另一个符号 / Another symbol
     * @return 相乘后的量纲 / Dimension after multiplication
     */
    fun multiplyWith(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity * other.quantity
    }

    /**
     * 除以另一个符号得到的新量纲
     * Get the resulting dimension from dividing by another symbol
     *
     * 示例 / Example:
     * ```kotlin
     * val distance = DimensionedSymbol("d", null, DerivedQuantity.Length, Meter)
     * val time = DimensionedSymbol("t", null, DerivedQuantity.Time, Second)
     * val speedDimension = distance.divideBy(time)  // DerivedQuantity.Speed (Length/Time)
     * ```
     *
     * @param other 另一个符号 / Another symbol
     * @return 相除后的量纲 / Dimension after division
     */
    fun divideBy(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity / other.quantity
    }
}
