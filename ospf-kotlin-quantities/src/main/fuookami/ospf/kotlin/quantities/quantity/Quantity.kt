/**
 * 物理量类
 * Physical Quantity Class
 *
 * 核心物理量类，包含值和单位，支持量纲检查和单位转换。
 * Core quantity class containing value and unit, supporting dimension checking and unit conversion.
 *
 * 提供完整的算术运算支持，包括加减乘除、比较、单位转换等。
 * Provides complete arithmetic operation support including addition, subtraction,
 * multiplication, division, comparison, and unit conversion.
 *
 * 支持的数值类型 / Supported number types:
 * - Int64, IntX (有符号整数 / Signed integers)
 * - UInt64, UIntX (无符号整数 / Unsigned integers)
 * - Flt64, FltX (浮点数 / Floating-point numbers)
 */
package fuookami.ospf.kotlin.quantities.quantity

import java.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.quantities.unit.*

/**
 * 物理量类
 * Physical Quantity Class
 *
 * 包含值和单位，支持量纲检查和单位转换。
 * Contains value and unit, supporting dimension checking and unit conversion.
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Quantity(Flt64(100.0), Meter)
 * val time = Flt64(10.0) * Second  // 使用扩展运算符 / Using extension operator
 * val speed = distance / time  // Quantity(Flt64(10.0), Meter/Second)
 * ```
 *
 * @param V 值类型 / Value type
 * @property value 物理量的值 / The value of the quantity
 * @property unit 物理量的单位 / The unit of the quantity
 */
data class Quantity<out V>(
    val value: V,
    val unit: PhysicalUnit
)

/**
 * 创建指定值的物理量
 * Create a quantity with specified value
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Meter.of(Flt64(100.0))
 * ```
 *
 * @param amount 值 / The value amount
 * @return 物理量 / The quantity
 */
fun <V> PhysicalUnit.of(amount: V): Quantity<V> {
    return Quantity(amount, this)
}

/**
 * 量纲不匹配异常
 * Dimension Mismatch Exception
 *
 * 当物理量运算时量纲不匹配时抛出。
 * Thrown when dimensions don't match during quantity operations.
 *
 * @param expected 期望的量纲符号 / Expected dimension symbol
 * @param actual 实际的量纲符号 / Actual dimension symbol
 * @param operation 操作名称 / Operation name
 */
class DimensionMismatchException(
    expected: String,
    actual: String,
    operation: String
) : Exception("Dimension mismatch: expected $expected, got $actual for $operation")

/**
 * 单位转换异常
 * Unit Conversion Exception
 *
 * 当单位转换失败时抛出。
 * Thrown when unit conversion fails.
 *
 * @param message 异常信息 / Exception message
 */
class UnitConversionException(message: String) : Exception(message)

/**
 * 根据值类型尝试单位转换
 * Try unit conversion based on value type
 *
 * 内部方法，根据值的实际类型调用对应的转换函数。
 * Internal method that calls the appropriate conversion function based on the actual value type.
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
private fun <V> Quantity<V>.tryConvertByValueType(unit: PhysicalUnit): Quantity<V>? {
    return when (value) {
        is Int64 -> convertKnownValueType<V, Int64>(unit) { target -> to(target) }
        is UInt64 -> convertKnownValueType<V, UInt64>(unit) { target -> to(target) }
        is IntX -> convertKnownValueType<V, IntX>(unit) { target -> to(target) }
        is Flt64 -> convertKnownValueType<V, Flt64>(unit) { target -> to(target) }
        is FltX -> convertKnownValueType<V, FltX>(unit) { target -> to(target) }
        else -> if (this.unit == unit) {
            Quantity(this.value, unit)
        } else {
            null
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V, T> Quantity<V>.convertKnownValueType(
    unit: PhysicalUnit,
    convert: Quantity<T>.(PhysicalUnit) -> Quantity<T>?
): Quantity<V>? {
    // 调用方已在 when(value) 分支确认 V 的运行时值类型是 T。
    // The caller has checked value in a when branch, so V's runtime value type is T here.
    return (this as Quantity<T>).convert(unit) as Quantity<V>?
}

/**
 * 抛出量纲不匹配异常
 * Throw dimension mismatch exception
 *
 * 内部方法，用于在运算时量纲不匹配时抛出异常。
 * Internal method for throwing exception when dimensions don't match during operations.
 *
 * @param lhs 左侧单位 / Left-hand unit
 * @param rhs 右侧单位 / Right-hand unit
 * @param operation 操作名称 / Operation name
 * @throws DimensionMismatchException 永远抛出 / Always throws
 */
private fun throwDimensionMismatch(lhs: PhysicalUnit, rhs: PhysicalUnit, operation: String): Nothing {
    throw DimensionMismatchException(
        expected = lhs.quantity.dimensionSymbol(),
        actual = rhs.quantity.dimensionSymbol(),
        operation = operation
    )
}

