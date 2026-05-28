/**
 * 物理量值范围扩展
 * Value Range Extensions for Physical Quantities
 *
 * 为物理量提供值范围相关的扩展属性和函数。
 * Provides value range-related extension properties and functions for physical quantities.
 *
 * 支持从 ValueRange、Bound、ValueWrapper 等包装类型中提取物理量值。
 * Supports extracting quantity values from wrapper types like ValueRange, Bound, and ValueWrapper.
 */
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 获取值范围物理量的下界
 * Get the lower bound of a value range quantity
 *
 * 返回一个新的物理量，其值为原物理量值范围的下界，单位不变。
 * Returns a new quantity with the lower bound value of the original quantity's value range,
 * keeping the same unit.
 *
 * 示例 / Example:
 * ```kotlin
 * val range = Quantity(ValueRange(Flt64(1.0), Flt64(5.0)), Meter)
 * val lower = range.lowerBound  // Quantity(Flt64(1.0), Meter)
 * ```
 */
val <V> Quantity<ValueRange<V>>.lowerBound where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.lowerBound, unit)

/**
 * 获取值范围物理量的上界
 * Get the upper bound of a value range quantity
 *
 * 返回一个新的物理量，其值为原物理量值范围的上界，单位不变。
 * Returns a new quantity with the upper bound value of the original quantity's value range,
 * keeping the same unit.
 *
 * 示例 / Example:
 * ```kotlin
 * val range = Quantity(ValueRange(Flt64(1.0), Flt64(5.0)), Meter)
 * val upper = range.upperBound  // Quantity(Flt64(5.0), Meter)
 * ```
 */
val <V> Quantity<ValueRange<V>>.upperBound where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.upperBound, unit)

/**
 * 获取值范围物理量的差值
 * Get the difference of a value range quantity
 *
 * 返回一个新的物理量，其值为原物理量值范围的上界减下界，单位不变。
 * Returns a new quantity with the difference between upper and lower bounds,
 * keeping the same unit.
 *
 * 示例 / Example:
 * ```kotlin
 * val range = Quantity(ValueRange(Flt64(1.0), Flt64(5.0)), Meter)
 * val diff = range.diff  // Quantity(Flt64(4.0), Meter)
 * ```
 */
val <V> Quantity<ValueRange<V>>.diff where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.diff, unit)

/**
 * 获取边界物理量的边界值
 * Get the bound value of a bound quantity
 *
 * 返回一个新的物理量，其值为原物理量边界的值，单位不变。
 * Returns a new quantity with the bound's value, keeping the same unit.
 *
 * 示例 / Example:
 * ```kotlin
 * val bound = Quantity(Bound(Flt64(3.0), inclusive = true), Meter)
 * val value = bound.boundValue  // Quantity(Flt64(3.0), Meter)
 * ```
 */
val <V> Quantity<Bound<V>>.boundValue where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.value, unit)

/**
 * 解包值包装器物理量
 * Unwrap a value wrapper quantity
 *
 * 将包装在 ValueWrapper 中的值提取出来，返回一个新的物理量。
 * Extracts the value wrapped in ValueWrapper and returns a new quantity.
 *
 * 示例 / Example:
 * ```kotlin
 * val wrapped = Quantity(ValueWrapper(Flt64(3.0)), Meter)
 * val unwrapped = wrapped.unwrap()  // Quantity(Flt64(3.0), Meter)
 * ```
 *
 * @return 解包后的物理量 / The unwrapped quantity
 */
fun <V> Quantity<ValueWrapper<V>>.unwrap(): Quantity<V> where V : RealNumber<V>, V : NumberField<V> {
    return Quantity(value.unwrap(), unit)
}

/**
 * 解包值包装器物理量（可空版本）
 * Unwrap a value wrapper quantity (nullable version)
 *
 * 将包装在 ValueWrapper 中的值提取出来，如果包装器为空则返回 null。
 * Extracts the value wrapped in ValueWrapper, returns null if the wrapper is empty.
 *
 * 示例 / Example:
 * ```kotlin
 * val wrapped = Quantity(ValueWrapper(Flt64(3.0)), Meter)
 * val unwrapped = wrapped.unwrapOrNull()  // Quantity(Flt64(3.0), Meter)
 *
 * val empty = Quantity(ValueWrapper<Flt64>(null), Meter)
 * val result = empty.unwrapOrNull()  // null
 * ```
 *
 * @return 解包后的物理量，如果包装器为空则返回 null / The unwrapped quantity, or null if the wrapper is empty
 */
fun <V> Quantity<ValueWrapper<V>>.unwrapOrNull(): Quantity<V>? where V : RealNumber<V>, V : NumberField<V> {
    return value.unwrapOrNull()?.let {
        Quantity(it, unit)
    }
}
