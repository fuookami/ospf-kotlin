package fuookami.ospf.kotlin.quantities.quantity


import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.Arithmetic
import fuookami.ospf.kotlin.math.algebra.concept.ArithmeticConstants
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumberConstants
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.Times
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.PartialOrd
import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.operator.Neg
import fuookami.ospf.kotlin.quantities.unit.*
import java.math.BigDecimal
import java.math.BigInteger

/**
 * 物理量类
 * 包含值和单位，支持量纲检查和单位转换
 * 
 * @param V 值类�?
 * @property value 物理量的�?
 * @property unit 物理量的单位
 */
data class Quantity<out V>(
    val value: V,
    val unit: PhysicalUnit
)

fun <V> PhysicalUnit.of(amount: V): Quantity<V> {
    return Quantity(amount, this)
}

/**
 * 量纲不匹配异�?
 */
class DimensionMismatchException(
    expected: String,
    actual: String,
    operation: String
) : Exception("Dimension mismatch: expected $expected, got $actual for $operation")

/**
 * 单位转换异常
 */
class UnitConversionException(message: String) : Exception(message)

typealias QuantityFlt64 = Quantity<Flt64>

@Suppress("UNCHECKED_CAST")
private fun <V> Quantity<V>.tryConvertByValueType(unit: PhysicalUnit): Quantity<V>? {
    return when (value) {
        is Int64 -> (this as Quantity<Int64>).to(unit) as Quantity<V>?
        is UInt64 -> (this as Quantity<UInt64>).to(unit) as Quantity<V>?
        is IntX -> (this as Quantity<IntX>).to(unit) as Quantity<V>?
        is Flt64 -> (this as Quantity<Flt64>).to(unit) as Quantity<V>?
        is FltX -> (this as Quantity<FltX>).to(unit) as Quantity<V>?
        else -> if (this.unit == unit) {
            Quantity(this.value, unit)
        } else {
            null
        }
    }
}

private fun throwDimensionMismatch(lhs: PhysicalUnit, rhs: PhysicalUnit, operation: String): Nothing {
    throw DimensionMismatchException(
        expected = lhs.quantity.dimensionSymbol(),
        actual = rhs.quantity.dimensionSymbol(),
        operation = operation
    )
}

fun <V> PhysicalUnit.withValue(value: V): Quantity<V> {
    return Quantity(value, this)
}

fun <V> PhysicalUnit.zero(constants: ArithmeticConstants<V>): Quantity<V> {
    return Quantity(constants.zero, this)
}

fun <V> PhysicalUnit.one(constants: ArithmeticConstants<V>): Quantity<V> {
    return Quantity(constants.one, this)
}

fun <V : RealNumber<V>> PhysicalUnit.two(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.two, this)
}

fun <V : RealNumber<V>> PhysicalUnit.three(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.three, this)
}

fun <V : RealNumber<V>> PhysicalUnit.five(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.five, this)
}

fun <V : RealNumber<V>> PhysicalUnit.ten(constants: RealNumberConstants<V>): Quantity<V> {
    return Quantity(constants.ten, this)
}

fun <V : FloatingNumber<V>> PhysicalUnit.pi(constants: FloatingNumberConstants<V>): Quantity<V> {
    return Quantity(constants.pi, this)
}

fun <V : FloatingNumber<V>> PhysicalUnit.e(constants: FloatingNumberConstants<V>): Quantity<V> {
    return Quantity(constants.e, this)
}

// Comparison requires same unit / 比较操作需要相同单位
// Throws NotImplementedError for same quantity different units (TODO: implement unit conversion) / 同量纲异单位抛出 NotImplementedError（待实现单位转换）
// Returns false/null for different quantities / 不同量纲返回 false/null
infix fun <V> Quantity<V>.eq(other: Quantity<V>): Boolean where V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value eq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return false
        this.value eq converted.value
    } else {
        false
    }
}