private val affineFlt64Tolerance = Flt64(1e-10)
private val affineFltXTolerance = FltX("1e-12")

private fun PhysicalUnit.toStandardValue(value: FltX): FltX {
    return when (val rule = conversionRule) {
        is UnitConversionRule.Linear -> value * rule.scale.value
        is UnitConversionRule.Affine -> value * rule.scale.value + rule.offset
    }
}

private fun PhysicalUnit.convertLinearDifferenceValue(value: FltX, unit: PhysicalUnit): FltX? {
    if (quantity != unit.quantity) return null
    return value * scale.value / unit.scale.value
}

private fun PhysicalUnit.linearDifferenceUnit(): PhysicalUnit {
    if (!isAffine) return this
    return AnonymousPhysicalUnit(
        quantity = quantity,
        conversionRule = UnitConversionRule.Linear(scale),
        name = name?.let { "$it difference" },
        symbol = symbol?.let { "delta($it)" },
        domain = domain
    )
}

private fun affineOrder(lhs: Flt64, rhs: Flt64): Order {
    val diff = lhs - rhs
    return if (diff.abs() <= affineFlt64Tolerance) {
        Order.Equal
    } else if (diff < Flt64.zero) {
        Order.Less()
    } else {
        Order.Greater()
    }
}

private fun affineOrder(lhs: FltX, rhs: FltX): Order {
    val diff = lhs - rhs
    return if (diff.abs() <= affineFltXTolerance) {
        Order.Equal
    } else if (diff < FltX.zero) {
        Order.Less()
    } else {
        Order.Greater()
    }
}

private fun affineOrderOf(lhs: Quantity<*>, rhs: Quantity<*>): Order? {
    if (lhs.unit.quantity != rhs.unit.quantity) return null
    if (!lhs.unit.isAffine && !rhs.unit.isAffine) return null

    val lhsValue = lhs.value
    val rhsValue = rhs.value
    return when {
        lhsValue is Flt64 && rhsValue is Flt64 -> {
            val converted = rhs.unit.convertValue(rhsValue.toFltX(), lhs.unit)?.toFlt64() ?: return null
            affineOrder(lhsValue, converted)
        }

        lhsValue is FltX && rhsValue is FltX -> {
            val converted = rhs.unit.convertValue(rhsValue, lhs.unit) ?: return null
            affineOrder(lhsValue, converted)
        }

        else -> null
    }
}

private fun requireNonAffineAddition(lhs: PhysicalUnit, rhs: PhysicalUnit) {
    require(!lhs.isAffine && !rhs.isAffine) {
        "Cannot add quantities with affine units as ordinary sums. Add a linear difference instead."
    }
}

private fun requireNonAffineScalarOperation(unit: PhysicalUnit, operation: String) {
    require(!unit.isAffine) {
        "Cannot $operation quantity with affine unit '$unit'. Use a linear difference unit instead."
    }
}

private fun Quantity<Flt64>.plusAffineAwareFlt64(other: Quantity<Flt64>): Quantity<Flt64>? {
    if (this.unit.quantity != other.unit.quantity) return null
    if (!this.unit.isAffine && !other.unit.isAffine) return null
    require(!(this.unit.isAffine && other.unit.isAffine)) {
        "Cannot add two affine temperature points. Add a linear temperature difference instead."
    }

    return if (this.unit.isAffine) {
        val delta = other.unit.convertLinearDifferenceValue(other.value.toFltX(), this.unit) ?: return null
        Quantity(this.value + delta.toFlt64(), this.unit)
    } else {
        val delta = this.unit.convertLinearDifferenceValue(this.value.toFltX(), other.unit) ?: return null
        Quantity(other.value + delta.toFlt64(), other.unit)
    }
}

private fun Quantity<FltX>.plusAffineAwareFltX(other: Quantity<FltX>): Quantity<FltX>? {
    if (this.unit.quantity != other.unit.quantity) return null
    if (!this.unit.isAffine && !other.unit.isAffine) return null
    require(!(this.unit.isAffine && other.unit.isAffine)) {
        "Cannot add two affine temperature points. Add a linear temperature difference instead."
    }

    return if (this.unit.isAffine) {
        val delta = other.unit.convertLinearDifferenceValue(other.value, this.unit) ?: return null
        Quantity(this.value + delta, this.unit)
    } else {
        val delta = this.unit.convertLinearDifferenceValue(this.value, other.unit) ?: return null
        Quantity(other.value + delta, other.unit)
    }
}

private fun Quantity<Flt64>.minusAffineAwareFlt64(other: Quantity<Flt64>): Quantity<Flt64>? {
    if (this.unit.quantity != other.unit.quantity) return null
    if (!this.unit.isAffine && !other.unit.isAffine) return null

    return if (this.unit.isAffine && other.unit.isAffine) {
        val difference = this.unit.toStandardValue(this.value.toFltX()) -
                other.unit.toStandardValue(other.value.toFltX())
        val differenceUnit = this.unit.linearDifferenceUnit()
        Quantity((difference / differenceUnit.scale.value).toFlt64(), differenceUnit)
    } else if (this.unit.isAffine) {
        val delta = other.unit.convertLinearDifferenceValue(other.value.toFltX(), this.unit) ?: return null
        Quantity(this.value - delta.toFlt64(), this.unit)
    } else {
        throw IllegalArgumentException(
            "Cannot subtract an affine temperature point from a linear difference."
        )
    }
}

