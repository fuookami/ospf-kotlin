/**
 * 泛型量纲几何基础设施。
 * Quantity geometry infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.ceil as quantityCeil
import fuookami.ospf.kotlin.quantities.quantity.div as quantityDiv
import fuookami.ospf.kotlin.quantities.quantity.eq as quantityEq
import fuookami.ospf.kotlin.quantities.quantity.floor as quantityFloor
import fuookami.ospf.kotlin.quantities.quantity.geq as quantityGeq
import fuookami.ospf.kotlin.quantities.quantity.gr as quantityGr
import fuookami.ospf.kotlin.quantities.quantity.leq as quantityLeq
import fuookami.ospf.kotlin.quantities.quantity.ls as quantityLs
import fuookami.ospf.kotlin.quantities.quantity.minus as quantityMinus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd as quantityPartialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus as quantityPlus
import fuookami.ospf.kotlin.quantities.quantity.round as quantityRound
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 对两个量纲值执行二元运算，自动处理类型转换和单位兼容性。
 * Perform a binary operation on two quantity values, handling type conversion and unit compatibility.
 *
 * @param lhs 左操作数 / left-hand operand
 * @param rhs 右操作数 / right-hand operand
 * @param op 二元运算函数 / binary operation function
 * @param symbol 运算符号，用于错误信息 / operation symbol for error messages
 * @return 运算结果或失败 / operation result or failure
 */
@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityBinary(
    lhs: Quantity<V>,
    rhs: Quantity<V>,
    op: (Quantity<FltX>, Quantity<FltX>) -> Quantity<FltX>?,
    symbol: String
): Ret<Quantity<V>> {
    return when (lhs.value) {
        is FltX -> {
            val value = op(lhs as Quantity<FltX>, rhs as Quantity<FltX>)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "Quantity operation '$symbol' failed: ${lhs.unit} vs ${rhs.unit}"
                )
            ok(value as Quantity<V>)
        }
        else -> Failed(
            ErrorCode.IllegalArgument,
            "Unsupported quantity numeric type for '$symbol': ${lhs.value::class.simpleName}"
        )
    }
}

/** 对两个量纲值执行加法运算 / Perform addition on two quantity values */
internal fun <V : FloatingNumber<V>> quantityPlusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityPlus(r) }, "+").value!!
}

/** 对两个量纲值执行减法运算 / Perform subtraction on two quantity values */
internal fun <V : FloatingNumber<V>> quantityMinusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityMinus(r) }, "-").value!!
}

/** 对两个量纲值执行乘法运算 / Perform multiplication on two quantity values */
internal fun <V : FloatingNumber<V>> quantityTimesByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityTimes(r) }, "*").value!!
}

/** 按值创建零物理量 / Create zero quantity by value */
internal fun <V : FloatingNumber<V>> quantityZeroByValue(sample: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(sample, sample)
}

/**
 * 按整数值缩放物理量 / Scale quantity by an unsigned integer value
 *
 * @param quantity 待缩放的物理量 / quantity to scale
 * @param scale 缩放因子 / scaling factor
 * @return 缩放后的物理量或失败 / scaled quantity or failure
 */
