/**
 * 二维装箱量纲运算核心。 / Core quantity operations for 2D packing.
 *
 * `ospf-kotlin-quantities` 导出的 `Quantity<V>.plus/minus/times/div` 是**可空且受特化约束**的扩展，
 * 且单位层运算会重复叠加导出量纲的基础幂次。二维装箱的领域代码需要**非空、泛型、按基础量纲正确合成**
 * 的量值运算，因此在此集中提供整套操作符，并在量纲不兼容时以 [Ret] 失败的形式暴露错误。
 * / The `plus/minus/times/div` extensions exported by the quantity module are nullable, constrained to
 * concrete specialisations, and the unit-level operators re-apply the base powers of derived dimensions.
 * 2D packing domain code needs non-null generic operators with correct base-power composition, so this file
 * provides them centrally while surfacing dimension mismatches as [Ret] failures.
*/
package fuookami.ospf.kotlin.framework.geometry2d

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.FundamentalQuantity
import fuookami.ospf.kotlin.quantities.dimension.FundamentalQuantityDimension
import fuookami.ospf.kotlin.quantities.dimension.QuantityDomain
import fuookami.ospf.kotlin.quantities.dimension.div
import fuookami.ospf.kotlin.quantities.dimension.times
import fuookami.ospf.kotlin.quantities.unit.AnonymousPhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.UnitConversionRule

/**
 * 构造量纲。 / Build a derived dimension from base powers.
 *
 * @param powers 基础量纲幂次 / base dimension powers
 * @return 量纲 / composed dimension
*/
private fun derivedDimension(powers: Map<FundamentalQuantityDimension, Int>): DerivedQuantity {
    return DerivedQuantity(
        quantities = powers.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) }
    )
}

/**
 * 合成两个单位相乘后的单位：基础量纲幂次相加。 / Compose the unit of a unit product: base powers are added.
 *
 * `PhysicalUnit.*` 会把导出量纲的基础幂次再叠加一次（`L × L` 得到 `L³`），这里改为直接按基础
 * 量纲幂次合成，`Meter × Meter` 与 `SquareMeter` 使用的单位完全一致。
 * / `PhysicalUnit.*` re-applies the base powers of derived dimensions (`L × L` becomes `L³`). This helper
 * composes base powers directly, so `Meter × Meter` equals the unit used by `SquareMeter`.
 *
 * @param lhs 左单位 / left-hand unit
 * @param rhs 右单位 / right-hand unit
 * @return 相乘后的单位 / composed unit
*/
internal fun productUnit(lhs: PhysicalUnit, rhs: PhysicalUnit): PhysicalUnit {
    val powers = LinkedHashMap<FundamentalQuantityDimension, Int>()
    for (quantity in lhs.quantity.quantities) {
        powers[quantity.dimension] = (powers[quantity.dimension] ?: 0) + quantity.index
    }
    for (quantity in rhs.quantity.quantities) {
        powers[quantity.dimension] = (powers[quantity.dimension] ?: 0) + quantity.index
    }
    return AnonymousPhysicalUnit(
        quantity = derivedDimension(powers),
        conversionRule = UnitConversionRule.Linear(
            lhs.scale * rhs.scale
        ),
        domain = lhs.domain * rhs.domain
    )
}

/**
 * 合成两个单位相除后的单位：基础量纲幂次相减。 / Compose the unit of a unit quotient: base powers are subtracted.
 *
 * @param lhs 左单位 / left-hand unit
 * @param rhs 右单位 / right-hand unit
 * @return 相除后的单位 / composed unit
*/
internal fun quotientUnit(lhs: PhysicalUnit, rhs: PhysicalUnit): PhysicalUnit {
    val powers = LinkedHashMap<FundamentalQuantityDimension, Int>()
    for (quantity in lhs.quantity.quantities) {
        powers[quantity.dimension] = (powers[quantity.dimension] ?: 0) + quantity.index
    }
    for (quantity in rhs.quantity.quantities) {
        powers[quantity.dimension] = (powers[quantity.dimension] ?: 0) - quantity.index
    }
    return AnonymousPhysicalUnit(
        quantity = derivedDimension(powers),
        conversionRule = UnitConversionRule.Linear(
            lhs.scale / rhs.scale
        ),
        domain = lhs.domain / rhs.domain
    )
}

