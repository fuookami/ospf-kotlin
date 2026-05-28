/**
 * 无符号整数模块
 * Unsigned Integer Module
 *
 * 本模块定义了无符号整数的类型系统，包括 UInt8、UInt16、UInt32、UInt64 和 UIntX（任意精度无符号整数）。
 * 这些类型提供了完整的算术运算、比较操作、类型转换以及数学函数支持。
 * 无符号整数只能表示非负值，适用于需要确保数值为正的场景。
 *
 * This module defines the unsigned integer type system, including UInt8, UInt16, UInt32, UInt64, and UIntX (arbitrary precision unsigned integer).
 * These types provide full support for arithmetic operations, comparison operations, type conversions, and mathematical functions.
 * Unsigned integers can only represent non-negative values, suitable for scenarios where positive values need to be ensured.
 */
package fuookami.ospf.kotlin.math.algebra.number

import java.math.BigInteger
import kotlin.math.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.orderOf
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.pow

/**
 * 使用浮点基数计算无符号整数的对数
 * Calculate the logarithm of an unsigned integer using a floating-point base
 *
 * @param floatValue Float 值表示
 *                   The Float value representation
 * @param doubleValue Double 值表示
 *                    The Double value representation
 * @param base 浮点数基数
 *              The floating-point base
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 对数结果，以浮点数表示；如果无法计算则返回 null
 *         The logarithm result as a floating-point number; null if cannot be calculated
 */
private fun uIntegerLogByFloatingBase(
    floatValue: Float,
    doubleValue: Double,
    base: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*>? = when (base) {
    is Flt32 -> Flt32(log(floatValue, base.value))
    is Flt64 -> Flt64(log(doubleValue, base.value))
    is FltX -> toFltX().log(base)
    else -> throw IllegalArgumentException("Unknown argument type to $source.log: ${base.javaClass}")
}

/**
 * 使用浮点指数计算无符号整数的幂
 * Calculate the power of an unsigned integer using a floating-point index
 *
 * @param floatValue Float 值表示
 *                   The Float value representation
 * @param doubleValue Double 值表示
 *                    The Double value representation
 * @param index 浮点数指数
 *              The floating-point index
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 幂运算结果，以浮点数表示
 *         The power operation result as a floating-point number
 */
private fun uIntegerPowByFloatingIndex(
    floatValue: Float,
    doubleValue: Double,
    index: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*> = when (index) {
    is Flt32 -> Flt32(floatValue.pow(index.value))
    is Flt64 -> Flt64(doubleValue.pow(index.value))
    is FltX -> toFltX().pow(index)
    else -> throw IllegalArgumentException("Unknown argument type to $source.pow: ${index.javaClass}")
}

/**
 * 无符号整数实现接口
 * Unsigned Integer Implementation Interface
 *
 * 提供无符号整数类型的通用实现，包括绝对值、倒数、自增自减、整数除法、
 * 对数、幂运算、平方、立方、三角函数等数学运算的默认实现。
 * 注意：无符号整数的倒数仅对单位值（1）有效。
 *
 * Provides common implementation for unsigned integer types, including default implementations
 * for absolute value, reciprocal, increment/decrement, integer division, logarithm, power operations,
 * square, cube, trigonometric functions and other mathematical operations.
 * Note: Reciprocal of unsigned integers is only valid for unit value (1).
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 */
interface UIntegerNumberImpl<Self : UIntegerNumberImpl<Self>> : UIntegerNumber<Self> {
    override fun abs() = copy()
    override fun reciprocal() = when (this) {
        constants.one -> constants.one.copy()
        else -> throw ArithmeticException("Reciprocal is undefined in UInteger domain for non-unit value: $this")
    }

    override fun intDiv(rhs: Self) = this / rhs

    override operator fun inc(): Self = this + constants.one
    override operator fun dec(): Self = this - constants.one

    override fun lg() = log(Flt64.ten)
    override fun lg2() = log(Flt64.two)
    override fun ln() = log(Flt64.e)

    override fun pow(index: Int) = pow(copy(), index, constants)
    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(Flt64.two.reciprocal())
    override fun cbrt() = pow(Flt64.three.reciprocal())

    override fun exp(): FloatingNumber<*> = toFlt64().exp()

    override fun sin(): FloatingNumber<*> = toFlt64().sin()
    override fun cos(): FloatingNumber<*> = toFlt64().cos()
    override fun sec(): FloatingNumber<*>? = toFlt64().sec()
    override fun csc(): FloatingNumber<*>? = toFlt64().csc()
    override fun tan(): FloatingNumber<*>? = toFlt64().tan()
    override fun cot(): FloatingNumber<*>? = toFlt64().cot()

    override fun asin(): FloatingNumber<*>? = toFlt64().asin()
    override fun acos(): FloatingNumber<*>? = toFlt64().acos()
    override fun asec(): FloatingNumber<*>? = toFlt64().asec()
    override fun acsc(): FloatingNumber<*>? = toFlt64().acsc()
    override fun atan(): FloatingNumber<*> = toFlt64().atan()
    override fun acot(): FloatingNumber<*>? = toFlt64().acot()

    override fun sinh(): FloatingNumber<*> = toFlt64().sinh()
    override fun cosh(): FloatingNumber<*> = toFlt64().cosh()
    override fun sech(): FloatingNumber<*> = toFlt64().sech()
    override fun csch(): FloatingNumber<*>? = toFlt64().csch()
    override fun tanh(): FloatingNumber<*> = toFlt64().tanh()
    override fun coth(): FloatingNumber<*>? = toFlt64().coth()

    override fun asinh(): FloatingNumber<*> = toFlt64().asinh()
    override fun acosh(): FloatingNumber<*>? = toFlt64().acosh()
    override fun asech(): FloatingNumber<*>? = toFlt64().asech()
    override fun acsch(): FloatingNumber<*>? = toFlt64().acsch()
    override fun atanh(): FloatingNumber<*>? = toFlt64().atanh()
    override fun acoth(): FloatingNumber<*>? = toFlt64().acoth()

    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    override infix fun until(rhs: Self) = if (rhs == constants.zero) {
        this.rangeTo(rhs)
    } else {
        this.rangeTo(rhs - constants.one)
    }
}

/**
 * UInt8 序列化器
 * UInt8 Serializer
 *
 * 用于 UInt8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt8 type in the Kotlin serialization framework.
 */
data object UInt8Serializer : KSerializer<UInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt8) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt8 {
        return UInt8(decoder.decodeInt().toUByte())
    }
}

