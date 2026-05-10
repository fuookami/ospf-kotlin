/**
 * 数值无符号整数模块
 * Numeric Unsigned Integer Module
 *
 * 本模块定义了带有数值语义的无符号整数类型，包括 NUInt8、NUInt16、NUInt32、NUInt64 和 NUIntX。
 * 与普通无符号整数不同，这些类型的除法运算返回有理数结果而非整数结果，
 * 从而提供更精确的数值计算。适用于需要精确数值计算的场景。
 *
 * This module defines unsigned integer types with numeric semantics, including NUInt8, NUInt16, NUInt32, NUInt64, and NUIntX.
 * Unlike regular unsigned integers, division operations of these types return rational number results instead of integer results,
 * thus providing more precise numerical calculations. Suitable for scenarios requiring precise numerical calculations.
 */
package fuookami.ospf.kotlin.math.algebra.number

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.functional.orderOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 数值无符号整数接口
 * Numeric Unsigned Integer Interface
 *
 * 提供数值无符号整数类型的通用实现，包括自增自减、对数、幂运算、
 * 三角函数等数学运算的默认实现。
 * 注意：除法运算返回有理数结果，减法运算可能返回有符号整数结果。
 *
 * Provides common implementation for numeric unsigned integer types, including default implementations
 * for increment/decrement, logarithm, power operations, trigonometric functions, and other mathematical operations.
 * Note: Division operation returns rational number result, subtraction operation may return signed integer result.
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 * @param I 底层无符号整数类型
 *           The underlying unsigned integer type
 */
interface NumericUInteger<Self, I>
    : NumericUIntegerNumber<Self, I> where Self : NumericUInteger<Self, I>, I : UIntegerNumber<I>, I : NumberField<I> {
    override operator fun inc() = this + constants.one

    override fun lg() = log(Flt64.ten)
    override fun lg2() = log(Flt64.two)
    override fun ln() = log(Flt64.e)

    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(Flt64.two.reciprocal())
    override fun cbrt() = pow(Flt64.three.reciprocal())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NumericUInteger.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NumericUInteger.pow: ${index.javaClass}")
    }

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
}

/**
 * 数值无符号整数常量抽象类
 * Abstract Numeric Unsigned Integer Constants
 *
 * 提供数值无符号整数类型的常用数值常量。
 * Provides common numeric constants for numeric unsigned integer types.
 *
 * @param Self 数值无符号整数类型
 *             The numeric unsigned integer type
 * @param I 底层无符号整数类型
 *           The underlying unsigned integer type
 * @param ctor 数值无符号整数构造函数
 *             The numeric unsigned integer constructor
 * @param constants 底层无符号整数常量对象
 *                  The underlying unsigned integer constants object
 */
abstract class NumericUIntegerConstants<Self, I>(
    private val ctor: (I) -> Self,
    private val constants: RealNumberConstants<I>
) : RealNumberConstants<Self> where Self : NumericUInteger<Self, I>, I : UIntegerNumber<I>, I : NumberField<I> {
    override val zero: Self get() = ctor(constants.zero)
    override val one: Self get() = ctor(constants.one)
    override val two: Self get() = ctor(constants.two)
    override val three: Self get() = ctor(constants.three)
    override val five: Self get() = ctor(constants.five)
    override val ten: Self get() = ctor(constants.ten)
    override val minimum: Self get() = ctor(constants.minimum)
    override val maximum: Self get() = ctor(constants.maximum)
}

/**
 * NUInt8 序列化器
 * NUInt8 Serializer
 *
 * 用于 NUInt8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NUInt8 type in the Kotlin serialization framework.
 */
data object NUInt8Serializer : KSerializer<NUInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NUInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NUInt8) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NUInt8 {
        return NUInt8(UInt8(decoder.decodeInt().toUByte()))
    }
}