@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByValue(
    quantity: Quantity<V>,
    scale: UInt64
): Ret<Quantity<V>> {
    return when (val value = quantity.value) {
        is FltX -> ok(Quantity(
            value = value * FltX(scale.toULong().toDouble()),
            unit = quantity.unit
        ) as Quantity<V>)
        else -> Failed(
            ErrorCode.IllegalArgument,
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

/**
 * 按 FltX 值缩放物理量 / Scale quantity by a FltX value
 *
 * @param quantity 待缩放的物理量 / quantity to scale
 * @param scale 缩放因子 / scaling factor
 * @return 缩放后的物理量或失败 / scaled quantity or failure
 */
@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByFltXValue(
    quantity: Quantity<V>,
    scale: FltX
): Ret<Quantity<V>> {
    return when (val value = quantity.value) {
        is FltX -> ok(Quantity(
            value = value * scale,
            unit = quantity.unit
        ) as Quantity<V>)
        else -> Failed(
            ErrorCode.IllegalArgument,
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

/**
 * 按值计算两个量纲值的比率 / Compute the ratio of two quantity values
 *
 * @param lhs 被除数量纲值 / dividend quantity value
 * @param rhs 除数量纲值 / divisor quantity value
 * @return 比率值或失败 / ratio value or failure
 */
@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityRatioByValue(lhs: Quantity<V>, rhs: Quantity<V>): Ret<V> {
    return when (lhs.value) {
        is FltX -> {
            val ratio = (lhs as Quantity<FltX>).quantityDiv(rhs as Quantity<FltX>)
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "Quantity ratio failed: ${lhs.unit} vs ${rhs.unit}"
                )
            ok(ratio.value as V)
        }
        else -> Failed(
            ErrorCode.IllegalArgument,
            "Unsupported quantity numeric type for '/': ${lhs.value::class.simpleName}"
        )
    }
}

/**
 * 重复累加同一量纲值指定次数。
 * Repeatedly add the same quantity value for the specified number of times.
 *
 * @param sample 待累加的量纲值 / quantity value to accumulate
 * @param times 重复次数 / number of repetitions
 * @return 累加结果 / accumulated result
 */
internal fun <V : FloatingNumber<V>> repeatedQuantitySumByValue(
    sample: Quantity<V>,
    times: UInt64
): Quantity<V> {
    var acc = quantityZeroByValue(sample)
    var i = UInt64.zero
    while (i < times) {
        acc = quantityPlusByValue(acc, sample)
        i += UInt64.one
    }
    return acc
}

/**
 * 将量纲值转换为目标单位的标量值 / Convert a quantity value to a scalar value in the target unit
 *
 * @param unit 目标物理单位 / target physical unit
 * @return 标量值或失败 / scalar value or failure
 */
private fun Quantity<FltX>.toScalar(unit: PhysicalUnit): Ret<FltX> {
    return if (this.unit == unit) {
        ok(this.value)
    } else {
        this.convertTo(unit)?.value?.let { ok(it) }
            ?: Failed(ErrorCode.IllegalArgument, "Incompatible unit: ${this.unit} vs $unit")
    }
}

/** 将标量值与物理单位组合为量纲值 / Combine a scalar value with a physical unit to form a quantity */
operator fun FltX.times(unit: PhysicalUnit): Quantity<FltX> = Quantity(this, unit)

/** 判断两个量纲值是否相等 / Check if two quantities are equal */
infix fun Quantity<FltX>.eq(rhs: Quantity<FltX>): Boolean = this.quantityEq(rhs)

/** 判断两个量纲值是否不等 / Check if two quantities are not equal */
infix fun Quantity<FltX>.neq(rhs: Quantity<FltX>): Boolean = !this.quantityEq(rhs)

/** 判断量纲值是否小于等于另一个量纲值 / Check if quantity is less than or equal to another quantity */
infix fun Quantity<FltX>.leq(rhs: Quantity<FltX>): Boolean = this.quantityLeq(rhs) ?: false

/** 判断量纲值是否大于等于另一个量纲值 / Check if quantity is greater than or equal to another quantity */
infix fun Quantity<FltX>.geq(rhs: Quantity<FltX>): Boolean = this.quantityGeq(rhs) ?: false

/**
 * 判断量纲值是否小于另一个量纲值 / Check if quantity is less than another quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否小于 / whether it is less
 */
infix fun Quantity<FltX>.ls(rhs: Quantity<FltX>): Boolean = this.quantityLs(rhs) ?: false

/**
 * 判断量纲值是否大于另一个量纲值 / Check if quantity is greater than another quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否大于 / whether it is greater
 */
infix fun Quantity<FltX>.gr(rhs: Quantity<FltX>): Boolean = this.quantityGr(rhs) ?: false

/**
 * 判断量纲值是否等于标量值 / Check if quantity equals a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否相等 / whether they are equal
 */
infix fun Quantity<FltX>.eq(rhs: FltX): Boolean = this.value == rhs

/**
 * 判断量纲值是否不等于标量值 / Check if quantity does not equal a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否不等 / whether they are not equal
 */
infix fun Quantity<FltX>.neq(rhs: FltX): Boolean = this.value != rhs

/**
 * 判断量纲值是否小于等于标量值 / Check if quantity is less than or equal to a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否小于等于 / whether it is less than or equal
 */
infix fun Quantity<FltX>.leq(rhs: FltX): Boolean = this.value <= rhs

/**
 * 判断量纲值是否大于等于标量值 / Check if quantity is greater than or equal to a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否大于等于 / whether it is greater than or equal
 */
infix fun Quantity<FltX>.geq(rhs: FltX): Boolean = this.value >= rhs

/**
 * 判断量纲值是否小于标量值 / Check if quantity is less than a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否小于 / whether it is less
 */
infix fun Quantity<FltX>.ls(rhs: FltX): Boolean = this.value < rhs

/**
 * 判断量纲值是否大于标量值 / Check if quantity is greater than a scalar value
 *
 * @param rhs 右侧标量值 / right-hand scalar value
 * @return 是否大于 / whether it is greater
 */
infix fun Quantity<FltX>.gr(rhs: FltX): Boolean = this.value > rhs

/**
 * 判断标量值是否等于量纲值 / Check if scalar value equals a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否相等 / whether they are equal
 */
infix fun FltX.eq(rhs: Quantity<FltX>): Boolean = this == rhs.value

/**
 * 判断标量值是否不等于量纲值 / Check if scalar value does not equal a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否不等 / whether they are not equal
 */
infix fun FltX.neq(rhs: Quantity<FltX>): Boolean = this != rhs.value

/**
 * 判断标量值是否小于等于量纲值 / Check if scalar value is less than or equal to a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否小于等于 / whether it is less than or equal
 */
infix fun FltX.leq(rhs: Quantity<FltX>): Boolean = this <= rhs.value

/**
 * 判断标量值是否大于等于量纲值 / Check if scalar value is greater than or equal to a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否大于等于 / whether it is greater than or equal
 */
infix fun FltX.geq(rhs: Quantity<FltX>): Boolean = this >= rhs.value

/**
 * 判断标量值是否小于量纲值 / Check if scalar value is less than a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否小于 / whether it is less
 */
infix fun FltX.ls(rhs: Quantity<FltX>): Boolean = this < rhs.value

/**
 * 判断标量值是否大于量纲值 / Check if scalar value is greater than a quantity
 *
 * @param rhs 右侧量纲值 / right-hand quantity value
 * @return 是否大于 / whether it is greater
 */
infix fun FltX.gr(rhs: Quantity<FltX>): Boolean = this > rhs.value

/** 加法运算 / Addition */
operator fun <V : FloatingNumber<V>> Quantity<V>.plus(rhs: Quantity<V>): Quantity<V> {
    return quantityPlusByValue(this, rhs)
}

/** 减法运算 / Subtraction */
operator fun <V : FloatingNumber<V>> Quantity<V>.minus(rhs: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(this, rhs)
}

/** 乘法运算 / Multiplication */
operator fun <V : FloatingNumber<V>> Quantity<V>.times(rhs: Quantity<V>): Quantity<V> {
    return quantityTimesByValue(this, rhs)
}

/** 除法运算 / Division */
operator fun <V : FloatingNumber<V>> Quantity<V>.div(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityDiv(r) }, "/").value!!
}

/** 标量加法 / Scalar addition */
operator fun Quantity<FltX>.plus(rhs: FltX): Quantity<FltX> = this + (rhs * this.unit)

/** 标量减法 / Scalar subtraction */
operator fun Quantity<FltX>.minus(rhs: FltX): Quantity<FltX> = this - (rhs * this.unit)

/** 标量乘法 / Scalar multiplication */
operator fun Quantity<FltX>.times(rhs: FltX): Quantity<FltX> = Quantity(this.value * rhs, this.unit)

/** 标量除法 / Scalar division */
operator fun Quantity<FltX>.div(rhs: FltX): Quantity<FltX> = Quantity(this.value / rhs, this.unit)

/** 标量加量纲 / Scalar plus quantity */
operator fun FltX.plus(rhs: Quantity<FltX>): Quantity<FltX> = (this * rhs.unit) + rhs

/** 标量减量纲 / Scalar minus quantity */
operator fun FltX.minus(rhs: Quantity<FltX>): Quantity<FltX> = (this * rhs.unit) - rhs

/** 标量乘量纲 / Scalar times quantity */
operator fun FltX.times(rhs: Quantity<FltX>): Quantity<FltX> = Quantity(this * rhs.value, rhs.unit)

/** 量纲取模运算 / Quantity remainder operation */
operator fun Quantity<FltX>.rem(rhs: Quantity<FltX>): Quantity<FltX> {
    val right = rhs.toScalar(this.unit).value!!
    return (this.value % right) * this.unit
}

/** 量纲对标量取模 / Quantity remainder by scalar */
operator fun Quantity<FltX>.rem(rhs: FltX): Quantity<FltX> {
    return (this.value % rhs) * this.unit
}

/** 标量对量纲取模 / Scalar remainder by quantity */
operator fun FltX.rem(rhs: Quantity<FltX>): Quantity<FltX> {
    return (this % rhs.value) * rhs.unit
}

/**
 * 向下取整 / Floor
 *
 * @return 向下取整后的标量值 / floored scalar value
 */
fun Quantity<FltX>.floor(): FltX = this.quantityFloor().value

/**
 * 向上取整 / Ceil
 *
 * @return 向上取整后的标量值 / ceiled scalar value
 */
fun Quantity<FltX>.ceil(): FltX = this.quantityCeil().value

/**
 * 四舍五入 / Round
 *
 * @return 四舍五入后的标量值 / rounded scalar value
 */
fun Quantity<FltX>.round(): FltX = this.quantityRound().value

/**
 * 转换为 Double 值 / Convert to Double value
 *
 * @return Double 值 / Double value
 */
fun Quantity<FltX>.toDouble(): Double = this.value.toDouble()

/**
 * 提取标量值 / Extract scalar value
 *
 * @return 标量值 / scalar value
 */
fun Quantity<FltX>.toScalarValue(): FltX = this.value

/**
 * 取绝对值 / Absolute value
 *
 * @return 绝对值结果 / absolute value result
 */
fun Quantity<FltX>.abs(): Quantity<FltX> {
    return if (this.value >= FltX.zero) {
        this
    } else {
        (-this.value) * this.unit
    }
}

/**
 * 对可迭代集合中的量纲值求和 / Sum quantity values from an iterable collection
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 量纲值之和 / sum of quantity values
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.sumOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var sum = selector(iterator.next())
    while (iterator.hasNext()) {
        sum = quantityPlusByValue(sum, selector(iterator.next()))
    }
    return sum
}

/**
 * 返回可迭代集合中量纲值的最大值，集合为空时抛出异常 / Return the maximum quantity value, throws if collection is empty
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 最大量纲值 / maximum quantity value
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.maxOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "max") is Order.Greater) {
            best = current
        }
    }
    return best
}

/**
 * 返回可迭代集合中量纲值的最大值，集合为空时返回 null / Return the maximum quantity value, or null if collection is empty
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 最大量纲值或 null / maximum quantity value or null
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.maxOfOrNullQuantity(selector: (T) -> Quantity<V>): Quantity<V>? {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "max") is Order.Greater) {
            best = current
        }
    }
    return best
}

/**
 * 返回可迭代集合中量纲值的最小值，集合为空时抛出异常 / Return the minimum quantity value, throws if collection is empty
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 最小量纲值 / minimum quantity value
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.minOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "min") is Order.Less) {
            best = current
        }
    }
    return best
}

/**
 * 返回可迭代集合中量纲值的最小值，集合为空时返回 null / Return the minimum quantity value, or null if collection is empty
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 最小量纲值或 null / minimum quantity value or null
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.minOfOrNullQuantity(selector: (T) -> Quantity<V>): Quantity<V>? {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "min") is Order.Less) {
            best = current
        }
    }
    return best
}

/**
 * 按量纲值升序排序 / Sort by quantity value in ascending order
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 排序后的列表 / sorted list
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        quantityOrd(selector(lhs), selector(rhs), "sort").value
    }
}

/**
 * 按量纲值降序排序 / Sort by quantity value in descending order
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 排序后的列表 / sorted list
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByDescendingQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        quantityOrd(selector(rhs), selector(lhs), "sort").value
    }
}

/**
 * 返回量纲值最大的元素，集合为空时抛出异常 / Return the element with the maximum quantity value, throws if collection is empty
 *
 * @param selector 从元素中提取量纲值的函数 / function to extract quantity from element
 * @return 量纲值最大的元素 / element with the maximum quantity value
 */
fun <T, V : FloatingNumber<V>> Iterable<T>.maxByQuantity(selector: (T) -> Quantity<V>): T {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var bestItem = iterator.next()
    var bestValue = selector(bestItem)
    while (iterator.hasNext()) {
        val item = iterator.next()
        val value = selector(item)
        if (quantityOrd(value, bestValue, "max") is Order.Greater) {
            bestValue = value
            bestItem = item
        }
    }
    return bestItem
}

/**
 * 二维量纲向量 / 2D quantity vector
 *
 * @param x x 分量 / x component
 * @param y y 分量 / y component
 */
data class QuantityVector2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    /** 向量加法 / Vector addition */
    operator fun plus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y)
        )
    }

    /** 向量减法 / Vector subtraction */
    operator fun minus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y)
        )
    }
}