infix fun <V> Quantity<V>.neq(other: Quantity<V>): Boolean where V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value neq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return false
        this.value neq converted.value
    } else {
        false
    }
}

infix fun <V> Quantity<V>.partialOrd(other: Quantity<V>): Order? where V : PartialOrd<V> {
    return if (this.unit == other.unit) {
        this.value partialOrd other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value partialOrd converted.value
    } else {
        null
    }
}

infix fun <V> Quantity<V>.ls(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value ls other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value ls converted.value
    } else {
        null
    }
}

infix fun <V> Quantity<V>.leq(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value leq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value leq converted.value
    } else {
        null
    }
}

infix fun <V> Quantity<V>.gr(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value gr other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value gr converted.value
    } else {
        null
    }
}

infix fun <V> Quantity<V>.geq(other: Quantity<V>): Boolean? where V : Ord<V> {
    return if (this.unit == other.unit) {
        this.value geq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return null
        this.value geq converted.value
    } else {
        null
    }
}

fun <V : RealNumber<V>> Quantity<V>.toInt64(): Quantity<Int64> {
    return Quantity(this.value.toInt64(), this.unit)
}

fun <V : RealNumber<V>> Quantity<V>.toUInt64(): Quantity<UInt64> {
    return Quantity(this.value.toUInt64(), this.unit)
}

fun <V : RealNumber<V>> Quantity<V>.toIntX(): Quantity<IntX> {
    return Quantity(this.value.toIntX(), this.unit)
}

fun <V : RealNumber<V>> Quantity<V>.toFlt64(): Quantity<Flt64> {
    return Quantity(this.value.toFlt64(), this.unit)
}

fun <V : RealNumber<V>> Quantity<V>.toFltX(): Quantity<FltX> {
    return Quantity(this.value.toFltX(), this.unit)
}

fun <F : FloatingImpl<F>> Quantity<F>.floor(): Quantity<F> {
    return Quantity(value.floor(), unit)
}

fun <F : FloatingImpl<F>> Quantity<F>.ceil(): Quantity<F> {
    return Quantity(value.ceil(), unit)
}

fun <F : FloatingImpl<F>> Quantity<F>.round(): Quantity<F> {
    return Quantity(value.round(), unit)
}


@JvmName("convertQuantityInt64")
fun Quantity<Int64>.to(unit: PhysicalUnit): Quantity<Int64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toInt64() * this.value, unit)
        }
    }
}

@JvmName("convertQuantityUInt64")
fun Quantity<UInt64>.to(unit: PhysicalUnit): Quantity<UInt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toUInt64() * this.value, unit)
        }
    }
}

fun Quantity<IntX>.to(unit: PhysicalUnit): Quantity<IntX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toIntX() * this.value, unit)
        }
    }
}

@JvmName("convertQuantityFlt64")
fun Quantity<Flt64>.to(unit: PhysicalUnit): Quantity<Flt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toFlt64() * this.value, unit)
        }
    }
}

@JvmName("convertQuantityFltX")
fun Quantity<FltX>.to(unit: PhysicalUnit): Quantity<FltX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toFltX() * this.value, unit)
        }
    }
}

operator fun <V : Arithmetic<V>> V.times(unit: PhysicalUnit): Quantity<V> {
    return Quantity(this, unit)
}

operator fun Int.times(unit: PhysicalUnit): Quantity<Int64> {
    return Quantity(Int64(this.toLong()), unit)
}

operator fun UInt.times(unit: PhysicalUnit): Quantity<UInt64> {
    return Quantity(UInt64(this.toULong()), unit)
}

operator fun BigInteger.times(unit: PhysicalUnit): Quantity<IntX> {
    return Quantity(IntX(this), unit)
}

operator fun Double.times(unit: PhysicalUnit): Quantity<Flt64> {
    return Quantity(Flt64(this), unit)
}

operator fun BigDecimal.times(unit: PhysicalUnit): Quantity<FltX> {
    return Quantity(FltX(this), unit)
}