private fun Quantity<FltX>.minusAffineAwareFltX(other: Quantity<FltX>): Quantity<FltX>? {
    if (this.unit.quantity != other.unit.quantity) return null
    if (!this.unit.isAffine && !other.unit.isAffine) return null

    return if (this.unit.isAffine && other.unit.isAffine) {
        val difference = this.unit.toStandardValue(this.value) -
                other.unit.toStandardValue(other.value)
        val differenceUnit = this.unit.linearDifferenceUnit()
        Quantity(difference / differenceUnit.scale.value, differenceUnit)
    } else if (this.unit.isAffine) {
        val delta = other.unit.convertLinearDifferenceValue(other.value, this.unit) ?: return null
        Quantity(this.value - delta, this.unit)
    } else {
        throw IllegalArgumentException(
            "Cannot subtract an affine temperature point from a linear difference."
        )
    }
}

/**
 * 用指定值创建物理量
 * Create quantity with specified value
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Meter.withValue(Flt64(100.0))
 * ```
 *
 * @param value 值 / The value
 * @return 物理量 / The quantity
 */
fun <V> PhysicalUnit.withValue(value: V): Quantity<V> {
    return Quantity(value, this)
}

/**
 * 创建零值物理量
 * Create zero quantity
 *
 * 示例 / Example:
 * ```kotlin
 * val zero = Meter.zero(Flt64.constants)
 * ```
 *
 * @param constants 算术常量 / Arithmetic constants
 * @return 零值物理量 / Zero quantity
 */
fun <V> PhysicalUnit.zero(constants: ArithmeticConstants<V>): Quantity<V> {
    return Quantity(constants.zero, this)
}

/**
 * 创建一值物理量
 * Create one quantity
 *
 * 示例 / Example:
 * ```kotlin
 * val one = Meter.one(Flt64.constants)
 * ```
 *
 * @param constants 算术常量 / Arithmetic constants
 * @return 一值物理量 / One quantity
 */
fun <V> PhysicalUnit.one(constants: ArithmeticConstants<V>): Quantity<V> {
    return Quantity(constants.one, this)
}

/**
 * 创建二值物理量
 * Create two quantity
 *
 * 示例 / Example:
 * ```kotlin
 * val two = Meter.two(Flt64.constants)
 * ```
 *
 * @param constants 实数常量 / Real number constants
 * @return 二值物理量 / Two quantity
 */
fun <V : RealNumber<V>> PhysicalUnit.two(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.two, this)
}

/**
 * 创建三值物理量
 * Create three quantity
 *
 * @param constants 实数常量 / Real number constants
 * @return 三值物理量 / Three quantity
 */
fun <V : RealNumber<V>> PhysicalUnit.three(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.three, this)
}

/**
 * 创建五值物理量
 * Create five quantity
 *
 * @param constants 实数常量 / Real number constants
 * @return 五值物理量 / Five quantity
 */
fun <V : RealNumber<V>> PhysicalUnit.five(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.five, this)
}

/**
 * 创建十值物理量
 * Create ten quantity
 *
 * @param constants 实数常量 / Real number constants
 * @return 十值物理量 / Ten quantity
 */
fun <V : RealNumber<V>> PhysicalUnit.ten(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.ten, this)
}

/**
 * 创建 pi 值物理量
 * Create pi quantity
 *
 * @param constants 浮点数常量 / Floating number constants
 * @return pi 值物理量 / Pi quantity
 */
fun <V : FloatingNumber<V>> PhysicalUnit.pi(constants: FloatingNumberConstants<V>): Quantity<V> {
    return Quantity(constants.pi, this)
}

/**
 * 创建 e 值物理量
 * Create e quantity
 *
 * @param constants 浮点数常量 / Floating number constants
 * @return e 值物理量 / e quantity
 */
fun <V : FloatingNumber<V>> PhysicalUnit.e(constants: FloatingNumberConstants<V>): Quantity<V> {
    return Quantity(constants.e, this)
}

// ============================================================================
// 比较操作 / Comparison operations
// 比较操作需要相同单位 / Comparison requires same unit
// 同量纲异单位会尝试单位转换 / Same dimension different units will try unit conversion
// 不同量纲返回 false/null / Different dimensions return false/null
// ============================================================================