/**
 * 量值加法：要求同量纲，结果沿用左操作数单位。 / Quantity addition: same dimension required, result keeps the left unit.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 右操作数 / right-hand operand
 * @return 加法结果 / addition result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.plus(rhs: Quantity<V>): Quantity<V> {
    return Quantity(value + rhs.value, unit)
}

/**
 * 量值减法：要求同量纲，结果沿用左操作数单位。 / Quantity subtraction: same dimension required, result keeps the left unit.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 右操作数 / right-hand operand
 * @return 减法结果 / subtraction result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.minus(rhs: Quantity<V>): Quantity<V> {
    return Quantity(value - rhs.value, unit)
}

/**
 * 量值乘法（标量缩放）：单位保持不变。 / Quantity multiplication by a scalar; the unit is unchanged.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 标量 / scalar
 * @return 乘法结果 / multiplication result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.times(rhs: V): Quantity<V> {
    return Quantity(value * rhs, unit)
}

/**
 * 量值乘法：单位按基础量纲幂次合成（`L × L = L²`）。 / Quantity multiplication; units are composed by base powers.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 右操作数 / right-hand operand
 * @return 乘法结果 / multiplication result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.times(rhs: Quantity<V>): Quantity<V> {
    return quantityMultiply(this, rhs)
}

/**
 * 两个量值的乘积：单位按基础量纲幂次合成，数值为标量乘积。 / Product of two quantities: units are composed by
 * base powers and the value is the scalar product.
 *
 * 例如 `2 m × 3 m = 6 m²`。 / For example `2 m × 3 m = 6 m²`.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 乘积量值 / product quantity
*/
fun <V : FloatingNumber<V>> quantityMultiply(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return Quantity(lhs.value * rhs.value, productUnit(lhs.unit, rhs.unit))
}

/**
 * 量值除法：单位按基础量纲幂次合成。 / Quantity division; units are composed by base powers.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 右操作数 / right-hand operand
 * @return 除法结果 / division result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.div(rhs: Quantity<V>): Quantity<V> {
    return Quantity(value / rhs.value, quotientUnit(unit, rhs.unit))
}

/**
 * 量值除以标量：单位保持不变。 / Quantity division by a scalar; the unit is unchanged.
 *
 * @param V 数值类型 / numeric type
 * @param rhs 标量 / scalar
 * @return 除法结果 / division result
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.div(rhs: V): Quantity<V> {
    return Quantity(value / rhs, unit)
}

/**
 * 量值取负。 / Quantity negation.
 *
 * @param V 数值类型 / numeric type
 * @return 取负结果 / negated quantity
*/
operator fun <V : FloatingNumber<V>> Quantity<V>.unaryMinus(): Quantity<V> {
    return Quantity(-value, unit)
}

/**
 * 判断两个量值是否同量纲。 / Check whether two quantities share a dimension.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否同量纲 / whether they share a dimension
*/
fun <V : FloatingNumber<V>> isSameDimension(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
    return lhs.unit.quantity == rhs.unit.quantity
}

/**
 * 量值加法，量纲不兼容时失败。 / Quantity addition, failing on a dimension mismatch.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 加法结果或失败 / addition result, or failure
*/
fun <V : FloatingNumber<V>> quantityPlusSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (!isSameDimension(lhs, rhs)) {
        return dimensionMismatch("+", lhs, rhs)
    }
    return ok(lhs + rhs)
}

/**
 * 量值减法，量纲不兼容时失败。 / Quantity subtraction, failing on a dimension mismatch.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 减法结果或失败 / subtraction result, or failure
*/
fun <V : FloatingNumber<V>> quantityMinusSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (!isSameDimension(lhs, rhs)) {
        return dimensionMismatch("-", lhs, rhs)
    }
    return ok(lhs - rhs)
}