/**
 * 基于 UInt8 的数值无符号整数
 * Numeric Unsigned Integer based on UInt8
 *
 * 使用 UInt8 作为底层类型的数值无符号整数。
 * 除法运算返回 URtn8 结果，减法运算可能返回 NInt8 结果，提供精确的数值计算。
 *
 * A numeric unsigned integer using UInt8 as the underlying type.
 * Division operation returns URtn8 result, subtraction operation may return NInt8 result, providing precise numerical calculations.
 *
 * @property value 底层的 UInt8 值
 *                 The underlying UInt8 value
 */
@JvmInline
@Serializable(with = NUInt8Serializer::class)
value class NUInt8(val value: UInt8) : NumericUInteger<NUInt8, UInt8>, Copyable<NUInt8> {
    /**
     * NUInt8 常量对象
     * NUInt8 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericUIntegerConstants<NUInt8, UInt8>(NUInt8::invoke, UInt8) {
        operator fun invoke(value: UInt8) = NUInt8(value)
    }

    override val constants: RealNumberConstants<NUInt8> get() = NUInt8

    override fun copy(): NUInt8 = NUInt8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override operator fun dec(): NUInt8 = NUInt8(value - UInt8.one)

    override fun partialOrd(rhs: NUInt8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NUInt8) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = URtn8(UInt8.one, value)
    override operator fun unaryMinus() = NInt8(-value.toInt8())
    override fun abs() = NUInt8(value.abs())

    override operator fun plus(rhs: NUInt8) = NUInt8(value + rhs.value)
    override operator fun minus(rhs: NUInt8) = NInt8(value.toInt8() - rhs.toInt8())
    override operator fun times(rhs: NUInt8) = NUInt8(value * rhs.value)
    override operator fun div(rhs: NUInt8) = URtn8(value, rhs.value)
    override operator fun rem(rhs: NUInt8) = NUInt8(value % rhs.value)
    override fun intDiv(rhs: NUInt8) = NUInt8(value / rhs.value)


    override fun pow(index: Int): URtn8 {
        return if (index >= 1) {
            URtn8(value.pow(index), UInt8.one)
        } else if (index <= -1) {
            URtn8(UInt8.one, value.pow(index))
        } else {
            URtn8.one
        }
    }



    override fun rangeTo(rhs: NUInt8) = NumericUIntegerRange(
        start = copy(),
        endInclusive = rhs,
        _step = one,
        constants = UInt8,
        ctor = UInt8::toNUInt8,
        converter = NUInt8::toUInt8
    )

    override infix fun until(rhs: NUInt8) = if (rhs == zero) {
        rangeTo(zero)
    } else {
        rangeTo((rhs - one).toNUInt8())
    }

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

/**
 * NUInt16 序列化器
 * NUInt16 Serializer
 *
 * 用于 NUInt16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NUInt16 type in the Kotlin serialization framework.
 */
data object NUInt16Serializer : KSerializer<NUInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NUInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NUInt16) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NUInt16 {
        return NUInt16(UInt16(decoder.decodeInt().toUShort()))
    }
}

/**
 * 基于 UInt16 的数值无符号整数
 * Numeric Unsigned Integer based on UInt16
 *
 * 使用 UInt16 作为底层类型的数值无符号整数。
 * 除法运算返回 URtn16 结果，减法运算可能返回 NInt16 结果，提供精确的数值计算。
 *
 * A numeric unsigned integer using UInt16 as the underlying type.
 * Division operation returns URtn16 result, subtraction operation may return NInt16 result, providing precise numerical calculations.
 *
 * @property value 底层的 UInt16 值
 *                 The underlying UInt16 value
 */
