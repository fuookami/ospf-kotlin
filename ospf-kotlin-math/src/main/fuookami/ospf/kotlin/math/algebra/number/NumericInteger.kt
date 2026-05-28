/**
 * 数值有符号整数模块
 * Numeric Signed Integer Module
 *
 * 本模块定义了带有数值语义的有符号整数类型，包括 NInt8、NInt16、NInt32、NInt64 和 NIntX。
 * 与普通有符号整数不同，这些类型的除法运算返回有理数结果而非整数结果，
 * 从而提供更精确的数值计算。适用于需要精确数值计算的场景。
 *
 * This module defines signed integer types with numeric semantics, including NInt8, NInt16, NInt32, NInt64, and NIntX.
 * Unlike regular signed integers, division operations of these types return rational number results instead of integer results,
 * thus providing more precise numerical calculations. Suitable for scenarios requiring precise numerical calculations.
 */
package fuookami.ospf.kotlin.math.algebra.number

import java.math.BigInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.orderOf
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.*

/**
 * 数值有符号整数接口
 * Numeric Signed Integer Interface
 *
 * 提供数值有符号整数类型的通用实现，包括自增自减、对数、幂运算、
 * 三角函数等数学运算的默认实现。
 * 注意：除法运算返回有理数结果。
 *
 * Provides common implementation for numeric signed integer types, including default implementations
 * for increment/decrement, logarithm, power operations, trigonometric functions, and other mathematical operations.
 * Note: Division operation returns rational number result.
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 * @param I 底层整数类型
 *           The underlying integer type
 */
interface NumericInteger<Self, I>
    : NumericIntegerNumber<Self, I> where Self : NumericInteger<Self, I>, I : IntegerNumber<I>, I : NumberField<I> {
    override operator fun inc() = this + constants.one
    override operator fun dec() = this - constants.one

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
        else -> throw IllegalArgumentException("Unknown argument type to NumericInteger.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NumericInteger.pow: ${index.javaClass}")
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

    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    override infix fun until(rhs: Self) = this.rangeTo(rhs - constants.one)
}

/**
 * 数值有符号整数常量抽象类
 * Abstract Numeric Signed Integer Constants
 *
 * 提供数值有符号整数类型的常用数值常量。
 * Provides common numeric constants for numeric signed integer types.
 *
 * @param Self 数值有符号整数类型
 *             The numeric signed integer type
 * @param I 底层整数类型
 *           The underlying integer type
 * @param ctor 数值有符号整数构造函数
 *             The numeric signed integer constructor
 * @param constants 底层整数常量对象
 *                  The underlying integer constants object
 */
abstract class NumericIntegerConstants<Self, I>(
    private val ctor: (I) -> Self,
    private val constants: RealNumberConstants<I>
) : RealNumberConstants<Self> where Self : NumericInteger<Self, I>, I : IntegerNumber<I>, I : NumberField<I> {
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
 * NInt8 序列化器
 * NInt8 Serializer
 *
 * 用于 NInt8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NInt8 type in the Kotlin serialization framework.
 */
data object NInt8Serializer : KSerializer<NInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt8) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NInt8 {
        return NInt8(Int8(decoder.decodeInt().toByte()))
    }
}

/**
 * 基于 Int8 的数值有符号整数
 * Numeric Signed Integer based on Int8
 *
 * 使用 Int8 作为底层类型的数值有符号整数。
 * 除法运算返回 Rtn8 结果，提供精确的数值计算。
 *
 * A numeric signed integer using Int8 as the underlying type.
 * Division operation returns Rtn8 result, providing precise numerical calculations.
 *
 * @property value 底层的 Int8 值
 *                 The underlying Int8 value
 */
@JvmInline
@Serializable(with = NInt8Serializer::class)
value class NInt8(val value: Int8) : NumericInteger<NInt8, Int8>, Copyable<NInt8> {
    /**
     * NInt8 常量对象
     * NInt8 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericIntegerConstants<NInt8, Int8>(NInt8::invoke, Int8) {
        operator fun invoke(value: Int8) = NInt8(value)
    }

    override val constants: RealNumberConstants<NInt8> get() = NInt8

    override fun copy() = NInt8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt8) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn8(Int8.one, value)
    override operator fun unaryMinus() = NInt8(-value)
    override fun abs() = NInt8(value.abs())

    override operator fun plus(rhs: NInt8) = NInt8(value + rhs.value)
    override operator fun minus(rhs: NInt8) = NInt8(value - rhs.value)
    override operator fun times(rhs: NInt8) = NInt8(value * rhs.value)
    override operator fun div(rhs: NInt8) = Rtn8(value, rhs.value)
    override operator fun rem(rhs: NInt8) = NInt8(value % rhs.value)
    override fun intDiv(rhs: NInt8) = NInt8(value / rhs.value)

    override fun pow(index: Int): Rtn8 {
        return if (index >= 1) {
            Rtn8(value.pow(index), Int8.one)
        } else if (index <= -1) {
            Rtn8(Int8.one, value.pow(index))
        } else {
            Rtn8.one
        }
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
 * NInt16 序列化器
 * NInt16 Serializer
 *
 * 用于 NInt16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NInt16 type in the Kotlin serialization framework.
 */
data object NInt16Serializer : KSerializer<NInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt16) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NInt16 {
        return NInt16(Int16(decoder.decodeInt().toShort()))
    }
}