/**
 * 8位无符号整数
 * 8-bit Unsigned Integer
 *
 * 基于 Kotlin UByte 类型封装的 8 位无符号整数，值范围为 0 到 255。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * An 8-bit unsigned integer encapsulated based on Kotlin UByte type, with value range from 0 to 255.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 UByte 值
 *                 The internal UByte value
 */
@JvmInline
@Serializable(with = UInt8Serializer::class)
value class UInt8(internal val value: UByte) : UIntegerNumberImpl<UInt8>, Copyable<UInt8> {
    /**
     * 从 Boolean 构造 UInt8 的构造函数
     * Constructor for UInt8 from Boolean
     *
     * @param value Boolean 值，true 对应 1，false 对应 0
     *              Boolean value, true corresponds to 1, false corresponds to 0
     */
    constructor(value: Boolean) : this(
        if (value) {
            1U
        } else {
            0U
        }
    )

    /**
     * UInt8 常量对象
     * UInt8 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt8> {
        @JvmStatic
        override val zero: UInt8 get() = UInt8(0U)

        @JvmStatic
        override val one: UInt8 get() = UInt8(1U)

        @JvmStatic
        override val two: UInt8 get() = UInt8(2U)

        @JvmStatic
        override val three: UInt8 get() = UInt8(3U)

        @JvmStatic
        override val five: UInt8 get() = UInt8(5U)

        @JvmStatic
        override val ten: UInt8 get() = UInt8(10U)

        @JvmStatic
        override val minimum: UInt8 get() = UInt8(UByte.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt8 get() = UInt8(UByte.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt8> get() = Companion

    override fun copy() = UInt8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int): String = value.toString(radix)

    override fun partialOrd(rhs: UInt8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt8) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = maximum - this

    override operator fun plus(rhs: UInt8) = UInt8((value + rhs.value).toUByte())
    override operator fun minus(rhs: UInt8) = UInt8((value - rhs.value).toUByte())
    override operator fun times(rhs: UInt8) = UInt8((value * rhs.value).toUByte())
    override operator fun div(rhs: UInt8) = UInt8((value / rhs.value).toUByte())
    override operator fun rem(rhs: UInt8) = UInt8((value % rhs.value).toUByte())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt8")

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt8")

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = copy()
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt16 序列化器
 * UInt16 Serializer
 *
 * 用于 UInt16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt16 type in the Kotlin serialization framework.
 */
data object UInt16Serializer : KSerializer<UInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt16) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt16 {
        return UInt16(decoder.decodeInt().toUShort())
    }
}

