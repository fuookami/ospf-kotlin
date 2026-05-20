/**
 * 有理数模块
 * Rational Number Module
 *
 * 本模块定义了有理数的类型系统，包括有符号有理数（Rtn8、Rtn16、Rtn32、Rtn64、RtnX）
 * 和无符号有理数（URtn8、URtn16、URtn32、URtn64、URtnX）。
 * 有理数以分子和分母的形式表示，在构造时自动进行约分化简。
 * 支持完整的算术运算、比较操作、类型转换以及各种数学函数。
 *
 * This module defines the rational number type system, including signed rational numbers (Rtn8, Rtn16, Rtn32, Rtn64, RtnX)
 * and unsigned rational numbers (URtn8, URtn16, URtn32, URtn64, URtnX).
 * Rational numbers are represented in numerator and denominator form, with automatic simplification during construction.
 * Supports full arithmetic operations, comparison operations, type conversions, and various mathematical functions.
 */
package fuookami.ospf.kotlin.math.algebra.number

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.gcd
import fuookami.ospf.kotlin.math.ordinary.pow
import fuookami.ospf.kotlin.utils.functional.orderOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.ConsistentCopyVisibility

/**
 * 确保分母不为零
 * Ensure denominator is not zero
 *
 * 检查分母是否为零，如果为零则抛出算术异常。
 * Checks if the denominator is zero, throws an arithmetic exception if it is.
 *
 * @param den 分母值
 *            The denominator value
 * @throws ArithmeticException 如果分母为零
 *                            If the denominator is zero
 */
private fun <I> ensureNonZeroDenominator(den: I)
        where I : Integer<I>, I : NumberField<I> {
    if (den eq den.constants.zero) {
        throw ArithmeticException("Rational denominator cannot be zero.")
    }
}

/**
 * 有理数序列化器抽象类
 * Abstract Rational Serializer
 *
 * 用于有理数类型的 Kotlin 序列化框架序列化器基类。
 * 使用 JSON 对象格式，包含分子（num）和分母（den）两个字段。
 *
 * Base class for rational number type serializers in the Kotlin serialization framework.
 * Uses JSON object format with numerator (num) and denominator (den) fields.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param name 序列化器名称
 *             The serializer name
 * @param ctor 有理数构造函数
 *             The rational number constructor
 */
abstract class RationalSerializer<Self, I>(
    name: String,
    val ctor: (I, I) -> Self,
) : KSerializer<Self> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
        element<JsonElement>("num")
        element<JsonElement>("den")
    }

    abstract val valueSerializer: KSerializer<I>

    override fun serialize(encoder: Encoder, value: Self) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            buildJsonObject {
                put("num", encoder.json.encodeToJsonElement(valueSerializer, value.num))
                put("den", encoder.json.encodeToJsonElement(valueSerializer, value.den))
            }
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Self {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        require(descriptor.elementNames.all { it in element })
        return ctor(
            decoder.json.decodeFromJsonElement(valueSerializer, element["num"]!!),
            decoder.json.decodeFromJsonElement(valueSerializer, element["den"]!!),
        )
    }
}

/**
 * 有理数抽象类
 * Abstract Rational Number
 *
 * 有理数类型的抽象基类，以分子和分母的形式表示有理数。
 * 提供了有理数的通用操作，包括算术运算、比较操作、类型转换、
 * 对数、幂运算、三角函数等数学运算的默认实现。
 *
 * Abstract base class for rational number types, representing rational numbers in numerator and denominator form.
 * Provides common operations for rational numbers, including default implementations for arithmetic operations,
 * comparison operations, type conversions, logarithm, power operations, trigonometric functions, and other mathematical operations.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param ctor 有理数构造函数
 *             The rational number constructor
 * @param integerConstants 整数常量对象
 *                          The integer constants object
 */