/**
 * 三维量纲向量 / 3D quantity vector
 *
 * @param x x 分量 / x component
 * @param y y 分量 / y component
 * @param z z 分量 / z component
 */
data class QuantityVector3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    /** 向量加法 / Vector addition */
    operator fun plus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y),
            z = quantityPlusByValue(z, rhs.z)
        )
    }

    /** 向量减法 / Vector subtraction */
    operator fun minus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y),
            z = quantityMinusByValue(z, rhs.z)
        )
    }
}

/**
 * 二维量纲点 / 2D quantity point
 *
 * @param x x 坐标 / x coordinate
 * @param y y 坐标 / y coordinate
 */
data class QuantityPoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    /** 加上向量偏移 / Add vector offset */
    operator fun plus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y)
        )
    }

    /** 减去向量偏移 / Subtract vector offset */
    operator fun minus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y)
        )
    }

    /**
     * 比较二维点的排序，若不可比较则返回 Equal / Compare ordering of 2D points, returns Equal if incomparable
     *
     * @param rhs 右侧二维量纲点 / right-hand 2D quantity point
     * @return 排序结果 / ordering result
     */
    infix fun ord(rhs: QuantityPoint2<V>): Order {
        return ordSafe(rhs).value ?: Order.Equal
    }

    /**
     * 安全比较二维点，若不可比较则返回失败 / Safely compare 2D points, returning failure if incomparable
     *
     * @param rhs 右侧二维量纲点 / right-hand 2D quantity point
     * @return 比较结果或失败 / order result or failure
     */
    infix fun ordSafe(rhs: QuantityPoint2<V>): Ret<Order> {
        val yOrder = when (val result = quantityOrdSafe(y, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (yOrder) {
            Order.Equal -> {}
            else -> return ok(yOrder)
        }
        return quantityOrdSafe(x, rhs.x, "x")
    }
}

/**
 * 三维量纲点 / 3D quantity point
 *
 * @param x x 坐标 / x coordinate
 * @param y y 坐标 / y coordinate
 * @param z z 坐标 / z coordinate
 */
data class QuantityPoint3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    /** 加上向量偏移 / Add vector offset */
    operator fun plus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y),
            z = quantityPlusByValue(z, offset.z)
        )
    }

    /** 减去向量偏移 / Subtract vector offset */
    operator fun minus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y),
            z = quantityMinusByValue(z, offset.z)
        )
    }

    /**
     * 比较三维点的排序，若不可比较则返回 Equal / Compare ordering of 3D points, returns Equal if incomparable
     *
     * @param rhs 右侧三维量纲点 / right-hand 3D quantity point
     * @return 排序结果 / ordering result
     */
    infix fun ord(rhs: QuantityPoint3<V>): Order {
        return ordSafe(rhs).value ?: Order.Equal
    }

    /**
     * 安全比较三维点，若不可比较则返回失败 / Safely compare 3D points, returning failure if incomparable
     *
     * @param rhs 右侧三维量纲点 / right-hand 3D quantity point
     * @return 比较结果或失败 / order result or failure
     */
    infix fun ordSafe(rhs: QuantityPoint3<V>): Ret<Order> {
        val zOrder = when (val result = quantityOrdSafe(z, rhs.z, "z")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (zOrder) {
            Order.Equal -> {}
            else -> return ok(zOrder)
        }
        val yOrder = when (val result = quantityOrdSafe(y, rhs.y, "y")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (yOrder) {
            Order.Equal -> {}
            else -> return ok(yOrder)
        }
        return quantityOrdSafe(x, rhs.x, "x")
    }
}

/**
 * 二维量纲矩形 / 2D quantity rectangle
 *
 * @param minX 最小 x 坐标 / minimum x coordinate
 * @param minY 最小 y 坐标 / minimum y coordinate
 * @param maxX 最大 x 坐标 / maximum x coordinate
 * @param maxY 最大 y 坐标 / maximum y coordinate
 */
data class Rectangle2<V : FloatingNumber<V>>(
    val minX: Quantity<V>,
    val minY: Quantity<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    init {
        require(quantityOrd(minX, maxX, "x") !is Order.Greater) { "minX should be <= maxX" }
        require(quantityOrd(minY, maxY, "y") !is Order.Greater) { "minY should be <= maxY" }
    }

    /** 矩形宽度 / Rectangle width */
    val width: Quantity<V> get() = quantityMinusByValue(maxX, minX)

    /** 矩形高度 / Rectangle height */
    val height: Quantity<V> get() = quantityMinusByValue(maxY, minY)

    /** 矩形面积 / Rectangle area */
    val area: Quantity<V> get() = quantityTimesByValue(width, height)

    /**
     * 计算与另一个矩形的交集 / Compute intersection with another rectangle
     *
     * @param rhs 另一个矩形 / another rectangle
     * @return 交集矩形或 null（无交集）/ intersection rectangle or null (no intersection)
     */
    fun intersect(rhs: Rectangle2<V>): Rectangle2<V>? {
        val left = quantityMax(minX, rhs.minX, "x")
        val right = quantityMin(maxX, rhs.maxX, "x")
        val bottom = quantityMax(minY, rhs.minY, "y")
        val top = quantityMin(maxY, rhs.maxY, "y")
        return if (quantityOrd(left, right, "x") is Order.Less
            && quantityOrd(bottom, top, "y") is Order.Less
        ) {
            Rectangle2(left, bottom, right, top)
        } else {
            null
        }
    }

    /**
     * 计算与另一个矩形的交集面积 / Compute intersection area with another rectangle
     *
     * @param rhs 另一个矩形 / another rectangle
     * @return 交集面积或 null（无交集）/ intersection area or null (no intersection)
     */
    fun intersectArea(rhs: Rectangle2<V>): Quantity<V>? {
        return intersect(rhs)?.area
    }
}

/**
 * 创建二维量纲点（泛型） / Create a 2D quantity point (generic)
 *
 * @param x x 坐标 / x coordinate
 * @param y y 坐标 / y coordinate
 * @return 二维量纲点 / 2D quantity point
 */
fun <V : FloatingNumber<V>> point2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityPoint2<V> {
    return QuantityPoint2(x = x, y = y)
}

/**
 * 创建三维量纲点（泛型） / Create a 3D quantity point (generic)
 *
 * @param x x 坐标 / x coordinate
 * @param y y 坐标 / y coordinate
 * @param z z 坐标 / z coordinate
 * @return 三维量纲点 / 3D quantity point
 */
fun <V : FloatingNumber<V>> point3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityPoint3<V> {
    return QuantityPoint3(x = x, y = y, z = z)
}

/**
 * 创建二维量纲向量（泛型） / Create a 2D quantity vector (generic)
 *
 * @param x x 分量 / x component
 * @param y y 分量 / y component
 * @return 二维量纲向量 / 2D quantity vector
 */
fun <V : FloatingNumber<V>> vector2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityVector2<V> {
    return QuantityVector2(x = x, y = y)
}

/**
 * 创建三维量纲向量（泛型） / Create a 3D quantity vector (generic)
 *
 * @param x x 分量 / x component
 * @param y y 分量 / y component
 * @param z z 分量 / z component
 * @return 三维量纲向量 / 3D quantity vector
 */
fun <V : FloatingNumber<V>> vector3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityVector3<V> {
    return QuantityVector3(x = x, y = y, z = z)
}

/** 创建原点二维量纲点（FltX，单位为米） / Create a 2D quantity point at origin (FltX, unit in meters) */
fun point2FltX(): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = Quantity(FltX.zero, Meter),
        y = Quantity(FltX.zero, Meter)
    )
}