/**
 * 构造量纲不兼容错误。 / Build a dimension-mismatch error.
 *
 * @param V 数值类型 / numeric type
 * @param symbol 运算符 / operator symbol
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 失败结果 / failure result
*/
private fun <V : FloatingNumber<V>> dimensionMismatch(
    symbol: String,
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Ret<Quantity<V>> {
    val lhsSymbol = lhs.unit.quantity.dimensionSymbol()
    val rhsSymbol = rhs.unit.quantity.dimensionSymbol()
    return Failed(
        ErrorCode.IllegalArgument,
        "量纲不兼容：$lhsSymbol $symbol $rhsSymbol / Incompatible dimensions: $lhsSymbol $symbol $rhsSymbol"
    )
}

/**
 * 量值比较，量纲不可比较时返回 null。 / Compare two quantities, returning null when they are incomparable.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 比较结果或 null / comparison order, or null
*/
fun <V : FloatingNumber<V>> quantityOrdOrNull(lhs: Quantity<V>, rhs: Quantity<V>): Order? {
    if (!isSameDimension(lhs, rhs)) {
        return null
    }
    return lhs.partialOrd(rhs)
}

/**
 * 量值比较，量纲不可比较时失败。 / Compare two quantities, failing when they are incomparable.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @param axis 比较轴名称 / axis name for the error message
 * @return 比较结果或失败 / comparison order, or failure
*/
fun <V : FloatingNumber<V>> quantityOrdSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Order> {
    val order = quantityOrdOrNull(lhs, rhs)
    if (order == null) {
        return Failed(
            ErrorCode.IllegalArgument,
            "轴 $axis 上的量纲不可比较：${lhs.unit} vs ${rhs.unit} / Incomparable quantities on axis $axis: ${lhs.unit} vs ${rhs.unit}"
        )
    }
    return ok(order)
}

/**
 * 量值取较大者，量纲不可比较时失败。 / Quantity maximum, failing when the quantities are incomparable.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @param axis 比较轴名称 / axis name for the error message
 * @return 较大的量值或失败 / the larger quantity, or failure
*/
fun <V : FloatingNumber<V>> quantityMaxSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return when (val result = quantityOrdSafe(lhs, rhs, axis)) {
        is Ok -> ok(if (result.value is Order.Greater) lhs else rhs)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 量值取较小者，量纲不可比较时失败。 / Quantity minimum, failing when the quantities are incomparable.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @param axis 比较轴名称 / axis name for the error message
 * @return 较小的量值或失败 / the smaller quantity, or failure
*/
fun <V : FloatingNumber<V>> quantityMinSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return when (val result = quantityOrdSafe(lhs, rhs, axis)) {
        is Ok -> ok(if (result.value is Order.Greater) rhs else lhs)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

/**
 * 构造与样本同单位的零值。 / Build a zero value sharing the sample's unit.
 *
 * @param V 数值类型 / numeric type
 * @param sample 样本量值 / sample quantity
 * @return 零值 / zero quantity
*/
fun <V : FloatingNumber<V>> quantityZeroOf(sample: Quantity<V>): Quantity<V> {
    return Quantity(sample.value.constants.zero, sample.unit)
}

/**
 * 构造与样本同单位的指定数值量值。 / Build a quantity with the sample's unit and the given scalar.
 *
 * @param V 数值类型 / numeric type
 * @param sample 样本量值 / sample quantity
 * @param scaler 数值 / scalar value
 * @return 量值 / quantity
*/
fun <V : FloatingNumber<V>> quantityOf(sample: Quantity<V>, scaler: V): Quantity<V> {
    return Quantity(scaler, sample.unit)
}

/**
 * 判断量值是否大于零。 / Check whether a quantity is greater than zero.
 *
 * @param V 数值类型 / numeric type
 * @param value 待检查的量值 / quantity to check
 * @return 是否大于零 / whether greater than zero
*/
fun <V : FloatingNumber<V>> quantityIsPositive(value: Quantity<V>): Boolean {
    return quantityGreater(value, quantityZeroOf(value))
}

/**
 * 判断量值是否小于等于零。 / Check whether a quantity is less than or equal to zero.
 *
 * @param V 数值类型 / numeric type
 * @param value 待检查的量值 / quantity to check
 * @return 是否小于等于零 / whether less than or equal to zero
*/
fun <V : FloatingNumber<V>> quantityIsNonPositive(value: Quantity<V>): Boolean {
    return quantityLessOrEqual(value, quantityZeroOf(value))
}

/**
 * 判断两个量值是否相等：要求同量纲且数值文本一致。 / Check whether two quantities are equal: same dimension and
 * identical value text.
 *
 * 使用文本比较是为了兼容不同数值特化（`Flt64` 与 `FltX` 的精度表示不同）。 / Text comparison keeps different
 * numeric specialisations comparable despite their different precision representations.
 *
 * @param V1 左数值类型 / left numeric type
 * @param V2 右数值类型 / right numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否相等 / whether they are equal
*/
fun <V1 : FloatingNumber<V1>, V2 : FloatingNumber<V2>> quantityEqual(lhs: Quantity<V1>, rhs: Quantity<V2>): Boolean {
    if (lhs.unit.quantity != rhs.unit.quantity) {
        return false
    }
    return lhs.value.toString() == rhs.value.toString()
}

/**
 * 判断左量值是否严格大于右量值。 / Check whether the left quantity is strictly greater than the right one.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否严格大于 / whether strictly greater
*/
fun <V : FloatingNumber<V>> quantityGreater(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
    return quantityOrdOrNull(lhs, rhs) is Order.Greater
}

/**
 * 判断左量值是否严格小于右量值。 / Check whether the left quantity is strictly less than the right one.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否严格小于 / whether strictly less
*/
fun <V : FloatingNumber<V>> quantityLess(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
    return quantityOrdOrNull(lhs, rhs) is Order.Less
}

/**
 * 判断左量值是否大于等于右量值。 / Check whether the left quantity is greater than or equal to the right one.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否大于等于 / whether greater than or equal
*/
fun <V : FloatingNumber<V>> quantityGreaterOrEqual(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
    return when (quantityOrdOrNull(lhs, rhs)) {
        is Order.Greater, is Order.Equal -> true
        is Order.Less, null -> false
    }
}

/**
 * 判断左量值是否小于等于右量值。 / Check whether the left quantity is less than or equal to the right one.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @return 是否小于等于 / whether less than or equal
*/
fun <V : FloatingNumber<V>> quantityLessOrEqual(lhs: Quantity<V>, rhs: Quantity<V>): Boolean {
    return when (quantityOrdOrNull(lhs, rhs)) {
        is Order.Less, is Order.Equal -> true
        is Order.Greater, null -> false
    }
}

/**
 * 对量值序列求和，空序列返回 null。 / Sum a sequence of quantities, returning null for an empty sequence.
 *
 * @param V 数值类型 / numeric type
 * @param values 量值序列 / quantity sequence
 * @return 合计或 null / total sum, or null for an empty sequence
*/
fun <V : FloatingNumber<V>> quantitySum(values: Iterable<Quantity<V>>): Quantity<V>? {
    val iterator = values.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var accumulator = iterator.next()
    while (iterator.hasNext()) {
        accumulator = accumulator + iterator.next()
    }
    return accumulator
}

/**
 * 对元素序列按选择器求和，空序列返回 null。 / Sum mapped elements, returning null for an empty sequence.
 *
 * @param T 元素类型 / element type
 * @param V 数值类型 / numeric type
 * @param selector 量值选择器 / quantity selector
 * @return 合计或 null / total sum, or null for an empty sequence
*/
fun <T, V : FloatingNumber<V>> Iterable<T>.sumOfQuantity(selector: (T) -> Quantity<V>): Quantity<V>? {
    return quantitySum(map(selector))
}

/**
 * 按量值选择器取最大元素，空序列返回 null。 / Pick the element with the maximum quantity, returning null for an empty sequence.
 *
 * @param T 元素类型 / element type
 * @param V 数值类型 / numeric type
 * @param selector 量值选择器 / quantity selector
 * @return 最大元素或 null / element with the maximum quantity, or null
*/
fun <T, V : FloatingNumber<V>> Iterable<T>.maxByQuantityOrNull(selector: (T) -> Quantity<V>): T? {
    var best: T? = null
    var bestValue: Quantity<V>? = null
    for (element in this) {
        val value = selector(element)
        val currentBest = bestValue
        if (currentBest == null) {
            best = element
            bestValue = value
            continue
        }
        if (quantityGreater(value, currentBest)) {
            best = element
            bestValue = value
        }
    }
    return best
}

/**
 * 按量值选择器取最小元素，空序列返回 null。 / Pick the element with the minimum quantity, returning null for an empty sequence.
 *
 * @param T 元素类型 / element type
 * @param V 数值类型 / numeric type
 * @param selector 量值选择器 / quantity selector
 * @return 最小元素或 null / element with the minimum quantity, or null
*/
fun <T, V : FloatingNumber<V>> Iterable<T>.minByQuantityOrNull(selector: (T) -> Quantity<V>): T? {
    var best: T? = null
    var bestValue: Quantity<V>? = null
    for (element in this) {
        val value = selector(element)
        val currentBest = bestValue
        if (currentBest == null) {
            best = element
            bestValue = value
            continue
        }
        if (quantityLess(value, currentBest)) {
            best = element
            bestValue = value
        }
    }
    return best
}

/**
 * 按量值选择器升序排序，不可比较的元素保持相对顺序。 / Sort ascending by a quantity selector; incomparable elements keep their order.
 *
 * @param T 元素类型 / element type
 * @param V 数值类型 / numeric type
 * @param selector 量值选择器 / quantity selector
 * @return 排序后的列表 / sorted list
*/
fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        when (quantityOrdOrNull(selector(lhs), selector(rhs))) {
            is Order.Greater -> 1
            is Order.Less -> -1
            is Order.Equal, null -> 0
        }
    }
}

/**
 * 按量值选择器降序排序，不可比较的元素保持相对顺序。 / Sort descending by a quantity selector; incomparable elements keep their order.
 *
 * @param T 元素类型 / element type
 * @param V 数值类型 / numeric type
 * @param selector 量值选择器 / quantity selector
 * @return 排序后的列表 / sorted list
*/
fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByDescendingQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        when (quantityOrdOrNull(selector(lhs), selector(rhs))) {
            is Order.Greater -> -1
            is Order.Less -> 1
            is Order.Equal, null -> 0
        }
    }
}