abstract class Rational<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val integerConstants: RealNumberConstants<I>
) : RationalNumber<Self, I> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    abstract val num: I
    abstract val den: I

    override fun copy() = ctor(num, den)

    override fun toString() = "($num / $den)"
    abstract fun toString(radix: Int): String;

    override fun partialOrd(rhs: Self) = orderOf((num * rhs.den).compareTo(den * rhs.num))
    override fun partialEq(rhs: Self) = num.eq(rhs.num) && den.eq(rhs.den)

    override operator fun unaryMinus() = ctor(-num, den)
    override fun reciprocal() = ctor(den, num)
    override fun abs() = ctor(num.abs(), den)

    override operator fun inc() = ctor(num + den, den)
    override operator fun dec() = ctor(num - den, den)

    override operator fun rem(rhs: Self): Self {
        val k = this intDiv rhs;
        return this - k * rhs;
    }

    override fun intDiv(rhs: Self): Self {
        val divisor = this / rhs;
        return ctor(divisor.num / divisor.den, integerConstants.one);
    }

    override fun log(base: FloatingNumber<*>) = toFltX().log(base)
    override fun lg() = log(FltX.ten)
    override fun lg2() = log(FltX.two)
    override fun ln() = log(FltX.e)

    override fun pow(index: FloatingNumber<*>) = toFltX().pow(index)
    override fun pow(index: Int) = pow(copy(), index, constants)
    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(FltX.two.reciprocal())
    override fun cbrt() = pow(FltX.three.reciprocal())

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

    override fun toInt8() = (num / den).toInt8()
    override fun toInt16() = (num / den).toInt16()
    override fun toInt32() = (num / den).toInt32()
    override fun toInt64() = (num / den).toInt64()
    override fun toIntX() = (num / den).toIntX()

    override fun toUInt8() = (num / den).toUInt8()
    override fun toUInt16() = (num / den).toUInt16()
    override fun toUInt32() = (num / den).toUInt32()
    override fun toUInt64() = (num / den).toUInt64()
    override fun toUIntX() = (num / den).toUIntX()

    override fun toRtn8() = Rtn8(num.toInt8(), den.toInt8())
    override fun toRtn16() = Rtn16(num.toInt16(), den.toInt16())
    override fun toRtn32() = Rtn32(num.toInt32(), den.toInt32())
    override fun toRtn64() = Rtn64(num.toInt64(), den.toInt64())
    override fun toRtnX() = RtnX(num.toIntX(), den.toIntX())

    override fun toURtn8() = URtn8(num.toUInt8(), den.toUInt8())
    override fun toURtn16() = URtn16(num.toUInt16(), den.toUInt16())
    override fun toURtn32() = URtn32(num.toUInt32(), den.toUInt32())
    override fun toURtn64() = URtn64(num.toUInt64(), den.toUInt64())
    override fun toURtnX() = URtnX(num.toUIntX(), den.toUIntX())

    override fun toFlt32() = num.toFlt32() / den.toFlt32()
    override fun toFlt64() = num.toFlt64() / den.toFlt64()
    override fun toFltX() = num.toFltX() / den.toFltX()
}

/**
 * 有理数常量抽象类
 * Abstract Rational Number Constants
 *
 * 有理数常量对象的抽象基类，提供常用的数值常量。
 * Abstract base class for rational number constants objects, providing common numeric constants.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param ctor 有理数构造函数
 *             The rational number constructor
 * @param constants 整数常量对象
 *                  The integer constants object
 */
abstract class RationalConstants<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val constants: RealNumberConstants<I>
) : RationalNumberConstants<Self, I> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val zero: Self get() = ctor(constants.zero, constants.one)
    override val one: Self get() = ctor(constants.one, constants.one)
    override val two: Self get() = ctor(constants.two, constants.one)
    override val three: Self get() = ctor(constants.three, constants.one)
    override val five: Self get() = ctor(constants.five, constants.one)
    override val ten: Self get() = ctor(constants.ten, constants.one)
    override val minimum: Self get() = ctor(constants.minimum, constants.one)
    override val maximum: Self get() = ctor(constants.maximum, constants.one)

    override val half: Self get() = ctor(constants.one, constants.two)

}

/**
 * Rtn8 序列化器
 * Rtn8 Serializer
 *
 * 用于 Rtn8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn8 type in the Kotlin serialization framework.
 */
data object Rtn8Serializer : RationalSerializer<Rtn8, Int8>("Rtn8", Rtn8::invoke) {
    override val valueSerializer = Int8Serializer
}