/**
 * 判断两个物理量是否相等
 * Check if two quantities are equal
 *
 * 同单位比较值；同量纲异单位尝试转换后比较；异量纲返回 false。
 * Compares values for same unit; tries conversion for same dimension different units;
 * returns false for different dimensions.
 *
 * 示例 / Example:
 * ```kotlin
 * val a = Flt64(1000.0) * Meter
 * val b = Flt64(1.0) * Kilometer
 * val result = a eq b  // true
 * ```
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否相等 / Whether equal
 */
infix fun <V> Quantity<V>.eq(other: Quantity<V>): Boolean where V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value eq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it is Order.Equal }
        val converted = other.tryConvertByValueType(this.unit) ?: return false
        this.value eq converted.value
    } else {
        false
    }
}

/**
 * 判断两个物理量是否不相等
 * Check if two quantities are not equal
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否不相等 / Whether not equal
 */
infix fun <V> Quantity<V>.neq(other: Quantity<V>): Boolean where V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value neq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it !is Order.Equal }
        val converted = other.tryConvertByValueType(this.unit) ?: return true
        this.value neq converted.value
    } else {
        true
    }
}

/**
 * 比较两个物理量的部分序关系
 * Compare partial order of two quantities
 *
 * 返回 Order.Less、Order.Equal、Order.Greater 或 null（无法比较）。
 * Returns Order.Less, Order.Equal, Order.Greater, or null (incomparable).
 *
 * @param other 另一个物理量 / Another quantity
 * @return 序关系，或 null 如果无法比较 / Order relation, or null if incomparable
 */
infix fun <V> Quantity<V>.partialOrd(other: Quantity<V>): Order? where V : PartialOrd<V> {
    return if (this.unit == other.unit) {
        this.value partialOrd other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it }
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value partialOrd converted.value
    } else {
        null
    }
}

/**
 * 判断是否小于
 * Check if less than
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否小于，或 null 如果无法比较 / Whether less than, or null if incomparable
 */
infix fun <V> Quantity<V>.ls(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value ls other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it is Order.Less }
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value ls converted.value
    } else {
        null
    }
}

/**
 * 判断是否小于等于
 * Check if less than or equal
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否小于等于，或 null 如果无法比较 / Whether less than or equal, or null if incomparable
 */
infix fun <V> Quantity<V>.leq(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value leq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it is Order.Less || it is Order.Equal }
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value leq converted.value
    } else {
        null
    }
}

/**
 * 判断是否大于
 * Check if greater than
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否大于，或 null 如果无法比较 / Whether greater than, or null if incomparable
 */
infix fun <V> Quantity<V>.gr(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value gr other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it is Order.Greater }
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value gr converted.value
    } else {
        null
    }
}

/**
 * 判断是否大于等于
 * Check if greater than or equal
 *
 * @param other 另一个物理量 / Another quantity
 * @return 是否大于等于，或 null 如果无法比较 / Whether greater than or equal, or null if incomparable
 */
infix fun <V> Quantity<V>.geq(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value geq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        affineOrderOf(this, other)?.let { return it is Order.Greater || it is Order.Equal }
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value geq converted.value
    } else {
        null
    }
}

// ============================================================================
// 数值类型转换 / Number type conversions
// ============================================================================

/**
 * 转换为 Int64 物理量
 * Convert to Int64 quantity
 *
 * @return Int64 物理量 / Int64 quantity
 */
fun <V : RealNumber<V>> Quantity<V>.toInt64(): Quantity<Int64> {
    return Quantity(this.value.toInt64(), this.unit)
}

/**
 * 转换为 UInt64 物理量
 * Convert to UInt64 quantity
 *
 * @return UInt64 物理量 / UInt64 quantity
 */
fun <V : RealNumber<V>> Quantity<V>.toUInt64(): Quantity<UInt64> {
    return Quantity(this.value.toUInt64(), this.unit)
}

/**
 * 转换为 IntX 物理量
 * Convert to IntX quantity
 *
 * @return IntX 物理量 / IntX quantity
 */
fun <V : RealNumber<V>> Quantity<V>.toIntX(): Quantity<IntX> {
    return Quantity(this.value.toIntX(), this.unit)
}

/**
 * 转换为 Flt64 物理量
 * Convert to Flt64 quantity
 *
 * @return Flt64 物理量 / Flt64 quantity
 */
fun <V : RealNumber<V>> Quantity<V>.toFlt64(): Quantity<Flt64> {
    return Quantity(this.value.toFlt64(), this.unit)
}

/**
 * 转换为 FltX 物理量
 * Convert to FltX quantity
 *
 * @return FltX 物理量 / FltX quantity
 */
fun <V : RealNumber<V>> Quantity<V>.toFltX(): Quantity<FltX> {
    return Quantity(this.value.toFltX(), this.unit)
}

/**
 * 向下取整
 * Floor the value
 *
 * @return 取整后的物理量 / Floored quantity
 */
fun <F : FloatingImpl<F>> Quantity<F>.floor(): Quantity<F> {
    return Quantity(value.floor(), unit)
}

/**
 * 向上取整
 * Ceil the value
 *
 * @return 取整后的物理量 / Ceiled quantity
 */
