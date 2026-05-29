/**
 * 类型擦除的变量包装器
 * Type-erased variable wrapper
 *
 * 将所有 VariableType 变体统一为单一类型参数 V，通过 IntoValue<V> 提供 V 类型的访问器。
 * Unifies all VariableType variants into a single type parameter V,
 * providing V-typed accessors via IntoValue<V> conversion.
 */
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 类型擦除的变量包装器
 * Type-erased variable wrapper
 *
 * 将所有 VariableType 变体统一为单一类型参数 V。
 * 在 Kotlin 中委托给 AbstractVariableItem 持有元数据，并通过 IntoValue<V> 转换提供 V 类型访问器。
 * Unifies all VariableTypeTrait variants into a single V-parametric type.
 * In Kotlin, delegates to AbstractVariableItem which holds all metadata,
 * and provides V-typed accessors via IntoValue<V> conversion.
 *
 * @param V 数值类型 / The number type
 * @property data 底层变量数据 / Underlying variable data
 * @property id 变量唯一标识 / Variable unique key
 * @property index 变量索引 / Variable index
 * @property name 变量名称 / Variable name
 * @property displayName 显示名称 / Display name
 * @property varType 变量类型 / Variable type
 * @property lowerBoundFlt64 Flt64 下界 / Flt64 lower bound
 * @property upperBoundFlt64 Flt64 上界 / Flt64 upper bound
 */
class AnyVariable<V : RealNumber<V>>(
    val data: AbstractVariableItem<*, *>
) {
    /** 变量唯一标识 / Variable unique key */
    val id: VariableItemKey get() = data.key
    /** 变量索引 / Variable index */
    val index: Int get() = data.index
    /** 变量名称 / Variable name */
    val name: String get() = data.name
    /** 显示名称 / Display name */
    val displayName: String? get() = data.displayName
    /** 变量类型 / Variable type */
    val varType: VariableType<*> get() = data.type

    /** Flt64 下界 / Flt64 lower bound */
    val lowerBoundFlt64: Flt64? get() = data.lowerBound?.value?.toFlt64()
    /** Flt64 上界 / Flt64 upper bound */
    val upperBoundFlt64: Flt64? get() = data.upperBound?.value?.toFlt64()

    /**
     * 获取 V 类型下界
     * Get V-typed lower bound
     *
     * @param converter 值转换器 / Value converter
     * @return V 类型下界，如果无界返回 null / V-typed lower bound, or null if unbounded
     */
    fun lowerBound(converter: IntoValue<V>): V? = lowerBoundFlt64?.let { converter.intoValue(it) }

    /**
     * 获取 V 类型上界
     * Get V-typed upper bound
     *
     * @param converter 值转换器 / Value converter
     * @return V 类型上界，如果无界返回 null / V-typed upper bound, or null if unbounded
     */
    fun upperBound(converter: IntoValue<V>): V? = upperBoundFlt64?.let { converter.intoValue(it) }

    /**
     * 检查 Flt64 值是否在有效范围内
     * Check if a Flt64 value is within valid range
     *
     * @param value 待检查的值 / Value to check
     * @return 是否有效 / Whether valid
     */
    fun isValidValue(value: Flt64): Boolean {
        val lb = lowerBoundFlt64
        val ub = upperBoundFlt64
        if (lb != null && value < lb) return false
        if (ub != null && value > ub) return false
        return true
    }

    /**
     * 检查 V 值是否在有效范围内
     * Check if a V value is within valid range
     *
     * @param value 待检查的值 / Value to check
     * @return 是否有效 / Whether valid
     */
    fun isValidValue(value: V): Boolean {
        val f64 = value.toFlt64()
        return isValidValue(f64)
    }

    override fun hashCode(): Int = data.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyVariable<*>) return false
        return data == other.data
    }
    override fun toString(): String = data.toString()

    companion object {
        /**
         * 从 AbstractVariableItem 创建 AnyVariable
         * Create AnyVariable from AbstractVariableItem
         *
         * @param V 数值类型 / The number type
         * @param item 变量项 / Variable item
         * @return AnyVariable 实例 / AnyVariable instance
         */
        fun <V : RealNumber<V>> from(item: AbstractVariableItem<*, *>): AnyVariable<V> {
            return AnyVariable(item)
        }
    }
}