/**
 * 基于 Int8 的有理数
 * Rational Number based on Int8
 *
 * 使用 8 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简，并确保符号规范化。
 *
 * A rational number type using 8-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction and ensures sign normalization.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn8Serializer::class)
@ConsistentCopyVisibility
data class Rtn8 internal constructor(
    override val num: Int8,
    override val den: Int8
) : Rational<Rtn8, Int8>(Rtn8::invoke, Int8), Copyable<Rtn8> {
    companion object : RationalConstants<Rtn8, Int8>(Rtn8::invoke, Int8) {
        operator fun invoke(num: Int8, den: Int8): Rtn8 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            val negative = (num < Int8.zero) xor (den < Int8.zero);
            return if (negative) {
                Rtn8(-num.abs() / divisor, den.abs() / divisor)
            } else {
                Rtn8(num.abs() / divisor, den.abs() / divisor)
            }
        }
    }

    override val constants: RealNumberConstants<Rtn8> get() = Companion

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: Rtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: Rtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: Rtn8) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: Rtn8) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn16 序列化器
 * Rtn16 Serializer
 *
 * 用于 Rtn16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn16 type in the Kotlin serialization framework.
 */
data object Rtn16Serializer : RationalSerializer<Rtn16, Int16>("Rtn16", Rtn16::invoke) {
    override val valueSerializer = Int16Serializer
}

/**
 * 基于 Int16 的有理数
 * Rational Number based on Int16
 *
 * 使用 16 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。
 *
 * A rational number type using 16-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn16Serializer::class)
@ConsistentCopyVisibility
data class Rtn16 internal constructor(
    override val num: Int16,
    override val den: Int16
) : Rational<Rtn16, Int16>(Rtn16::invoke, Int16), Copyable<Rtn16> {
    companion object : RationalConstants<Rtn16, Int16>(Rtn16::invoke, Int16) {
        operator fun invoke(num: Int16, den: Int16): Rtn16 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return Rtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn16> get() = Rtn16

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: Rtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: Rtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: Rtn16) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: Rtn16) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn32 序列化器
 * Rtn32 Serializer
 *
 * 用于 Rtn32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn32 type in the Kotlin serialization framework.
 */
data object Rtn32Serializer : RationalSerializer<Rtn32, Int32>("Rtn32", Rtn32::invoke) {
    override val valueSerializer = Int32Serializer
}

/**
 * 基于 Int32 的有理数
 * Rational Number based on Int32
 *
 * 使用 32 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。这是常用的有理数类型。
 *
 * A rational number type using 32-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction. This is a commonly used rational number type.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn32Serializer::class)
@ConsistentCopyVisibility
data class Rtn32 internal constructor(
    override val num: Int32,
    override val den: Int32
) : Rational<Rtn32, Int32>(Rtn32::invoke, Int32), Copyable<Rtn32> {
    companion object : RationalConstants<Rtn32, Int32>(Rtn32::invoke, Int32) {
        operator fun invoke(num: Int32, den: Int32): Rtn32 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return Rtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn32> get() = Rtn32

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: Rtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: Rtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: Rtn32) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: Rtn32) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn64 序列化器
 * Rtn64 Serializer
 *
 * 用于 Rtn64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn64 type in the Kotlin serialization framework.
 */
data object Rtn64Serializer : RationalSerializer<Rtn64, Int64>("Rtn64", Rtn64::invoke) {
    override val valueSerializer = Int64Serializer
}

/**
 * 基于 Int64 的有理数
 * Rational Number based on Int64
 *
 * 使用 64 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。适用于需要更大数值范围的情况。
 *
 * A rational number type using 64-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for cases requiring larger numerical range.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn64Serializer::class)
@ConsistentCopyVisibility
data class Rtn64 internal constructor(
    override val num: Int64,
    override val den: Int64
) : Rational<Rtn64, Int64>(Rtn64::invoke, Int64), Copyable<Rtn64> {
    companion object : RationalConstants<Rtn64, Int64>(Rtn64::invoke, Int64), Flt64ValueConverter<Rtn64> {
        operator fun invoke(num: Int64, den: Int64): Rtn64 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return Rtn64(num / divisor, den / divisor)
        }

        override val zero: Rtn64 get() = Rtn64(Int64.zero, Int64.one)
        override val one: Rtn64 get() = Rtn64(Int64.one, Int64.one)

        override fun intoValue(value: Flt64): Rtn64 = value.toRtn64()
    }

    override val constants: RealNumberConstants<Rtn64> get() = Rtn64

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: Rtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: Rtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: Rtn64) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: Rtn64) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * RtnX 序列化器
 * RtnX Serializer
 *
 * 用于 RtnX（任意精度有理数）类型的 Kotlin 序列化框架序列化器。
 * Serializer for the RtnX (arbitrary precision rational number) type in the Kotlin serialization framework.
 */