fun <F : FloatingImpl<F>> Quantity<F>.ceil(): Quantity<F> {
    return Quantity(value.ceil(), unit)
}

/**
 * 四舍五入
 * Round the value
 *
 * @return 取整后的物理量 / Rounded quantity
 */
fun <F : FloatingImpl<F>> Quantity<F>.round(): Quantity<F> {
    return Quantity(value.round(), unit)
}

// ============================================================================
// 单位转换 / Unit conversion
// 整数类型：非整数转换因子返回 null（请使用 Flt64 进行精确转换）
// Integer types: Non-integer conversion factors return null (use Flt64 for accurate conversion)
// ============================================================================

/**
 * Int64 物理量的单位转换
 * Unit conversion for Int64 quantity
 *
 * 如果转换因子不是整数，返回 null。请使用 Flt64 进行精确转换。
 * Returns null if conversion factor is not an integer. Use Flt64 for accurate conversion.
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Int64(1000) * Meter
 * val km = distance.to(Kilometer)  // Quantity(Int64(1), Kilometer)
 *
 * val time = Int64(1) * Minute
 * val seconds = time.to(Second)  // Quantity(Int64(60), Second)
 *
 * val hours = time.to(Hour)  // null (转换因子 1/60 不是整数 / factor 1/60 not integer)
 * ```
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
@JvmName("convertQuantityInt64")
fun Quantity<Int64>.to(unit: PhysicalUnit): Quantity<Int64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else if (this.unit.isAffine || unit.isAffine) {
        // 仿射单位转换不适用于整数类型（存在偏移量）
        // Affine unit conversion is not applicable to integer types (has offset)
        null
    } else {
        this.unit.to(unit)?.value?.let { factor ->
            // 检查转换因子是否为整数 / Check if conversion factor is an integer
            val factorFloor = factor.floor()
            if (factorFloor neq factor) {
                // 非整数因子：返回 null（请使用 Flt64 进行精确转换）
                // Non-integer factor: return null (use Flt64 for accurate conversion)
                null
            } else {
                Quantity(factor.toInt64() * this.value, unit)
            }
        }
    }
}

/**
 * UInt64 物理量的单位转换
 * Unit conversion for UInt64 quantity
 *
 * 如果转换因子不是整数，返回 null。请使用 Flt64 进行精确转换。
 * Returns null if conversion factor is not an integer. Use Flt64 for accurate conversion.
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
@JvmName("convertQuantityUInt64")
fun Quantity<UInt64>.to(unit: PhysicalUnit): Quantity<UInt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else if (this.unit.isAffine || unit.isAffine) {
        // 仿射单位转换不适用于整数类型（存在偏移量）
        // Affine unit conversion is not applicable to integer types (has offset)
        null
    } else {
        this.unit.to(unit)?.value?.let { factor ->
            // 检查转换因子是否为整数 / Check if conversion factor is an integer
            val factorFloor = factor.floor()
            if (factorFloor neq factor) {
                // 非整数因子：返回 null（请使用 Flt64 进行精确转换）
                // Non-integer factor: return null (use Flt64 for accurate conversion)
                null
            } else {
                Quantity(factor.toUInt64() * this.value, unit)
            }
        }
    }
}

/**
 * IntX 物理量的单位转换
 * Unit conversion for IntX quantity
 *
 * 如果转换因子不是整数，返回 null。请使用 Flt64 进行精确转换。
 * Returns null if conversion factor is not an integer. Use Flt64 for accurate conversion.
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
fun Quantity<IntX>.to(unit: PhysicalUnit): Quantity<IntX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else if (this.unit.isAffine || unit.isAffine) {
        // 仿射单位转换不适用于整数类型（存在偏移量）
        // Affine unit conversion is not applicable to integer types (has offset)
        null
    } else {
        this.unit.to(unit)?.value?.let { factor ->
            // 检查转换因子是否为整数 / Check if conversion factor is an integer
            val factorFloor = factor.floor()
            if (factorFloor neq factor) {
                // 非整数因子：返回 null（请使用 Flt64 进行精确转换）
                // Non-integer factor: return null (use Flt64 for accurate conversion)
                null
            } else {
                Quantity(factor.toIntX() * this.value, unit)
            }
        }
    }
}

/**
 * Flt64 物理量的单位转换
 * Unit conversion for Flt64 quantity
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(1000.0) * Meter
 * val km = distance.to(Kilometer)  // Quantity(Flt64(1.0), Kilometer)
 * ```
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
@JvmName("convertQuantityFlt64")
fun Quantity<Flt64>.to(unit: PhysicalUnit): Quantity<Flt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else if (this.unit.isAffine || unit.isAffine) {
        // 仿射转换需要完整的值转换，不能只用比例因子
        // Affine conversion requires full value conversion, not just a scale factor
        this.unit.convertValue(this.value.toFltX(), unit)?.let {
            Quantity(it.toFlt64(), unit)
        }
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toFlt64() * this.value, unit)
        }
    }
}

/**
 * FltX 物理量的单位转换
 * Unit conversion for FltX quantity
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果无法转换 / Converted quantity, or null if conversion failed
 */