/**
 * 创建二维量纲点（FltX） / Create a 2D quantity point (FltX)
 *
 * @param x x 坐标，默认为零 / x coordinate, defaults to zero
 * @param y y 坐标，默认为零 / y coordinate, defaults to zero
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 二维量纲点 / 2D quantity point
 */
fun point2FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = Quantity(x, unit),
        y = Quantity(y, unit)
    )
}

/**
 * 从几何点创建二维量纲点（FltX） / Create a 2D quantity point from a geometric point (FltX)
 *
 * @param point 几何点 / geometric point
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 二维量纲点 / 2D quantity point
 */
fun point2FltX(
    point: Point<Dim2, FltX>,
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return point2FltX(point[0], point[1], unit)
}

/** 创建原点三维量纲点（FltX，单位为米） / Create a 3D quantity point at origin (FltX, unit in meters) */
fun point3FltX(): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = Quantity(FltX.zero, Meter),
        y = Quantity(FltX.zero, Meter),
        z = Quantity(FltX.zero, Meter)
    )
}

/**
 * 创建三维量纲点（FltX） / Create a 3D quantity point (FltX)
 *
 * @param x x 坐标，默认为零 / x coordinate, defaults to zero
 * @param y y 坐标，默认为零 / y coordinate, defaults to zero
 * @param z z 坐标，默认为零 / z coordinate, defaults to zero
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 三维量纲点 / 3D quantity point
 */