data object RtnXSerializer : RationalSerializer<RtnX, IntX>("RtnX", RtnX::invoke) {
    override val valueSerializer = IntXSerializer
}

/**
 * 任意精度有理数
 * Arbitrary Precision Rational Number
 *
 * 使用任意精度有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。适用于需要精确计算的场景。
 *
 * A rational number type using arbitrary precision signed integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for scenarios requiring precise calculations.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = RtnXSerializer::class)
@ConsistentCopyVisibility
data class RtnX internal constructor(
    override val num: IntX,
    override val den: IntX
) : Rational<RtnX, IntX>(RtnX::invoke, IntX), Copyable<RtnX> {
    companion object : RationalConstants<RtnX, IntX>(RtnX::invoke, IntX), Flt64ValueConverter<RtnX> {
        operator fun invoke(num: Int, den: Int): RtnX {
            return RtnX(IntX(num.toLong()), IntX(den.toLong()))
        }

        operator fun invoke(num: IntX, den: IntX): RtnX {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return RtnX(num / divisor, den / divisor)
        }

        override val zero: RtnX get() = RtnX(IntX.zero, IntX.one)
        override val one: RtnX get() = RtnX(IntX.one, IntX.one)

        override fun intoValue(value: Flt64): RtnX = value.toRtnX()
    }

    override val constants: RealNumberConstants<RtnX> get() = RtnX
    override val isBounded: Boolean get() = false
    override val minBound: RtnX? get() = null
    override val maxBound: RtnX? get() = null

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: RtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: RtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: RtnX) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: RtnX) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn8 序列化器
 * URtn8 Serializer
 *
 * 用于 URtn8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn8 type in the Kotlin serialization framework.
 */
object URtn8Serializer : RationalSerializer<URtn8, UInt8>("URtn8", URtn8::invoke) {
    override val valueSerializer = UInt8Serializer
}

/**
 * 基于 UInt8 的无符号有理数
 * Unsigned Rational Number based on UInt8
 *
 * 使用 8 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。
 *
 * An unsigned rational number type using 8-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn8Serializer::class)
@ConsistentCopyVisibility
data class URtn8 internal constructor(
    override val num: UInt8,
    override val den: UInt8
) : Rational<URtn8, UInt8>(URtn8::invoke, UInt8), Copyable<URtn8> {
    companion object : RationalConstants<URtn8, UInt8>(URtn8::invoke, UInt8) {
        operator fun invoke(num: UInt8, den: UInt8): URtn8 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return URtn8(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn8> get() = URtn8

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: URtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: URtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: URtn8) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: URtn8) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn16 序列化器
 * URtn16 Serializer
 *
 * 用于 URtn16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn16 type in the Kotlin serialization framework.
 */
object URtn16Serializer : RationalSerializer<URtn16, UInt16>("URtn16", URtn16::invoke) {
    override val valueSerializer = UInt16Serializer
}

/**
 * 基于 UInt16 的无符号有理数
 * Unsigned Rational Number based on UInt16
 *
 * 使用 16 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。
 *
 * An unsigned rational number type using 16-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn16Serializer::class)
@ConsistentCopyVisibility
data class URtn16 internal constructor(
    override val num: UInt16,
    override val den: UInt16
) : Rational<URtn16, UInt16>(URtn16::invoke, UInt16), Copyable<URtn16> {
    companion object : RationalConstants<URtn16, UInt16>(URtn16::invoke, UInt16) {
        operator fun invoke(num: UInt16, den: UInt16): URtn16 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return URtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn16> get() = URtn16

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: URtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: URtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: URtn16) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: URtn16) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn32 序列化器
 * URtn32 Serializer
 *
 * 用于 URtn32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn32 type in the Kotlin serialization framework.
 */