/**
 * 16位无符号整数
 * 16-bit Unsigned Integer
 *
 * 基于 Kotlin UShort 类型封装的 16 位无符号整数，值范围为 0 到 65535。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * A 16-bit unsigned integer encapsulated based on Kotlin UShort type, with value range from 0 to 65535.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 UShort 值
 *                 The internal UShort value
 */
@JvmInline
@Serializable(with = UInt16Serializer::class)
value class UInt16(internal val value: UShort) : UIntegerNumberImpl<UInt16>, Copyable<UInt16> {
    /**
     * UInt16 常量对象
     * UInt16 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt16> {
        @JvmStatic
        override val zero: UInt16 get() = UInt16(0U)

        @JvmStatic
        override val one: UInt16 get() = UInt16(1U)

        @JvmStatic
        override val two: UInt16 get() = UInt16(2U)

        @JvmStatic
        override val three: UInt16 get() = UInt16(3U)

        @JvmStatic
        override val five: UInt16 get() = UInt16(5U)

        @JvmStatic
        override val ten: UInt16 get() = UInt16(10U)

        @JvmStatic
        override val minimum: UInt16 get() = UInt16(UShort.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt16 get() = UInt16(UShort.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt16> get() = Companion

    override fun copy() = UInt16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt16) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = maximum - this

    override operator fun plus(rhs: UInt16) = UInt16((value + rhs.value).toUShort())
    override operator fun minus(rhs: UInt16) = UInt16((value - rhs.value).toUShort())
    override operator fun times(rhs: UInt16) = UInt16((value * rhs.value).toUShort())
    override operator fun div(rhs: UInt16) = UInt16((value / rhs.value).toUShort())
    override operator fun rem(rhs: UInt16) = UInt16((value % rhs.value).toUShort())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt16")

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt16")

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = copy()
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt32 序列化器
 * UInt32 Serializer
 *
 * 用于 UInt32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt32 type in the Kotlin serialization framework.
 */
data object UInt32Serializer : KSerializer<UInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt32) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt32 {
        return UInt32(decoder.decodeInt().toUInt())
    }
}

/**
 * 32位无符号整数
 * 32-bit Unsigned Integer
 *
 * 基于 Kotlin UInt 类型封装的 32 位无符号整数，值范围为 0 到 4294967295。
 * 支持完整的算术运算、比较操作和类型转换。这是常用的无符号整数类型。
 *
 * A 32-bit unsigned integer encapsulated based on Kotlin UInt type, with value range from 0 to 4294967295.
 * Supports full arithmetic operations, comparison operations, and type conversions. This is a commonly used unsigned integer type.
 *
 * @property value 内部的 UInt 值
 *                 The internal UInt value
 */
