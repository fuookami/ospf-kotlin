package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.div
import fuookami.ospf.kotlin.quantities.dimension.times
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/**
 * 携带量纲信息的符号
 * Symbol with dimension information
 *
 * 用于量纲语义校验，确保符号表达式具备物理意义。
 * Used for dimension semantic validation to ensure symbol expressions have physical meaning.
 *
 * @property name 符号名称
 * @property displayName 显示名称
 * @property quantity 量纲
 * @property preferredUnit 首选单位
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
     */
    fun canAddTo(other: DimensionedSymbol): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 与另一个符号相乘得到的新量纲
     * Get the resulting dimension from multiplying with another symbol
     */
    fun multiplyWith(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity * other.quantity
    }

    /**
     * 除以另一个符号得到的新量纲
     * Get the resulting dimension from dividing by another symbol
     */
    fun divideBy(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity / other.quantity
    }
}