@JvmName("plusQuantityInt64")
operator fun Quantity<Int64>.plus(other: Quantity<Int64>): Quantity<Int64> {
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

@JvmName("plusQuantityUInt64")
operator fun Quantity<UInt64>.plus(other: Quantity<UInt64>): Quantity<UInt64> {
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

@JvmName("plusQuantityIntX")
operator fun Quantity<IntX>.plus(other: Quantity<IntX>): Quantity<IntX> {
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

@JvmName("plusQuantityFlt64")
operator fun Quantity<Flt64>.plus(other: Quantity<Flt64>): Quantity<Flt64> {
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

@JvmName("plusQuantityFltX")
operator fun Quantity<FltX>.plus(other: Quantity<FltX>): Quantity<FltX> {
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

@JvmName("minusQuantityInt64")
operator fun Quantity<Int64>.minus(other: Quantity<Int64>): Quantity<Int64> {
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

@JvmName("minusQuantityUInt64")
operator fun Quantity<UInt64>.minus(other: Quantity<UInt64>): Quantity<UInt64> {
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

@JvmName("minusQuantityIntX")
operator fun Quantity<IntX>.minus(other: Quantity<IntX>): Quantity<IntX> {
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

@JvmName("minusQuantityFlt64")
operator fun Quantity<Flt64>.minus(other: Quantity<Flt64>): Quantity<Flt64> {
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

@JvmName("minusQuantityFltX")
operator fun Quantity<FltX>.minus(other: Quantity<FltX>): Quantity<FltX> {
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

operator fun <V> Quantity<V>.times(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    // 不同量纲相乘，产生新的量�?
    val newUnit = this.unit * other.unit
    return Quantity(this.value * other.value, newUnit)
}

operator fun <V> Quantity<V>.times(other: V): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    return Quantity(this.value * other, this.unit)
}

operator fun <V> V.times(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    return Quantity(this * other.value, other.unit)
}

operator fun <V> Quantity<V>.div(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    // 不同量纲相除，产生新的量�?
    val newUnit = this.unit / other.unit
    return Quantity(this.value / other.value, newUnit)
}

operator fun <V> Quantity<V>.div(other: V): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    return Quantity(this.value / other, this.unit)
}

operator fun <V> V.div(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    return Quantity(this / other.value, other.unit.reciprocal())
}

operator fun <V> Quantity<V>.unaryMinus(): Quantity<V> where V : Arithmetic<V>, V : Neg<V> {
    return Quantity(-this.value, this.unit)
}

// ============================================================================
// 单位转换方法（带错误处理�?
// ============================================================================

/**
 * 转换到另一个单位（带错误处理）
 * @param unit 目标单位
 * @return 转换后的物理量，如果量纲不匹配返�?null
 */
fun <V> Quantity<V>.convertTo(unit: PhysicalUnit): Quantity<V>? {
    if (!this.unit.sameDimension(unit)) {
        return null
    }

    return this.tryConvertByValueType(unit)
}

/**
 * 转换到标准单�?
 * @param system 单位�?
 * @return 转换后的物理量，如果无法转换返回 null
 */
fun <V> Quantity<V>.toStandardUnit(system: UnitSystem): Quantity<V>? {
    val standardUnit = system.standardUnitForDimension(this.unit.quantity) ?: return null
    return convertTo(standardUnit)
}

// ============================================================================
// 值类型映射方�?
// ============================================================================

/**
 * 映射值类�?
 * @param f 值转换函�?
 */
fun <V, U> Quantity<V>.mapValue(f: (V) -> U): Quantity<U> {
    return Quantity(f(this.value), this.unit)
}

/**
 * 尝试映射值类�?
 * @param f 值转换函数，可能失败
 */
fun <V, U> Quantity<V>.tryMapValue(f: (V) -> U?): Quantity<U>? {
    val newValue = f(this.value) ?: return null
    return Quantity(newValue, this.unit)
}