fun point3FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = Quantity(x, unit),
        y = Quantity(y, unit),
        z = Quantity(z, unit)
    )
}

/**
 * 从几何点创建三维量纲点（FltX） / Create a 3D quantity point from a geometric point (FltX)
 *
 * @param point 几何点 / geometric point
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 三维量纲点 / 3D quantity point
 */
fun point3FltX(
    point: Point<Dim3, FltX>,
    unit: PhysicalUnit = Meter
): QuantityPoint3<FltX> {
    return point3FltX(point[0], point[1], point[2], unit)
}

/**
 * 从几何向量创建三维量纲点（FltX） / Create a 3D quantity point from a geometric vector (FltX)
 *
 * @param vector 几何向量 / geometric vector
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 三维量纲点 / 3D quantity point
 */
fun point3FltX(vector: Vector<Dim3, FltX>, unit: PhysicalUnit = Meter): QuantityPoint3<FltX> {
    return point3FltX(vector[0], vector[1], vector[2], unit)
}

/**
 * 创建二维量纲向量（FltX） / Create a 2D quantity vector (FltX)
 *
 * @param x x 分量，默认为零 / x component, defaults to zero
 * @param y y 分量，默认为零 / y component, defaults to zero
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 二维量纲向量 / 2D quantity vector
 */