@JvmInline
@Serializable(with = UInt32Serializer::class)
value class UInt32(internal val value: UInt) : UIntegerNumberImpl<UInt32>, Copyable<UInt32> {
    /**
     * UInt32 常量对象
     * UInt32 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt32> {
        @JvmStatic
        override val zero: UInt32 get() = UInt32(0U)

        @JvmStatic
        override val one: UInt32 get() = UInt32(1U)

        @JvmStatic
        override val two: UInt32 get() = UInt32(2U)

        @JvmStatic
        override val three: UInt32 get() = UInt32(3U)

        @JvmStatic
        override val five: UInt32 get() = UInt32(5U)

        @JvmStatic
        override val ten: UInt32 get() = UInt32(10U)

        @JvmStatic
        override val minimum: UInt32 get() = UInt32(UInt.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt32 get() = UInt32(UInt.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt32> get() = UInt32

    override fun copy() = UInt32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt32) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = maximum - this

    override operator fun plus(rhs: UInt32) = UInt32(value + rhs.value)
    override operator fun minus(rhs: UInt32) = UInt32(value - rhs.value)
    override operator fun times(rhs: UInt32) = UInt32(value * rhs.value)
    override operator fun div(rhs: UInt32) = UInt32(value / rhs.value)
    override operator fun rem(rhs: UInt32) = UInt32(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt32")

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt32")

    fun toInt() = value.toInt()
    fun toLong() = value.toLong()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = copy()
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt64 序列化器
 * UInt64 Serializer
 *
 * 用于 UInt64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt64 type in the Kotlin serialization framework.
 */
data object UInt64Serializer : KSerializer<UInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: UInt64) {
        encoder.encodeLong(value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): UInt64 {
        return UInt64(decoder.decodeLong().toULong())
    }
}

/**
 * 64位无符号整数
 * 64-bit Unsigned Integer
 *
 * 基于 Kotlin ULong 类型封装的 64 位无符号整数，值范围为 0 到 18446744073709551615。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要更大数值范围的情况。
 *
 * A 64-bit unsigned integer encapsulated based on Kotlin ULong type, with value range from 0 to 18446744073709551615.
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for cases requiring larger numerical range.
 *
 * @property value 内部的 ULong 值
 *                 The internal ULong value
 */
@JvmInline
@Serializable(with = UInt64Serializer::class)
value class UInt64(internal val value: ULong) : UIntegerNumberImpl<UInt64>, Copyable<UInt64> {
    /**
     * 从 Int 构造 UInt64 的构造函数
     * Constructor for UInt64 from Int
     *
     * @param value Int 值
     *              The Int value
     */
    constructor(value: Int) : this(value.toULong())

    /**
     * UInt64 常量对象
     * UInt64 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt64> {
        @JvmStatic
        override val zero: UInt64 get() = UInt64(0UL)

        @JvmStatic
        override val one: UInt64 get() = UInt64(1UL)

        @JvmStatic
        override val two: UInt64 get() = UInt64(2UL)

        @JvmStatic
        override val three: UInt64 get() = UInt64(3UL)

        @JvmStatic
        override val five: UInt64 get() = UInt64(5UL)

        @JvmStatic
        override val ten: UInt64 get() = UInt64(10UL)

        @JvmStatic
        override val minimum: UInt64 get() = UInt64(ULong.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt64 get() = UInt64(ULong.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt64> get() = UInt64

    override fun copy() = UInt64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt64) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = maximum - this

    override operator fun plus(rhs: UInt64) = UInt64(value + rhs.value)
    override operator fun minus(rhs: UInt64) = UInt64(value - rhs.value)
    override operator fun times(rhs: UInt64) = UInt64(value * rhs.value)
    override operator fun div(rhs: UInt64) = UInt64(value / rhs.value)
    override operator fun rem(rhs: UInt64) = UInt64(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt64")

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt64")

    fun toInt() = value.toInt()
    fun toLong() = value.toLong()
    fun toULong() = value

    val indices get() = zero until this

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = copy()
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toString())
}

/**
 * UIntX 序列化器
 * UIntX Serializer
 *
 * 用于 UIntX（任意精度无符号整数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化，以支持任意大小的无符号整数。
 *
 * Serializer for the UIntX (arbitrary precision unsigned integer) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization to support unsigned integers of arbitrary size.
 */
data object UIntXSerializer : KSerializer<UIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UIntX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): UIntX {
        return UIntX(decoder.decodeString())
    }
}