@JvmName("convertQuantityFltX")
fun Quantity<FltX>.to(unit: PhysicalUnit): Quantity<FltX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else if (this.unit.isAffine || unit.isAffine) {
        // 仿射转换需要完整的值转换，不能只用比例因子
        // Affine conversion requires full value conversion, not just a scale factor
        this.unit.convertValue(this.value, unit)?.let {
            Quantity(it, unit)
        }
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toFltX() * this.value, unit)
        }
    }
}

// ============================================================================
// 物理量创建运算符 / Quantity creation operators
// ============================================================================

/**
 * 数值与单位的乘法运算符
 * Multiplication operator for number and unit
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(100.0) * Meter
 * ```
 *
 * @param unit 单位 / Unit
 * @return 物理量 / Quantity
 */
operator fun <V : Arithmetic<V>> V.times(unit: PhysicalUnit): Quantity<V> {
    return Quantity(this, unit)
}

/**
 * Int 与单位的乘法运算符（转换为 Int64）
 * Multiplication operator for Int and unit (converts to Int64)
 *
 * 示例 / Example:
 * ```kotlin
 * val count = 5 * Meter  // Quantity(Int64(5), Meter)
 * ```
 *
 * @param unit 单位 / Unit
 * @return Int64 物理量 / Int64 quantity
 */
operator fun Int.times(unit: PhysicalUnit): Quantity<Int64> {
    return Quantity(Int64(this.toLong()), unit)
}

/**
 * UInt 与单位的乘法运算符（转换为 UInt64）
 * Multiplication operator for UInt and unit (converts to UInt64)
 *
 * @param unit 单位 / Unit
 * @return UInt64 物理量 / UInt64 quantity
 */
operator fun UInt.times(unit: PhysicalUnit): Quantity<UInt64> {
    return Quantity(UInt64(this.toULong()), unit)
}

/**
 * BigInteger 与单位的乘法运算符（转换为 IntX）
 * Multiplication operator for BigInteger and unit (converts to IntX)
 *
 * @param unit 单位 / Unit
 * @return IntX 物理量 / IntX quantity
 */
operator fun BigInteger.times(unit: PhysicalUnit): Quantity<IntX> {
    return Quantity(IntX(this), unit)
}

/**
 * Double 与单位的乘法运算符（转换为 Flt64）
 * Multiplication operator for Double and unit (converts to Flt64)
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = 100.0 * Meter  // Quantity(Flt64(100.0), Meter)
 * ```
 *
 * @param unit 单位 / Unit
 * @return Flt64 物理量 / Flt64 quantity
 */
operator fun Double.times(unit: PhysicalUnit): Quantity<Flt64> {
    return Quantity(Flt64(this), unit)
}

/**
 * BigDecimal 与单位的乘法运算符（转换为 FltX）
 * Multiplication operator for BigDecimal and unit (converts to FltX)
 *
 * @param unit 单位 / Unit
 * @return FltX 物理量 / FltX quantity
 */
operator fun BigDecimal.times(unit: PhysicalUnit): Quantity<FltX> {
    return Quantity(FltX(this), unit)
}

// ============================================================================
// 加法运算 / Addition operations
// 同单位直接相加；同量纲异单位转换后相加；异量纲抛出异常
// Same unit adds directly; same dimension different units converts then adds;
// different dimensions throws exception
// ============================================================================