fun vector2FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityVector2<FltX> {
    return QuantityVector2(
        x = Quantity(x, unit),
        y = Quantity(y, unit)
    )
}

/**
 * 从几何向量创建二维量纲向量（FltX） / Create a 2D quantity vector from a geometric vector (FltX)
 *
 * @param vector 几何向量 / geometric vector
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 二维量纲向量 / 2D quantity vector
 */
fun vector2FltX(vector: Vector<Dim2, FltX>, unit: PhysicalUnit = Meter): QuantityVector2<FltX> {
    return vector2FltX(vector[0], vector[1], unit)
}

/**
 * 创建三维量纲向量（FltX） / Create a 3D quantity vector (FltX)
 *
 * @param x x 分量，默认为零 / x component, defaults to zero
 * @param y y 分量，默认为零 / y component, defaults to zero
 * @param z z 分量，默认为零 / z component, defaults to zero
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 三维量纲向量 / 3D quantity vector
 */
fun vector3FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityVector3<FltX> {
    return QuantityVector3(
        x = Quantity(x, unit),
        y = Quantity(y, unit),
        z = Quantity(z, unit)
    )
}

/**
 * 从几何向量创建三维量纲向量（FltX） / Create a 3D quantity vector from a geometric vector (FltX)
 *
 * @param vector 几何向量 / geometric vector
 * @param unit 物理单位，默认为米 / physical unit, defaults to Meter
 * @return 三维量纲向量 / 3D quantity vector
 */