/**
 * 统计某个单元尺寸在给定可用长度方向上的可容纳数量。 / Count how many copies of a cell size fit along an
 * available length.
 *
 * 泛型数值类型没有 `toDouble()`，因此用同量纲的重复相减实现，并在 `limit` 处截断以避免病态输入造成的
 * 长时间循环。 / Generic numeric types have no `toDouble()`, so the count is derived by repeated subtraction
 * with the same dimension, capped at `limit` to keep pathological inputs from looping for a long time.
 *
 * @param V 数值类型 / numeric type
 * @param available 可用长度 / available length
 * @param cell 单元尺寸 / cell size
 * @param limit 计数上限 / counting limit
 * @return 可容纳数量，参数非正或量纲不匹配时为零 / fitting amount, zero for non-positive inputs or a
 * dimension mismatch
*/
fun <V : FloatingNumber<V>> countFitting(
    available: Quantity<V>,
    cell: Quantity<V>,
    limit: Int = 100000
): Int {
    if (!quantityIsPositive(available) || !quantityIsPositive(cell)) {
        return 0
    }
    if (!isSameDimension(available, cell)) {
        return 0
    }
    var remaining = available
    var count = 0
    while (count < limit && quantityGreaterOrEqual(remaining, cell)) {
        remaining -= cell
        count += 1
    }
    return count
}

/**
 * 把单元尺寸的整数倍换算为量值。 / Convert an integer multiple of a cell size into a quantity.
 *
 * @param V 数值类型 / numeric type
 * @param cell 单元尺寸 / cell size
 * @param times 倍数 / multiple
 * @return 量值 / quantity
*/
fun <V : FloatingNumber<V>> scaledQuantity(cell: Quantity<V>, times: Int): Quantity<V> {
    var acc = quantityZeroOf(cell)
    for (index in 0 until times) {
        acc += cell
    }
    return acc
}

/**
 * 取两个量值中较大者，不可比较时取左值。 / Take the larger of two quantities, preferring the left when
 * incomparable.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左值 / left value
 * @param rhs 右值 / right value
 * @return 较大值 / larger value
*/
fun <V : FloatingNumber<V>> quantityMaxOf(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return when (quantityOrdOrNull(lhs, rhs)) {
        is Order.Less -> rhs
        else -> lhs
    }
}