/**
 * Int64 物理量的加法
 * Addition for Int64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量 / Sum quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("plusQuantityInt64")
operator fun Quantity<Int64>.plus(other: Quantity<Int64>): Quantity<Int64> {
    requireNonAffineAddition(this.unit, other.unit)
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "addition")
    }
}

/**
 * UInt64 物理量的加法
 * Addition for UInt64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量 / Sum quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("plusQuantityUInt64")
operator fun Quantity<UInt64>.plus(other: Quantity<UInt64>): Quantity<UInt64> {
    requireNonAffineAddition(this.unit, other.unit)
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "addition")
    }
}

/**
 * IntX 物理量的加法
 * Addition for IntX quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量 / Sum quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("plusQuantityIntX")
operator fun Quantity<IntX>.plus(other: Quantity<IntX>): Quantity<IntX> {
    requireNonAffineAddition(this.unit, other.unit)
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "addition")
    }
}

/**
 * Flt64 物理量的加法
 * Addition for Flt64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量 / Sum quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("plusQuantityFlt64")
operator fun Quantity<Flt64>.plus(other: Quantity<Flt64>): Quantity<Flt64> {
    if (this.unit.quantity != other.unit.quantity) {
        throwDimensionMismatch(this.unit, other.unit, "addition")
    }
    this.plusAffineAwareFlt64(other)?.let { return it }
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    }
}

/**
 * FltX 物理量的加法
 * Addition for FltX quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量 / Sum quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("plusQuantityFltX")
operator fun Quantity<FltX>.plus(other: Quantity<FltX>): Quantity<FltX> {
    if (this.unit.quantity != other.unit.quantity) {
        throwDimensionMismatch(this.unit, other.unit, "addition")
    }
    this.plusAffineAwareFltX(other)?.let { return it }
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    }
}

// ============================================================================
// 减法运算 / Subtraction operations
// ============================================================================

/**
 * Int64 物理量的减法
 * Subtraction for Int64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量 / Difference quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("minusQuantityInt64")
operator fun Quantity<Int64>.minus(other: Quantity<Int64>): Quantity<Int64> {
    require(!this.unit.isAffine && !other.unit.isAffine) {
        "Cannot subtract integer quantities with affine units."
    }
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "subtraction")
    }
}

/**
 * UInt64 物理量的减法
 * Subtraction for UInt64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量 / Difference quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("minusQuantityUInt64")
operator fun Quantity<UInt64>.minus(other: Quantity<UInt64>): Quantity<UInt64> {
    require(!this.unit.isAffine && !other.unit.isAffine) {
        "Cannot subtract integer quantities with affine units."
    }
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "subtraction")
    }
}

/**
 * IntX 物理量的减法
 * Subtraction for IntX quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量 / Difference quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("minusQuantityIntX")
operator fun Quantity<IntX>.minus(other: Quantity<IntX>): Quantity<IntX> {
    require(!this.unit.isAffine && !other.unit.isAffine) {
        "Cannot subtract integer quantities with affine units."
    }
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        throwDimensionMismatch(this.unit, other.unit, "subtraction")
    }
}

/**
 * Flt64 物理量的减法
 * Subtraction for Flt64 quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量 / Difference quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("minusQuantityFlt64")
operator fun Quantity<Flt64>.minus(other: Quantity<Flt64>): Quantity<Flt64> {
    if (this.unit.quantity != other.unit.quantity) {
        throwDimensionMismatch(this.unit, other.unit, "subtraction")
    }
    this.minusAffineAwareFlt64(other)?.let { return it }
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    }
}

/**
 * FltX 物理量的减法
 * Subtraction for FltX quantities
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量 / Difference quantity
 * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
 */
@JvmName("minusQuantityFltX")
operator fun Quantity<FltX>.minus(other: Quantity<FltX>): Quantity<FltX> {
    if (this.unit.quantity != other.unit.quantity) {
        throwDimensionMismatch(this.unit, other.unit, "subtraction")
    }
    this.minusAffineAwareFltX(other)?.let { return it }
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    }
}

// ============================================================================
// 乘法运算 / Multiplication operations
// 物理量相乘产生新的量纲（例如：距离 * 时间 = 距离*时间）
// Multiplying quantities produces new dimension (e.g., distance * time = distance*time)
// ============================================================================

/**
 * 物理量之间的乘法
 * Multiplication between quantities
 *
 * 不同量纲相乘，产生新的量纲。
 * Multiplying different dimensions produces a new dimension.
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(100.0) * Meter
 * val time = Flt64(10.0) * Second
 * val speed = distance * time  // Quantity(Flt64(1000.0), Meter*Second)
 * ```
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相乘后的物理量 / Product quantity
 */
operator fun <V> Quantity<V>.times(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    // 仿射单位不允许参与乘法运算
    // Affine units cannot participate in multiplication
    require(!this.unit.isAffine && !other.unit.isAffine) {
        "Cannot multiply quantities with affine units (e.g., absolute temperature). " +
        "Use temperature differences for multiplication."
    }
    // 不同量纲相乘，产生新的量纲 / Different dimensions multiply to produce new dimension
    val newUnit = this.unit * other.unit
    return Quantity(this.value * other.value, newUnit)
}

/**
 * 物理量与标量的乘法
 * Multiplication between quantity and scalar
 *
 * @param other 标量值 / Scalar value
 * @return 相乘后的物理量 / Product quantity
 */
operator fun <V> Quantity<V>.times(other: V): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    requireNonAffineScalarOperation(this.unit, "multiply")
    return Quantity(this.value * other, this.unit)
}

/**
 * 标量与物理量的乘法
 * Multiplication between scalar and quantity
 *
 * @param other 物理量 / Quantity
 * @return 相乘后的物理量 / Product quantity
 */
operator fun <V> V.times(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    requireNonAffineScalarOperation(other.unit, "multiply")
    return Quantity(this * other.value, other.unit)
}

// ============================================================================
// 除法运算 / Division operations
// 物理量相除产生新的量纲（例如：距离 / 时间 = 速度）
// Dividing quantities produces new dimension (e.g., distance / time = speed)
// ============================================================================