fun vector3FltX(vector: Vector<Dim3, FltX>, unit: PhysicalUnit = Meter): QuantityVector3<FltX> {
    return vector3FltX(vector[0], vector[1], vector[2], unit)
}

/** 二维量纲点加上几何点偏移 / Add geometric point offset to 2D quantity point */
operator fun QuantityPoint2<FltX>.plus(offset: Point<Dim2, FltX>): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

/** 两个二维量纲点相减得到向量 / Subtract two 2D quantity points to get a vector */
operator fun QuantityPoint2<FltX>.minus(rhs: QuantityPoint2<FltX>): QuantityVector2<FltX> {
    return QuantityVector2(
        x = x - rhs.x,
        y = y - rhs.y
    )
}

/**
 * 判断两个二维量纲点是否相等 / Check if two 2D quantity points are equal
 *
 * @param rhs 右侧二维量纲点 / right-hand 2D quantity point
 * @return 是否相等 / whether they are equal
 */
infix fun QuantityPoint2<FltX>.eq(rhs: QuantityPoint2<FltX>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y
}

/**
 * 判断两个二维量纲点是否不等 / Check if two 2D quantity points are not equal
 *
 * @param rhs 右侧二维量纲点 / right-hand 2D quantity point
 * @return 是否不等 / whether they are not equal
 */
infix fun QuantityPoint2<FltX>.neq(rhs: QuantityPoint2<FltX>): Boolean = !(this eq rhs)

/** 二维量纲点加上几何向量偏移 / Add geometric vector offset to 2D quantity point */
operator fun QuantityPoint2<FltX>.plus(offset: Vector<Dim2, FltX>): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

/** 三维量纲点加上几何点偏移 / Add geometric point offset to 3D quantity point */
operator fun QuantityPoint3<FltX>.plus(offset: Point<Dim3, FltX>): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

/** 两个三维量纲点相减得到向量 / Subtract two 3D quantity points to get a vector */
operator fun QuantityPoint3<FltX>.minus(rhs: QuantityPoint3<FltX>): QuantityVector3<FltX> {
    return QuantityVector3(
        x = x - rhs.x,
        y = y - rhs.y,
        z = z - rhs.z
    )
}

/**
 * 判断两个三维量纲点是否相等 / Check if two 3D quantity points are equal
 *
 * @param rhs 右侧三维量纲点 / right-hand 3D quantity point
 * @return 是否相等 / whether they are equal
 */
infix fun QuantityPoint3<FltX>.eq(rhs: QuantityPoint3<FltX>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y && this.z eq rhs.z
}

/**
 * 判断两个三维量纲点是否不等 / Check if two 3D quantity points are not equal
 *
 * @param rhs 右侧三维量纲点 / right-hand 3D quantity point
 * @return 是否不等 / whether they are not equal
 */
infix fun QuantityPoint3<FltX>.neq(rhs: QuantityPoint3<FltX>): Boolean = !(this eq rhs)

/** 三维量纲点加上几何向量偏移 / Add geometric vector offset to 3D quantity point */
operator fun QuantityPoint3<FltX>.plus(offset: Vector<Dim3, FltX>): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

/**
 * 比较两个量纲值，若单位不可比较则返回 null / Compare two quantity values, returning null if units are incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 比较结果或 null / Order result or null
 */
internal fun <V : FloatingNumber<V>> quantityOrdOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order? {
    return lhs.quantityPartialOrd(rhs)
}

/**
 * 安全比较两个量纲值，若单位不可比较则返回失败 / Safely compare two quantity values, returning failure if units are incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 比较结果或失败 / Order result or failure
 */
internal fun <V : FloatingNumber<V>> quantityOrdSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Order> {
    return quantityOrdOrNull(lhs, rhs, axis)?.let { ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

/**
 * 比较两个量纲值的排序，若不可比较则返回 Equal / Compare ordering of two quantities, returns Equal if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 排序结果 / ordering result
 */
internal fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return quantityOrdOrNull(lhs, rhs, axis) ?: Order.Equal
}

/**
 * 比较两个量纲值，若单位不可比较则返回 null / Compare two quantities, returning null if units are incomparable
 *
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 比较结果或 null / Order result or null
 */
infix fun <V : FloatingNumber<V>> Quantity<V>.ordOrNull(rhs: Quantity<V>): Order? {
    return quantityOrdOrNull(this, rhs, "quantity")
}

/**
 * 安全比较两个量纲值，若单位不可比较则返回失败 / Safely compare two quantities, returning failure if units are incomparable
 *
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 比较结果或失败 / Order result or failure
 */
infix fun <V : FloatingNumber<V>> Quantity<V>.ordSafe(rhs: Quantity<V>): Ret<Order> {
    return quantityOrdSafe(this, rhs, "quantity")
}

/** 比较两个量纲值的排序，若不可比较则返回 Equal / Compare ordering of two quantities, returns Equal if incomparable */
infix fun <V : FloatingNumber<V>> Quantity<V>.ord(rhs: Quantity<V>): Order {
    return quantityOrd(this, rhs, "quantity")
}

/** 返回两个量纲值中较大者，若不可比较则返回左侧值 / Return the larger of two quantities, or left value if incomparable */
fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityMax(lhs, rhs, "max")
}