/**
 * 任意精度无符号整数
 * Arbitrary Precision Unsigned Integer
 *
 * 基于 Java BigInteger 类型封装的任意精度无符号整数，没有固定的数值范围限制（只能表示非负值）。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要极大数值或精确计算的场景。
 *
 * An arbitrary precision unsigned integer encapsulated based on Java BigInteger type, with no fixed numerical range limit (can only represent non-negative values).
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for scenarios requiring extremely large numbers or precise calculations.
 *
 * @property value 内部的 BigInteger 值，必须为非负数
 *                 The internal BigInteger value, must be non-negative
 */
@JvmInline
@Serializable(with = UIntXSerializer::class)
value class UIntX(internal val value: BigInteger) : UIntegerNumberImpl<UIntX>, Copyable<UIntX> {
    /**
     * UIntX 常量对象
     * UIntX Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UIntX> {
        @JvmStatic
        override val zero: UIntX get() = UIntX(0L)

        @JvmStatic
        override val one: UIntX get() = UIntX(1L)

        @JvmStatic
        override val two: UIntX get() = UIntX(2L)

        @JvmStatic
        override val three: UIntX get() = UIntX(3L)

        @JvmStatic
        override val five: UIntX get() = UIntX(5L)

        @JvmStatic
        override val ten: UIntX get() = UIntX(10L)

        @JvmStatic
        override val minimum: UIntX get() = UIntX(0L)

        @JvmStatic
        override val maximum: UIntX get() = UIntX(Double.MAX_VALUE.toString())
    }

    /**
     * 从 Long 构造 UIntX 的构造函数
     * Constructor for UIntX from Long
     *
     * @param value Long 值，必须为非负数
     *              The Long value, must be non-negative
     */
    constructor(value: Long) : this(BigInteger.valueOf(value))

    /**
     * 从字符串构造 UIntX 的构造函数
     * Constructor for UIntX from String
     *
     * @param value 字符串表示的数值
     *              The string representation of the value
     * @param radix 进制基数，默认为 10
     *              The radix base, defaults to 10
     */
    constructor(value: String, radix: Int = 10) : this(BigInteger(value, radix))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Illegal negative value to UIntX: $value")
        }
    }

    override val constants: RealNumberConstants<UIntX> get() = UIntX
    override val isBounded: Boolean get() = false
    override val minBound: UIntX? get() = null
    override val maxBound: UIntX? get() = null

    override fun copy() = UIntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int): String = value.toString(radix)

    override fun partialOrd(rhs: UIntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UIntX) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = maximum - this

    override operator fun plus(rhs: UIntX) = UIntX(value + rhs.value)
    override operator fun minus(rhs: UIntX) = UIntX(value - rhs.value)
    override operator fun times(rhs: UIntX) = UIntX(value * rhs.value)
    override operator fun div(rhs: UIntX) = UIntX(value / rhs.value)
    override operator fun rem(rhs: UIntX) = UIntX(value % rhs.value)

    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0)) as FltX
    override fun ln() = log(FltX.e) as FltX

    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.pow: ${index.javaClass}")
    }

    override fun sqrt() = pow(FltX(1.0 / 2.0)) as FltX
    override fun cbrt() = pow(FltX(1.0 / 3.0)) as FltX
    override fun exp() = toFltX().exp()

    override fun sin() = toFltX().sin()
    override fun cos() = toFltX().cos()
    override fun sec() = toFltX().sec()
    override fun csc() = toFltX().csc()
    override fun tan() = toFltX().tan()
    override fun cot() = toFltX().cot()

    override fun asin() = toFltX().asin()
    override fun acos() = toFltX().acos()
    override fun asec() = toFltX().asec()
    override fun acsc() = toFltX().acsc()
    override fun atan() = toFltX().atan()
    override fun acot() = toFltX().acot()

    override fun sinh() = toFltX().sinh()
    override fun cosh() = toFltX().cosh()
    override fun sech() = toFltX().sech()
    override fun csch() = toFltX().csch()
    override fun tanh() = toFltX().tanh()
    override fun coth() = toFltX().coth()

    override fun asinh() = toFltX().asinh()
    override fun acosh() = toFltX().acosh()
    override fun asech() = toFltX().asech()
    override fun acsch() = toFltX().acsch()
    override fun atanh() = toFltX().atanh()
    override fun acoth() = toFltX().acoth()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value)

    override fun toUInt8() = UInt8(value.toLong().toUByte())
    override fun toUInt16() = UInt16(value.toLong().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = copy()

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toBigDecimal())
}