/**
 * 基于 Int16 的数值有符号整数
 * Numeric Signed Integer based on Int16
 *
 * 使用 Int16 作为底层类型的数值有符号整数。
 * 除法运算返回 Rtn16 结果，提供精确的数值计算。
 *
 * A numeric signed integer using Int16 as the underlying type.
 * Division operation returns Rtn16 result, providing precise numerical calculations.
 *
 * @property value 底层的 Int16 值
 *                 The underlying Int16 value
 */
@JvmInline
@Serializable(with = NInt16Serializer::class)
value class NInt16(val value: Int16) : NumericInteger<NInt16, Int16>, Copyable<NInt16> {
    /**
     * NInt16 常量对象
     * NInt16 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericIntegerConstants<NInt16, Int16>(NInt16::invoke, Int16) {
        operator fun invoke(value: Int16) = NInt16(value)
    }

    override val constants: RealNumberConstants<NInt16> get() = NInt16

    override fun copy() = NInt16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt16) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn16(Int16.one, value)
    override operator fun unaryMinus() = NInt16(-value)
    override fun abs() = NInt16(value.abs())

    override operator fun plus(rhs: NInt16) = NInt16(value + rhs.value)
    override operator fun minus(rhs: NInt16) = NInt16(value - rhs.value)
    override operator fun times(rhs: NInt16) = NInt16(value * rhs.value)
    override operator fun div(rhs: NInt16) = Rtn16(value, rhs.value)
    override operator fun rem(rhs: NInt16) = NInt16(value % rhs.value)
    override fun intDiv(rhs: NInt16) = NInt16(value / rhs.value)

    override fun pow(index: Int): Rtn16 {
        return if (index >= 1) {
            Rtn16(value.pow(index), Int16.one)
        } else if (index <= -1) {
            Rtn16(Int16.one, value.pow(index))
        } else {
            Rtn16.one
        }
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
 * NInt32 序列化器
 * NInt32 Serializer
 *
 * 用于 NInt32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NInt32 type in the Kotlin serialization framework.
 */
data object NInt32Serializer : KSerializer<NInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt32) {
        encoder.encodeInt(value.value.value)
    }

    override fun deserialize(decoder: Decoder): NInt32 {
        return NInt32(Int32(decoder.decodeInt()))
    }
}

/**
 * 基于 Int32 的数值有符号整数
 * Numeric Signed Integer based on Int32
 *
 * 使用 Int32 作为底层类型的数值有符号整数。
 * 除法运算返回 Rtn32 结果，提供精确的数值计算。
 * 这是常用的数值有符号整数类型。
 *
 * A numeric signed integer using Int32 as the underlying type.
 * Division operation returns Rtn32 result, providing precise numerical calculations.
 * This is a commonly used numeric signed integer type.
 *
 * @property value 底层的 Int32 值
 *                 The underlying Int32 value
 */
@JvmInline
@Serializable(with = NInt32Serializer::class)
value class NInt32(val value: Int32) : NumericInteger<NInt32, Int32>, Copyable<NInt32> {
    /**
     * NInt32 常量对象
     * NInt32 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericIntegerConstants<NInt32, Int32>(NInt32::invoke, Int32) {
        operator fun invoke(value: Int32) = NInt32(value)
    }

    override val constants: RealNumberConstants<NInt32> get() = NInt32

    override fun copy(): NInt32 = NInt32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt32) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn32(Int32.one, value)
    override operator fun unaryMinus() = NInt32(-value)
    override fun abs() = NInt32(value.abs())

    override operator fun plus(rhs: NInt32) = NInt32(value + rhs.value)
    override operator fun minus(rhs: NInt32) = NInt32(value - rhs.value)
    override operator fun times(rhs: NInt32) = NInt32(value * rhs.value)
    override operator fun div(rhs: NInt32) = Rtn32(value, rhs.value)
    override operator fun rem(rhs: NInt32) = NInt32(value % rhs.value)
    override fun intDiv(rhs: NInt32) = NInt32(value / rhs.value)

    override fun pow(index: Int): Rtn32 {
        return if (index >= 1) {
            Rtn32(value.pow(index), Int32.one)
        } else if (index <= -1) {
            Rtn32(Int32.one, value.pow(index))
        } else {
            Rtn32.one
        }
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
 * NInt64 序列化器
 * NInt64 Serializer
 *
 * 用于 NInt64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the NInt64 type in the Kotlin serialization framework.
 */
data object NInt64Serializer : KSerializer<NInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: NInt64) {
        encoder.encodeLong(value.value.value)
    }

    override fun deserialize(decoder: Decoder): NInt64 {
        return NInt64(Int64(decoder.decodeLong()))
    }
}