/**
 * 返回两个量纲值中较大者，若不可比较则返回 null / Return the larger of two quantities, or null if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 较大值或 null / Larger value or null
 */
fun <V : FloatingNumber<V>> maxOrNull(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>? {
    return quantityMaxOrNull(lhs, rhs, "max")
}

/**
 * 安全返回两个量纲值中较大者，若不可比较则返回失败 / Safely return the larger of two quantities, or failure if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 较大值或失败 / Larger value or failure
 */
fun <V : FloatingNumber<V>> maxSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    return quantityMaxSafe(lhs, rhs, "max")
}

/**
 * 返回多个量纲值中的最大值 / Return the maximum of multiple quantity values
 *
 * @param lhs 第一个量纲值 / first quantity value
 * @param rhs 第二个量纲值 / second quantity value
 * @param rest 其余量纲值 / remaining quantity values
 * @return 最大量纲值 / maximum quantity value
 */
fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

/** 返回两个量纲值中较小者，若不可比较则返回左侧值 / Return the smaller of two quantities, or left value if incomparable */
fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityMin(lhs, rhs, "min")
}

/**
 * 返回两个量纲值中较小者，若不可比较则返回 null / Return the smaller of two quantities, or null if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 较小值或 null / Smaller value or null
 */
fun <V : FloatingNumber<V>> minOrNull(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>? {
    return quantityMinOrNull(lhs, rhs, "min")
}

/**
 * 安全返回两个量纲值中较小者，若不可比较则返回失败 / Safely return the smaller of two quantities, or failure if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @return 较小值或失败 / Smaller value or failure
 */
fun <V : FloatingNumber<V>> minSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    return quantityMinSafe(lhs, rhs, "min")
}

/**
 * 返回多个量纲值中的最小值 / Return the minimum of multiple quantity values
 *
 * @param lhs 第一个量纲值 / first quantity value
 * @param rhs 第二个量纲值 / second quantity value
 * @param rest 其余量纲值 / remaining quantity values
 * @return 最小量纲值 / minimum quantity value
 */
fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
    var current = min(lhs, rhs)
    for (value in rest) {
        current = min(current, value)
    }
    return current
}

/**
 * 返回两个量纲值中较大者，若不可比较则返回左侧值 / Return the larger of two quantities, or left value if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较大值 / larger value
 */
internal fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return quantityMaxOrNull(lhs, rhs, axis) ?: lhs
}

/**
 * 返回两个量纲值中较大者，若不可比较则返回 null / Return the larger of two quantities, or null if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较大值或 null / Larger value or null
 */
internal fun <V : FloatingNumber<V>> quantityMaxOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V>? {
    return when (quantityOrdOrNull(lhs, rhs, axis) ?: return null) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

/**
 * 安全返回两个量纲值中较大者，若不可比较则返回失败 / Safely return the larger of two quantities, or failure if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较大值或失败 / Larger value or failure
 */
internal fun <V : FloatingNumber<V>> quantityMaxSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return quantityMaxOrNull(lhs, rhs, axis)?.let { ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

/**
 * 返回两个量纲值中较小者，若不可比较则返回左侧值 / Return the smaller of two quantities, or left value if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较小值 / smaller value
 */
internal fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return quantityMinOrNull(lhs, rhs, axis) ?: lhs
}

/**
 * 返回两个量纲值中较小者，若不可比较则返回 null / Return the smaller of two quantities, or null if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较小值或 null / Smaller value or null
 */
internal fun <V : FloatingNumber<V>> quantityMinOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V>? {
    return when (quantityOrdOrNull(lhs, rhs, axis) ?: return null) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

/**
 * 安全返回两个量纲值中较小者，若不可比较则返回失败 / Safely return the smaller of two quantities, or failure if incomparable
 *
 * @param lhs 左侧量纲值 / Left-hand quantity value
 * @param rhs 右侧量纲值 / Right-hand quantity value
 * @param axis 比较轴名称，用于错误信息 / Axis name for error messages
 * @return 较小值或失败 / Smaller value or failure
 */
internal fun <V : FloatingNumber<V>> quantityMinSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return quantityMinOrNull(lhs, rhs, axis)?.let { ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}