object URtn32Serializer : RationalSerializer<URtn32, UInt32>("URtn32", URtn32::invoke) {
    override val valueSerializer = UInt32Serializer
}

/**
 * 基于 UInt32 的无符号有理数
 * Unsigned Rational Number based on UInt32
 *
 * 使用 32 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。这是常用的无符号有理数类型。
 *
 * An unsigned rational number type using 32-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. This is a commonly used unsigned rational number type.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn32Serializer::class)
@ConsistentCopyVisibility
data class URtn32 internal constructor(
    override val num: UInt32,
    override val den: UInt32
) : Rational<URtn32, UInt32>(URtn32::invoke, UInt32), Copyable<URtn32> {
    companion object : RationalConstants<URtn32, UInt32>(URtn32::invoke, UInt32) {
        operator fun invoke(num: UInt32, den: UInt32): URtn32 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return URtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn32> get() = URtn32

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: URtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: URtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: URtn32) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: URtn32) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn64 序列化器
 * URtn64 Serializer
 *
 * 用于 URtn64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn64 type in the Kotlin serialization framework.
 */
object URtn64Serializer : RationalSerializer<URtn64, UInt64>("URtn64", URtn64::invoke) {
    override val valueSerializer = UInt64Serializer
}

/**
 * 基于 UInt64 的无符号有理数
 * Unsigned Rational Number based on UInt64
 *
 * 使用 64 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。适用于需要更大数值范围的情况。
 *
 * An unsigned rational number type using 64-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for cases requiring larger numerical range.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn64Serializer::class)
@ConsistentCopyVisibility
data class URtn64 internal constructor(
    override val num: UInt64,
    override val den: UInt64
) : Rational<URtn64, UInt64>(URtn64::invoke, UInt64), Copyable<URtn64> {
    companion object : RationalConstants<URtn64, UInt64>(URtn64::invoke, UInt64) {
        operator fun invoke(num: UInt64, den: UInt64): URtn64 {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return URtn64(num / divisor, den / divisor)
        }

        operator fun invoke(num: Int, den: Int): URtn64 {
            return this(UInt64(num), UInt64(den))
        }
    }

    override val constants: RealNumberConstants<URtn64> get() = URtn64

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: URtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: URtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: URtn64) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: URtn64) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtnX 序列化器
 * URtnX Serializer
 *
 * 用于 URtnX（任意精度无符号有理数）类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtnX (arbitrary precision unsigned rational number) type in the Kotlin serialization framework.
 */
object URtnXSerializer : RationalSerializer<URtnX, UIntX>("URtnX", URtnX::invoke) {
    override val valueSerializer = UIntXSerializer
}

/**
 * 任意精度无符号有理数
 * Arbitrary Precision Unsigned Rational Number
 *
 * 使用任意精度无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。适用于需要精确计算的场景。
 *
 * An unsigned rational number type using arbitrary precision unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for scenarios requiring precise calculations.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtnXSerializer::class)
@ConsistentCopyVisibility
data class URtnX internal constructor(
    override val num: UIntX,
    override val den: UIntX
) : Rational<URtnX, UIntX>(URtnX::invoke, UIntX), Copyable<URtnX> {
    companion object : RationalConstants<URtnX, UIntX>(URtnX::invoke, UIntX) {
        operator fun invoke(num: UIntX, den: UIntX): URtnX {
            ensureNonZeroDenominator(den)
            val divisor = gcd(num.abs(), den.abs())
            return URtnX(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtnX> get() = URtnX
    override val isBounded: Boolean get() = false
    override val minBound: URtnX? get() = null
    override val maxBound: URtnX? get() = null

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override operator fun plus(rhs: URtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override operator fun minus(rhs: URtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override operator fun times(rhs: URtnX) = invoke(num * rhs.num, den * rhs.den)
    override operator fun div(rhs: URtnX) = invoke(num * rhs.den, rhs.num * den)
}