/**
 * 基于 Int64 的数值有符号整数
 * Numeric Signed Integer based on Int64
 *
 * 使用 Int64 作为底层类型的数值有符号整数。
 * 除法运算返回 Rtn64 结果，提供精确的数值计算。
 * 适用于需要更大数值范围的情况。
 *
 * A numeric signed integer using Int64 as the underlying type.
 * Division operation returns Rtn64 result, providing precise numerical calculations.
 * Suitable for cases requiring larger numerical range.
 *
 * @property value 底层的 Int64 值
 *                 The underlying Int64 value
 */
@JvmInline
@Serializable(with = NInt64Serializer::class)
value class NInt64(val value: Int64) : NumericInteger<NInt64, Int64>, Copyable<NInt64> {
    /**
     * NInt64 常量对象
     * NInt64 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericIntegerConstants<NInt64, Int64>(NInt64::invoke, Int64) {
        operator fun invoke(value: Int64) = NInt64(value)
    }

    override val constants: RealNumberConstants<NInt64> get() = NInt64

    override fun copy(): NInt64 = NInt64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt64) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn64(Int64.one, value)
    override operator fun unaryMinus() = NInt64(-value)
    override fun abs() = NInt64(value.abs())

    override operator fun plus(rhs: NInt64) = NInt64(value + rhs.value)
    override operator fun minus(rhs: NInt64) = NInt64(value - rhs.value)
    override operator fun times(rhs: NInt64) = NInt64(value * rhs.value)
    override operator fun div(rhs: NInt64) = Rtn64(value, rhs.value)
    override operator fun rem(rhs: NInt64) = NInt64(value % rhs.value)
    override fun intDiv(rhs: NInt64) = NInt64(value / rhs.value)

    override fun pow(index: Int): Rtn64 {
        return if (index >= 1) {
            Rtn64(value.pow(index), Int64.one)
        } else if (index <= -1) {
            Rtn64(Int64.one, value.pow(index))
        } else {
            Rtn64.one
        }
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
 * NIntX 序列化器
 * NIntX Serializer
 *
 * 用于 NIntX（任意精度数值有符号整数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化。
 *
 * Serializer for the NIntX (arbitrary precision numeric signed integer) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization.
 */
data object NIntXSerializer : KSerializer<NIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NIntX) {
        encoder.encodeString(value.value.toString(10))
    }

    override fun deserialize(decoder: Decoder): NIntX {
        return NIntX(IntX(BigInteger(decoder.decodeString())))
    }
}

/**
 * 任意精度数值有符号整数
 * Arbitrary Precision Numeric Signed Integer
 *
 * 使用 IntX 作为底层类型的任意精度数值有符号整数。
 * 除法运算返回 RtnX 结果，提供精确的数值计算。
 * 适用于需要极大数值或精确计算的场景。
 *
 * An arbitrary precision numeric signed integer using IntX as the underlying type.
 * Division operation returns RtnX result, providing precise numerical calculations.
 * Suitable for scenarios requiring extremely large numbers or precise calculations.
 *
 * @property value 底层的 IntX 值
 *                 The underlying IntX value
 */
@JvmInline
@Serializable(NIntXSerializer::class)
value class NIntX(val value: IntX) : NumericInteger<NIntX, IntX>, Copyable<NIntX> {
    /**
     * NIntX 常量对象
     * NIntX Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : NumericIntegerConstants<NIntX, IntX>(NIntX::invoke, IntX) {
        operator fun invoke(value: IntX) = NIntX(value)
    }

    override val constants: RealNumberConstants<NIntX> get() = NIntX

    override fun copy(): NIntX = NIntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NIntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NIntX) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = RtnX(IntX.one, value)
    override operator fun unaryMinus() = NIntX(-value)
    override fun abs() = NIntX(value.abs())

    override operator fun plus(rhs: NIntX) = NIntX(value + rhs.value)
    override operator fun minus(rhs: NIntX) = NIntX(value - rhs.value)
    override operator fun times(rhs: NIntX) = NIntX(value * rhs.value)
    override operator fun div(rhs: NIntX) = RtnX(value, rhs.value)
    override operator fun rem(rhs: NIntX) = NIntX(value % rhs.value)
    override fun intDiv(rhs: NIntX) = NIntX(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NIntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0))
    override fun ln() = log(FltX.e)

    override fun pow(index: Int): RtnX {
        return if (index >= 1) {
            RtnX(value.pow(index), IntX.one)
        } else if (index <= -1) {
            RtnX(IntX.one, value.pow(index))
        } else {
            RtnX.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NIntX.log: ${index.javaClass}")
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