@JvmInline
@Serializable(with = NUInt16Serializer::class)
value class NUInt16(val value: UInt16) : NumericUInteger<NUInt16, UInt16>, Copyable<NUInt16> {
    /**
     * NUInt16 常量对象
     * NUInt16 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericUIntegerConstants<NUInt16, UInt16>(NUInt16::invoke, UInt16) {
        operator fun invoke(value: UInt16) = NUInt16(value)
    }

    override val constants: RealNumberConstants<NUInt16> get() = NUInt16

    override fun copy(): NUInt16 = NUInt16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override operator fun dec(): NUInt16 = NUInt16(value - UInt16.one)

    override fun partialOrd(rhs: NUInt16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NUInt16) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = URtn16(UInt16.one, value)
    override operator fun unaryMinus() = NInt16(-value.toInt16())
    override fun abs() = NUInt16(value.abs())

    override operator fun plus(rhs: NUInt16) = NUInt16(value + rhs.value)
    override operator fun minus(rhs: NUInt16) = NInt16(value.toInt16() - rhs.toInt16())
    override operator fun times(rhs: NUInt16) = NUInt16(value * rhs.value)
    override operator fun div(rhs: NUInt16) = URtn16(value, rhs.value)
    override operator fun rem(rhs: NUInt16) = NUInt16(value % rhs.value)
    override fun intDiv(rhs: NUInt16) = NUInt16(value / rhs.value)


    override fun pow(index: Int): URtn16 {
        return if (index >= 1) {
            URtn16(value.pow(index), UInt16.one)
        } else if (index <= -1) {
            URtn16(UInt16.one, value.pow(index))
        } else {
            URtn16.one
        }
    }



    override fun rangeTo(rhs: NUInt16) = NumericUIntegerRange(
        start = copy(),
        endInclusive = rhs,
        _step = one,
        constants = UInt16,
        ctor = UInt16::toNUInt16,
        converter = NUInt16::toUInt16
    )

    override infix fun until(rhs: NUInt16) = if (rhs == zero) {
        rangeTo(zero)
    } else {
        rangeTo((rhs - one).toNUInt16())
    }

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

/**
 * NUInt32 序列化器
 * NUInt32 Serializer
 *
 * 用于 NUInt32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NUInt32 type in the Kotlin serialization framework.
 */
data object NUInt32Serializer : KSerializer<NUInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NUInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NUInt32) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NUInt32 {
        return NUInt32(UInt32(decoder.decodeInt().toUInt()))
    }
}

/**
 * 基于 UInt32 的数值无符号整数
 * Numeric Unsigned Integer based on UInt32
 *
 * 使用 UInt32 作为底层类型的数值无符号整数。
 * 除法运算返回 URtn32 结果，减法运算可能返回 NInt32 结果，提供精确的数值计算。
 * 这是常用的数值无符号整数类型。
 *
 * A numeric unsigned integer using UInt32 as the underlying type.
 * Division operation returns URtn32 result, subtraction operation may return NInt32 result, providing precise numerical calculations.
 * This is a commonly used numeric unsigned integer type.
 *
 * @property value 底层的 UInt32 值
 *                 The underlying UInt32 value
 */