/**
 * 物理量之间的除法
 * Division between quantities
 *
 * 不同量纲相除，产生新的量纲。
 * Dividing different dimensions produces a new dimension.
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(100.0) * Meter
 * val time = Flt64(10.0) * Second
 * val speed = distance / time  // Quantity(Flt64(10.0), Meter/Second)
 * ```
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相除后的物理量 / Quotient quantity
 */
operator fun <V> Quantity<V>.div(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    // 仿射单位不允许参与除法运算
    // Affine units cannot participate in division
    require(!this.unit.isAffine && !other.unit.isAffine) {
        "Cannot divide quantities with affine units (e.g., absolute temperature). " +
        "Use temperature differences for division."
    }
    // 不同量纲相除，产生新的量纲 / Different dimensions divide to produce new dimension
    val newUnit = this.unit / other.unit
    return Quantity(this.value / other.value, newUnit)
}

/**
 * 物理量与标量的除法
 * Division between quantity and scalar
 *
 * @param other 标量值 / Scalar value
 * @return 相除后的物理量 / Quotient quantity
 */
operator fun <V> Quantity<V>.div(other: V): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    requireNonAffineScalarOperation(this.unit, "divide")
    return Quantity(this.value / other, this.unit)
}

/**
 * 标量与物理量的除法
 * Division between scalar and quantity
 *
 * 结果物理量的单位为原单位的倒数。
 * Result quantity's unit is the reciprocal of the original unit.
 *
 * @param other 物理量 / Quantity
 * @return 相除后的物理量 / Quotient quantity
 */
operator fun <V> V.div(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    requireNonAffineScalarOperation(other.unit, "divide")
    return Quantity(this / other.value, other.unit.reciprocal())
}

// ============================================================================
// 负号运算 / Negation operation
// ============================================================================

/**
 * 物理量的负号运算
 * Negation of quantity
 *
 * @return 负值物理量 / Negated quantity
 */
operator fun <V> Quantity<V>.unaryMinus(): Quantity<V> where V : Arithmetic<V>, V : Neg<V> {
    requireNonAffineScalarOperation(this.unit, "negate")
    return Quantity(-this.value, this.unit)
}

// ============================================================================
// 单位转换方法（带错误处理）/ Unit conversion methods (with error handling)
// ============================================================================

/**
 * 转换到另一个单位（带错误处理）
 * Convert to another unit (with error handling)
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(1000.0) * Meter
 * val km = distance.convertTo(Kilometer)  // Quantity(Flt64(1.0), Kilometer)
 *
 * val time = Flt64(5.0) * Second
 * val result = time.convertTo(Meter)  // null (量纲不匹配 / dimension mismatch)
 * ```
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，如果量纲不匹配返回 null / Converted quantity, or null if dimensions don't match
 */
fun <V> Quantity<V>.convertTo(unit: PhysicalUnit): Quantity<V>? {
    if (!this.unit.sameDimension(unit)) {
        return null
    }

    return this.tryConvertByValueType(unit)
}

/**
 * 转换到标准单位
 * Convert to standard unit
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(1000.0) * Meter
 * val standard = distance.toStandardUnit(UnitSystem.SI)  // Quantity(Flt64(1000.0), Meter)
 *
 * val mass = Flt64(1.0) * Pound
 * val standardMass = mass.toStandardUnit(UnitSystem.SI)  // Quantity(Flt64(0.45359237), Kilogram)
 * ```
 *
 * @param system 单位系统 / Unit system
 * @return 转换后的物理量，如果无法转换返回 null / Converted quantity, or null if conversion failed
 */
fun <V> Quantity<V>.toStandardUnit(system: UnitSystem): Quantity<V>? {
    val standardUnit = system.standardUnitForDimension(this.unit.quantity) ?: return null
    return convertTo(standardUnit)
}

// ============================================================================
// 值类型映射方法 / Value type mapping methods
// ============================================================================

/**
 * 映射值类型
 * Map value type
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(100.0) * Meter
 * val doubled = distance.mapValue { it * Flt64(2.0) }  // Quantity(Flt64(200.0), Meter)
 * ```
 *
 * @param f 值转换函数 / Value transformation function
 * @return 映射后的物理量 /Mapped quantity
 */
fun <V, U> Quantity<V>.mapValue(f: (V) -> U): Quantity<U> {
    return Quantity(f(this.value), this.unit)
}

/**
 * 尝试映射值类型
 * Try to map value type
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = Flt64(100.0) * Meter
 * val result = distance.tryMapValue { v -> v.toInt64OrNull() }  // Quantity(Int64(100), Meter) or null
 * ```
 *
 * @param f 值转换函数，可能失败 / Value transformation function that may fail
 * @return 映射后的物理量，或 null 如果转换失败 /Mapped quantity, or null if transformation failed
 */
fun <V, U> Quantity<V>.tryMapValue(f: (V) -> U?): Quantity<U>? {
    val newValue = f(this.value) ?: return null
    return Quantity(newValue, this.unit)
}