/**
 * 将 Boolean 值转换为 UInt8
 * Convert Boolean value to UInt8
 *
 * @return 如果为 true 则返回 UInt8.one，否则返回 UInt8.zero
 *         Returns UInt8.one if true, otherwise UInt8.zero
 */
fun Boolean.toUInt8() = if (this) {
    UInt8.one
} else {
    UInt8.zero
}

/**
 * 将 Boolean 值转换为 UInt64
 * Convert Boolean value to UInt64
 *
 * @return 如果为 true 则返回 UInt64.one，否则返回 UInt64.zero
 *         Returns UInt64.one if true, otherwise UInt64.zero
 */
fun Boolean.toUInt64() = if (this) {
    UInt64.one
} else {
    UInt64.zero
}

/**
 * 将字符串转换为 UInt8
 * Convert string to UInt8
 *
 * @return UInt8 值
 *         The UInt8 value
 */
fun String.toUInt8() = UInt8(toUByte())

/**
 * 将字符串转换为 UInt8，如果转换失败则返回 null
 * Convert string to UInt8, returns null if conversion fails
 *
 * @return UInt8 值或 null
 *         The UInt8 value or null
 */
fun String.toUInt8OrNull() = toUByteOrNull()?.let { UInt8(it) }

/**
 * 将字符串转换为 UInt16
 * Convert string to UInt16
 *
 * @return UInt16 值
 *         The UInt16 value
 */
fun String.toUInt16() = UInt16(toUShort())

/**
 * 将字符串转换为 UInt16，如果转换失败则返回 null
 * Convert string to UInt16, returns null if conversion fails
 *
 * @return UInt16 值或 null
 *         The UInt16 value or null
 */
fun String.toUInt16OrNull() = toUShortOrNull()?.let { UInt16(it) }

/**
 * 将字符串转换为 UInt32
 * Convert string to UInt32
 *
 * @return UInt32 值
 *         The UInt32 value
 */
fun String.toUInt32() = UInt32(toUInt())

/**
 * 将字符串转换为 UInt32，如果转换失败则返回 null
 * Convert string to UInt32, returns null if conversion fails
 *
 * @return UInt32 值或 null
 *         The UInt32 value or null
 */
fun String.toUInt32OrNull() = toUIntOrNull()?.let { UInt32(it) }

/**
 * 将字符串转换为 UInt64
 * Convert string to UInt64
 *
 * @return UInt64 值
 *         The UInt64 value
 */
fun String.toUInt64() = UInt64(toULong())

/**
 * 将字符串转换为 UInt64，如果转换失败则返回 null
 * Convert string to UInt64, returns null if conversion fails
 *
 * @return UInt64 值或 null
 *         The UInt64 value or null
 */
fun String.toUInt64OrNull() = toULongOrNull()?.let { UInt64(it) }

/**
 * 将字符串转换为 UIntX
 * Convert string to UIntX
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return UIntX 值
 *         The UIntX value
 */
fun String.toUIntX(radix: Int = 10) = UIntX(this, radix)

/**
 * 将字符串转换为 UIntX，如果转换失败则返回 null
 * Convert string to UIntX, returns null if conversion fails
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return UIntX 值或 null
 *         The UIntX value or null
 */
fun String.toUIntXOrNull(radix: Int = 10) = runCatching { UIntX(this, radix) }.getOrNull()