@JvmInline
@Serializable(with = NUInt32Serializer::class)
value class NUInt32(val value: UInt32) : NumericUInteger<NUInt32, UInt32>, Copyable<NUInt32> {
    /**
     * NUInt32 常量对象
     * NUInt32 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericUIntegerConstants<NUInt32, UInt32>(NUInt32::invoke, UInt32) {
        operator fun invoke(value: UInt32) = NUInt32(value)
    }

    override val constants: RealNumberConstants<NUInt32> get() = NUInt32

    override fun copy(): NUInt32 = NUInt32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override operator fun dec(): NUInt32 = NUInt32(value - UInt32.one)

    override fun partialOrd(rhs: NUInt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NUInt32) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = URtn32(UInt32.one, value)
    override operator fun unaryMinus() = NInt32(-value.toInt32())
    override fun abs() = NUInt32(value.abs())

    override operator fun plus(rhs: NUInt32) = NUInt32(value + rhs.value)
    override operator fun minus(rhs: NUInt32) = NInt32(value.toInt32() - rhs.toInt32())
    override operator fun times(rhs: NUInt32) = NUInt32(value * rhs.value)
    override operator fun div(rhs: NUInt32) = URtn32(value, rhs.value)
    override operator fun rem(rhs: NUInt32) = NUInt32(value % rhs.value)
    override fun intDiv(rhs: NUInt32) = NUInt32(value / rhs.value)


    override fun pow(index: Int): URtn32 {
        return if (index >= 1) {
            URtn32(value.pow(index), UInt32.one)
        } else if (index <= -1) {
            URtn32(UInt32.one, value.pow(index))
        } else {
            URtn32.one
        }
    }



    override fun rangeTo(rhs: NUInt32) = NumericUIntegerRange(
        start = copy(),
        endInclusive = rhs,
        _step = one,
        constants = UInt32,
        ctor = UInt32::toNUInt32,
        converter = NUInt32::toUInt32
    )

    override infix fun until(rhs: NUInt32) = if (rhs == zero) {
        rangeTo(zero)
    } else {
        rangeTo((rhs - one).toNUInt32())
    }

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

/**
 * NUInt64 序列化器
 * NUInt64 Serializer
 *
 * 用于 NUInt64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NUInt64 type in the Kotlin serialization framework.
 */
data object NUInt64Serializer : KSerializer<NUInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NUInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: NUInt64) {
        encoder.encodeLong(value.value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): NUInt64 {
        return NUInt64(UInt64(decoder.decodeLong().toULong()))
    }
}

/**
 * 基于 UInt64 的数值无符号整数
 * Numeric Unsigned Integer based on UInt64
 *
 * 使用 UInt64 作为底层类型的数值无符号整数。
 * 除法运算返回 URtn64 结果，减法运算可能返回 NInt64 结果，提供精确的数值计算。
 * 适用于需要更大数值范围的情况。
 *
 * A numeric unsigned integer using UInt64 as the underlying type.
 * Division operation returns URtn64 result, subtraction operation may return NInt64 result, providing precise numerical calculations.
 * Suitable for cases requiring larger numerical range.
 *
 * @property value 底层的 UInt64 值
 *                 The underlying UInt64 value
 */
@JvmInline
@Serializable(with = NUInt64Serializer::class)
value class NUInt64(val value: UInt64) : NumericUInteger<NUInt64, UInt64>, Copyable<NUInt64> {
    /**
     * NUInt64 常量对象
     * NUInt64 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericUIntegerConstants<NUInt64, UInt64>(NUInt64::invoke, UInt64) {
        operator fun invoke(value: UInt64) = NUInt64(value)
    }

    override val constants: RealNumberConstants<NUInt64> get() = NUInt64

    override fun copy(): NUInt64 = NUInt64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override operator fun dec(): NUInt64 = NUInt64(value - UInt64.one)

    override fun partialOrd(rhs: NUInt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NUInt64) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = URtn64(UInt64.one, value)
    override operator fun unaryMinus() = NInt64(-value.toInt64())
    override fun abs() = NUInt64(value.abs())

    override operator fun plus(rhs: NUInt64) = NUInt64(value + rhs.value)
    override operator fun minus(rhs: NUInt64) = NInt64(value.toInt64() - rhs.toInt64())
    override operator fun times(rhs: NUInt64) = NUInt64(value * rhs.value)
    override operator fun div(rhs: NUInt64) = URtn64(value, rhs.value)
    override operator fun rem(rhs: NUInt64) = NUInt64(value % rhs.value)
    override fun intDiv(rhs: NUInt64) = NUInt64(value / rhs.value)


    override fun pow(index: Int): URtn64 {
        return if (index >= 1) {
            URtn64(value.pow(index), UInt64.one)
        } else if (index <= -1) {
            URtn64(UInt64.one, value.pow(index))
        } else {
            URtn64.one
        }
    }



    override fun rangeTo(rhs: NUInt64) = NumericUIntegerRange(
        start = copy(),
        endInclusive = rhs,
        _step = one,
        constants = UInt64,
        ctor = UInt64::toNUInt64,
        converter = NUInt64::toUInt64
    )

    override infix fun until(rhs: NUInt64) = if (rhs == zero) {
        rangeTo(zero)
    } else {
        rangeTo((rhs - one).toNUInt64())
    }

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

/**
 * NUIntX 序列化器
 * NUIntX Serializer
 *
 * 用于 NUIntX（任意精度数值无符号整数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化。
 *
 * Serializer for the NUIntX (arbitrary precision numeric unsigned integer) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization.
 */
class NUIntXSerializer : KSerializer<NUIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NUIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NUIntX) {
        encoder.encodeString(value.value.toString(10))
    }

    override fun deserialize(decoder: Decoder): NUIntX {
        return NUIntX(UIntX(decoder.decodeString()))
    }
}

/**
 * 任意精度数值无符号整数
 * Arbitrary Precision Numeric Unsigned Integer
 *
 * 使用 UIntX 作为底层类型的任意精度数值无符号整数。
 * 除法运算返回 URtnX 结果，减法运算可能返回 NIntX 结果，提供精确的数值计算。
 * 适用于需要极大数值或精确计算的场景。
 *
 * An arbitrary precision numeric unsigned integer using UIntX as the underlying type.
 * Division operation returns URtnX result, subtraction operation may return NIntX result, providing precise numerical calculations.
 * Suitable for scenarios requiring extremely large numbers or precise calculations.
 *
 * @property value 底层的 UIntX 值
 *                 The underlying UIntX value
 */
@JvmInline
@Serializable(with = NUIntXSerializer::class)
value class NUIntX(val value: UIntX) : NumericUInteger<NUIntX, UIntX>, Copyable<NUIntX> {
    /**
     * NUIntX 常量对象
     * NUIntX Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericUIntegerConstants<NUIntX, UIntX>(NUIntX::invoke, UIntX) {
        operator fun invoke(value: UIntX) = NUIntX(value)
    }

    override val constants: RealNumberConstants<NUIntX> get() = NUIntX

    override fun copy(): NUIntX = NUIntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override operator fun dec(): NUIntX = NUIntX(value - UIntX.one)

    override fun partialOrd(rhs: NUIntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NUIntX) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = URtnX(UIntX.one, value)
    override operator fun unaryMinus() = NIntX(-value.toIntX())
    override fun abs() = NUIntX(value.abs())

    override operator fun plus(rhs: NUIntX) = NUIntX(value + rhs.value)
    override operator fun minus(rhs: NUIntX) = NIntX(value.toIntX() - rhs.toIntX())
    override operator fun times(rhs: NUIntX) = NUIntX(value * rhs.value)
    override operator fun div(rhs: NUIntX) = URtnX(value, rhs.value)
    override operator fun rem(rhs: NUIntX) = NUIntX(value % rhs.value)
    override fun intDiv(rhs: NUIntX) = NUIntX(value / rhs.value)

    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NUIntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0))
    override fun ln() = log(FltX.e)

    override fun pow(index: Int): URtnX {
        return if (index >= 1) {
            URtnX(value.pow(index), UIntX.one)
        } else if (index <= -1) {
            URtnX(UIntX.one, value.pow(index))
        } else {
            URtnX.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NUIntX.pow: ${index.javaClass}")
    }

    override fun sqrt() = pow(FltX(1.0 / 2.0))
    override fun cbrt() = pow(FltX(1.0 / 3.0))

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

    override fun rangeTo(rhs: NUIntX) = NumericUIntegerRange(
        start = copy(),
        endInclusive = rhs,
        _step = one,
        constants = UIntX,
        ctor = UIntX::toNUIntX,
        converter = NUIntX::toUIntX
    )

    override infix fun until(rhs: NUIntX) = if (rhs == zero) {
        rangeTo(zero)
    } else {
        rangeTo((rhs - one).toNUIntX())
    }

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}





